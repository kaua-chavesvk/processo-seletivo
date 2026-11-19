package br.dev.selecao.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app")
public record DuracaoTokens(@DefaultValue("15m") Duration accessTtl) {
  public DuracaoTokens {
    if (accessTtl.toSeconds() < 1)
      throw new IllegalArgumentException("app.access-ttl deve ser positivo");
  }
}
