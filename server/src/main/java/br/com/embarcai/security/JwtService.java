package br.com.embarcai.security;

import br.com.embarcai.model.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final Algorithm algorithm;
    private final long expiracaoHoras;

    public JwtService(
        @Value("${app.jwt-secret}") String secret,
        @Value("${app.jwt-expiracao-horas:72}") long expiracaoHoras
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expiracaoHoras = expiracaoHoras;
    }

    public String emitir(Usuario usuario) {
        Instant agora = Instant.now();
        return JWT.create()
            .withIssuer("embarcai")
            .withSubject(String.valueOf(usuario.getId()))
            .withClaim("nome", usuario.getNome())
            .withClaim("email", usuario.getEmail())
            .withClaim("role", usuario.getRole())
            .withIssuedAt(Date.from(agora))
            .withExpiresAt(Date.from(agora.plus(expiracaoHoras, ChronoUnit.HOURS)))
            .sign(algorithm);
    }

    public UsuarioAutenticado ler(String token) {
        DecodedJWT jwt = JWT.require(algorithm).withIssuer("embarcai").build().verify(token);
        return new UsuarioAutenticado(
            Long.valueOf(jwt.getSubject()),
            jwt.getClaim("nome").asString(),
            jwt.getClaim("email").asString(),
            jwt.getClaim("role").asString()
        );
    }
}
