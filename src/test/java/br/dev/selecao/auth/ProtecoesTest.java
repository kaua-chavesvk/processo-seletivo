package br.dev.selecao.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.dev.selecao.comum.Banco;
import br.dev.selecao.comum.Falha;
import br.dev.selecao.usuario.Usuario;
import br.dev.selecao.usuario.UsuarioRepo;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class ProtecoesTest {
  @Test
  void verificaSenhaMesmoParaContaAusenteOuInativa() {
    var encoder = mock(PasswordEncoder.class);
    var usuarios = mock(UsuarioRepo.class);
    var tokens = mock(Tokens.class);
    when(encoder.encode(anyString())).thenReturn("hash-ficticio");
    var auth = new AuthApi(mock(Banco.class), tokens, usuarios, encoder);
    var dados = new AuthApi.Login("demo@example.invalid", "senha-de-teste");

    when(usuarios.bloquearPorEmail(dados.email())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> auth.login(dados)).isInstanceOf(Falha.class);
    verify(encoder).matches(dados.senha(), "hash-ficticio");

    var inativo = mock(Usuario.class);
    when(inativo.ativo()).thenReturn(false);
    when(inativo.senhaHash()).thenReturn("hash-da-conta");
    when(usuarios.bloquearPorEmail(dados.email())).thenReturn(Optional.of(inativo));
    when(encoder.matches(dados.senha(), "hash-da-conta")).thenReturn(true);
    assertThatThrownBy(() -> auth.login(dados)).isInstanceOf(Falha.class);
    verify(encoder).matches(dados.senha(), "hash-da-conta");
    verifyNoInteractions(tokens);
  }

  @Test
  void reposicaoDeAutenticacaoEhGradual() {
    var limite = new LimiteRequisicoes();
    long agora = TimeUnit.SECONDS.toNanos(59);
    for (int i = 0; i < 20; i++) assertThat(limite.permitir("a", true, agora)).isTrue();
    assertThat(limite.permitir("a", true, agora + TimeUnit.SECONDS.toNanos(1))).isFalse();
    assertThat(limite.permitir("a", true, agora + TimeUnit.SECONDS.toNanos(3))).isTrue();
    assertThat(limite.permitir("a", true, agora + TimeUnit.SECONDS.toNanos(3))).isFalse();
  }

  @Test
  void producaoRejeitaConfiguracaoInseguraAntesDeAceitarTrafego() {
    var env = new MockEnvironment().withProperty("spring.datasource.password", "selecao");
    var jdbc = mock(JdbcTemplate.class);
    var verificar = new Producao().conferirProducao(env, jdbc);
    assertThatThrownBy(verificar::afterSingletonsInstantiated)
        .hasMessageContaining("senha exclusiva");
    env.setProperty("spring.datasource.password", "somente-para-teste");
    env.setProperty("app.jwt", java.util.Base64.getEncoder().encodeToString(new byte[32]));
    assertThatThrownBy(verificar::afterSingletonsInstantiated).hasMessageContaining("TLS");
    env.setProperty("server.ssl.enabled", "true");
    when(jdbc.queryForObject(contains("from pg_roles"), eq(Boolean.class))).thenReturn(true);
    assertThatThrownBy(verificar::afterSingletonsInstantiated).hasMessageContaining("privilégios");
    when(jdbc.queryForObject(contains("from pg_roles"), eq(Boolean.class))).thenReturn(false);
    assertThatThrownBy(verificar::afterSingletonsInstantiated).hasMessageContaining("PostgreSQL");
    when(jdbc.queryForObject(contains("from pg_stat_ssl"), eq(Boolean.class))).thenReturn(true);
    assertThatCode(verificar::afterSingletonsInstantiated).doesNotThrowAnyException();
  }

  @Test
  void migraBCryptDepoisDeVerificarASenha() {
    var senhas = new Seguranca().senhas();
    String antigo = new BCryptPasswordEncoder().encode("senha-de-teste");
    assertThat(senhas.matches("senha-de-teste", antigo)).isTrue();
    assertThat(senhas.upgradeEncoding(antigo)).isTrue();
    String novo = senhas.encode("senha-de-teste");
    assertThat(novo).startsWith("{argon2id}$argon2id$");
    assertThat(senhas.matches("senha-incorreta", novo)).isFalse();
    assertThat(senhas.upgradeEncoding(novo)).isFalse();
  }

  @Test
  void limitaPorClienteERenovaAJanela() {
    var limite = new LimiteRequisicoes();
    long agora = System.nanoTime();
    for (int i = 0; i < 20; i++) assertThat(limite.permitir("a", true, agora)).isTrue();
    assertThat(limite.permitir("a", true, agora)).isFalse();
    assertThat(limite.permitir("b", true, agora)).isTrue();
    for (int i = 20; i < 180; i++) assertThat(limite.permitir("a", false, agora)).isTrue();
    assertThat(limite.permitir("a", false, agora)).isFalse();
    assertThat(limite.permitir("a", true, agora + TimeUnit.MINUTES.toNanos(1))).isTrue();
  }
}
