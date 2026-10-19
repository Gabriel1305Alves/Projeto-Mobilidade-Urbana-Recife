package br.com.embarcai.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.embarcai.model.Relato;
import br.com.embarcai.negocio.RegrasNegocio;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrevisaoRiscoTest {
    private static final ZonedDateTime TERCA_PICO_OUTUBRO =
        ZonedDateTime.of(2026, 10, 20, 18, 0, 0, 0, PrevisaoRisco.RECIFE);
    private static final ZonedDateTime TERCA_MADRUGADA_OUTUBRO = TERCA_PICO_OUTUBRO.withHour(3);
    private static final ZonedDateTime DOMINGO_NOITE = ZonedDateTime.of(2026, 10, 18, 23, 0, 0, 0, PrevisaoRisco.RECIFE);

    @Test
    void picoTemMaisRiscoQueMadrugada() {
        double pico = probabilidade(TERCA_PICO_OUTUBRO, List.of(), List.of());
        double madrugada = probabilidade(TERCA_MADRUGADA_OUTUBRO, List.of(), List.of());
        assertTrue(pico > madrugada);
        assertEquals("baixo", PrevisaoRisco.nivel(madrugada));
    }

    @Test
    void historicoNoMesmoHorarioAumentaRisco() {
        List<Relato> historico = new ArrayList<>();
        for (int dia = 1; dia <= 6; dia++) {
            historico.add(relato("transito", TERCA_PICO_OUTUBRO.minusDays(dia), 0, RegrasNegocio.NAO_CONFIRMADO));
        }
        double sem = probabilidade(TERCA_PICO_OUTUBRO, List.of(), List.of());
        double com = probabilidade(TERCA_PICO_OUTUBRO, historico, List.of());
        assertTrue(com > sem);
        assertEquals("alto", PrevisaoRisco.nivel(com));
    }

    @Test
    void alertasAtivosDeixamRiscoAltoMesmoDeMadrugada() {
        List<Relato> ativos = List.of(
            relato("transito", DOMINGO_NOITE.minusMinutes(15), 8, RegrasNegocio.NAO_CONFIRMADO),
            relato("alagamento", DOMINGO_NOITE.minusMinutes(30), 5, RegrasNegocio.CONFIRMADO_COMUNIDADE)
        );
        PrevisaoRisco.Estimativa e = PrevisaoRisco.estimar(DOMINGO_NOITE, List.of(), ativos);
        assertEquals("alto", PrevisaoRisco.nivel(e.probabilidade()));
        assertTrue(e.fatores().get(0).texto().startsWith("2 alertas ativos"));
    }

    @Test
    void alertaPerdeForcaAteExpirar() {
        List<Relato> ativos = List.of(
            relato("alagamento", DOMINGO_NOITE.minusMinutes(10), 5, RegrasNegocio.CONFIRMADO_COMUNIDADE)
        );
        double agora = probabilidade(DOMINGO_NOITE, List.of(), ativos);
        double umaHora = probabilidade(DOMINGO_NOITE.plusHours(1), List.of(), ativos);
        double tresHoras = probabilidade(DOMINGO_NOITE.plusHours(3), List.of(), ativos);
        assertTrue(agora > umaHora && umaHora > tresHoras);
        assertEquals(probabilidade(DOMINGO_NOITE.plusHours(3), List.of(), List.of()), tresHoras, 1e-9);
    }

    @Test
    void viradaDeDomingoParaSegundaNaoAumentaRiscoNaMadrugada() {
        double domingo23h = probabilidade(DOMINGO_NOITE, List.of(), List.of());
        double segunda0h = probabilidade(DOMINGO_NOITE.plusHours(1), List.of(), List.of());
        double segunda1h = probabilidade(DOMINGO_NOITE.plusHours(2), List.of(), List.of());
        assertTrue(segunda0h <= domingo23h + 1e-9);
        assertTrue(segunda1h <= domingo23h + 1e-9);
    }

    @Test
    void fatoresIndicamSeAumentamOuReduzem() {
        PrevisaoRisco.Estimativa e = PrevisaoRisco.estimar(TERCA_MADRUGADA_OUTUBRO, List.of(), List.of());
        assertEquals("reduz", e.fatores().get(0).efeito());
        assertEquals("aumenta", PrevisaoRisco.estimar(TERCA_PICO_OUTUBRO, List.of(), List.of()).fatores().get(0).efeito());
    }

    @Test
    void tipoProvavelSegueChuvaEAlertas() {
        ZonedDateTime junho = TERCA_MADRUGADA_OUTUBRO.withMonth(6);
        assertEquals("alagamento", PrevisaoRisco.estimar(junho, List.of(), List.of()).tipoProvavel());
        assertEquals("transito", PrevisaoRisco.estimar(TERCA_PICO_OUTUBRO, List.of(), List.of()).tipoProvavel());
        List<Relato> bloqueio = List.of(relato("bloqueio", TERCA_PICO_OUTUBRO.minusMinutes(5), 3, RegrasNegocio.NAO_CONFIRMADO));
        assertEquals("bloqueio", PrevisaoRisco.estimar(TERCA_PICO_OUTUBRO, List.of(), bloqueio).tipoProvavel());
    }

    @Test
    void reconheceHoraNaPergunta() {
        assertEquals(18, Assistente.horaCitada("vai atrasar às 18h?"));
        assertEquals(7, Assistente.horaCitada("e as 7 horas?"));
        assertEquals(null, Assistente.horaCitada("como está a 020?"));
    }

    private static double probabilidade(ZonedDateTime momento, List<Relato> passados, List<Relato> ativos) {
        return PrevisaoRisco.estimar(momento, passados, ativos).probabilidade();
    }

    private static Relato relato(String tipo, ZonedDateTime quando, int confirmacoes, String confiabilidade) {
        Relato r = new Relato();
        r.setTipo(tipo);
        r.setCreatedAt(quando.toOffsetDateTime());
        r.setConfirmacoes(confirmacoes);
        r.setConfiabilidade(confiabilidade);
        return r;
    }
}
