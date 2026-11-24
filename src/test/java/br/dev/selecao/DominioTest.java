package br.dev.selecao;

import static br.dev.selecao.candidatura.Etapa.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.dev.selecao.candidatura.*;
import br.dev.selecao.comum.Falha;
import br.dev.selecao.curriculo.ArmazenamentoLocal;
import br.dev.selecao.empresa.Cnpj;
import br.dev.selecao.usuario.Papel;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

class DominioTest {
  private MultipartProperties configuracaoPdf(long limite) {
    var propriedades = new MultipartProperties();
    propriedades.setMaxFileSize(DataSize.ofBytes(limite));
    return propriedades;
  }

  @Test
  void informaLimiteConfigurado(@TempDir Path raiz) throws Exception {
    var local = new ArmazenamentoLocal(raiz.toString(), configuracaoPdf(1536));
    var arquivo = mock(MultipartFile.class);
    assertThatThrownBy(() -> local.salvar(arquivo)).hasMessage("Envie um PDF de até 1536 bytes");
  }

  @Test
  void matrizCompletaDoFunil() {
    record Transicao(Papel papel, Etapa origem, Etapa destino) {}
    var permitidas = Set.of(
        new Transicao(Papel.RECRUTADOR, TRIAGEM, ENTREVISTA),
        new Transicao(Papel.RECRUTADOR, TRIAGEM, REPROVADO),
        new Transicao(Papel.RECRUTADOR, ENTREVISTA, TESTE_TECNICO),
        new Transicao(Papel.RECRUTADOR, ENTREVISTA, REPROVADO),
        new Transicao(Papel.RECRUTADOR, TESTE_TECNICO, PROPOSTA),
        new Transicao(Papel.RECRUTADOR, TESTE_TECNICO, REPROVADO),
        new Transicao(Papel.RECRUTADOR, PROPOSTA, CONTRATADO),
        new Transicao(Papel.RECRUTADOR, PROPOSTA, REPROVADO),
        new Transicao(Papel.CANDIDATO, TRIAGEM, DESISTIU),
        new Transicao(Papel.CANDIDATO, ENTREVISTA, DESISTIU),
        new Transicao(Papel.CANDIDATO, TESTE_TECNICO, DESISTIU),
        new Transicao(Papel.CANDIDATO, PROPOSTA, DESISTIU));
    for (var papel : Papel.values()) {
      for (var origem : Etapa.values()) {
        for (var destino : Etapa.values()) {
          var caso = new Transicao(papel, origem, destino);
          var resultado = catchThrowable(() -> Funil.validar(origem, destino, papel));
          if (permitidas.contains(caso)) assertThat(resultado).as("%s", caso).isNull();
          else assertThat(resultado).as("%s", caso).isInstanceOf(TransicaoInvalida.class);
        }
      }
    }
  }

  @Test
  void cnpj() {
    assertThat(Cnpj.valido("12.ABC.345/01DE-35")).isTrue();
    assertThat(Cnpj.valido("04.252.011/0001-10")).isTrue();
    assertThat(Cnpj.valido("04.252.011/0001-11")).isFalse();
    assertThat(Cnpj.valido("00000000000000")).isFalse();
  }

  @Test
  void removeArquivoParcialQuandoConteudoUltrapassaLimite(@TempDir Path raiz) throws Exception {
    var arquivo = mock(MultipartFile.class);
    when(arquivo.getContentType()).thenReturn("application/pdf");
    byte[] conteudo = new byte[5 * 1024 * 1024 + 1];
    System.arraycopy(
        "%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII), 0, conteudo, 0, 4);
    when(arquivo.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(conteudo));
    assertThatThrownBy(
            () ->
                new ArmazenamentoLocal(raiz.toString(), configuracaoPdf(5 * 1024 * 1024))
                    .salvar(arquivo))
        .isInstanceOf(Falha.class);
    assertThat(raiz).isEmptyDirectory();
  }

  @Test
  void rejeitaArquivoGrandeAntesDeLer(@TempDir Path raiz) throws Exception {
    var local = new ArmazenamentoLocal(raiz.toString(), configuracaoPdf(5 * 1024 * 1024));
    var arquivo = mock(MultipartFile.class);
    when(arquivo.getContentType()).thenReturn("application/pdf");
    when(arquivo.getSize()).thenReturn(5L * 1024 * 1024 + 1);
    assertThatThrownBy(() -> local.salvar(arquivo)).isInstanceOf(Falha.class);
    verify(arquivo, never()).getInputStream();
  }
}
