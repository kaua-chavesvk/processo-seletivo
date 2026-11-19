package br.dev.selecao.comum;

import jakarta.persistence.*;
import org.springframework.stereotype.Component;
import java.util.UUID;
@Component
public class Banco {
    @PersistenceContext public EntityManager em;
    public <T> T obter(Class<T> tipo, UUID id) { return buscar(tipo, id, LockModeType.NONE); }
    public <T> T bloquear(Class<T> tipo, UUID id) { return buscar(tipo, id, LockModeType.PESSIMISTIC_WRITE); }
    private <T> T buscar(Class<T> tipo, UUID id, LockModeType lock) {
        T obj = em.find(tipo, id, lock);
        if (obj == null) throw Falha.ausente();
        return obj;
    }
    public <T> T inserir(T entidade) { em.persist(entidade); em.flush(); return entidade; }
}
