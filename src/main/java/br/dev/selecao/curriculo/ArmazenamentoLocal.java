package br.dev.selecao.curriculo;

import br.dev.selecao.comum.Falha;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.core.io.*;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "app.curriculos.modo",
    havingValue = "local",
    matchIfMissing = true)
public class ArmazenamentoLocal implements ArmazenamentoCurriculo {
  private final Path raiz;

  private final DataSize tamanho;

  public ArmazenamentoLocal(
      @Value("${app.uploads}") String diretorio, MultipartProperties multipart) throws IOException {
    tamanho = multipart.getMaxFileSize();
    raiz = Files.createDirectories(Path.of(diretorio).toAbsolutePath().normalize());
  }

  public String salvar(MultipartFile arquivo) {
    String chave = DocumentoPdf.novaChave();
    try {
      var destino = Files.newOutputStream(caminho(chave), StandardOpenOption.CREATE_NEW);
      try (destino) {
        DocumentoPdf.copiar(arquivo, destino, tamanho.toBytes());
      } catch (IOException | RuntimeException ex) {
        remover(chave);
        throw ex;
      }
      return chave;
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public Resource carregar(String chave) {
    var recurso = new FileSystemResource(caminho(chave));
    if (!recurso.exists()) throw Falha.ausente();
    return recurso;
  }

  public void remover(String chave) {
    try {
      Files.deleteIfExists(caminho(chave));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Path caminho(String chave) {
    return raiz.resolve(DocumentoPdf.validarChave(chave));
  }
}
