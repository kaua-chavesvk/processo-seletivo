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
public class ArmazenamentoLocal implements ArmazenamentoCurriculo {
  private final Path raiz;

  private final DataSize tamanho;

  public ArmazenamentoLocal(
      @Value("${app.uploads}") String diretorio, MultipartProperties multipart) throws IOException {
    tamanho = multipart.getMaxFileSize();
    raiz = Files.createDirectories(Path.of(diretorio).toAbsolutePath().normalize());
  }

  public String salvar(MultipartFile arquivo) {
    long limite = tamanho.toBytes();
    if (!"application/pdf".equalsIgnoreCase(arquivo.getContentType()) || arquivo.getSize() > limite)
      throw Falha.invalida("Envie um PDF de até " + limiteFormatado(limite));
    String chave = UUID.randomUUID() + ".pdf";
    try (var entrada = arquivo.getInputStream()) {
      byte[] prefixo = entrada.readNBytes(4);
      if (!Arrays.equals(prefixo, "%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
        throw Falha.invalida("Conteúdo PDF inválido");
      var arquivoNovo = Files.newOutputStream(caminho(chave), StandardOpenOption.CREATE_NEW);
      try (var saida = arquivoNovo) {
        saida.write(prefixo);
        long total = prefixo.length;
        byte[] buffer = new byte[8192];
        int lidos;
        while ((lidos = entrada.read(buffer)) != -1) {
          total += lidos;
          if (total > limite) throw Falha.invalida("PDF excede o limite de tamanho");
          saida.write(buffer, 0, lidos);
        }
      } catch (IOException | RuntimeException ex) {
        remover(chave);
        throw ex;
      }
      return chave;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String limiteFormatado(long bytes) {
    if (bytes % (1024 * 1024) == 0) return bytes / (1024 * 1024) + " MB";
    if (bytes % 1024 == 0) return bytes / 1024 + " KB";
    return bytes + " bytes";
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
    if (!chave.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf"))
      throw Falha.ausente();
    return raiz.resolve(chave);
  }
}
