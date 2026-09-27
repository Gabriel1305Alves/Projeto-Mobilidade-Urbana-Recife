package br.com.embarcai.controller;

import br.com.embarcai.config.ApiException;
import br.com.embarcai.dto.NovoRelatoRequest;
import br.com.embarcai.dto.RelatoResposta;
import br.com.embarcai.dto.SaudeResposta;
import br.com.embarcai.dto.StatsResposta;
import br.com.embarcai.model.Linha;
import br.com.embarcai.model.Relato;
import br.com.embarcai.model.RelatoConfirmacao;
import br.com.embarcai.model.StatusLinha;
import br.com.embarcai.model.Usuario;
import br.com.embarcai.negocio.RegrasNegocio;
import br.com.embarcai.repository.LinhaRepository;
import br.com.embarcai.repository.RelatoConfirmacaoRepository;
import br.com.embarcai.repository.RelatoRepository;
import br.com.embarcai.repository.UsuarioRepository;
import br.com.embarcai.security.UsuarioAutenticado;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RelatoController {
    private final LinhaRepository linhas;
    private final RelatoRepository relatos;
    private final RelatoConfirmacaoRepository confirmacoes;
    private final UsuarioRepository usuarios;

    public RelatoController(
        LinhaRepository linhas,
        RelatoRepository relatos,
        RelatoConfirmacaoRepository confirmacoes,
        UsuarioRepository usuarios
    ) {
        this.linhas = linhas;
        this.relatos = relatos;
        this.confirmacoes = confirmacoes;
        this.usuarios = usuarios;
    }

    @GetMapping("/saude")
    public SaudeResposta saude() {
        relatos.count();
        return new SaudeResposta(true);
    }

    @GetMapping("/stats")
    public StatsResposta stats() {
        OffsetDateTime hoje = LocalDate.now(ZoneOffset.UTC).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime semana = OffsetDateTime.now().minusDays(7);
        long total = relatos.count();
        long confirmados = relatos.countByConfirmacoesGreaterThanEqual(2);
        int precisao = total == 0 ? 0 : (int) Math.round(confirmados * 100.0 / total);
        return new StatsResposta(
            relatos.countByCreatedAtGreaterThanEqual(hoje),
            relatos.contarAutoresDesde(semana),
            precisao
        );
    }

    @PostMapping("/linhas/{codigo}/relatos")
    public ResponseEntity<RelatoResposta> criar(
        @PathVariable String codigo,
        @RequestBody NovoRelatoRequest body,
        Authentication authentication
    ) {
        Usuario usuario = usuarioLogado(authentication);
        if (!StatusLinha.tipoValido(body == null ? null : body.tipo())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Escolhe o que aconteceu na linha.");
        }
        Linha linha = linhas.findByCodigoIgnoreCase(codigo)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Linha não encontrada."));
        if (relatos.existsByUsuarioIdAndLinhaAndTipoAndCreatedAtGreaterThanEqual(
            usuario.getId(), linha, body.tipo(), RegrasNegocio.limiteAntiSpam()
        )) {
            throw new ApiException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Você já reportou este problema nesta linha. Espere 30 minutos para enviar de novo."
            );
        }
        Relato relato = new Relato();
        relato.setLinha(linha);
        relato.setUsuario(usuario);
        relato.setTipo(body.tipo());
        relato.setMensagem(corta(body.mensagem(), 280));
        relato.setAutor(usuario.getNome());
        relato.setConfirmacoes(1);
        relato.setConfiabilidade(RegrasNegocio.confiabilidadeDe(body.tipo(), 1));
        relato.setLatitude(body.latitude());
        relato.setLongitude(body.longitude());
        relato.setLocal(corta(body.local(), 120));
        relatos.save(relato);
        RelatoConfirmacao confirmacao = new RelatoConfirmacao();
        confirmacao.setRelato(relato);
        confirmacao.setUsuario(usuario);
        confirmacoes.save(confirmacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(RelatoResposta.de(relato));
    }

    @PostMapping("/relatos/{id}/confirmar")
    public Map<String, Object> confirmar(@PathVariable Long id, Authentication authentication) {
        Usuario usuario = usuarioLogado(authentication);
        Relato relato = relatos.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ocorrência não encontrada."));
        if (!RegrasNegocio.aindaAtivo(relato)) {
            throw new ApiException(HttpStatus.GONE, "Este alerta já expirou.");
        }
        if (confirmacoes.existsByRelatoIdAndUsuarioId(relato.getId(), usuario.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Você já confirmou esta ocorrência.");
        }
        RelatoConfirmacao confirmacao = new RelatoConfirmacao();
        confirmacao.setRelato(relato);
        confirmacao.setUsuario(usuario);
        confirmacoes.save(confirmacao);
        int total = relato.getConfirmacoes() == null ? 1 : relato.getConfirmacoes() + 1;
        relato.setConfirmacoes(total);
        relato.setConfiabilidade(RegrasNegocio.confiabilidadeDe(relato.getTipo(), total));
        relatos.save(relato);
        return Map.of(
            "id", relato.getId(),
            "confirmacoes", relato.getConfirmacoes(),
            "confiabilidade", relato.getConfiabilidade()
        );
    }

    private Usuario usuarioLogado(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioAutenticado atual)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Faça login para reportar ou confirmar ocorrência.");
        }
        return usuarios.findById(atual.id())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Faça login para continuar."));
    }

    private String corta(String valor, int max) {
        if (valor == null) return null;
        String texto = valor.trim();
        if (texto.isEmpty()) return null;
        return texto.length() > max ? texto.substring(0, max) : texto;
    }
}
