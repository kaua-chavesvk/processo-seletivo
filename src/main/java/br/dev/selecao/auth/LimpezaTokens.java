package br.dev.selecao.auth;

import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LimpezaTokens {
  private final RefreshRepo renovacoes;

  public LimpezaTokens(RefreshRepo renovacoes) {
    this.renovacoes = renovacoes;
  }

  @Scheduled(cron = "0 17 3 * * *", zone = "UTC")
  @Transactional
  public void removerExpirados() {
    renovacoes.removerExpirados(Instant.now());
  }
}
