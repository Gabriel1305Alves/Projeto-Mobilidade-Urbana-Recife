package br.com.embarcai.ia;

import br.com.embarcai.model.StatusLinha;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Transforma texto livre em atributos para os modelos: minúsculas, sem acento,
 * sem stopwords e com um radical de 4 letras para juntar variações
 * ("alagou", "alagada", "alagamento" → "alag").
 */
public final class Tokenizador {
    private static final int TAMANHO_RADICAL = 4;
    private static final Set<String> STOPWORDS = Set.of(
        "a", "o", "as", "os", "um", "uma", "uns", "umas", "de", "da", "do", "das", "dos",
        "em", "na", "no", "nas", "nos", "por", "pra", "pro", "para", "com", "sem", "e", "ou",
        "que", "se", "ao", "aos", "the", "me", "eu", "voce", "vc", "ja", "mas", "mais", "muito",
        "muita", "ta", "tah", "esta", "estao", "tem", "ter", "foi", "ser", "sao", "isso", "esse",
        "essa", "este", "aqui", "ali", "la", "perto", "proximo", "proxima", "sentido", "agora"
    );

    private Tokenizador() {}

    public static List<String> tokens(String texto) {
        List<String> saida = new ArrayList<>();
        for (String palavra : StatusLinha.semAcento(texto).split("[^a-z0-9]+")) {
            if (palavra.length() < 2 || STOPWORDS.contains(palavra) || palavra.chars().allMatch(Character::isDigit)) {
                continue;
            }
            saida.add(palavra);
            if (palavra.length() > TAMANHO_RADICAL + 1) {
                saida.add(palavra.substring(0, TAMANHO_RADICAL) + "~");
            }
        }
        return saida;
    }
}
