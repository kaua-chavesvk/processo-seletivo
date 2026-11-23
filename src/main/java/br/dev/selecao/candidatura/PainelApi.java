package br.dev.selecao.candidatura;

import static br.dev.selecao.usuario.Papel.*;

import br.dev.selecao.comum.*;
import br.dev.selecao.usuario.*;
import br.dev.selecao.vaga.*;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/painel")
@Transactional(readOnly = true)
public class PainelApi {
  private final Ator ator;
  private final Banco banco;
  private final UsuarioRepo usuarios;
  private final CandidaturaRepo candidaturas;
  private final VagaRepo vagas;
  private final VagaMapper mapper;

  public PainelApi(
      Ator ator,
      Banco banco,
      UsuarioRepo usuarios,
      CandidaturaRepo candidaturas,
      VagaRepo vagas,
      VagaMapper mapper) {
    this.ator = ator;
    this.banco = banco;
    this.usuarios = usuarios;
    this.candidaturas = candidaturas;
    this.vagas = vagas;
    this.mapper = mapper;
  }

  public record Perfil(UUID id, String nome, Papel papel, UUID empresaId) {}

  @GetMapping("/perfil")
  public Perfil perfil() {
    var usuario = ator.exigir(ADMIN, CANDIDATO, RECRUTADOR);
    var empresa =
        usuario.getPapel() == RECRUTADOR
            ? usuarios.empresaDoRecrutador(usuario.getId()).orElseThrow(Falha::negado)
            : null;
    return new Perfil(usuario.getId(), usuario.getNome(), usuario.getPapel(), empresa);
  }

  @GetMapping("/vagas")
  public Page<VagaMapper.Resumo> vagas(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    var id = ator.exigir(RECRUTADOR).getId();
    var empresa = usuarios.empresaDoRecrutador(id).orElseThrow(Falha::negado);
    return vagas.findByEmpresaId(empresa, pagina(page, size)).map(mapper::resumo);
  }

  @GetMapping("/vagas/{id}/candidaturas")
  public Page<CandidaturaRepo.Cartao> daVaga(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    ator.exigir(RECRUTADOR);
    ator.empresa(banco.obter(Vaga.class, id).getEmpresaId());
    return candidaturas.cartoesDaVaga(id, pagina(page, size));
  }

  @GetMapping("/minhas")
  public Page<CandidaturaRepo.Cartao> minhas(
      @RequestParam(required = false) UUID vagaId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    var candidato = ator.exigir(CANDIDATO).getId();
    var pagina = pagina(page, size);
    return vagaId == null
        ? candidaturas.cartoesDoCandidato(candidato, pagina)
        : candidaturas.cartoesDoCandidatoNaVaga(candidato, vagaId, pagina);
  }

  private Pageable pagina(int page, int size) {
    return new Pagina(page, size, "criada_em,desc").validar(Set.of("criada_em"));
  }
}
