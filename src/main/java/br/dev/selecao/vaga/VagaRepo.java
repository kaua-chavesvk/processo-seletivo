package br.dev.selecao.vaga;

import org.springframework.data.jpa.repository.*;
import java.util.UUID;
public interface VagaRepo extends JpaRepository<Vaga,UUID>, JpaSpecificationExecutor<Vaga> {}
