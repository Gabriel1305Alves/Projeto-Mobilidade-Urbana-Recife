package br.com.embarcai.dto;

import br.com.embarcai.model.Relato;
import br.com.embarcai.model.StatusLinha;
import br.com.embarcai.negocio.RegrasNegocio;
import java.time.OffsetDateTime;

public record RelatoResposta(
    Long id,
    String tipo,
    String mensagem,
    String autor,
    Integer confirmacoes,
    OffsetDateTime createdAt,
    String rotulo,
    String confiabilidade,
    String confiabilidadeRotulo,
    Double latitude,
    Double longitude,
    String local
) {
    public static RelatoResposta de(Relato relato) {
        String confiabilidade = relato.getConfiabilidade() == null
            ? RegrasNegocio.NAO_CONFIRMADO
            : relato.getConfiabilidade();
        return new RelatoResposta(
            relato.getId(),
            relato.getTipo(),
            relato.getMensagem(),
            relato.getAutor(),
            relato.getConfirmacoes(),
            relato.getCreatedAt(),
            StatusLinha.rotulo(relato.getTipo()),
            confiabilidade,
            RegrasNegocio.rotuloConfiabilidade(confiabilidade),
            relato.getLatitude(),
            relato.getLongitude(),
            relato.getLocal()
        );
    }
}
