package br.dev.selecao.candidatura;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "historico_etapa")
public class Historico {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  UUID id;

  UUID candidaturaId;

  @Enumerated(EnumType.STRING)
  Etapa etapaAnterior;

  @Enumerated(EnumType.STRING)
  Etapa etapaNova;

  UUID alteradoPor;
  String observacao;
  Instant alteradoEm;

  public UUID getId() {
    return id;
  }

  public UUID getCandidaturaId() {
    return candidaturaId;
  }

  public Etapa getEtapaAnterior() {
    return etapaAnterior;
  }

  public Etapa getEtapaNova() {
    return etapaNova;
  }

  public UUID getAlteradoPor() {
    return alteradoPor;
  }

  public String getObservacao() {
    return observacao;
  }

  public Instant getAlteradoEm() {
    return alteradoEm;
  }
}
