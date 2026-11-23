package br.dev.selecao.candidatura;

import java.time.Instant;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CandidaturaMapper {
  record Resumo(
      UUID id,
      UUID vagaId,
      UUID candidatoId,
      Etapa etapaAtual,
      Instant criadaEm,
      Instant atualizadaEm) {}

  record HistoricoDto(
      UUID id,
      UUID candidaturaId,
      Etapa etapaAnterior,
      Etapa etapaNova,
      UUID alteradoPor,
      String observacao,
      Instant alteradoEm) {}

  HistoricoDto historico(Historico origem);

  Resumo resumo(Candidatura candidatura);
}
