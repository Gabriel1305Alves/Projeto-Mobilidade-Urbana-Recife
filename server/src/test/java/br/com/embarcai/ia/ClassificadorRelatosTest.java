package br.com.embarcai.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.embarcai.dto.ClassificacaoResposta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClassificadorRelatosTest {
    private final ClassificadorRelatos classificador = new ClassificadorRelatos(null);

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "alagamento|A rua tá toda alagada depois da chuva",
        "alagamento|Água no joelho perto da parada",
        "transito|Engarrafamento parado na Caxangá",
        "transito|Trânsito muito lento no centro",
        "acidente|Batida entre moto e carro",
        "acidente|Atropelaram um pedestre no cruzamento",
        "bloqueio|Rua interditada por obra",
        "bloqueio|Protesto fechou a avenida",
    })
    void classificaFrasesForaDoCorpus(String esperado, String texto) {
        ClassificacaoResposta resposta = classificador.classificar(texto);
        assertEquals(esperado, resposta.tipo(), () -> texto + " → " + resposta.probabilidades());
    }

    @Test
    void probabilidadesSomamUm() {
        double soma = classificador.classificar("chuva forte e trânsito parado").probabilidades()
            .values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, soma, 0.01);
    }

    @Test
    void textoSemPalavrasConhecidasNaoGeraSugestao() {
        ClassificacaoResposta resposta = classificador.classificar("xyzzy qwerty");
        assertNull(resposta.tipo());
        assertFalse(resposta.confiavel());
    }

    @Test
    void aprendeComNovosRelatos() {
        String texto = "vazamento de esgoto na pista";
        for (int i = 0; i < 5; i++) classificador.aprender("bloqueio", texto);
        ClassificacaoResposta resposta = classificador.classificar(texto);
        assertEquals("bloqueio", resposta.tipo());
        assertTrue(resposta.confiavel());
    }
}
