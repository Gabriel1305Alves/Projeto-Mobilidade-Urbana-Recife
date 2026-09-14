package br.com.embarcai.dto;

import br.com.embarcai.model.Relato;
import br.com.embarcai.model.StatusLinha;
import java.time.OffsetDateTime;

public record RelatoResposta(
    Long id,
    String tipo,
    String mensagem,
    String autor,
    Integer confirmacoes,
    OffsetDateTime createdAt,
    String rotulo
) {
    public static RelatoResposta de(Relato relato) {
        return new RelatoResposta(
            relato.getId(),
            relato.getTipo(),
            relato.getMensagem(),
            relato.getAutor(),
            relato.getConfirmacoes(),
            relato.getCreatedAt(),
            StatusLinha.rotulo(relato.getTipo())
        );
    }
}
