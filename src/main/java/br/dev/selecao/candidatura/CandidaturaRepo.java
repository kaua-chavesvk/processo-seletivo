package br.dev.selecao.candidatura;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface CandidaturaRepo extends JpaRepository<Candidatura,UUID> {
    boolean existsByVagaIdAndCandidatoId(UUID vaga,UUID candidato);
    Page<Candidatura> findByCandidatoId(UUID candidato,Pageable pagina);
    Page<Candidatura> findByVagaId(UUID vaga,Pageable pagina);
    Page<Candidatura> findByVagaIdAndEtapaAtual(UUID vaga,Etapa etapa,Pageable pagina);
}
