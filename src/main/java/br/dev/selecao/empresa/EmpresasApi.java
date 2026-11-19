package br.dev.selecao.empresa;

import br.dev.selecao.comum.*;
import br.dev.selecao.usuario.Papel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/empresas") @Transactional
public class EmpresasApi {
    public record Dados(@NotBlank @Size(max=255) String nome,@NotBlank String cnpj,@Size(max=255) String site) {
        @AssertTrue(message="CNPJ inválido") public boolean isCnpjValido() { return Cnpj.valido(cnpj); }
    }
    private final Ator ator;
    private final Banco banco;
    public EmpresasApi(Ator ator,Banco banco) { this.ator=ator; this.banco=banco; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Empresa criar(@RequestBody @Valid Dados d) {
        ator.exigir(Papel.ADMIN); Empresa e=new Empresa(); e.criadaEm=Instant.now(); preencher(e,d); return banco.inserir(e);
    }
    @PutMapping("/{id}") public Empresa editar(@PathVariable UUID id,@RequestBody @Valid Dados d) {
        ator.exigir(Papel.ADMIN); Empresa e=banco.bloquear(Empresa.class,id); preencher(e,d); return e;
    }
    @GetMapping public List<Empresa> listar(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        ator.exigir(Papel.ADMIN); var p=new Pagina(page,size,"nome").validar(Set.of("nome"));
        return banco.em.createQuery("from Empresa order by nome,id",Empresa.class)
            .setFirstResult((int)p.getOffset()).setMaxResults(size).getResultList();
    }
    @GetMapping("/{id}") public Empresa obter(@PathVariable UUID id) {
        ator.exigir(Papel.ADMIN); return banco.obter(Empresa.class,id);
    }
    private void preencher(Empresa e,Dados d) { e.nome=d.nome.trim(); e.cnpj=Cnpj.normalizar(d.cnpj); e.site=d.site; }
}
