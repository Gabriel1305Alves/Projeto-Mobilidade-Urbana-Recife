package br.com.embarcai.ia;

import br.com.embarcai.dto.ClassificacaoResposta;
import br.com.embarcai.model.StatusLinha;
import br.com.embarcai.repository.RelatoRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Sugere o tipo da ocorrência (trânsito, alagamento, bloqueio, acidente)
 * a partir da descrição escrita pelo passageiro.
 */
@Service
public class ClassificadorRelatos {
    /** Abaixo disso a sugestão não é mostrada como confiável. */
    public static final double LIMIAR_CONFIANCA = 0.55;
    static final String CORPUS = "ia/relatos-treino.txt";

    private final RelatoRepository relatos;
    private final NaiveBayes modelo = new NaiveBayes();

    public ClassificadorRelatos(RelatoRepository relatos) {
        this.relatos = relatos;
        Corpus.treinar(modelo, CORPUS);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void treinarComHistorico() {
        relatos.findByMensagemIsNotNull().forEach(r -> aprender(r.getTipo(), r.getMensagem()));
    }

    public void aprender(String tipo, String mensagem) {
        if (StatusLinha.tipoValido(tipo) && mensagem != null && !mensagem.isBlank()) {
            modelo.treinar(tipo, mensagem);
        }
    }

    public ClassificacaoResposta classificar(String texto) {
        Map<String, Double> probabilidades = modelo.probabilidades(texto);
        if (probabilidades.isEmpty()) {
            return new ClassificacaoResposta(null, null, 0, false, Map.of());
        }
        Map<String, Double> arredondadas = new LinkedHashMap<>();
        probabilidades.forEach((tipo, p) -> arredondadas.put(tipo, Math.round(p * 1000) / 1000.0));
        Map.Entry<String, Double> melhor = probabilidades.entrySet().iterator().next();
        double confianca = arredondadas.get(melhor.getKey());
        return new ClassificacaoResposta(
            melhor.getKey(),
            StatusLinha.rotulo(melhor.getKey()),
            confianca,
            confianca >= LIMIAR_CONFIANCA,
            arredondadas
        );
    }

    public int exemplosTreinados() {
        return modelo.totalDocumentos();
    }
}
