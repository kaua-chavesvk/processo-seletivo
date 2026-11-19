package br.dev.selecao.auth;

import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

final class LimiteRequisicoes {
  private static final long MINUTO = TimeUnit.MINUTES.toNanos(1);
  private static final int CAPACIDADE = 16_384;
  private final LinkedHashMap<String, Baldes> clientes = new LinkedHashMap<>(16, .75f, true);
  private final Baldes compartilhado = new Baldes();

  synchronized boolean permitir(String endereco, boolean autenticacao, long agora) {
    var uso = clientes.get(endereco);
    if (uso == null) {
      if (clientes.size() >= CAPACIDADE
          && agora - clientes.firstEntry().getValue().ultimo >= MINUTO) clientes.pollFirstEntry();
      if (clientes.size() < CAPACIDADE) {
        uso = new Baldes();
        clientes.put(endereco, uso);
      } else uso = compartilhado;
    }
    return uso.consumir(autenticacao, agora);
  }

  private static final class Baldes {
    long ultimo;
    double geral = 180;
    double auth = 20;

    boolean consumir(boolean autenticacao, long agora) {
      double minutos = Math.max(0, agora - ultimo) / (double) MINUTO;
      ultimo = agora;
      geral = Math.min(180, geral + minutos * 180);
      auth = Math.min(20, auth + minutos * 20);
      if (geral < 1 || (autenticacao && auth < 1)) return false;
      geral--;
      if (autenticacao) auth--;
      return true;
    }
  }
}
