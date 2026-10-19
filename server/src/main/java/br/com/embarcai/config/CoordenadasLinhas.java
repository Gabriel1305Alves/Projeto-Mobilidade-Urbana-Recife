package br.com.embarcai.config;

import br.com.embarcai.model.Linha;
import br.com.embarcai.repository.LinhaRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Preenche origem/destino no mapa das linhas cadastradas sem coordenada (ex.: pela tela de admin). */
@Component
public class CoordenadasLinhas implements ApplicationRunner {
    private final LinhaRepository linhas;
    private final Geocodificador geocodificador;

    public CoordenadasLinhas(LinhaRepository linhas, Geocodificador geocodificador) {
        this.linhas = linhas;
        this.geocodificador = geocodificador;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Linha> pendentes = linhas.findAllByOrderByCodigoAsc().stream()
            .filter(l -> l.getLatOrigem() == null || l.getLatDestino() == null)
            .toList();
        if (pendentes.isEmpty()) return;
        Thread t = new Thread(() -> pendentes.forEach(this::preencher), "coordenadas-linhas");
        t.setDaemon(true);
        t.start();
    }

    /** Busca só o que falta; devolve a própria linha, já com as coordenadas encontradas. */
    public Linha preencher(Linha linha) {
        boolean mudou = false;
        if (linha.getLatOrigem() == null) {
            var origem = geocodificador.buscar(linha.getOrigem());
            if (origem.isPresent()) {
                linha.setLatOrigem(origem.get().lat());
                linha.setLngOrigem(origem.get().lng());
                mudou = true;
            }
        }
        if (linha.getLatDestino() == null) {
            var destino = geocodificador.buscar(linha.getDestino());
            if (destino.isPresent()) {
                linha.setLatDestino(destino.get().lat());
                linha.setLngDestino(destino.get().lng());
                mudou = true;
            }
        }
        return mudou && linha.getId() != null ? linhas.save(linha) : linha;
    }
}
