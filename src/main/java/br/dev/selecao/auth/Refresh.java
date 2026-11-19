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
}
