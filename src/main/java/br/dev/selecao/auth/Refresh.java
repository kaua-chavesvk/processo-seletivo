package br.dev.selecao.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "refresh_token")
public class Refresh {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  UUID id;

  UUID usuarioId;
  String hashToken;
  Instant expiraEm;
  boolean revogado;

  public UUID getId() {
    return id;
  }

  public UUID getUsuarioId() {
    return usuarioId;
  }

  @com.fasterxml.jackson.annotation.JsonIgnore
  public String getHashToken() {
    return hashToken;
  }

  public Instant getExpiraEm() {
    return expiraEm;
  }

  public boolean isRevogado() {
    return revogado;
  }
}
