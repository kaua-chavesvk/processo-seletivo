package br.dev.selecao.curriculo;

import br.dev.selecao.comum.Falha;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.web.multipart.MultipartFile;

final class DocumentoPdf {
  private static final Pattern CHAVE =
      Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf");
  private static final byte[] ASSINATURA = "%PDF".getBytes(StandardCharsets.US_ASCII);

  private DocumentoPdf() {}

  static String novaChave() {
    return UUID.randomUUID() + ".pdf";
  }

  static String validarChave(String chave) {
    if (chave == null || !CHAVE.matcher(chave).matches()) throw Falha.ausente();
    return chave;
  }

  static void copiar(MultipartFile arquivo, OutputStream destino, long limite) throws IOException {
    if (!"application/pdf".equalsIgnoreCase(arquivo.getContentType()) || arquivo.getSize() > limite)
      throw Falha.invalida("Envie um PDF de até " + limiteFormatado(limite));
    try (var entrada = arquivo.getInputStream()) {
      byte[] prefixo = entrada.readNBytes(ASSINATURA.length);
      if (!Arrays.equals(prefixo, ASSINATURA)) throw Falha.invalida("Conteúdo PDF inválido");
      long total = prefixo.length;
      if (total > limite) throw Falha.invalida("PDF excede o limite de tamanho");
      destino.write(prefixo);
      byte[] buffer = new byte[8192];
      int lidos;
      while ((lidos = entrada.read(buffer)) != -1) {
        total += lidos;
        if (total > limite) throw Falha.invalida("PDF excede o limite de tamanho");
        destino.write(buffer, 0, lidos);
      }
    }
  }

  private static String limiteFormatado(long bytes) {
    if (bytes % (1024 * 1024) == 0) return bytes / (1024 * 1024) + " MB";
    if (bytes % 1024 == 0) return bytes / 1024 + " KB";
    return bytes + " bytes";
  }
}
