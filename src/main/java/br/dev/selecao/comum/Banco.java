package br.dev.selecao.comum;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class Banco {
  @PersistenceContext private EntityManager em;

  public <T> T obter(Class<T> tipo, UUID id) {
    return buscar(tipo, id, LockModeType.NONE);
  }

  public <T> T bloquear(Class<T> tipo, UUID id) {
    return buscar(tipo, id, LockModeType.PESSIMISTIC_WRITE);
  }

  private <T> T buscar(Class<T> tipo, UUID id, LockModeType lock) {
    T entidade = em.find(tipo, id, lock);
    if (entidade == null) throw Falha.ausente();
    return entidade;
  }

  public <T> T inserir(T entidade) {
    em.persist(entidade);
    em.flush();
    return entidade;
  }
}
