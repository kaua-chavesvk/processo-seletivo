package br.dev.selecao.vaga;

import br.dev.selecao.comum.*;
import br.dev.selecao.usuario.Papel;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@Transactional
public class VagasApi {
  public record Dados(
      @NotBlank @Size(max = 255) String titulo,
      @NotBlank @Size(max = 16000) String descricao,
      @NotNull Modalidade modalidade,
      @NotNull Nivel nivel,
      @NotBlank @Size(max = 255) String localizacao,
      @NotNull @Size(max = 64) Set<@NotBlank @Size(max = 255) String> tecnologias) {}

  public record Mudanca(@NotNull Status status) {}

  private final Banco banco;
  private final Ator ator;
  private final VagaRepo vagas;
  private final VagaMapper mapper;

  public VagasApi(Banco banco, Ator ator, VagaRepo vagas, VagaMapper mapper) {
    this.banco = banco;
    this.ator = ator;
    this.vagas = vagas;
    this.mapper = mapper;
  }

  @PostMapping("/empresas/{empresaId}/vagas")
  @ResponseStatus(HttpStatus.CREATED)
  public VagaMapper.Resumo criar(@PathVariable UUID empresaId, @RequestBody @Valid Dados d) {
    ator.empresa(empresaId);
    Vaga v = new Vaga();
    v.empresaId = empresaId;
    v.status = Status.RASCUNHO;
    v.criadaEm = Instant.now();
    preencher(v, d);
    return mapper.resumo(banco.inserir(v));
  }

  @PutMapping("/vagas/{id}")
  public VagaMapper.Resumo editar(@PathVariable UUID id, @RequestBody @Valid Dados d) {
    var v = paraEdicao(id);
    if (v.status != Status.RASCUNHO) throw Falha.invalida("A edição exige uma vaga em rascunho");
    preencher(v, d);
    return mapper.resumo(v);
  }

  @PatchMapping("/vagas/{id}/status")
  public VagaMapper.Resumo status(@PathVariable UUID id, @RequestBody @Valid Mudanca d) {
    var v = paraEdicao(id);
    if (d.status == Status.RASCUNHO || (v.status == Status.ENCERRADA && d.status != v.status))
      throw Falha.invalida("Mudança de status não permitida");
    v.status = d.status;
    v.atualizadaEm = Instant.now();
    return mapper.resumo(v);
  }

  @GetMapping("/vagas/{id}")
  public VagaMapper.Resumo obter(@PathVariable UUID id) {
    var v = banco.obter(Vaga.class, id);
    var u = ator.opcional();
    if (u != null && u.papel() == Papel.RECRUTADOR) ator.empresa(v.empresaId);
    else if (v.status != Status.PUBLICADA && (u == null || u.papel() != Papel.ADMIN))
      throw Falha.ausente();
    return mapper.resumo(v);
  }

  @GetMapping("/vagas")
  public Page<VagaMapper.Resumo> listar(
      @RequestParam(required = false) Modalidade modalidade,
      @RequestParam(required = false) Nivel nivel,
      @RequestParam(required = false) String tecnologia,
      @RequestParam(required = false) String localizacao,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "criada_em,desc") String sort) {
    var pagina = new Pagina(page, size, sort).validar(Set.of("criada_em", "titulo"));
    return vagas
        .findAll(
            (root, q, cb) -> {
              var termos = new ArrayList<Predicate>();
              termos.add(cb.equal(root.get("status"), Status.PUBLICADA));
              if (modalidade != null) termos.add(cb.equal(root.get("modalidade"), modalidade));
              if (nivel != null) termos.add(cb.equal(root.get("nivel"), nivel));
              if (tecnologia != null)
                termos.add(
                    cb.isMember(tecnologia.toLowerCase(Locale.ROOT), root.get("tecnologias")));
              if (localizacao != null)
                termos.add(
                    cb.equal(
                        cb.lower(root.get("localizacao")), localizacao.toLowerCase(Locale.ROOT)));
              return cb.and(termos.toArray(Predicate[]::new));
            },
            pagina)
        .map(mapper::resumo);
  }

  private Vaga paraEdicao(UUID id) {
    ator.exigir(Papel.RECRUTADOR);
    var v = banco.bloquear(Vaga.class, id);
    ator.empresa(v.empresaId);
    return v;
  }

  private void preencher(Vaga v, Dados d) {
    v.titulo = d.titulo.trim();
    v.descricao = d.descricao;
    v.modalidade = d.modalidade;
    v.nivel = d.nivel;
    v.localizacao = d.localizacao.trim();
    v.atualizadaEm = Instant.now();
    v.tecnologias.clear();
    d.tecnologias.forEach(t -> v.tecnologias.add(t.trim().toLowerCase(Locale.ROOT)));
  }
}
