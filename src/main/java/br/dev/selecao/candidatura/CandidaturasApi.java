package br.dev.selecao.candidatura;

import static br.dev.selecao.usuario.Papel.*;

import br.dev.selecao.comum.*;
import br.dev.selecao.curriculo.ArmazenamentoCurriculo;
import br.dev.selecao.usuario.*;
import br.dev.selecao.vaga.*;
import jakarta.validation.Valid;
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

@RestController
@Transactional
public class CandidaturasApi {
  public record Mudanca(@NotNull Etapa novaEtapa, @Size(max = 4000) String observacao) {}

  private final Banco banco;
  private final Ator ator;
  private final ArmazenamentoCurriculo arquivos;
  private final CandidaturaRepo candidaturas;
  private final CandidaturaMapper mapper;

  public CandidaturasApi(
      Banco banco,
      Ator ator,
      ArmazenamentoCurriculo arquivos,
      CandidaturaRepo candidaturas,
      CandidaturaMapper mapper) {
    this.banco = banco;
    this.ator = ator;
    this.arquivos = arquivos;
    this.candidaturas = candidaturas;
    this.mapper = mapper;
  }

  @PostMapping(value = "/vagas/{id}/candidaturas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public CandidaturaMapper.Resumo inscrever(
      @PathVariable UUID id, @RequestPart MultipartFile curriculo) {
    var u = ator.exigir(CANDIDATO);
    var vaga = banco.bloquear(Vaga.class, id);
    if (vaga.getStatus() != Status.PUBLICADA)
      throw Falha.invalida("Vaga indisponível para inscrição");
    if (candidaturas.existsByVagaIdAndCandidatoId(id, u.id()))
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
    c.candidatoId = u.id();
    c.curriculoCaminho = chave;
    c.etapaAtual = Etapa.TRIAGEM;
    c.criadaEm = c.atualizadaEm = Instant.now();
    banco.inserir(c);
    registrar(c, null, u, null);
    return mapper.resumo(c);
  }

  @GetMapping("/candidaturas/minhas")
  public Page<CandidaturaMapper.Resumo> minhas(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "criada_em,desc") String sort) {
    return candidaturas
        .findByCandidatoId(ator.exigir(CANDIDATO).id(), pagina(page, size, sort))
        .map(mapper::resumo);
  }

  @GetMapping("/empresas/{empresaId}/vagas/{vagaId}/candidaturas")
  public Page<CandidaturaMapper.Resumo> daVaga(
      @PathVariable UUID empresaId,
      @PathVariable UUID vagaId,
      @RequestParam(required = false) Etapa etapa,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "criada_em,desc") String sort) {
    var u = ator.exigir(RECRUTADOR, ADMIN);
    if (u.papel() == RECRUTADOR) ator.empresa(empresaId);
    var v = banco.obter(Vaga.class, vagaId);
    if (u.papel() == RECRUTADOR) ator.empresa(v.getEmpresaId());
    if (!v.getEmpresaId().equals(empresaId)) throw Falha.ausente();
    var p = pagina(page, size, sort);
    return (etapa == null
            ? candidaturas.findByVagaId(vagaId, p)
            : candidaturas.findByVagaIdAndEtapaAtual(vagaId, etapa, p))
        .map(mapper::resumo);
  }

  @PatchMapping("/candidaturas/{id}/etapa")
  public CandidaturaMapper.Resumo avancar(@PathVariable UUID id, @RequestBody @Valid Mudanca d) {
    var u = ator.exigir(RECRUTADOR);
    var c = banco.bloquear(Candidatura.class, id);
    acesso(c, u);
    return mudar(c, d.novaEtapa, u, d.observacao);
  }

  @PatchMapping("/candidaturas/{id}/desistencia")
  public CandidaturaMapper.Resumo desistir(@PathVariable UUID id) {
    var u = ator.exigir(CANDIDATO);
    var c = banco.bloquear(Candidatura.class, id);
    acesso(c, u);
    return mudar(c, Etapa.DESISTIU, u, null);
  }

  @GetMapping("/candidaturas/{id}/historico")
  public List<Historico> historico(@PathVariable UUID id) {
    var u = ator.exigir(ADMIN, RECRUTADOR, CANDIDATO);
    acesso(banco.obter(Candidatura.class, id), u);
    return banco
        .em
        .createQuery(
            "from Historico where candidaturaId=:id order by alteradoEm,id", Historico.class)
        .setParameter("id", id)
        .getResultList();
  }

  @GetMapping(value = "/candidaturas/{id}/curriculo", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<Resource> curriculo(@PathVariable UUID id) {
    var u = ator.exigir(ADMIN, RECRUTADOR, CANDIDATO);
    var c = banco.obter(Candidatura.class, id);
    acesso(c, u);
    if (u.papel() == CANDIDATO) throw Falha.negado();
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header("Content-Disposition", "attachment; filename=curriculo.pdf")
        .body(arquivos.carregar(c.curriculoCaminho));
  }

  private Pageable pagina(int page, int size, String sort) {
    return new Pagina(page, size, sort).validar(Set.of("criada_em"));
  }

  private void acesso(Candidatura c, Usuario u) {
    if (u.papel() == CANDIDATO && !c.candidatoId.equals(u.id())) throw Falha.ausente();
    if (u.papel() == RECRUTADOR) ator.empresa(banco.obter(Vaga.class, c.vagaId).getEmpresaId());
  }

  private CandidaturaMapper.Resumo mudar(Candidatura c, Etapa nova, Usuario u, String obs) {
    Etapa anterior = c.etapaAtual;
    Funil.validar(anterior, nova, u.papel());
    c.etapaAtual = nova;
    c.atualizadaEm = Instant.now();
    registrar(c, anterior, u, obs);
    return mapper.resumo(c);
  }

  private void registrar(Candidatura c, Etapa anterior, Usuario u, String obs) {
    var h = new Historico();
    h.candidaturaId = c.id;
    h.etapaAnterior = anterior;
    h.etapaNova = c.etapaAtual;
    h.alteradoPor = u.id();
    h.observacao = obs;
    h.alteradoEm = c.atualizadaEm;
    banco.inserir(h);
  }
}
