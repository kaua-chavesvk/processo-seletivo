package br.dev.selecao.empresa;

import br.dev.selecao.comum.*;
import br.dev.selecao.usuario.Papel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/empresas")
@Transactional
public class EmpresasApi {
  public record Dados(
      @NotBlank @Size(max = 255) String nome, @NotBlank String cnpj, @Size(max = 255) String site) {
    @AssertTrue(message = "CNPJ inválido")
    public boolean isCnpjValido() {
      return Cnpj.valido(cnpj);
    }
  }

  public record Resumo(UUID id, String nome, String cnpj, String site, Instant criadaEm) {
    static Resumo de(Empresa e) {
      return new Resumo(e.id, e.nome, e.cnpj, e.site, e.criadaEm);
    }
  }

  private final EmpresaRepo empresas;
  private final Ator ator;
  private final Banco banco;

  public EmpresasApi(Ator ator, Banco banco, EmpresaRepo empresas) {
    this.empresas = empresas;
    this.ator = ator;
    this.banco = banco;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Resumo criar(@RequestBody @Valid Dados d) {
    ator.exigir(Papel.ADMIN);
    Empresa e = new Empresa();
    e.criadaEm = Instant.now();
    preencher(e, d);
    return Resumo.de(banco.inserir(e));
  }

  @PutMapping("/{id}")
  public Resumo editar(@PathVariable UUID id, @RequestBody @Valid Dados d) {
    ator.exigir(Papel.ADMIN);
    Empresa e = banco.bloquear(Empresa.class, id);
    preencher(e, d);
    return Resumo.de(e);
  }

  @GetMapping
  public org.springframework.data.domain.Page<Resumo> listar(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "nome") String sort) {
    ator.exigir(Papel.ADMIN);
    var pagina = new Pagina(page, size, sort).validar(Set.of("nome", "criada_em"));
    return empresas.findAll(pagina).map(Resumo::de);
  }

  @GetMapping("/{id}")
  public Resumo obter(@PathVariable UUID id) {
    ator.exigir(Papel.ADMIN);
    return Resumo.de(banco.obter(Empresa.class, id));
  }

  private void preencher(Empresa e, Dados d) {
    e.nome = d.nome.trim();
    e.cnpj = Cnpj.normalizar(d.cnpj);
    e.site = d.site;
  }
}
