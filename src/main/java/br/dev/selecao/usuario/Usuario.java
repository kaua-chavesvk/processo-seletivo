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

  public UUID getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public String getEmail() {
    return email;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  @com.fasterxml.jackson.annotation.JsonIgnore
  public String getSenhaHash() {
    return senhaHash;
  }

  public Papel getPapel() {
    return papel;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void atualizarHash(String hash) {
    senhaHash = hash;
  }
}
