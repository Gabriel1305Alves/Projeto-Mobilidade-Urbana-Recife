package br.com.embarcai.config;

import br.com.embarcai.model.Linha;
import br.com.embarcai.model.Relato;
import br.com.embarcai.repository.LinhaRepository;
import br.com.embarcai.repository.RelatoRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {
    private final LinhaRepository linhas;
    private final RelatoRepository relatos;

    public DataSeeder(LinhaRepository linhas, RelatoRepository relatos) {
        this.linhas = linhas;
        this.relatos = relatos;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (linhas.count() == 0) {
            List.of(
                nova("020", "Barra de Jangada / Rio Doce", "Barra de Jangada", "Rio Doce"),
                nova("101", "Recife / Boa Viagem", "Recife", "Boa Viagem"),
                nova("040", "Caxangá / Joana Bezerra", "TI Caxangá", "TI Joana Bezerra"),
                nova("052", "Dois Unidos / Derby", "Dois Unidos", "Derby"),
                nova("163", "Ibura / Boa Vista", "TI Ibura", "Boa Vista"),
                nova("190", "Cajueiro Seco / Conde da Boa Vista", "TI Cajueiro Seco", "Conde da Boa Vista"),
                nova("201", "Macaxeira / Caxangá", "TI Macaxeira", "Caxangá"),
                nova("321", "PE-15 / Cais de Santa Rita", "TI PE-15", "Cais de Santa Rita"),
                nova("411", "CDU / Boa Vista", "TI CDU", "Boa Vista"),
                nova("513", "PRG / Rio Doce", "TI PRG", "Rio Doce"),
                nova("1900", "Circular Centro", "Rua do Sol", "Avenida Guararapes")
            ).forEach(linhas::save);
        }

        Linha linha020 = linhas.findByCodigo("020").orElseGet(() ->
            linhas.save(nova("020", "Barra de Jangada / Rio Doce", "Barra de Jangada", "Rio Doce"))
        );

        if (!relatos.existsByLinhaAndMensagemContainingIgnoreCase(linha020, "Agamenon")) {
            relatos.save(ocorrencia(linha020, "transito", "Av. Agamenon Magalhães, sentido centro", 8, 14));
            relatos.save(ocorrencia(linha020, "alagamento", "Rua da Aurora, próximo ao Terminal", 5, 32));
        } else {
            relatos.findAll().stream()
                .filter(r -> r.getMensagem() != null)
                .forEach(r -> {
                    if (r.getMensagem().contains("Agamenon")) {
                        r.setCreatedAt(OffsetDateTime.now().minusMinutes(14));
                        relatos.save(r);
                    } else if (r.getMensagem().contains("Aurora")) {
                        r.setCreatedAt(OffsetDateTime.now().minusMinutes(32));
                        relatos.save(r);
                    }
                });
        }
    }

    private Linha nova(String codigo, String nome, String origem, String destino) {
        Linha linha = new Linha();
        linha.setCodigo(codigo);
        linha.setNome(nome);
        linha.setOrigem(origem);
        linha.setDestino(destino);
        return linha;
    }

    private Relato ocorrencia(Linha linha, String tipo, String mensagem, int confirmacoes, int minutos) {
        Relato relato = new Relato();
        relato.setLinha(linha);
        relato.setTipo(tipo);
        relato.setMensagem(mensagem);
        relato.setAutor("Comunidade");
        relato.setConfirmacoes(confirmacoes);
        relato.setCreatedAt(OffsetDateTime.now().minusMinutes(minutos));
        return relato;
    }
}
