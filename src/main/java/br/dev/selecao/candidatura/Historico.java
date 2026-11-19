package br.dev.selecao.candidatura;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "historico_etapa")
public class Historico {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    public UUID candidaturaId;
    @Enumerated(EnumType.STRING)
    public Etapa etapaAnterior;
    @Enumerated(EnumType.STRING)
    public Etapa etapaNova;
    public UUID alteradoPor;
    public String observacao;
    public Instant alteradoEm;
}
