package br.com.embarcai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssistenteResposta(
    String resposta,
    String intencao,
    double confianca,
    String linha,
    List<String> sugestoes,
    Acao acao
) {
    public record Acao(String rotulo, String rota) {}
}
