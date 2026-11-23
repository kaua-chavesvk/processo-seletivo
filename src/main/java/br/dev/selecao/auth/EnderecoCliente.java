package br.dev.selecao.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class EnderecoCliente {
  private final Set<String> proxies;

  EnderecoCliente(@Value("${app.proxies-confiaveis:}") String configuracao) {
    proxies =
        Arrays.stream(configuracao.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(
                s -> {
                  var ip = literal(s);
                  if (ip == null)
                    throw new IllegalArgumentException(
                        "APP_PROXIES_CONFIAVEIS exige endereços IP literais");
                  return ip;
                })
            .collect(Collectors.toUnmodifiableSet());
  }

  String identificar(HttpServletRequest request) {
    String remoto = request.getRemoteAddr();
    if (proxies.isEmpty()) return remoto;
    String origem = literal(remoto);
    if (origem == null || !proxies.contains(origem)) return remoto;
    var valores = request.getHeaders("X-Real-IP");
    if (!valores.hasMoreElements()) return remoto;
    String cliente = literal(valores.nextElement());
    return cliente != null && !valores.hasMoreElements() ? cliente : remoto;
  }

  private static String literal(String valor) {
    if (valor == null || valor.length() > 45) return null;
    // Limita a literais antes de chamar o parser de IP para evitar resolução DNS.
    boolean v6 = valor.indexOf(':') >= 0 && valor.matches("[a-fA-F0-9:.]+");
    if (!v6 && !valor.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) return null;
    try {
      return InetAddress.getByName(valor).getHostAddress();
    } catch (UnknownHostException e) {
      return null;
    }
  }
}
