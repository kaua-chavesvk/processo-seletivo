package br.dev.selecao.usuario;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepo extends JpaRepository<Usuario, UUID> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query(
      "select u from Usuario u where lower(u.email)=lower(:email)")
  Optional<Usuario> bloquearPorEmail(
      @org.springframework.data.repository.query.Param("email") String email);

  Optional<Usuario> findByEmailIgnoreCase(String email);

  @org.springframework.data.jpa.repository.Query(
      value = "select empresa_id from recrutador_empresa where usuario_id=:id",
      nativeQuery = true)
  Optional<UUID> empresaDoRecrutador(UUID id);
}
