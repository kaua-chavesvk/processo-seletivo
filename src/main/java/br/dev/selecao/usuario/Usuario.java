package br.dev.selecao.usuario;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "usuario")
public class Usuario {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  UUID id;

  String nome;
  String email;
  String senhaHash;

  @Enumerated(EnumType.STRING)
  Papel papel;

  boolean ativo;
  Instant criadoEm;

  public UUID id() {
    return id;
  }

  public String senhaHash() {
    return senhaHash;
  }

  public Papel papel() {
    return papel;
  }

  public boolean ativo() {
    return ativo;
  }

  public void atualizarHash(String hash) {
    senhaHash = hash;
  }
}
