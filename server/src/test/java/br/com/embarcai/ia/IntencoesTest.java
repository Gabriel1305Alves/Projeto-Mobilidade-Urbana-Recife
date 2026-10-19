package br.com.embarcai.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class IntencoesTest {
    private static final NaiveBayes MODELO = new NaiveBayes();

    static {
        Corpus.treinar(MODELO, Assistente.CORPUS);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "saudacao|oi",
        "saudacao|bom dia!",
        "status_linha|a linha tá atrasada hoje?",
        "previsao|qual o risco de atrasar mais tarde?",
        "alagamento|alguma rua alagada?",
        "relatar|como faço para reportar um acidente?",
        "linhas|quais linhas vocês têm?",
        "sobre|como funciona o embarcaí?",
        "agradecimento|valeu pela ajuda",
        "alternativa|tem outro ônibus que faz o mesmo caminho?",
        "alternativa|me recomenda outra linha",
    })
    void reconheceIntencao(String esperada, String frase) {
        Map<String, Double> probabilidades = MODELO.probabilidades(frase);
        Map.Entry<String, Double> melhor = probabilidades.entrySet().iterator().next();
        assertEquals(esperada, melhor.getKey(), () -> frase + " → " + probabilidades);
        assertTrue(melhor.getValue() >= Assistente.LIMIAR_INTENCAO, () -> frase + " → " + probabilidades);
    }
}
