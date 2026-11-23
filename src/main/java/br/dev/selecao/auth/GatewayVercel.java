package br.dev.selecao.auth;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration
@Profile("vercel")
class GatewayVercel {
  static boolean ativo(Environment ambiente) {
    return ambiente.matchesProfiles("prod & vercel")
        && "1".equals(ambiente.getProperty("VERCEL"))
        && "production".equals(ambiente.getProperty("VERCEL_ENV"));
  }

  @Bean
  WebServerFactoryCustomizer<TomcatServletWebServerFactory> origemPublica(Environment ambiente) {
    if (!ativo(ambiente))
      throw new IllegalStateException("O perfil vercel exige produção no gateway da Vercel");
    String host = ambiente.getRequiredProperty("APP_PUBLIC_HOST");
    if (!host.matches("[a-z0-9-]+(?:\\.[a-z0-9-]+)+"))
      throw new IllegalStateException("APP_PUBLIC_HOST deve conter o hostname público");
    return fabrica ->
        fabrica.addConnectorCustomizers(
            conector -> {
              conector.setScheme("https");
              conector.setSecure(true);
              conector.setProxyName(host);
              conector.setProxyPort(443);
            });
  }
}
