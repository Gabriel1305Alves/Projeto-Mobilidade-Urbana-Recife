package br.com.embarcai.controller;

import br.com.embarcai.config.ApiException;
import br.com.embarcai.dto.AssistenteRequest;
import br.com.embarcai.dto.AssistenteResposta;
import br.com.embarcai.dto.ClassificacaoResposta;
import br.com.embarcai.dto.ClassificarRequest;
import br.com.embarcai.dto.PrevisaoResposta;
import br.com.embarcai.dto.RecomendacaoResposta;
import br.com.embarcai.ia.Assistente;
import br.com.embarcai.ia.ClassificadorRelatos;
import br.com.embarcai.ia.PrevisaoRisco;
import br.com.embarcai.ia.RecomendadorLinhas;
import br.com.embarcai.model.Linha;
import br.com.embarcai.repository.LinhaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ia")
public class IaController {
    private static final int MAX_TEXTO = 500;

    private final ClassificadorRelatos classificador;
    private final PrevisaoRisco previsao;
    private final Assistente assistente;
    private final RecomendadorLinhas recomendador;
    private final LinhaRepository linhas;

    public IaController(
        ClassificadorRelatos classificador,
        PrevisaoRisco previsao,
        Assistente assistente,
        RecomendadorLinhas recomendador,
        LinhaRepository linhas
    ) {
        this.classificador = classificador;
        this.previsao = previsao;
        this.assistente = assistente;
        this.recomendador = recomendador;
        this.linhas = linhas;
    }

    @PostMapping("/classificar")
    public ClassificacaoResposta classificar(@RequestBody ClassificarRequest body) {
        return classificador.classificar(limita(body == null ? null : body.texto()));
    }

    @GetMapping("/linhas/{codigo}/previsao")
    public PrevisaoResposta previsao(@PathVariable String codigo) {
        Linha linha = linhas.findByCodigoIgnoreCase(codigo)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Linha não encontrada."));
        return previsao.prever(linha);
    }

    @GetMapping("/linhas/{codigo}/alternativas")
    public RecomendacaoResposta alternativas(@PathVariable String codigo) {
        Linha linha = linhas.findByCodigoIgnoreCase(codigo)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Linha não encontrada."));
        return recomendador.recomendar(linha);
    }

    @PostMapping("/assistente")
    public AssistenteResposta assistente(@RequestBody AssistenteRequest body) {
        if (body == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Escreva uma pergunta.");
        return assistente.responder(limita(body.mensagem()), body.linha());
    }

    private String limita(String texto) {
        if (texto == null) return "";
        return texto.length() > MAX_TEXTO ? texto.substring(0, MAX_TEXTO) : texto;
    }
}
