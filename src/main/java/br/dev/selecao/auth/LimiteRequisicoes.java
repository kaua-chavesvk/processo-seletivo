package br.dev.selecao.auth;

import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

final class LimiteRequisicoes {
  private static final long MINUTO = TimeUnit.MINUTES.toNanos(1);
  private static final int CAPACIDADE = 16_384;
  private final LinkedHashMap<String, Baldes> clientes = new LinkedHashMap<>(16, .75f, true);

  synchronized boolean permitir(String endereco, boolean autenticacao, long agora) {
    var uso = clientes.get(endereco);
    if (uso == null) {
      if (clientes.size() == CAPACIDADE) {
        if (agora - clientes.firstEntry().getValue().ultimo < MINUTO) return false;
        clientes.pollFirstEntry();
      }
      uso = new Baldes(agora);
      clientes.put(endereco, uso);
    }
    return uso.consumir(autenticacao, agora);
  }

  private static final class Baldes {
    // Um token custa MINUTO créditos; cada nanossegundo repõe a taxa por minuto.
    private static final int GERAL = 180;
    private static final int AUTH = 20;
    long ultimo;
    long geral = GERAL * MINUTO;
    long auth = AUTH * MINUTO;

    Baldes(long agora) {
      ultimo = agora;
    }

    boolean consumir(boolean autenticacao, long agora) {
      long decorrido = Math.clamp(agora - ultimo, 0L, MINUTO);
      if (agora - ultimo > 0) ultimo = agora;
      geral = Math.min(GERAL * MINUTO, geral + decorrido * GERAL);
      auth = Math.min(AUTH * MINUTO, auth + decorrido * AUTH);
      if (geral < MINUTO || (autenticacao && auth < MINUTO)) return false;
      geral -= MINUTO;
      if (autenticacao) auth -= MINUTO;
      return true;
    }
  }
}
