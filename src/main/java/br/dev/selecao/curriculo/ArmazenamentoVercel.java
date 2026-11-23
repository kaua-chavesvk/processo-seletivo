package br.dev.selecao.curriculo;

import br.dev.selecao.comum.Falha;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.core.io.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@ConditionalOnProperty(name = "app.curriculos.modo", havingValue = "vercel-blob")
class ArmazenamentoVercel implements ArmazenamentoCurriculo {
  private static final String API = "https://vercel.com/api/blob";
  private final HttpClient http =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(5))
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();
  private final String token;
  private final String store;
  private final long limite;
  private final ObjectMapper json;

  ArmazenamentoVercel(
      @Value("${BLOB_READ_WRITE_TOKEN}") String token,
      @Value("${BLOB_STORE_ID}") String store,
      MultipartProperties multipart,
      ObjectMapper json) {
    this.store = store.replaceFirst("^store_", "");
    if (!this.store.matches("[A-Za-z0-9]+") || token.isBlank())
      throw new IllegalStateException("Configure a identidade do armazenamento privado");
    this.token = token;
    this.limite = multipart.getMaxFileSize().toBytes();
    this.json = json;
  }

  private String caminho(String chave) {
    return "curriculos/" + DocumentoPdf.validarChave(chave);
  }

  private URI privado(String chave) {
    return URI.create(
        "https://"
            + store.toLowerCase(Locale.ROOT)
            + ".private.blob.vercel-storage.com/"
            + caminho(chave));
  }

  private HttpRequest.Builder pedido(URI uri) {
    return HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(30))
        .header("Authorization", "Bearer " + token);
  }

  private HttpRequest.Builder api(String path) {
    return pedido(URI.create(API + path))
        .header("x-api-version", "13")
        .header("x-vercel-blob-store-id", store);
  }

  private <T> HttpResponse<T> enviar(HttpRequest.Builder req, HttpResponse.BodyHandler<T> tipo) {
    try {
      return http.send(req.build(), tipo);
    } catch (IOException e) {
      throw new UncheckedIOException("Falha de comunicação com o armazenamento", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Operação interrompida", e);
    }
  }

  private void conferir(int status) {
    if (status == 404) throw Falha.ausente();
    if (status / 100 != 2) throw new IllegalStateException("Armazenamento retornou HTTP " + status);
  }

  public String salvar(MultipartFile arquivo) {
    String chave = DocumentoPdf.novaChave();
    var bytes = new ByteArrayOutputStream();
    try {
      DocumentoPdf.copiar(arquivo, bytes, limite);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    String nome = URLEncoder.encode(caminho(chave), StandardCharsets.UTF_8);
    var req =
        api("/?pathname=" + nome)
            .header("x-vercel-blob-access", "private")
            .header("x-content-type", "application/pdf")
            .header("x-add-random-suffix", "0")
            .header("x-allow-overwrite", "0")
            .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray()));
    conferir(enviar(req, HttpResponse.BodyHandlers.discarding()).statusCode());
    return chave;
  }

  public Resource carregar(String chave) {
    var res = enviar(pedido(privado(chave)).GET(), HttpResponse.BodyHandlers.ofInputStream());
    if (res.statusCode() / 100 != 2) {
      try (var corpo = res.body()) {
        conferir(res.statusCode());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return new InputStreamResource(res.body());
  }

  public void remover(String chave) {
    try {
      var body = json.writeValueAsBytes(Map.of("urls", List.of(privado(chave).toString())));
      var req =
          api("/delete")
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofByteArray(body));
      conferir(enviar(req, HttpResponse.BodyHandlers.discarding()).statusCode());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
