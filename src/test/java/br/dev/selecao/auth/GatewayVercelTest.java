package br.dev.selecao.auth;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

class GatewayVercelTest {
  @Test
  void exigePerfilDeProducaoEVerificacaoDoCertificadoNeon() {
    var env =
        new MockEnvironment()
            .withProperty("VERCEL", "1")
            .withProperty("VERCEL_ENV", "production")
            .withProperty("spring.datasource.url", "jdbc:postgresql://teste.neon.tech/trilha")
            .withProperty("spring.datasource.hikari.data-source-properties.sslmode", "verify-full")
            .withProperty(
                "spring.datasource.hikari.data-source-properties.sslrootcert",
                "/etc/ssl/certs/ca-certificates.crt");
    assertThat(GatewayVercel.ativo(env)).isFalse();
    env.setActiveProfiles("prod", "vercel");
    assertThat(Producao.conexaoNeonVerificada(env)).isTrue();
    env.setProperty(
        "spring.datasource.url", "jdbc:postgresql://teste.neon.tech/trilha?sslmode=disable");
    assertThat(Producao.conexaoNeonVerificada(env)).isFalse();
    env.setProperty("VERCEL_ENV", "development");
    assertThat(GatewayVercel.ativo(env)).isFalse();
  }

  @Test
  void usaIpDoGatewayApenasNoContainerGerenciado() {
    var env =
        new MockEnvironment().withProperty("VERCEL", "1").withProperty("VERCEL_ENV", "production");
    var origem = new EnderecoCliente("");
    var req = new MockHttpServletRequest();
    req.setRemoteAddr("192.0.2.1");
    req.addHeader("X-Vercel-Forwarded-For", "192.0.2.42");
    origem.configurarGateway(env);
    assertThat(origem.identificar(req)).isEqualTo("192.0.2.1");
    env.setActiveProfiles("prod", "vercel");
    origem.configurarGateway(env);
    assertThat(origem.identificar(req)).isEqualTo("192.0.2.42");
  }
}
