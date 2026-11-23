package br.dev.selecao.vaga;

import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface VagaRepo extends JpaRepository<Vaga, UUID>, JpaSpecificationExecutor<Vaga> {
  org.springframework.data.domain.Page<Vaga> findByEmpresaId(
      UUID empresaId, org.springframework.data.domain.Pageable pagina);
}
