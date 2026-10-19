package br.com.embarcai.ia;

import br.com.embarcai.dto.PrevisaoResposta;
import br.com.embarcai.dto.RecomendacaoResposta;
import br.com.embarcai.model.Linha;
import br.com.embarcai.model.StatusLinha;
import br.com.embarcai.negocio.RegrasNegocio;
import br.com.embarcai.repository.LinhaRepository;
import br.com.embarcai.repository.RelatoRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Recomenda linhas alternativas: filtra por conteúdo (linhas com pontos finais próximos
 * ou de mesmo nome) e ordena pelo risco previsto agora, pontuação = similaridade × (1 − risco).
 */
@Service
public class RecomendadorLinhas {
    static final double RAIO_KM = 2.5;
    static final double SIMILARIDADE_MINIMA = 0.3;
    static final double PROXIMIDADE_RELEVANTE = 0.5;
    static final double MESMO_PONTO_KM = 0.5;
    static final int MAX_ALTERNATIVAS = 3;
    private static final double RAIO_TERRA_KM = 6371;

    private final LinhaRepository linhas;
    private final RelatoRepository relatos;
    private final PrevisaoRisco previsao;

    public RecomendadorLinhas(LinhaRepository linhas, RelatoRepository relatos, PrevisaoRisco previsao) {
        this.linhas = linhas;
        this.relatos = relatos;
        this.previsao = previsao;
    }

    public RecomendacaoResposta recomendar(Linha linha) {
        PrevisaoResposta atual = previsao.prever(linha);
        List<RecomendacaoResposta.Alternativa> alternativas = new ArrayList<>();
        for (Linha candidata : linhas.findAllByOrderByCodigoAsc()) {
            if (candidata.getCodigo().equalsIgnoreCase(linha.getCodigo())) continue;
            Afinidade afinidade = afinidade(linha, candidata);
            if (afinidade.similaridade() < SIMILARIDADE_MINIMA) continue;

            PrevisaoResposta p = previsao.prever(candidata);
            int ativos = relatos.findTop40ByLinhaAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                candidata, RegrasNegocio.inicioJanelaAtiva()
            ).size();
            List<String> motivos = new ArrayList<>(afinidade.motivos());
            if (p.probabilidade() < atual.probabilidade()) {
                motivos.add("Risco menor que a " + linha.getCodigo() + " ("
                    + p.probabilidade() + "% contra " + atual.probabilidade() + "%)");
            }
            motivos.add(ativos == 0 ? "Sem ocorrências ativas"
                : ativos + (ativos == 1 ? " ocorrência ativa" : " ocorrências ativas"));

            alternativas.add(new RecomendacaoResposta.Alternativa(
                candidata.getCodigo(),
                candidata.getNome(),
                percentual(afinidade.similaridade()),
                p.probabilidade(),
                p.nivel(),
                p.nivelRotulo(),
                ativos,
                percentual(pontuacao(afinidade.similaridade(), p.probabilidade() / 100.0)),
                motivos
            ));
        }
        alternativas.sort(Comparator.comparingInt(RecomendacaoResposta.Alternativa::pontuacao).reversed());
        return new RecomendacaoResposta(
            linha.getCodigo(),
            atual.probabilidade(),
            atual.nivel(),
            alternativas.stream().limit(MAX_ALTERNATIVAS).toList()
        );
    }

    static double pontuacao(double similaridade, double risco) {
        return similaridade * (1 - risco);
    }

    /** Compara os pontos finais das duas linhas, no mesmo sentido ou invertido, e fica com o melhor pareamento. */
    static Afinidade afinidade(Linha a, Linha b) {
        Ponto ao = origem(a), ad = destino(a), bo = origem(b), bd = destino(b);
        double direto = proximidade(ao, bo) + proximidade(ad, bd);
        double invertido = proximidade(ao, bd) + proximidade(ad, bo);
        Ponto[][] pares = direto >= invertido
            ? new Ponto[][] {{ao, bo}, {ad, bd}}
            : new Ponto[][] {{ao, bd}, {ad, bo}};

        List<String> motivos = new ArrayList<>();
        for (Ponto[] par : pares) {
            if (proximidade(par[0], par[1]) < PROXIMIDADE_RELEVANTE) continue;
            double km = distanciaKm(par[0], par[1]);
            motivos.add(mesmoNome(par[0], par[1]) || km < MESMO_PONTO_KM
                ? "Também atende " + par[0].nome()
                : String.format(Locale.forLanguageTag("pt-BR"), "Ponto final a %.1f km de %s", km, par[0].nome()));
        }
        return new Afinidade(Math.max(direto, invertido) / 2, motivos);
    }

    static double proximidade(Ponto p, Ponto q) {
        if (mesmoNome(p, q)) return 1;
        double km = distanciaKm(p, q);
        return Double.isNaN(km) ? 0 : Math.exp(-km / RAIO_KM);
    }

    static double distanciaKm(Ponto p, Ponto q) {
        if (p.lat() == null || p.lng() == null || q.lat() == null || q.lng() == null) return Double.NaN;
        double dLat = Math.toRadians(q.lat() - p.lat());
        double dLng = Math.toRadians(q.lng() - p.lng());
        double h = Math.pow(Math.sin(dLat / 2), 2)
            + Math.cos(Math.toRadians(p.lat())) * Math.cos(Math.toRadians(q.lat())) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * RAIO_TERRA_KM * Math.asin(Math.sqrt(h));
    }

    private static boolean mesmoNome(Ponto p, Ponto q) {
        String a = nomeNormalizado(p.nome());
        return !a.isEmpty() && a.equals(nomeNormalizado(q.nome()));
    }

    private static String nomeNormalizado(String nome) {
        return StatusLinha.semAcento(nome).replaceFirst("^ti\\s+", "").trim();
    }

    private static Ponto origem(Linha l) {
        return new Ponto(l.getOrigem(), l.getLatOrigem(), l.getLngOrigem());
    }

    private static Ponto destino(Linha l) {
        return new Ponto(l.getDestino(), l.getLatDestino(), l.getLngDestino());
    }

    private static int percentual(double v) {
        return (int) Math.round(v * 100);
    }

    record Ponto(String nome, Double lat, Double lng) {}

    record Afinidade(double similaridade, List<String> motivos) {}
}
