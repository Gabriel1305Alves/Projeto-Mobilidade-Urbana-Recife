package br.com.embarcai.ia;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Classificador Naive Bayes multinomial com suavização de Laplace.
 * Aprende de forma incremental: cada exemplo novo só soma contagens.
 */
public class NaiveBayes {
    private static final double ALFA = 1.0;

    private final Map<String, Map<String, Integer>> contagemPorClasse = new HashMap<>();
    private final Map<String, Integer> totalTokensPorClasse = new HashMap<>();
    private final Map<String, Integer> documentosPorClasse = new HashMap<>();
    private final Set<String> vocabulario = new HashSet<>();
    private int totalDocumentos;

    public synchronized void treinar(String classe, String texto) {
        List<String> tokens = Tokenizador.tokens(texto);
        if (tokens.isEmpty()) return;
        Map<String, Integer> contagem = contagemPorClasse.computeIfAbsent(classe, c -> new HashMap<>());
        for (String token : tokens) {
            contagem.merge(token, 1, Integer::sum);
            vocabulario.add(token);
        }
        totalTokensPorClasse.merge(classe, tokens.size(), Integer::sum);
        documentosPorClasse.merge(classe, 1, Integer::sum);
        totalDocumentos++;
    }

    /** Probabilidade de cada classe, em ordem decrescente. Vazio se nenhuma palavra for conhecida. */
    public synchronized Map<String, Double> probabilidades(String texto) {
        List<String> conhecidos = Tokenizador.tokens(texto).stream().filter(vocabulario::contains).toList();
        if (conhecidos.isEmpty() || totalDocumentos == 0) return Map.of();

        Map<String, Double> logs = new HashMap<>();
        for (String classe : documentosPorClasse.keySet()) {
            double log = Math.log(documentosPorClasse.get(classe) / (double) totalDocumentos);
            Map<String, Integer> contagem = contagemPorClasse.get(classe);
            double denominador = totalTokensPorClasse.get(classe) + ALFA * vocabulario.size();
            for (String token : conhecidos) {
                log += Math.log((contagem.getOrDefault(token, 0) + ALFA) / denominador);
            }
            logs.put(classe, log);
        }

        double maior = logs.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double soma = logs.values().stream().mapToDouble(v -> Math.exp(v - maior)).sum();
        Map<String, Double> saida = new LinkedHashMap<>();
        logs.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
            .forEach(e -> saida.put(e.getKey(), Math.exp(e.getValue() - maior) / soma));
        return saida;
    }

    public synchronized int totalDocumentos() {
        return totalDocumentos;
    }

    public synchronized int tamanhoVocabulario() {
        return vocabulario.size();
    }
}
