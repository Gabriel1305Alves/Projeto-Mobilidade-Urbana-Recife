package br.com.embarcai.controller;

import br.com.embarcai.config.ApiException;
import br.com.embarcai.dto.AuthResposta;
import br.com.embarcai.dto.CadastroRequest;
import br.com.embarcai.dto.LoginRequest;
import br.com.embarcai.dto.UsuarioResposta;
import br.com.embarcai.model.Usuario;
import br.com.embarcai.negocio.RegrasNegocio;
import br.com.embarcai.repository.RelatoConfirmacaoRepository;
import br.com.embarcai.repository.RelatoRepository;
import br.com.embarcai.repository.UsuarioRepository;
import br.com.embarcai.security.JwtService;
import br.com.embarcai.security.UsuarioAutenticado;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {
    private static final String AUTOR_ANONIMO = "Passageiro";

    private final UsuarioRepository usuarios;
    private final RelatoRepository relatos;
    private final RelatoConfirmacaoRepository confirmacoes;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(
        UsuarioRepository usuarios,
        RelatoRepository relatos,
        RelatoConfirmacaoRepository confirmacoes,
        PasswordEncoder encoder,
        JwtService jwt
    ) {
        this.usuarios = usuarios;
        this.relatos = relatos;
        this.confirmacoes = confirmacoes;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<AuthResposta> cadastrar(@RequestBody CadastroRequest body) {
        String nome = texto(body == null ? null : body.nome());
        String email = email(body == null ? null : body.email());
        String senha = body == null ? null : body.senha();
        if (nome == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Informa o seu nome.");
        }
        if (email == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Informa um e-mail válido.");
        }
        if (senha == null || senha.length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A senha precisa ter pelo menos 6 caracteres.");
        }
        if (body == null || body.consentimento() == null || !body.consentimento()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "É preciso aceitar o tratamento dos dados para criar a conta.");
        }
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Já existe uma conta com esse e-mail.");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(encoder.encode(senha));
        usuario.setRole(RegrasNegocio.PAPEL_PASSAGEIRO);
        usuario.setConsentiuEm(OffsetDateTime.now());
        usuarios.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(autenticado(usuario));
    }

    @PostMapping("/login")
    public AuthResposta login(@RequestBody LoginRequest body) {
        String email = email(body == null ? null : body.email());
        String senha = body == null ? null : body.senha();
        Usuario usuario = email == null ? null : usuarios.findByEmailIgnoreCase(email).orElse(null);
        if (usuario == null || senha == null || !encoder.matches(senha, usuario.getSenha())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        }
        return autenticado(usuario);
    }

    @GetMapping("/me")
    public UsuarioResposta me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioAutenticado atual)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Faça login para continuar.");
        }
        Usuario usuario = usuarios.findById(atual.id())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Faça login para continuar."));
        return UsuarioResposta.de(usuario);
    }

    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<Void> excluir(Authentication authentication) {
        Usuario usuario = usuarioAutenticado(authentication);
        confirmacoes.deleteByUsuarioId(usuario.getId());
        relatos.findByUsuarioId(usuario.getId()).forEach(relato -> {
            relato.setUsuario(null);
            relato.setAutor(AUTOR_ANONIMO);
            relatos.save(relato);
        });
        usuarios.delete(usuario);
        return ResponseEntity.noContent().build();
    }

    private Usuario usuarioAutenticado(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioAutenticado atual)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Faça login para continuar.");
        }
        return usuarios.findById(atual.id())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Faça login para continuar."));
    }

    private AuthResposta autenticado(Usuario usuario) {
        return new AuthResposta(jwt.emitir(usuario), UsuarioResposta.de(usuario));
    }

    private String texto(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : (limpo.length() > 120 ? limpo.substring(0, 120) : limpo);
    }

    private String email(String valor) {
        String limpo = texto(valor);
        if (limpo == null) return null;
        String email = limpo.toLowerCase();
        if (!email.contains("@") || email.length() < 5) return null;
        return email;
    }
}
