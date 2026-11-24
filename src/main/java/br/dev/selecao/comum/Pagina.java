package br.dev.selecao.comum;

import java.util.Set;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;

public record Pagina(int page, int size, String sort) {
  public static void validarLimites(int page, int size) {
    if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE)
      throw new Falha(HttpStatus.BAD_REQUEST, "Paginação inválida");
  }

  public Pageable validar(Set<String> campos) {
    validarLimites(page, size);
    String[] partes = sort.split(",", -1);
    if (partes.length > 2
        || !campos.contains(partes[0])
        || (partes.length == 2 && !Set.of("asc", "desc").contains(partes[1])))
      throw new Falha(HttpStatus.BAD_REQUEST, "Ordenação inválida");
    String campo = partes[0].equals("criada_em") ? "criadaEm" : partes[0];
    Sort.Direction direcao =
        partes.length == 2 && partes[1].equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
    return PageRequest.of(page, size, Sort.by(direcao, campo).and(Sort.by("id")));
  }
}
