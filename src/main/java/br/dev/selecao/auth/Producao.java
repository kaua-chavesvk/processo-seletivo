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
    SmartInitializingSingleton conferirProducao(Environment env, JdbcTemplate jdbc) {
        return () -> {
            String senha = env.getRequiredProperty("spring.datasource.password");
            if (senha.isBlank() || senha.equals("selecao") || senha.equals("postgres"))
                throw new IllegalStateException("Configure uma senha exclusiva para o banco");
            if (Base64.getDecoder().decode(env.getRequiredProperty("app.jwt")).length < 32)
                throw new IllegalStateException("JWT_SECRET requer pelo menos 32 bytes");
            if (!env.getProperty("server.ssl.enabled", Boolean.class, false)
                    || env.getProperty("debug", Boolean.class, false) || env.getProperty("trace", Boolean.class, false))
                throw new IllegalStateException("O perfil prod exige TLS e debug/trace desligados");
            Boolean privilegiado = jdbc.queryForObject("""
                select rolsuper or rolcreaterole or rolcreatedb or rolbypassrls
                    or has_schema_privilege(current_schema(), 'CREATE')
                    or pg_has_role(current_user, (select relowner from pg_class where oid='usuario'::regclass), 'USAGE')
                from pg_roles where rolname=current_user
                """, Boolean.class);
            if (Boolean.TRUE.equals(privilegiado))
                throw new IllegalStateException("A conta da API requer privilégios restritos de execução");
            Boolean tlsBanco = jdbc.queryForObject(
                "select ssl from pg_stat_ssl where pid=pg_backend_pid()", Boolean.class);
            if (!Boolean.TRUE.equals(tlsBanco))
                throw new IllegalStateException("A conexão com o PostgreSQL de produção requer TLS");
        };
    }
}
