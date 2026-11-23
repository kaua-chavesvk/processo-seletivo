package br.dev.selecao.auth;

import java.util.Base64;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("prod")
public class Producao {
  @Bean
  static org.springframework.beans.factory.config.BeanFactoryPostProcessor exigirCaPostgres(
      Environment env) {
    return factory -> {
      if (env.getProperty("POSTGRES_SSL_ROOT_CERT", "").isBlank())
        throw new IllegalStateException(
            "Defina POSTGRES_SSL_ROOT_CERT com o caminho da CA do PostgreSQL");
    };
  }

  @Bean
  SmartInitializingSingleton conferirProducao(Environment env, JdbcTemplate jdbc) {
    return () -> {
      String senha = env.getRequiredProperty("spring.datasource.password");
      if (senha.isBlank() || senha.equals("selecao") || senha.equals("postgres"))
        throw new IllegalStateException("Configure uma senha exclusiva para o banco");
      if (Base64.getDecoder().decode(env.getRequiredProperty("app.jwt")).length < 32)
        throw new IllegalStateException("JWT_SECRET requer pelo menos 32 bytes");
      if ((!env.getProperty("server.ssl.enabled", Boolean.class, false)
              && !GatewayVercel.ativo(env))
          || env.getProperty("debug", Boolean.class, false)
          || env.getProperty("trace", Boolean.class, false))
        throw new IllegalStateException("O perfil prod exige TLS e debug/trace desligados");
      Boolean privilegiado =
          jdbc.queryForObject(
              """
select rolsuper or rolcreaterole or rolcreatedb or rolbypassrls
    or has_schema_privilege(current_schema(), 'CREATE')
    or pg_has_role(current_user, (select relowner from pg_class where oid='usuario'::regclass), 'USAGE')
from pg_roles where rolname=current_user
""",
              Boolean.class);
      if (Boolean.TRUE.equals(privilegiado))
        throw new IllegalStateException("A conta da API requer privilégios restritos de execução");
      Boolean tlsBanco =
          jdbc.queryForObject(
              "select ssl from pg_stat_ssl where pid=pg_backend_pid()", Boolean.class);
      if (!Boolean.TRUE.equals(tlsBanco) && !conexaoNeonVerificada(env))
        throw new IllegalStateException("A conexão com o PostgreSQL de produção requer TLS");
    };
  }

  // O proxy do Neon termina o TLS antes do processo que fornece pg_stat_ssl.
  static boolean conexaoNeonVerificada(Environment env) {
    return GatewayVercel.ativo(env)
        && env.getProperty("spring.datasource.url", "")
            .matches("jdbc:postgresql://[a-zA-Z0-9.-]+\\.neon\\.tech(:5432)?/[a-zA-Z0-9_-]+")
        && "verify-full"
            .equals(env.getProperty("spring.datasource.hikari.data-source-properties.sslmode"))
        && !env.getProperty("spring.datasource.hikari.data-source-properties.sslrootcert", "")
            .isBlank();
  }
}
