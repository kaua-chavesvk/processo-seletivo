package br.dev.selecao.usuario;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    public String nome;
    public String email;
    public String senhaHash;
    @Enumerated(EnumType.STRING)
    public Papel papel;
    public boolean ativo;
    public Instant criadoEm;
}
