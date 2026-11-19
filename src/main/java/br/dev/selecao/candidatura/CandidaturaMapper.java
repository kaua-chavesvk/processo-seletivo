package br.dev.selecao.candidatura;

import org.mapstruct.Mapper;
import java.util.UUID;
import java.time.Instant;
@Mapper(componentModel="spring")
public interface CandidaturaMapper {
    record Resumo(UUID id,UUID vagaId,UUID candidatoId,Etapa etapaAtual,Instant criadaEm,Instant atualizadaEm) {}
    Resumo resumo(Candidatura candidatura);
}
