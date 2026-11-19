package br.dev.selecao.candidatura;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "candidatura")
public class Candidatura {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    public UUID vagaId;
    public UUID candidatoId;
    public String curriculoCaminho;
    @Enumerated(EnumType.STRING)
    public Etapa etapaAtual;
    public Instant criadaEm;
    public Instant atualizadaEm;
}
