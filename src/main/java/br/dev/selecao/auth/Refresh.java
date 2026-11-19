package br.dev.selecao.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "refresh_token")
public class Refresh {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    public UUID usuarioId;
    public String hashToken;
    public Instant expiraEm;
    public boolean revogado;
}
