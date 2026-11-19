package br.dev.selecao.auth;

import io.jsonwebtoken.*;
import java.time.*;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Tokens {
  private final Duration validade;

  long segundos() {
    return validade.toSeconds();
  }

  private final SecretKey chave;

  public Tokens(@Value("${app.jwt}") String configurada, DuracaoTokens config) {
    validade = config.accessTtl();
    chave =
        configurada.isBlank()
            ? Jwts.SIG.HS256.key().build()
            : io.jsonwebtoken.security.Keys.hmacShaKeyFor(Base64.getDecoder().decode(configurada));
  }

  public String emitir(UUID id) {
    Instant agora = Instant.now();
    return Jwts.builder()
        .subject(id.toString())
        .issuedAt(Date.from(agora))
        .expiration(Date.from(agora.plusSeconds(segundos())))
        .signWith(chave)
        .compact();
  }

  public UUID ler(String jwt) {
    return UUID.fromString(
        Jwts.parser().verifyWith(chave).build().parseSignedClaims(jwt).getPayload().getSubject());
  }
}
