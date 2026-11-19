package br.dev.selecao.auth;

import br.dev.selecao.usuario.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.cors.CorsUtils;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.*;
import java.util.*;
@Configuration
public class Seguranca {
    @Bean PasswordEncoder senhas() {
        var argon = new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
        var encoder = new DelegatingPasswordEncoder("argon2id", Map.of("argon2id", argon));
        encoder.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
        return encoder;
    }
    @Bean SecurityFilterChain filtros(HttpSecurity http, Tokens tokens, UsuarioRepo usuarios) throws Exception {
        var limite = new LimiteRequisicoes();
        var filtro = new OncePerRequestFilter() {
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
                String id = UUID.randomUUID().toString();
                req.setAttribute("correlationId",id);
                res.setHeader("X-Correlation-ID",id);
                String rota = req.getServletPath();
                if (rota.isEmpty()) rota = req.getRequestURI().substring(req.getContextPath().length());
                if (CorsUtils.isCorsRequest(req) || "cross-site".equals(req.getHeader("Sec-Fetch-Site"))) {
                    falha(res,id,403,"Origem não permitida"); return;
                }
                if (!limite.permitir(req.getRemoteAddr(), rota.startsWith("/auth/"), System.nanoTime())) {
                    res.setHeader("Retry-After","60");
                    falha(res,id,429,"Limite de requisições atingido"); return;
                }
                String header = req.getHeader("Authorization");
                if (header != null) {
                    try {
                        if (!header.startsWith("Bearer ")) throw new IllegalArgumentException();
                        var u = usuarios.findById(tokens.ler(header.substring(7))).filter(x -> x.ativo).orElseThrow();
                        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u,null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + u.papel.name()))));
                    } catch (io.jsonwebtoken.JwtException | IllegalArgumentException | NoSuchElementException ex) {
                        negar(res,id); return;
                    }
                }
                chain.doFilter(req,res);
            }
        };
        return http.csrf(c -> c.disable()).requestCache(c -> c.disable()).logout(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.contentSecurityPolicy(p -> p.policyDirectives(
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; object-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'"))
                .referrerPolicy(p -> p.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
            .authorizeHttpRequests(a -> a.requestMatchers("/auth/**","/swagger-ui.html","/swagger-ui/**","/v3/api-docs/**","/error").permitAll()
            .requestMatchers(org.springframework.http.HttpMethod.GET,"/vagas","/vagas/*").permitAll()
            .requestMatchers("/usuarios/me").hasRole("CANDIDATO")
            .requestMatchers("/usuarios","/usuarios/**","/empresas","/empresas/*").hasRole("ADMIN")
            .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> negar(res,(String)req.getAttribute("correlationId")))
                .accessDeniedHandler((req,res,ex) -> falha(res,(String)req.getAttribute("correlationId"),403,"Forbidden")))
            .addFilterBefore(filtro, UsernamePasswordAuthenticationFilter.class).build();
    }
    private static void negar(HttpServletResponse res,String id) throws IOException {
        falha(res,id,401,"Unauthorized");
    }
    private static void falha(HttpServletResponse res,String id,int status,String titulo) throws IOException {
        org.slf4j.LoggerFactory.getLogger(Seguranca.class).warn("correlationId={} status={}",id,status);
        res.setStatus(status); res.setContentType("application/problem+json"); res.setCharacterEncoding("UTF-8");
        res.getWriter().printf("{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"correlationId\":\"%s\"}",titulo,status,id);
    }
}
