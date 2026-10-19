package br.com.embarcai.ia;

import br.com.embarcai.dto.PrevisaoResposta;
import br.com.embarcai.dto.PrevisaoResposta.Fator;
import br.com.embarcai.model.Linha;
import br.com.embarcai.model.Relato;
import br.com.embarcai.model.StatusLinha;
import br.com.embarcai.negocio.RegrasNegocio;
import br.com.embarcai.repository.RelatoRepository;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Estima a chance de a linha ter problema (atraso, alagamento, bloqueio...) num momento.
 *
 * Duas fontes se combinam como "pelo menos um problema acontece" (ou-ruidoso):
 * p = 1 − (1 − p_contexto) × Π (1 − p_alerta).
 *  - p_contexto: modelo logístico com horário, dia da semana, histórico da linha e chuva.
 *  - p_alerta: cada alerta ativo, pela confiabilidade, perdendo força até expirar (2 h).
 */
@Service
public class PrevisaoRisco {
    public static final ZoneId RECIFE = ZoneId.of("America/Recife");
    static final int DIAS_HISTORICO = 28;
    static final int HORAS_A_FRENTE = 4;

    static final double INTERCEPTO = -2.2;
    static final double PESO_PICO = 1.6;
    static final double PESO_HORARIO_COMERCIAL = 0.5;
    static final double PESO_MADRUGADA = -1.0;
    static final double PESO_HISTORICO = 0.8;
    static final double PESO_CHUVA = 0.7;
    static final double PESO_FIM_DE_SEMANA = -0.6;

    static final double P_ALERTA_BASE = 0.35;
    static final double P_POR_CONFIRMACAO = 0.05;
    static final double P_ALERTA_MAX = 0.8;
    static final double P_ALERTA_CONFIRMADO = 0.85;

    private final RelatoRepository relatos;

    public PrevisaoRisco(RelatoRepository relatos) {
        this.relatos = relatos;
    }

    public PrevisaoResposta prever(Linha linha) {
        return prever(linha, ZonedDateTime.now(RECIFE));
    }

    /** Estimativa para uma hora específica do dia (hoje, ou amanhã se a hora já passou). */
    public Estimativa preverAs(Linha linha, int hora) {
        ZonedDateTime agora = ZonedDateTime.now(RECIFE);
        ZonedDateTime alvo = agora.truncatedTo(ChronoUnit.HOURS).withHour(hora);
        if (hora < agora.getHour()) alvo = alvo.plusDays(1);
        List<Relato> historico = historicoDe(linha, agora);
        return estimar(alvo, passados(historico), ativos(historico));
    }

    public PrevisaoResposta prever(Linha linha, ZonedDateTime inicio) {
        List<Relato> historico = historicoDe(linha, inicio);
        List<Relato> ativos = ativos(historico);
        List<Relato> passados = passados(historico);

        Estimativa agora = estimar(inicio, passados, ativos);
        List<PrevisaoResposta.Hora> horas = new ArrayList<>();
        for (int i = 0; i < HORAS_A_FRENTE; i++) {
            ZonedDateTime momento = inicio.plusHours(i);
            Estimativa e = i == 0 ? agora : estimar(momento, passados, ativos);
            horas.add(new PrevisaoResposta.Hora(momento.getHour(), percentual(e.probabilidade()), nivel(e.probabilidade())));
        }
        String nivel = nivel(agora.probabilidade());
        return new PrevisaoResposta(
            linha.getCodigo(),
            nivel,
            rotuloNivel(nivel),
            percentual(agora.probabilidade()),
            agora.tipoProvavel(),
            StatusLinha.rotulo(agora.tipoProvavel()),
            agora.fatores(),
            horas,
            passados.size(),
            ativos.size()
        );
    }

    private List<Relato> historicoDe(Linha linha, ZonedDateTime referencia) {
        return relatos.findByLinhaAndCreatedAtGreaterThanEqual(
            linha, referencia.minusDays(DIAS_HISTORICO).toOffsetDateTime()
        );
    }

    private static List<Relato> ativos(List<Relato> historico) {
        return historico.stream().filter(RegrasNegocio::aindaAtivo).toList();
    }

    private static List<Relato> passados(List<Relato> historico) {
        return historico.stream().filter(r -> !RegrasNegocio.aindaAtivo(r)).toList();
    }

    /** Núcleo do modelo, sem acesso a banco, para poder ser testado isoladamente. */
    public static Estimativa estimar(ZonedDateTime momento, List<Relato> passados, List<Relato> ativos) {
        int hora = momento.getHour();
        List<Fator> fatores = new ArrayList<>();
        double z = INTERCEPTO;

        boolean pico = (hora >= 6 && hora <= 8) || (hora >= 17 && hora <= 19);
        boolean comercial = hora >= 9 && hora <= 16;
        boolean madrugada = hora >= 22 || hora <= 4;
        if (pico) {
            z += PESO_PICO;
            fatores.add(aumenta("Horário de pico"));
        } else if (comercial) {
            z += PESO_HORARIO_COMERCIAL;
            fatores.add(aumenta("Horário comercial"));
        } else if (madrugada) {
            z += PESO_MADRUGADA;
            fatores.add(reduz("Noite/madrugada: trânsito leve"));
        }

        DayOfWeek dia = momento.getDayOfWeek();
        if (!madrugada && (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY)) {
            z += PESO_FIM_DE_SEMANA;
            fatores.add(reduz("Fim de semana: menos movimento"));
        }

        List<Relato> naFaixa = passados.stream()
            .filter(r -> distanciaHoras(r.getCreatedAt().atZoneSameInstant(RECIFE).getHour(), hora) <= 1)
            .toList();
        if (!naFaixa.isEmpty()) {
            z += PESO_HISTORICO * Math.log1p(naFaixa.size());
            fatores.add(aumenta(naFaixa.size() + (naFaixa.size() == 1 ? " ocorrência" : " ocorrências")
                + " nesse horário nas últimas 4 semanas"));
        }

        double chuva = intensidadeChuva(momento.getMonthValue());
        if (chuva > 0) {
            z += PESO_CHUVA * chuva;
            fatores.add(aumenta(chuva >= 1 ? "Período chuvoso no Recife (abril a julho)" : "Início/fim da quadra chuvosa"));
        }

        double semProblema = 1 - 1 / (1 + Math.exp(-z));
        Map<String, Double> pesoAlertas = new HashMap<>();
        int vigentes = 0;
        int confirmados = 0;
        for (Relato r : ativos) {
            double p = probabilidadeAlerta(r, momento);
            if (p <= 0) continue;
            semProblema *= 1 - p;
            pesoAlertas.merge(r.getTipo(), p, Double::sum);
            vigentes++;
            if (RegrasNegocio.CONFIRMADO_COMUNIDADE.equals(r.getConfiabilidade())) confirmados++;
        }
        if (vigentes > 0) {
            fatores.add(0, aumenta(vigentes + (vigentes == 1 ? " alerta ativo" : " alertas ativos") + " na linha"
                + (confirmados > 0 ? " (" + confirmados + " confirmado pela comunidade)" : "")));
        }

        if (fatores.isEmpty()) fatores.add(reduz("Nenhum fator de risco relevante"));
        double probabilidade = 1 - semProblema;
        return new Estimativa(probabilidade, tipoProvavel(naFaixa, pesoAlertas, pico, chuva), fatores);
    }

    /** Chance de o alerta afetar a viagem no momento: confiabilidade × fração da janela de 2 h que ainda resta. */
    static double probabilidadeAlerta(Relato r, ZonedDateTime momento) {
        double base = RegrasNegocio.CONFIRMADO_COMUNIDADE.equals(r.getConfiabilidade())
            ? P_ALERTA_CONFIRMADO
            : Math.min(P_ALERTA_MAX, P_ALERTA_BASE + P_POR_CONFIRMACAO * r.getConfirmacoes());
        double idade = Math.max(0, Duration.between(r.getCreatedAt(), momento.toOffsetDateTime()).toMinutes());
        double restante = 1 - idade / RegrasNegocio.JANELA_ALERTA_ATIVO.toMinutes();
        return restante <= 0 ? 0 : base * restante;
    }

    static double intensidadeChuva(int mes) {
        if (mes >= 4 && mes <= 7) return 1.0;
        if (mes == 3 || mes == 8) return 0.5;
        return 0;
    }

    static String tipoProvavel(List<Relato> naFaixa, Map<String, Double> pesoAlertas, boolean pico, double chuva) {
        Map<String, Double> pontos = new HashMap<>(Map.of(
            "transito", pico ? 1.5 : 1.0,
            "alagamento", 0.3 + 1.5 * chuva,
            "acidente", 0.3,
            "bloqueio", 0.2
        ));
        naFaixa.forEach(r -> pontos.merge(r.getTipo(), 1.0, Double::sum));
        pesoAlertas.forEach((tipo, p) -> pontos.merge(tipo, 3 * p, Double::sum));
        return pontos.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
    }

    static String nivel(double p) {
        if (p >= 0.6) return "alto";
        if (p >= 0.35) return "moderado";
        return "baixo";
    }

    static String rotuloNivel(String nivel) {
        return switch (nivel) {
            case "alto" -> "Risco alto";
            case "moderado" -> "Risco moderado";
            default -> "Risco baixo";
        };
    }

    private static Fator aumenta(String texto) {
        return new Fator(texto, "aumenta");
    }

    private static Fator reduz(String texto) {
        return new Fator(texto, "reduz");
    }

    private static int distanciaHoras(int a, int b) {
        int d = Math.abs(a - b);
        return Math.min(d, 24 - d);
    }

    private static int percentual(double p) {
        return (int) Math.round(p * 100);
    }

    public record Estimativa(double probabilidade, String tipoProvavel, List<Fator> fatores) {}
}
