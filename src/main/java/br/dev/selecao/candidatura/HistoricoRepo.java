package br.dev.selecao.candidatura;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoRepo extends JpaRepository<Historico, UUID> {
  List<Historico> findByCandidaturaIdOrderByAlteradoEmAscIdAsc(UUID candidaturaId);
}
