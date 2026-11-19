package br.dev.selecao.auth;

import br.dev.selecao.usuario.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
@Configuration
public class Bootstrap {
    @Bean ApplicationRunner admin(Contas contas, UsuarioRepo usuarios,
        @Value("${app.admin-email}") String email,@Value("${app.admin-password}") String senha) {
        return args -> {
            if (email.isBlank() && senha.isBlank()) return;
            if (!email.contains("@") || senha.length()<8) throw new IllegalStateException("Configure email e senha de administrador válidos");
            if (usuarios.findByEmailIgnoreCase(email).isEmpty()) contas.criar("Administrador",email,senha,Papel.ADMIN,null);
        };
    }
}
