package br.com.embarcai.dto;

import br.com.embarcai.model.SolicitacaoLinha;
import java.time.OffsetDateTime;

public record SolicitacaoLinhaResposta(
    Long id,
    String codigo,
    String nome,
    String origem,
    String destino,
    String status,
    String solicitante,
    OffsetDateTime createdAt
) {
    public static SolicitacaoLinhaResposta de(SolicitacaoLinha s) {
        return new SolicitacaoLinhaResposta(
            s.getId(),
            s.getCodigo(),
            s.getNome(),
            s.getOrigem(),
            s.getDestino(),
            s.getStatus(),
            s.getUsuario() == null ? null : s.getUsuario().getNome(),
            s.getCreatedAt()
        );
    }
}
