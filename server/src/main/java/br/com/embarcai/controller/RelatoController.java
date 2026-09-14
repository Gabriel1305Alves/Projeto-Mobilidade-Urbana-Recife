package br.com.embarcai.controller;

import br.com.embarcai.config.ApiException;
import br.com.embarcai.dto.NovoRelatoRequest;
import br.com.embarcai.dto.RelatoResposta;
import br.com.embarcai.dto.SaudeResposta;
import br.com.embarcai.dto.StatsResposta;
import br.com.embarcai.model.Linha;
import br.com.embarcai.model.Relato;
import br.com.embarcai.model.StatusLinha;
import br.com.embarcai.repository.LinhaRepository;
import br.com.embarcai.repository.RelatoRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final Map<String, Boolean> confirmacoes = new ConcurrentHashMap<>();

    public RelatoController(LinhaRepository linhas, RelatoRepository relatos) {
        this.linhas = linhas;
        this.relatos = relatos;
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
    public ResponseEntity<RelatoResposta> criar(@PathVariable String codigo, @RequestBody NovoRelatoRequest body) {
        if (!StatusLinha.tipoValido(body.tipo())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Escolhe o que aconteceu na linha.");
        }
        Linha linha = linhas.findByCodigo(codigo)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Linha não encontrada."));
        Relato relato = new Relato();
        relato.setLinha(linha);
        relato.setTipo(body.tipo());
        relato.setMensagem(corta(body.mensagem(), 280));
        relato.setAutor(corta(body.autor(), 40));
        return ResponseEntity.status(HttpStatus.CREATED).body(RelatoResposta.de(relatos.save(relato)));
    }

    @PostMapping("/relatos/{id}/confirmar")
    public Map<String, Object> confirmar(@PathVariable Long id, HttpServletRequest request) {
        String chave = request.getRemoteAddr() + ":" + id;
        if (confirmacoes.containsKey(chave)) {
            throw new ApiException(HttpStatus.CONFLICT, "Você já confirmou esta ocorrência.");
        }
        Relato relato = relatos.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ocorrência não encontrada."));
        relato.setConfirmacoes(relato.getConfirmacoes() + 1);
        relatos.save(relato);
        confirmacoes.put(chave, true);
        return Map.of("id", relato.getId(), "confirmacoes", relato.getConfirmacoes());
    }

    private String corta(String valor, int max) {
        if (valor == null) return null;
        String texto = valor.trim();
        if (texto.isEmpty()) return null;
        return texto.length() > max ? texto.substring(0, max) : texto;
    }
}
