package br.dev.selecao.vaga;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "vaga")
public class Vaga {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  UUID id;

  UUID empresaId;
  String titulo;
  String descricao;

  @Enumerated(EnumType.STRING)
  Modalidade modalidade;

  @Enumerated(EnumType.STRING)
  Nivel nivel;

  String localizacao;

  @Enumerated(EnumType.STRING)
  Status status;

  Instant criadaEm;
  Instant atualizadaEm;

  @ElementCollection
  @CollectionTable(name = "vaga_tecnologia", joinColumns = @JoinColumn(name = "vaga_id"))
  @Column(name = "tecnologia")
  Set<String> tecnologias = new TreeSet<>();

  public UUID getId() {
    return id;
  }

  public UUID getEmpresaId() {
    return empresaId;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getDescricao() {
    return descricao;
  }

  public Modalidade getModalidade() {
    return modalidade;
  }

  public Nivel getNivel() {
    return nivel;
  }

  public String getLocalizacao() {
    return localizacao;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getCriadaEm() {
    return criadaEm;
  }

  public Instant getAtualizadaEm() {
    return atualizadaEm;
  }

  public Set<String> getTecnologias() {
    return tecnologias;
  }
}
