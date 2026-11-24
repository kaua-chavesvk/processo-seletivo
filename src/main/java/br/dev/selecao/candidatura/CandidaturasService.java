package br.dev.selecao.candidatura;

import static br.dev.selecao.usuario.Papel.*;

import br.dev.selecao.comum.*;
import br.dev.selecao.curriculo.ArmazenamentoCurriculo;
import br.dev.selecao.usuario.*;
import br.dev.selecao.vaga.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@org.springframework.stereotype.Service
@Transactional
public class CandidaturasService {
  private final Banco banco;
  private final Ator ator;
  private final ArmazenamentoCurriculo arquivos;
  private final CandidaturaRepo candidaturas;
  private final CandidaturaMapper mapper;
  private final HistoricoRepo historicos;

  public CandidaturasService(
      Banco banco,
      Ator ator,
      ArmazenamentoCurriculo arquivos,
      CandidaturaRepo candidaturas,
      CandidaturaMapper mapper,
      HistoricoRepo historicos) {
    this.historicos = historicos;
    this.banco = banco;
    this.ator = ator;
    this.arquivos = arquivos;
    this.candidaturas = candidaturas;
    this.mapper = mapper;
  }

  public CandidaturaMapper.Resumo inscrever(UUID id, MultipartFile curriculo) {
    var u = ator.exigir(CANDIDATO);
    var vaga = banco.bloquear(Vaga.class, id);
    if (vaga.getStatus() != Status.PUBLICADA)
      throw Falha.invalida("Vaga indisponível para inscrição");
    if (candidaturas.existsByVagaIdAndCandidatoId(id, u.getId()))
      throw Falha.conflito("Candidatura já existente");
    String chave = arquivos.salvar(curriculo);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) arquivos.remover(chave);
          }
        });
    var c = new Candidatura();
    c.vagaId = id;
    c.candidatoId = u.getId();
    c.curriculoCaminho = chave;
    c.etapaAtual = Etapa.TRIAGEM;
    c.criadaEm = c.atualizadaEm = Instant.now();
    banco.inserir(c);
    registrar(c, null, u, null);
    return mapper.resumo(c);
  }

  public Page<CandidaturaMapper.Resumo> minhas(int page, int size, String sort) {
    return candidaturas
        .findByCandidatoId(ator.exigir(CANDIDATO).getId(), pagina(page, size, sort))
        .map(mapper::resumo);
  }

  public Page<CandidaturaMapper.Resumo> daVaga(
      UUID empresaId, UUID vagaId, Etapa etapa, int page, int size, String sort) {
    var u = ator.exigir(RECRUTADOR, ADMIN);
    if (u.getPapel() == RECRUTADOR) ator.empresa(empresaId);
    var v = banco.obter(Vaga.class, vagaId);
    if (!v.getEmpresaId().equals(empresaId))
      throw u.getPapel() == RECRUTADOR ? Falha.negado() : Falha.ausente();
    var p = pagina(page, size, sort);
    return (etapa == null
            ? candidaturas.findByVagaId(vagaId, p)
            : candidaturas.findByVagaIdAndEtapaAtual(vagaId, etapa, p))
        .map(mapper::resumo);
  }

  public CandidaturaMapper.Resumo avancar(UUID id, Etapa novaEtapa, String observacao) {
    var u = ator.exigir(RECRUTADOR);
    var c = banco.bloquear(Candidatura.class, id);
    acesso(c, u);
    return mudar(c, novaEtapa, u, observacao);
  }

  public CandidaturaMapper.Resumo desistir(UUID id) {
    var u = ator.exigir(CANDIDATO);
    var c = banco.bloquear(Candidatura.class, id);
    acesso(c, u);
    return mudar(c, Etapa.DESISTIU, u, null);
  }

  public Page<CandidaturaMapper.HistoricoDto> historico(UUID id, Pageable pagina) {
    var u = ator.exigir(ADMIN, RECRUTADOR, CANDIDATO);
    acesso(banco.obter(Candidatura.class, id), u);
    return historicos
        .findByCandidaturaIdOrderByAlteradoEmAscIdAsc(id, pagina)
        .map(mapper::historico);
  }

  public Resource curriculo(UUID id) {
    var u = ator.exigir(ADMIN, RECRUTADOR, CANDIDATO);
    var c = banco.obter(Candidatura.class, id);
    acesso(c, u);
    if (u.getPapel() == CANDIDATO) throw Falha.negado();
    return arquivos.carregar(c.getCurriculoCaminho());
  }

  private Pageable pagina(int page, int size, String sort) {
    return new Pagina(page, size, sort).validar(Set.of("criada_em"));
  }

  private void acesso(Candidatura c, Usuario u) {
    if (u.getPapel() == CANDIDATO && !c.getCandidatoId().equals(u.getId())) throw Falha.ausente();
    if (u.getPapel() == RECRUTADOR)
      ator.empresa(banco.obter(Vaga.class, c.getVagaId()).getEmpresaId());
  }

  private CandidaturaMapper.Resumo mudar(Candidatura c, Etapa nova, Usuario u, String obs) {
    Etapa anterior = c.getEtapaAtual();
    Funil.validar(anterior, nova, u.getPapel());
    c.etapaAtual = nova;
    c.atualizadaEm = Instant.now();
    registrar(c, anterior, u, obs);
    return mapper.resumo(c);
  }

  private void registrar(Candidatura c, Etapa anterior, Usuario u, String obs) {
    var h = new Historico();
    h.candidaturaId = c.getId();
    h.etapaAnterior = anterior;
    h.etapaNova = c.getEtapaAtual();
    h.alteradoPor = u.getId();
    h.observacao = obs;
    h.alteradoEm = c.getAtualizadaEm();
    banco.inserir(h);
  }
}
