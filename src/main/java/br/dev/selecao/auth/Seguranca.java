package br.dev.selecao.auth;

import br.dev.selecao.usuario.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class Seguranca {
  @Bean
  PasswordEncoder senhas() {
    var argon = new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    var encoder = new DelegatingPasswordEncoder("argon2id", Map.of("argon2id", argon));
    encoder.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
    return encoder;
  }

  @Bean
  SecurityFilterChain filtros(HttpSecurity http, Tokens tokens, UsuarioRepo usuarios)
      throws Exception {
    var filtro = new FiltroSeguranca(tokens, usuarios);
    return http.csrf(c -> c.disable())
        .requestCache(c -> c.disable())
        .logout(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(
            h ->
                h.contentSecurityPolicy(
                        p ->
                            p.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self'"
                                    + " 'unsafe-inline'; img-src 'self' data:; object-src 'none';"
                                    + " frame-ancestors 'none'; base-uri 'none'; form-action"
                                    + " 'self'"))
                    .referrerPolicy(
                        p ->
                            p.policy(
                                org.springframework.security.web.header.writers
                                    .ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/auth/**",
                        "/actuator/health",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/error")
                    .permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/vagas", "/vagas/*")
                    .permitAll()
                    .requestMatchers("/usuarios/me")
                    .hasRole("CANDIDATO")
                    .requestMatchers("/usuarios", "/usuarios/**", "/empresas", "/empresas/*")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> negar(res, (String) req.getAttribute("correlationId")))
                    .accessDeniedHandler(
                        (req, res, ex) ->
                            falha(
                                res, (String) req.getAttribute("correlationId"), 403, "Forbidden")))
        .addFilterBefore(filtro, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  static void negar(HttpServletResponse res, String id) throws IOException {
    falha(res, id, 401, "Unauthorized");
  }

  static void falha(HttpServletResponse res, String id, int status, String titulo)
      throws IOException {
    org.slf4j.LoggerFactory.getLogger(Seguranca.class)
        .warn("correlationId={} status={}", id, status);
    res.setStatus(status);
    res.setContentType("application/problem+json");
    res.setCharacterEncoding("UTF-8");
    res.getWriter()
        .printf(
            "{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"correlationId\":\"%s\"}",
            titulo, status, id);
  }
}
