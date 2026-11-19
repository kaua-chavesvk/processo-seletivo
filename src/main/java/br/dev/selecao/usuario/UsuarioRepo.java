package br.dev.selecao.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UsuarioRepo extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
}
