package br.dev.selecao.usuario;

import br.dev.selecao.comum.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@Transactional
public class UsuariosApi {
  public record Cadastro(
      @NotBlank @Size(max = 255) String nome,
      @NotBlank @Email @Size(max = 255) String email,
      @NotNull @Size(min = 8, max = Contas.MAX_SENHA) String senha) {}

  public record Novo(@NotNull @Valid Cadastro dados, @NotNull Papel papel, UUID empresaId) {}

  public record Perfil(
      @NotBlank @Size(max = 255) String nome, @NotBlank @Email @Size(max = 255) String email) {}

  public record Atividade(@NotNull Boolean ativo) {}

  public record Resumo(UUID id, String nome, String email, Papel papel, boolean ativo) {
    public static Resumo de(Usuario u) {
      return new Resumo(u.id, u.nome, u.email, u.papel, u.ativo);
    }
  }

  private final Ator ator;
  private final Banco banco;
  private final Contas contas;
  private final UsuarioRepo usuarios;

  public UsuariosApi(Ator ator, Banco banco, Contas contas, UsuarioRepo usuarios) {
    this.ator = ator;
    this.banco = banco;
    this.contas = contas;
    this.usuarios = usuarios;
  }

  @PostMapping("/auth/cadastro")
  @ResponseStatus(HttpStatus.CREATED)
  public Resumo cadastro(@Valid @RequestBody Cadastro d) {
    return Resumo.de(contas.criar(d.nome, d.email, d.senha, Papel.CANDIDATO, null));
  }

  @PostMapping("/usuarios")
  @ResponseStatus(HttpStatus.CREATED)
  public Resumo novo(@Valid @RequestBody Novo n) {
    ator.exigir(Papel.ADMIN);
    return Resumo.de(
        contas.criar(n.dados.nome, n.dados.email, n.dados.senha, n.papel, n.empresaId));
  }

  @GetMapping("/usuarios")
  public Page<Resumo> listar(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    ator.exigir(Papel.ADMIN);
    return usuarios.findAll(new Pagina(page, size, "nome").validar(Set.of("nome"))).map(Resumo::de);
  }

  @PatchMapping("/usuarios/{id}/atividade")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void atividade(@PathVariable UUID id, @Valid @RequestBody Atividade d) {
    ator.exigir(Papel.ADMIN);
    contas.atividade(id, d.ativo);
  }

  @PutMapping("/usuarios/me")
  public Resumo perfil(@Valid @RequestBody Perfil d) {
    var u = banco.bloquear(Usuario.class, ator.exigir(Papel.CANDIDATO).id);
    String email = d.email.toLowerCase(Locale.ROOT);
    if (usuarios.findByEmailIgnoreCase(email).filter(outro -> !outro.id.equals(u.id)).isPresent())
      throw Falha.conflito("Email já cadastrado");
    u.nome = d.nome.trim();
    u.email = email;
    return Resumo.de(u);
  }
}
