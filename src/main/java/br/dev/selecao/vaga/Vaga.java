package br.dev.selecao.vaga;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "vaga")
public class Vaga {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    public UUID empresaId;
    public String titulo;
    public String descricao;
    @Enumerated(EnumType.STRING)
    public Modalidade modalidade;
    @Enumerated(EnumType.STRING)
    public Nivel nivel;
    public String localizacao;
    @Enumerated(EnumType.STRING)
    public Status status;
    public Instant criadaEm;
    public Instant atualizadaEm;
    @ElementCollection
    @CollectionTable(name = "vaga_tecnologia", joinColumns = @JoinColumn(name = "vaga_id"))
    @Column(name = "tecnologia")
    public Set<String> tecnologias = new TreeSet<>();
}
