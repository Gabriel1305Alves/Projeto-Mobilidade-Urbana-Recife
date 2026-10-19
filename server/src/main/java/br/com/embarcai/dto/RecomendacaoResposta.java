package br.com.embarcai.dto;

import java.util.List;

public record RecomendacaoResposta(
    String linha,
    int probabilidadeAtual,
    String nivelAtual,
    List<Alternativa> alternativas
) {
    public record Alternativa(
        String codigo,
        String nome,
        int similaridade,
        int probabilidade,
        String nivel,
        String nivelRotulo,
        int ocorrenciasAtivas,
        int pontuacao,
        List<String> motivos
    ) {}
}
