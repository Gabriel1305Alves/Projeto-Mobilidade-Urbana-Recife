package br.com.embarcai.dto;

import br.com.embarcai.model.Linha;
import br.com.embarcai.model.Relato;
import br.com.embarcai.model.StatusLinha;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LinhaResposta(
    Long id,
    String codigo,
    String nome,
    String origem,
    String destino,
    StatusLinha.StatusDto status,
    List<RelatoResposta> relatos
) {
    public static LinhaResposta resumo(Linha linha, List<Relato> relatos) {
        return new LinhaResposta(
            linha.getId(), linha.getCodigo(), linha.getNome(), linha.getOrigem(), linha.getDestino(),
            StatusLinha.de(relatos), null
        );
    }

    public static LinhaResposta detalhe(Linha linha, List<Relato> relatos) {
        return new LinhaResposta(
            linha.getId(), linha.getCodigo(), linha.getNome(), linha.getOrigem(), linha.getDestino(),
            StatusLinha.de(relatos), relatos.stream().map(RelatoResposta::de).toList()
        );
    }
}
