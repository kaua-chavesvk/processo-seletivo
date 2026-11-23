package br.dev.selecao.curriculo;

import static org.assertj.core.api.Assertions.*;

import br.dev.selecao.comum.Falha;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

class ArmazenamentoS3Test {
  @Test
  void transferePdfPorHttpERecusaDocumentoInvalido() throws Exception {
    var objetos = new ConcurrentHashMap<String, byte[]>();
    var recebidos = new java.util.concurrent.atomic.AtomicInteger();
    var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.createContext(
        "/selecao/curriculos/",
        req -> {
          try (req) {
            recebidos.incrementAndGet();
            var chave = req.getRequestURI().getPath();
            if (req.getRequestHeaders().getFirst("Authorization") == null) {
              req.sendResponseHeaders(403, -1);
              return;
            }
            switch (req.getRequestMethod()) {
              case "PUT" -> {
                objetos.put(chave, req.getRequestBody().readAllBytes());
                req.getResponseHeaders().add("ETag", "\"ficticio\"");
                req.sendResponseHeaders(200, -1);
              }
              case "GET" -> {
                var pdf = objetos.get(chave);
                if (pdf == null) req.sendResponseHeaders(404, -1);
                else {
                  req.sendResponseHeaders(200, pdf.length);
                  req.getResponseBody().write(pdf);
                }
              }
              case "DELETE" -> {
                objetos.remove(chave);
                req.sendResponseHeaders(204, -1);
              }
              default -> req.sendResponseHeaders(405, -1);
            }
          }
        });
    servidor.start();
    try (var cliente =
        S3Client.builder()
            .region(Region.US_EAST_1)
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .forcePathStyle(true)
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .endpointOverride(URI.create("http://127.0.0.1:" + servidor.getAddress().getPort()))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("teste", "teste")))
            .build()) {
      var arquivos = new ArmazenamentoS3(cliente, "selecao", new MultipartProperties());
      byte[] pdf = "%PDF-1.7 exemplo".getBytes(StandardCharsets.US_ASCII);
      var invalido =
          new MockMultipartFile("curriculo", "exemplo.pdf", "application/pdf", new byte[] {0});
      assertThatThrownBy(() -> arquivos.salvar(invalido)).isInstanceOf(Falha.class);
      assertThat(recebidos.get()).isZero();
      String chave =
          arquivos.salvar(
              new MockMultipartFile("curriculo", "exemplo.pdf", "application/pdf", pdf));
      assertThat(objetos.get("/selecao/curriculos/" + chave)).containsExactly(pdf);
      try (var resposta = arquivos.carregar(chave).getInputStream()) {
        assertThat(resposta.readAllBytes()).containsExactly(pdf);
      }
      arquivos.remover(chave);
      assertThat(objetos).isEmpty();
      assertThatThrownBy(() -> arquivos.carregar(chave)).isInstanceOf(Falha.class);
    } finally {
      servidor.stop(0);
    }
  }
}
