package br.dev.selecao.empresa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "empresa")
public class Empresa {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  UUID id;

  String nome;
  String cnpj;
  String site;
  Instant criadaEm;

  public UUID getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public String getCnpj() {
    return cnpj;
  }

  public String getSite() {
    return site;
  }

  public Instant getCriadaEm() {
    return criadaEm;
  }
}
