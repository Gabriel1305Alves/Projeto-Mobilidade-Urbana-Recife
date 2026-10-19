package br.com.embarcai.ia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

/** Lê arquivos "classe|texto" do classpath e treina um modelo com eles. */
public final class Corpus {
    private Corpus() {}

    public static void treinar(NaiveBayes modelo, String caminho) {
        try (BufferedReader leitor = new BufferedReader(
            new InputStreamReader(new ClassPathResource(caminho).getInputStream(), StandardCharsets.UTF_8)
        )) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                linha = linha.trim();
                int separador = linha.indexOf('|');
                if (linha.isEmpty() || linha.startsWith("#") || separador < 1) continue;
                modelo.treinar(linha.substring(0, separador).trim(), linha.substring(separador + 1));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler o corpus " + caminho, e);
        }
    }
}
