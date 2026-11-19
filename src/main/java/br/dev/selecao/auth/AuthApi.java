package br.dev.selecao.auth;

import br.dev.selecao.comum.*;
import br.dev.selecao.usuario.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
@RestController
@RequestMapping("/auth")
@Transactional
public class AuthApi {
    public record Login(@NotBlank @Email @Size(max=255) String email,@NotBlank @Size(max=72) String senha) {}
    public record Renovacao(@NotBlank @Size(max=256) String refreshToken) {}
    public record Sessao(String accessToken,String refreshToken,long expiresIn) {}
    private final Banco banco;
    private final Tokens tokens;
    private final UsuarioRepo usuarios;
    private final PasswordEncoder encoder;
    private final SecureRandom random=new SecureRandom();
    public AuthApi(Banco banco,Tokens tokens,UsuarioRepo usuarios,PasswordEncoder encoder) {
        this.banco=banco; this.tokens=tokens; this.usuarios=usuarios; this.encoder=encoder;
    }
    @PostMapping("/login")
    public Sessao login(@Valid @RequestBody Login d) {
        var id=usuarios.findByEmailIgnoreCase(d.email).orElseThrow(Falha::credencial).id;
        var u=banco.bloquear(Usuario.class,id);
        if (!u.ativo || !encoder.matches(d.senha,u.senhaHash)) throw Falha.credencial();
        if (encoder.upgradeEncoding(u.senhaHash)) u.senhaHash = encoder.encode(d.senha);
        return emitir(u.id);
    }
    @PostMapping("/refresh")
    public Sessao renovar(@Valid @RequestBody Renovacao d) { return emitir(consumir(d.refreshToken)); }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sair(@Valid @RequestBody Renovacao d) { consumir(d.refreshToken); }
    private UUID consumir(String token) {
        String hash=hash(token);
        var id=banco.em.createQuery("select r.usuarioId from Refresh r where r.hashToken=:h",UUID.class)
            .setParameter("h",hash).getResultStream().findFirst().orElseThrow(Falha::credencial);
        if (!banco.bloquear(Usuario.class,id).ativo) throw Falha.credencial();
        int usados=banco.em.createQuery("update Refresh set revogado=true where hashToken=:h and revogado=false and expiraEm>:agora")
            .setParameter("h",hash).setParameter("agora",Instant.now()).executeUpdate();
        if (usados!=1) throw Falha.credencial();
        return id;
    }
    private Sessao emitir(UUID id) {
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        String valor=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Refresh r=new Refresh(); r.usuarioId=id; r.hashToken=hash(valor); r.expiraEm=Instant.now().plusSeconds(30L*86400);
        banco.inserir(r); return new Sessao(tokens.emitir(id),valor,900);
    }
    private String hash(String valor) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
