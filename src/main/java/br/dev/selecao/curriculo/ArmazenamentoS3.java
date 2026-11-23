package br.dev.selecao.curriculo;

import br.dev.selecao.comum.Falha;
import java.io.*;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.core.io.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Component
@ConditionalOnProperty(name = "app.curriculos.modo", havingValue = "s3")
public class ArmazenamentoS3 implements ArmazenamentoCurriculo {
  private final S3Client cliente;
  private final String bucket;
  private final long limite;

  public ArmazenamentoS3(
      S3Client cliente,
      @Value("${app.curriculos.s3.bucket}") String bucket,
      MultipartProperties multipart) {
    if (bucket.isBlank())
      throw new IllegalArgumentException("Configure o bucket privado dos currículos");
    this.cliente = cliente;
    this.bucket = bucket;
    this.limite = multipart.getMaxFileSize().toBytes();
  }

  public String salvar(MultipartFile arquivo) {
    String chave = DocumentoPdf.novaChave();
    try {
      Path temporario = Files.createTempFile("curriculo-", ".pdf");
      try {
        try (var saida = Files.newOutputStream(temporario)) {
          DocumentoPdf.copiar(arquivo, saida, limite);
        }
        cliente.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(objeto(chave))
                .contentType("application/pdf")
                .build(),
            RequestBody.fromFile(temporario));
        return chave;
      } finally {
        Files.deleteIfExists(temporario);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public Resource carregar(String chave) {
    try {
      var resposta =
          cliente.getObject(GetObjectRequest.builder().bucket(bucket).key(objeto(chave)).build());
      return new InputStreamResource(resposta);
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) throw Falha.ausente();
      throw ex;
    }
  }

  public void remover(String chave) {
    cliente.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objeto(chave)).build());
  }

  private String objeto(String chave) {
    return "curriculos/" + DocumentoPdf.validarChave(chave);
  }
}
