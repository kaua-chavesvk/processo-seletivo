package br.dev.selecao.candidatura;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "candidatura")
public class Candidatura {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  UUID id;

  UUID vagaId;
  UUID candidatoId;
  String curriculoCaminho;

  @Enumerated(EnumType.STRING)
  Etapa etapaAtual;

  Instant criadaEm;
  Instant atualizadaEm;

  public UUID getId() {
    return id;
  }

  public UUID getVagaId() {
    return vagaId;
  }

  public UUID getCandidatoId() {
    return candidatoId;
  }

  public String getCurriculoCaminho() {
    return curriculoCaminho;
  }

  public Etapa getEtapaAtual() {
    return etapaAtual;
  }

  public Instant getCriadaEm() {
    return criadaEm;
  }

  public Instant getAtualizadaEm() {
    return atualizadaEm;
  }
}
