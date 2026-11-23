package br.dev.selecao.auth;

import br.dev.selecao.usuario.UsuarioRepo;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

final class FiltroSeguranca extends OncePerRequestFilter {
  private final LimiteRequisicoes limite = new LimiteRequisicoes();
  private final Tokens tokens;
  private final UsuarioRepo usuarios;

  private final EnderecoCliente endereco;

  FiltroSeguranca(Tokens tokens, UsuarioRepo usuarios, EnderecoCliente endereco) {
    this.endereco = endereco;
    this.tokens = tokens;
    this.usuarios = usuarios;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String id = UUID.randomUUID().toString();
    req.setAttribute("correlationId", id);
    res.setHeader("X-Correlation-ID", id);
    String rota = req.getServletPath();
    if (rota.isEmpty()) rota = req.getRequestURI().substring(req.getContextPath().length());
    boolean abrirInicio =
        "/".equals(rota)
            && "GET".equals(req.getMethod())
            && "navigate".equals(req.getHeader("Sec-Fetch-Mode"))
            && "document".equals(req.getHeader("Sec-Fetch-Dest"));
    if (CorsUtils.isCorsRequest(req)
        || (!abrirInicio && "cross-site".equals(req.getHeader("Sec-Fetch-Site")))) {
      Seguranca.falha(res, id, 403, "Origem não permitida");
      return;
    }
    if (!limite.permitir(endereco.identificar(req), rota.startsWith("/auth/"), System.nanoTime())) {
      res.setHeader("Retry-After", "60");
      Seguranca.falha(res, id, 429, "Limite de requisições atingido");
      return;
    }
    String header = req.getHeader("Authorization");
    if (header != null) {
      try {
        if (!header.startsWith("Bearer ")) throw new IllegalArgumentException();
        var u =
            usuarios
                .findById(tokens.ler(header.substring(7)))
                .filter(x -> x.isAtivo())
                .orElseThrow();
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    u, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getPapel().name()))));
      } catch (io.jsonwebtoken.JwtException
          | IllegalArgumentException
          | NoSuchElementException ex) {
        Seguranca.negar(res, id);
        return;
      }
    }
    chain.doFilter(req, res);
  }
}
