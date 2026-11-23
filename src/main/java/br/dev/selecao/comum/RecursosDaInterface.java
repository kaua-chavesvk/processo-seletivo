package br.dev.selecao.comum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.web.bind.annotation.*;

@RestController
class RecursosDaInterface {
  private final Configuracao recursos;

  record Configuracao(long limitePdf, boolean swagger) {}

  RecursosDaInterface(
      MultipartProperties multipart,
      @Value("${springdoc.swagger-ui.enabled:true}") boolean swagger) {
    recursos = new Configuracao(multipart.getMaxFileSize().toBytes(), swagger);
  }

  @GetMapping("/interface/config")
  Configuracao recursos() {
    return recursos;
  }
}
