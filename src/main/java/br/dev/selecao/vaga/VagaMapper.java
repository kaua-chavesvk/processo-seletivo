package br.dev.selecao.vaga;

import org.mapstruct.Mapper;
import java.time.Instant;
import java.util.*;
@Mapper(componentModel="spring")
public interface VagaMapper {
    record Resumo(UUID id,UUID empresaId,String titulo,String descricao,Modalidade modalidade,Nivel nivel,
        String localizacao,Status status,Instant criadaEm,Instant atualizadaEm,Set<String> tecnologias) {}
    Resumo resumo(Vaga vaga);
}
