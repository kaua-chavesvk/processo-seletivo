package br.dev.selecao.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Janela fixa global por instância, indexada pelo endereço do par TCP. */
final class LimiteRequisicoes {
    private static final long MINUTO = TimeUnit.MINUTES.toNanos(1);
    private static final int MAX_ENDERECOS = 16_384;
    private final Map<String, Contagem> clientes = new HashMap<>();
    private long inicio = System.nanoTime();

    synchronized boolean permitir(String endereco, boolean autenticacao, long agora) {
        if (agora - inicio >= MINUTO) {
            clientes.clear();
            inicio = agora;
        }
        if (clientes.size() >= MAX_ENDERECOS && !clientes.containsKey(endereco)) return false;
        var uso = clientes.computeIfAbsent(endereco, chave -> new Contagem());
        if (uso.total >= 180 || (autenticacao && uso.autenticacao >= 20)) return false;
        uso.total++;
        if (autenticacao) uso.autenticacao++;
        return true;
    }

    private static final class Contagem {
        int autenticacao;
        int total;
    }
}
