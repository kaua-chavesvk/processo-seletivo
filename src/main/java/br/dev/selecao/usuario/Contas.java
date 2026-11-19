package br.dev.selecao.usuario;

import br.dev.selecao.comum.*;
import br.dev.selecao.empresa.Empresa;
import java.time.Instant;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Contas {
  public static final int MAX_SENHA = 72;
  private final Banco banco;
  private final UsuarioRepo usuarios;
  private final PasswordEncoder encoder;

  public Contas(Banco banco, UsuarioRepo usuarios, PasswordEncoder encoder) {
    this.banco = banco;
    this.usuarios = usuarios;
    this.encoder = encoder;
  }

  public Usuario criar(String nome, String email, String senha, Papel papel, UUID empresa) {
    if ((papel == Papel.RECRUTADOR) != (empresa != null))
      throw Falha.invalida("Informe empresa apenas para recrutadores");
    if (empresa != null) banco.obter(Empresa.class, empresa);
    if (usuarios.findByEmailIgnoreCase(email).isPresent())
      throw Falha.conflito("Email já cadastrado");
    if (senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_SENHA)
      throw Falha.invalida("Senha excede 72 bytes");
    Usuario u = new Usuario();
    u.nome = nome.trim();
    u.email = email.toLowerCase(Locale.ROOT);
    u.senhaHash = encoder.encode(senha);
    u.ativo = true;
    u.papel = papel;
    u.criadoEm = Instant.now();
    banco.inserir(u);
    if (empresa != null)
      banco
          .em
          .createNativeQuery("insert into recrutador_empresa(usuario_id,empresa_id) values (:u,:e)")
          .setParameter("u", u.id)
          .setParameter("e", empresa)
          .executeUpdate();
    return u;
  }

  public void atividade(UUID id, boolean ativo) {
    banco.bloquear(Usuario.class, id).ativo = ativo;
    if (!ativo)
      banco
          .em
          .createQuery("update Refresh set revogado=true where usuarioId=:id")
          .setParameter("id", id)
          .executeUpdate();
  }
}
