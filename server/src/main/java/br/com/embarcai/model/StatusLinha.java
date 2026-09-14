package br.com.embarcai.model;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class StatusLinha {
    public static final List<String> TIPOS = List.of("transito", "alagamento", "bloqueio", "acidente");
    private static final Map<String, String> ROTULOS = Map.of(
        "transito", "Trânsito intenso",
        "alagamento", "Alagamento",
        "bloqueio", "Bloqueio",
        "acidente", "Acidente",
        "atencao", "Atenção no trajeto",
        "ok", "Trajeto tranquilo"
    );

    private StatusLinha() {}

    public static String rotulo(String tipo) {
        return ROTULOS.getOrDefault(tipo, tipo);
    }

    public static boolean tipoValido(String tipo) {
        return TIPOS.contains(tipo);
    }

    public static StatusDto de(List<Relato> relatos) {
        OffsetDateTime limite = OffsetDateTime.now().minusHours(2);
        List<Relato> recentes = relatos.stream()
            .filter(r -> !r.getCreatedAt().isBefore(limite))
            .toList();
        if (recentes.isEmpty()) {
            return new StatusDto("ok", rotulo("ok"), null, 0);
        }
        Relato ultimo = recentes.get(0);
        return new StatusDto("atencao", rotulo("atencao"), ultimo.getCreatedAt(), recentes.size());
    }

    public static String semAcento(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase();
    }

    public record StatusDto(String tipo, String rotulo, OffsetDateTime atualizadoEm, int totalRecentes) {}
}
