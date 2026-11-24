package br.dev.selecao.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface RefreshRepo extends JpaRepository<Refresh, UUID> {
  @Query("select r.usuarioId from Refresh r where r.hashToken=:hash")
  Optional<UUID> titular(String hash);

  @Modifying
  @Query(
      "update Refresh set revogado=true where hashToken=:hash and revogado=false and"
          + " expiraEm>:agora")
  int consumir(String hash, Instant agora);

  @Modifying
  @Query("update Refresh set revogado=true where usuarioId=:id")
  void revogarDoUsuario(UUID id);

  @Modifying
  @Query("delete from Refresh where expiraEm<=:agora")
  void removerExpirados(Instant agora);
}
