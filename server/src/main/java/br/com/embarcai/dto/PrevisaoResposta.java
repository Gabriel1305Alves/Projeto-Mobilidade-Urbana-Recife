package br.com.embarcai.dto;

import java.util.List;

public record PrevisaoResposta(
    String linha,
    String nivel,
    String nivelRotulo,
    int probabilidade,
    String tipoProvavel,
    String tipoProvavelRotulo,
    List<Fator> fatores,
    List<Hora> proximasHoras,
    int ocorrenciasAnalisadas,
    int alertasAtivos
) {
    public record Hora(int hora, int probabilidade, String nivel) {}

    /** efeito: "aumenta" ou "reduz" o risco. */
    public record Fator(String texto, String efeito) {}
}
