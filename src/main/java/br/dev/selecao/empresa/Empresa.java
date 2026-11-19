package br.dev.selecao.empresa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "empresa")
public class Empresa {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    public String nome;
    public String cnpj;
    public String site;
    public Instant criadaEm;
}
