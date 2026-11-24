package br.dev.selecao.comum;

import org.springframework.http.HttpStatus;

public class Falha extends RuntimeException {
  public final HttpStatus status;

  public Falha(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public static Falha ausente() {
    return new Falha(HttpStatus.NOT_FOUND, "Recurso não encontrado");
  }

  public static Falha negado() {
    return new Falha(HttpStatus.FORBIDDEN, "Acesso negado");
  }

  public static Falha invalida(String msg) {
    return new Falha(HttpStatus.UNPROCESSABLE_ENTITY, msg);
  }

  public static Falha conflito(String msg) {
    return new Falha(HttpStatus.CONFLICT, msg);
  }

  public static Falha credencial() {
    return new Falha(HttpStatus.UNAUTHORIZED, "Credencial inválida ou expirada");
  }
}
