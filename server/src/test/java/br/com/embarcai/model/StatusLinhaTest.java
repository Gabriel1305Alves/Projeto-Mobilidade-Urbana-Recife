package br.com.embarcai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatusLinhaTest {
    @Test
    void reconheceOsQuatroTiposDeOcorrencia() {
        assertTrue(StatusLinha.tipoValido("transito"));
        assertTrue(StatusLinha.tipoValido("alagamento"));
        assertTrue(StatusLinha.tipoValido("bloqueio"));
        assertTrue(StatusLinha.tipoValido("acidente"));
        assertFalse(StatusLinha.tipoValido("chuva"));
        assertFalse(StatusLinha.tipoValido(null));
    }

    @Test
    void removeAcentoParaABuscaDaLinha() {
        assertEquals("transito intenso", StatusLinha.semAcento("Trânsito Intenso"));
        assertEquals("", StatusLinha.semAcento(null));
    }

    @Test
    void linhaSemAlertaRecenteFicaTranquila() {
        Relato antigo = new Relato();
        antigo.setCreatedAt(OffsetDateTime.now().minusHours(5));
        StatusLinha.StatusDto status = StatusLinha.de(List.of(antigo));
        assertEquals("ok", status.tipo());
        assertEquals(0, status.totalRecentes());
    }

    @Test
    void alertaRecenteColocaALinhaEmAtencao() {
        Relato recente = new Relato();
        recente.setCreatedAt(OffsetDateTime.now().minusMinutes(10));
        StatusLinha.StatusDto status = StatusLinha.de(List.of(recente));
        assertEquals("atencao", status.tipo());
        assertEquals(1, status.totalRecentes());
    }
}
