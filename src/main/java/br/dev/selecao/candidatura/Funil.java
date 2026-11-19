package br.dev.selecao.candidatura;

import br.dev.selecao.usuario.Papel;
import java.util.Map;
import static br.dev.selecao.candidatura.Etapa.*;
public final class Funil {
    private Funil() {}
    private static final Map<Etapa, Etapa> PROXIMA = Map.of(
        TRIAGEM, ENTREVISTA, ENTREVISTA, TESTE_TECNICO,
        TESTE_TECNICO, PROPOSTA, PROPOSTA, CONTRATADO);
    public static void validar(Etapa atual, Etapa nova, Papel ator) {
        if (PROXIMA.containsKey(atual)) {
            if (ator == Papel.CANDIDATO && nova == DESISTIU) return;
            if (ator == Papel.RECRUTADOR && (nova == REPROVADO || nova == PROXIMA.get(atual))) return;
        }
        throw new TransicaoInvalida();
    }
}
