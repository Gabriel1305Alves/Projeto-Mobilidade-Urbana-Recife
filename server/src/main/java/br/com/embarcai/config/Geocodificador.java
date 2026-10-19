package br.com.embarcai.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Descobre a coordenada de um lugar do Grande Recife pelo nome, usando o Nominatim (OpenStreetMap).
 * A política de uso do Nominatim exige User-Agent identificado e no máximo 1 requisição por segundo.
 */
@Component
public class Geocodificador {
    private static final Logger log = LoggerFactory.getLogger(Geocodificador.class);
    private static final String URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "Embarcai/1.0 (projeto academico, Grande Recife)";
    /** Caixa que cobre a Região Metropolitana do Recife: oeste, norte, leste, sul. */
    private static final String GRANDE_RECIFE = "-35.25,-7.65,-34.80,-8.45";
    private static final long INTERVALO_MS = 1100;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json;
    private long ultimaChamada;

    public Geocodificador(ObjectMapper json) {
        this.json = json;
    }

    public synchronized Optional<Coordenada> buscar(String lugar) {
        if (lugar == null || lugar.isBlank()) return Optional.empty();
        try {
            long espera = ultimaChamada + INTERVALO_MS - System.currentTimeMillis();
            if (espera > 0) Thread.sleep(espera);
            ultimaChamada = System.currentTimeMillis();

            String query = "q=" + URLEncoder.encode(lugar.trim(), StandardCharsets.UTF_8)
                + "&format=jsonv2&limit=1&countrycodes=br&bounded=1&viewbox=" + GRANDE_RECIFE;
            HttpRequest request = HttpRequest.newBuilder(URI.create(URL + "?" + query))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
            HttpResponse<String> resposta = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() != 200) return Optional.empty();

            JsonNode resultados = json.readTree(resposta.body());
            if (!resultados.isArray() || resultados.isEmpty()) return Optional.empty();
            JsonNode primeiro = resultados.get(0);
            return Optional.of(new Coordenada(primeiro.get("lat").asDouble(), primeiro.get("lon").asDouble()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Não foi possível localizar \"{}\": {}", lugar, e.getMessage());
            return Optional.empty();
        }
    }

    public record Coordenada(double lat, double lng) {}
}
