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
    List<RelatoResposta> relatos,
    Double distanciaKm,
    Double latOrigem,
    Double lngOrigem,
    Double latDestino,
    Double lngDestino
) {
    public static LinhaResposta resumo(Linha linha, List<Relato> relatos) {
        return de(linha, relatos, null, null);
    }

    public static LinhaResposta resumo(Linha linha, List<Relato> relatos, Double distanciaKm) {
        return de(linha, relatos, null, distanciaKm);
    }

    public static LinhaResposta detalhe(Linha linha, List<Relato> relatos) {
        return de(linha, relatos, relatos.stream().map(RelatoResposta::de).toList(), null);
    }

    private static LinhaResposta de(Linha linha, List<Relato> relatos, List<RelatoResposta> lista, Double distanciaKm) {
        return new LinhaResposta(
            linha.getId(), linha.getCodigo(), linha.getNome(), linha.getOrigem(), linha.getDestino(),
            StatusLinha.de(relatos), lista, distanciaKm,
            linha.getLatOrigem(), linha.getLngOrigem(), linha.getLatDestino(), linha.getLngDestino()
        );
    }
}
