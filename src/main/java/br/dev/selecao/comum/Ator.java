package br.dev.selecao.comum;

import static br.dev.selecao.usuario.Papel.*;

import br.dev.selecao.usuario.*;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class Ator {
  private final Banco banco;

  public Ator(Banco banco) {
    this.banco = banco;
  }

  public Usuario opcional() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getPrincipal() instanceof Usuario u ? u : null;
  }

  public Usuario exigir(Papel... papeis) {
    Usuario u = opcional();
    if (u == null) throw Falha.credencial();
    for (Papel papel : papeis) if (u.getPapel() == papel) return u;
    throw Falha.negado();
  }

  public void empresa(UUID id) {
    var recrutador = exigir(RECRUTADOR);
    if (banco
        .em
        .createNativeQuery("select 1 from recrutador_empresa where usuario_id=:u and empresa_id=:e")
        .setParameter("u", recrutador.getId())
        .setParameter("e", id)
        .getResultList()
        .isEmpty()) throw Falha.negado();
  }
}
