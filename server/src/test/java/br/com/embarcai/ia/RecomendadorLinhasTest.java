package br.com.embarcai.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.embarcai.model.Linha;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecomendadorLinhasTest {
    private static final Linha L020 = linha("020", "Barra de Jangada", -8.2275, -34.9378, "Rio Doce", -7.9667, -34.8392);
    private static final Linha L513 = linha("513", "TI Rio Doce", -7.9667, -34.8392, "TI PE-15", -7.9780, -34.8580);
    private static final Linha L163 = linha("163", "TI Ibura", -8.1210, -34.9360, "Boa Vista", -8.0570, -34.8850);
    private static final Linha L190 = linha("190", "TI Cajueiro Seco", -8.1680, -34.9260, "Conde da Boa Vista", -8.0575, -34.8820);
    private static final Linha L101 = linha("101", "Recife", -8.0632, -34.8711, "Boa Viagem", -8.1220, -34.8989);

    @Test
    void mesmoDestinoGeraAfinidade() {
        RecomendadorLinhas.Afinidade a = RecomendadorLinhas.afinidade(L020, L513);
        assertTrue(a.similaridade() >= RecomendadorLinhas.SIMILARIDADE_MINIMA);
        assertTrue(a.motivos().contains("Também atende Rio Doce"), a.motivos()::toString);
    }

    @Test
    void destinoProximoContaMesmoComNomeDiferente() {
        RecomendadorLinhas.Afinidade a = RecomendadorLinhas.afinidade(L163, L190);
        assertTrue(a.similaridade() >= RecomendadorLinhas.SIMILARIDADE_MINIMA);
        assertEquals(List.of("Também atende Boa Vista"), a.motivos());
    }

    @Test
    void reconheceLinhaNoSentidoInverso() {
        Linha inversa = linha("999", "Rio Doce", -7.9667, -34.8392, "Barra de Jangada", -8.2275, -34.9378);
        assertEquals(1.0, RecomendadorLinhas.afinidade(L020, inversa).similaridade(), 1e-9);
    }

    @Test
    void trajetosDistantesNaoSaoRecomendados() {
        assertTrue(RecomendadorLinhas.afinidade(L020, L101).similaridade() < RecomendadorLinhas.SIMILARIDADE_MINIMA);
    }

    @Test
    void semCoordenadasUsaNomeDoTerminal() {
        Linha a = linha("1", "TI Macaxeira", null, null, "Derby", null, null);
        Linha b = linha("2", "Macaxeira", null, null, "Boa Vista", null, null);
        assertEquals(0.5, RecomendadorLinhas.afinidade(a, b).similaridade(), 1e-9);
    }

    @Test
    void menorRiscoGanhaNaPontuacao() {
        assertTrue(RecomendadorLinhas.pontuacao(0.5, 0.1) > RecomendadorLinhas.pontuacao(0.5, 0.6));
        assertTrue(RecomendadorLinhas.pontuacao(0.9, 0.3) > RecomendadorLinhas.pontuacao(0.4, 0.3));
    }

    private static Linha linha(
        String codigo, String origem, Double latO, Double lngO, String destino, Double latD, Double lngD
    ) {
        Linha l = new Linha();
        l.setCodigo(codigo);
        l.setNome(origem + " / " + destino);
        l.setOrigem(origem);
        l.setDestino(destino);
        l.setLatOrigem(latO);
        l.setLngOrigem(lngO);
        l.setLatDestino(latD);
        l.setLngDestino(lngD);
        return l;
    }
}
