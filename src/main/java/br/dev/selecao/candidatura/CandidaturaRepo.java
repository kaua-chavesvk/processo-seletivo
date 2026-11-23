package br.dev.selecao.candidatura;

import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidaturaRepo extends JpaRepository<Candidatura, UUID> {
  boolean existsByVagaIdAndCandidatoId(UUID vaga, UUID candidato);

  Page<Candidatura> findByCandidatoId(UUID candidato, Pageable pagina);

  Page<Candidatura> findByVagaId(UUID vaga, Pageable pagina);

  Page<Candidatura> findByVagaIdAndEtapaAtual(UUID vaga, Etapa etapa, Pageable pagina);

  interface Cartao {
    UUID getId();

    UUID getVagaId();

    String getNome();

    String getTitulo();

    Etapa getEtapaAtual();

    java.time.Instant getCriadaEm();
  }

  String CARTOES =
      "select c.id as id, v.id as vagaId, u.nome as nome, v.titulo as titulo, c.etapaAtual as"
          + " etapaAtual, c.criadaEm as criadaEm from Candidatura c join Usuario u on"
          + " u.id=c.candidatoId join Vaga v on v.id=c.vagaId ";

  @org.springframework.data.jpa.repository.Query(CARTOES + "where v.id=:vagaId")
  Page<Cartao> cartoesDaVaga(UUID vagaId, Pageable pagina);

  @org.springframework.data.jpa.repository.Query(CARTOES + "where u.id=:candidatoId")
  Page<Cartao> cartoesDoCandidato(UUID candidatoId, Pageable pagina);

  @org.springframework.data.jpa.repository.Query(
      CARTOES + "where c.candidatoId=:candidatoId and c.vagaId=:vagaId")
  Page<Cartao> cartoesDoCandidatoNaVaga(UUID candidatoId, UUID vagaId, Pageable pagina);
}
