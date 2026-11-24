package br.dev.selecao.candidatura;

import static br.dev.selecao.usuario.Papel.*;

import br.dev.selecao.comum.*;
import br.dev.selecao.usuario.*;
import br.dev.selecao.vaga.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.transaction.support.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class CandidaturasApi {
  public record Mudanca(@NotNull Etapa novaEtapa, @Size(max = 4000) String observacao) {}

  private final CandidaturasService servico;

  public CandidaturasApi(CandidaturasService servico) {
    this.servico = servico;
  }

  @PostMapping(value = "/vagas/{id}/candidaturas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public CandidaturaMapper.Resumo inscrever(
      @PathVariable UUID id, @RequestPart MultipartFile curriculo) {
    return servico.inscrever(id, curriculo);
  }

  @GetMapping("/candidaturas/minhas")
  public Page<CandidaturaMapper.Resumo> minhas(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "criada_em,desc") String sort) {
    return servico.minhas(page, size, sort);
  }

  @GetMapping("/empresas/{empresaId}/vagas/{vagaId}/candidaturas")
  public Page<CandidaturaMapper.Resumo> daVaga(
      @PathVariable UUID empresaId,
      @PathVariable UUID vagaId,
      @RequestParam(required = false) Etapa etapa,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "criada_em,desc") String sort) {
    return servico.daVaga(empresaId, vagaId, etapa, page, size, sort);
  }

  @PatchMapping("/candidaturas/{id}/etapa")
  public CandidaturaMapper.Resumo avancar(@PathVariable UUID id, @RequestBody @Valid Mudanca d) {
    return servico.avancar(id, d.novaEtapa(), d.observacao());
  }

  @PatchMapping("/candidaturas/{id}/desistencia")
  public CandidaturaMapper.Resumo desistir(@PathVariable UUID id) {
    return servico.desistir(id);
  }

  @GetMapping(
      value = "/candidaturas/{id}/historico",
      params = {"!page", "!size"})
  public List<CandidaturaMapper.HistoricoDto> historico(@PathVariable UUID id) {
    return servico.historico(id, Pageable.unpaged()).getContent();
  }

  @GetMapping("/candidaturas/{id}/historico")
  public Page<CandidaturaMapper.HistoricoDto> historicoPaginado(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pagina.validarLimites(page, size);
    return servico.historico(id, PageRequest.of(page, size));
  }

  @GetMapping(value = "/candidaturas/{id}/curriculo", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<Resource> curriculo(@PathVariable UUID id) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header("Content-Disposition", "attachment; filename=curriculo.pdf")
        .body(servico.curriculo(id));
  }
}
