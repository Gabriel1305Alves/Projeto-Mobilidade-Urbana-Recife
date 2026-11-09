package br.com.embarcai.negocio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.embarcai.model.Linha;
import br.com.embarcai.model.Relato;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class RegrasNegocioTest {
    @Test
    void alertaDentroDeDuasHorasContinuaAtivo() {
        Relato relato = new Relato();
        relato.setCreatedAt(OffsetDateTime.now().minusMinutes(90));
        assertTrue(RegrasNegocio.aindaAtivo(relato));
    }

    @Test
    void alertaMaisVelhoQueDuasHorasSaiDaListagem() {
        Relato relato = new Relato();
        relato.setCreatedAt(OffsetDateTime.now().minusHours(2).minusMinutes(1));
        assertFalse(RegrasNegocio.aindaAtivo(relato));
        assertFalse(RegrasNegocio.aindaAtivo(null));
    }

    @Test
    void alagamentoComCincoPessoasViraConfirmadoPelaComunidade() {
        assertEquals(
            RegrasNegocio.CONFIRMADO_COMUNIDADE,
            RegrasNegocio.confiabilidadeDe(RegrasNegocio.TIPO_ALAGAMENTO, 5)
        );
        assertEquals(
            "Confirmado pela comunidade",
            RegrasNegocio.rotuloConfiabilidade(RegrasNegocio.CONFIRMADO_COMUNIDADE)
        );
    }

    @Test
    void menosDeCincoOuOutroTipoPermanecemNaoConfirmados() {
        assertEquals(
            RegrasNegocio.NAO_CONFIRMADO,
            RegrasNegocio.confiabilidadeDe(RegrasNegocio.TIPO_ALAGAMENTO, 4)
        );
        assertEquals(
            RegrasNegocio.NAO_CONFIRMADO,
            RegrasNegocio.confiabilidadeDe("transito", 20)
        );
        assertEquals("Não confirmado", RegrasNegocio.rotuloConfiabilidade(RegrasNegocio.NAO_CONFIRMADO));
    }

    @Test
    void distanciaEntreOrigemEDestinoDaLinha020FicaNaFaixaEsperada() {
        double km = RegrasNegocio.distanciaKm(-8.2275, -34.9378, -7.9667, -34.8392);
        assertTrue(km > 25 && km < 40, () -> "distância calculada: " + km);
    }

    @Test
    void distanciaDaLinhaUsaOPontoMaisProximo() {
        Linha linha = new Linha();
        linha.setLatOrigem(-8.2275);
        linha.setLngOrigem(-34.9378);
        linha.setLatDestino(-7.9667);
        linha.setLngDestino(-34.8392);

        double noDestino = RegrasNegocio.distanciaDaLinha(linha, -7.9667, -34.8392);
        assertEquals(0.0, RegrasNegocio.arredondaKm(noDestino));
        assertNull(RegrasNegocio.arredondaKm(null));
    }
}
