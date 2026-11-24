package br.dev.selecao.comum;

import static br.dev.selecao.usuario.Papel.*;

import br.dev.selecao.usuario.*;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class Ator {
  private final UsuarioRepo usuarios;

  public Ator(UsuarioRepo usuarios) {
    this.usuarios = usuarios;
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
    if (usuarios.empresaDoRecrutador(recrutador.getId()).filter(id::equals).isEmpty())
      throw Falha.negado();
  }
}
