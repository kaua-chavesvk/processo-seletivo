package br.dev.selecao.auth;

import br.dev.selecao.comum.Banco;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LimpezaTokens {
  private final Banco banco;

  public LimpezaTokens(Banco banco) {
    this.banco = banco;
  }

  @Scheduled(cron = "0 17 3 * * *", zone = "UTC")
  @Transactional
  public void removerExpirados() {
    banco
        .em
        .createQuery("delete from Refresh where expiraEm <= :agora")
        .setParameter("agora", Instant.now())
        .executeUpdate();
  }
}
