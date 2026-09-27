package br.com.embarcai.negocio;

import br.com.embarcai.model.Linha;
import br.com.embarcai.model.Relato;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Regras de negócio do Embarcaí — o sistema decide com base no uso real,
 * não aceita qualquer relato ou confirmação.
 */
public final class RegrasNegocio {
    /** Alerta some da listagem ativa depois deste tempo. */
    public static final Duration JANELA_ALERTA_ATIVO = Duration.ofHours(2);
    /** Mesmo usuário não pode repetir o mesmo tipo na mesma linha neste intervalo. */
    public static final Duration INTERVALO_ANTI_SPAM = Duration.ofMinutes(30);
    /** Alagamento vira "Confirmado pela Comunidade" com este número de pessoas distintas. */
    public static final int CONFIRMACOES_COMUNIDADE = 5;
    public static final String TIPO_ALAGAMENTO = "alagamento";
    public static final String NAO_CONFIRMADO = "nao_confirmado";
    public static final String CONFIRMADO_COMUNIDADE = "confirmado_comunidade";
    public static final String PAPEL_ADMIN = "ADMIN";
    public static final String PAPEL_PASSAGEIRO = "PASSAGEIRO";

    private RegrasNegocio() {}

    public static OffsetDateTime inicioJanelaAtiva() {
        return OffsetDateTime.now().minus(JANELA_ALERTA_ATIVO);
    }

    public static OffsetDateTime limiteAntiSpam() {
        return OffsetDateTime.now().minus(INTERVALO_ANTI_SPAM);
    }

    /** Regra de exclusão: trânsito (e demais alertas) saem da tela depois de 2 horas. */
    public static boolean aindaAtivo(Relato relato) {
        return relato != null
            && relato.getCreatedAt() != null
            && !relato.getCreatedAt().isBefore(inicioJanelaAtiva());
    }

    public static String confiabilidadeDe(String tipo, int confirmacoesDistintas) {
        if (TIPO_ALAGAMENTO.equals(tipo) && confirmacoesDistintas >= CONFIRMACOES_COMUNIDADE) {
            return CONFIRMADO_COMUNIDADE;
        }
        return NAO_CONFIRMADO;
    }

    public static String rotuloConfiabilidade(String confiabilidade) {
        if (CONFIRMADO_COMUNIDADE.equals(confiabilidade)) {
            return "Confirmado pela comunidade";
        }
        return "Não confirmado";
    }

    public static double distanciaKm(double lat1, double lng1, double lat2, double lng2) {
        double raio = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return raio * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static Double distanciaDaLinha(Linha linha, double lat, double lng) {
        Double origem = (linha.getLatOrigem() == null || linha.getLngOrigem() == null)
            ? null : distanciaKm(lat, lng, linha.getLatOrigem(), linha.getLngOrigem());
        Double destino = (linha.getLatDestino() == null || linha.getLngDestino() == null)
            ? null : distanciaKm(lat, lng, linha.getLatDestino(), linha.getLngDestino());
        if (origem == null) return destino;
        if (destino == null) return origem;
        return Math.min(origem, destino);
    }

    public static Double arredondaKm(Double km) {
        if (km == null) return null;
        return Math.round(km * 10.0) / 10.0;
    }
}
