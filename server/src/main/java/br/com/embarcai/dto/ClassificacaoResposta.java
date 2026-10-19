package br.com.embarcai.dto;

import java.util.Map;

public record ClassificacaoResposta(
    String tipo,
    String rotulo,
    double confianca,
    boolean confiavel,
    Map<String, Double> probabilidades
) {}
