package br.com.embarcai.ia;

import br.com.embarcai.dto.AssistenteResposta;
import br.com.embarcai.dto.PrevisaoResposta;
import br.com.embarcai.dto.RecomendacaoResposta;
import br.com.embarcai.model.Linha;
import br.com.embarcai.model.Relato;
import br.com.embarcai.model.StatusLinha;
import br.com.embarcai.negocio.RegrasNegocio;
import br.com.embarcai.repository.LinhaRepository;
import br.com.embarcai.repository.RelatoRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Chatbot do Embarcaí: identifica a intenção da pergunta com Naive Bayes,
 * extrai a linha e a hora citadas e responde com dados reais do sistema.
 */
@Service
public class Assistente {
    static final double LIMIAR_INTENCAO = 0.4;
    static final String CORPUS = "ia/intencoes-treino.txt";
    private static final Pattern CODIGO = Pattern.compile("(?<!\\d)(\\d{3,4})(?!\\d)");
    private static final Pattern HORA = Pattern.compile("(?<!\\d)([01]?\\d|2[0-3])\\s*(?:h\\b|hs\\b|horas?\\b|:00)");

    private final NaiveBayes intencoes = new NaiveBayes();
    private final LinhaRepository linhas;
    private final RelatoRepository relatos;
    private final PrevisaoRisco previsao;
    private final RecomendadorLinhas recomendador;

    public Assistente(
        LinhaRepository linhas, RelatoRepository relatos, PrevisaoRisco previsao, RecomendadorLinhas recomendador
    ) {
        this.linhas = linhas;
        this.relatos = relatos;
        this.previsao = previsao;
        this.recomendador = recomendador;
        Corpus.treinar(intencoes, CORPUS);
    }

    public AssistenteResposta responder(String mensagem, String linhaContexto) {
        if (mensagem == null || mensagem.isBlank()) {
            return resposta("Me conta o que você quer saber sobre o seu ônibus.", null, 0, null);
        }
        Optional<Linha> citada = linhaCitada(mensagem);
        Linha linha = citada.or(() -> linhaPorCodigo(linhaContexto)).orElse(null);
        Integer hora = horaCitada(mensagem);

        Map<String, Double> probabilidades = intencoes.probabilidades(mensagem);
        String intencao = null;
        double confianca = 0;
        if (!probabilidades.isEmpty()) {
            Map.Entry<String, Double> melhor = probabilidades.entrySet().iterator().next();
            if (melhor.getValue() >= LIMIAR_INTENCAO) {
                intencao = melhor.getKey();
                confianca = Math.round(melhor.getValue() * 100) / 100.0;
            }
        }
        if (hora != null && (intencao == null || intencao.equals("status_linha"))) intencao = "previsao";
        if (intencao == null && citada.isPresent()) intencao = "status_linha";
        if (intencao == null) {
            return resposta(
                "Não entendi bem. Posso dizer como está uma linha, prever o risco de atraso "
                    + "nas próximas horas, sugerir outra linha para o mesmo trajeto ou avisar onde tem alagamento.",
                null, 0, linha
            );
        }

        return switch (intencao) {
            case "saudacao" -> resposta(
                "Olá! Sou o assistente do Embarcaí. Pergunte, por exemplo, “como está a 020?” "
                    + "ou “vai atrasar às 18h?”.",
                intencao, confianca, linha
            );
            case "agradecimento" -> resposta("Por nada! Boa viagem 🚌", intencao, confianca, linha);
            case "status_linha" -> linha == null ? pedirLinha(intencao, confianca) : status(linha, confianca);
            case "previsao" -> linha == null ? pedirLinha(intencao, confianca) : previsao(linha, hora, confianca);
            case "alternativa" -> linha == null ? pedirLinha(intencao, confianca) : alternativas(linha, confianca);
            case "alagamento" -> alagamentos(linha, confianca);
            case "relatar" -> comoRelatar(linha, confianca);
            case "linhas" -> listarLinhas(confianca);
            default -> resposta(
                "O Embarcaí junta alertas de quem está no trajeto (trânsito, alagamento, bloqueio e acidente) "
                    + "e usa IA para sugerir o tipo do relato, prever o risco de atraso, recomendar linhas alternativas "
                    + "e responder aqui no chat.",
                intencao, confianca, linha
            );
        };
    }

    private AssistenteResposta status(Linha linha, double confianca) {
        List<Relato> ativos = relatos.findTop40ByLinhaAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            linha, RegrasNegocio.inicioJanelaAtiva()
        );
        String cabecalho = "Linha " + linha.getCodigo() + " (" + linha.getNome() + ")";
        String texto;
        if (ativos.isEmpty()) {
            PrevisaoResposta p = previsao.prever(linha);
            texto = cabecalho + ": trajeto tranquilo, nenhuma ocorrência nas últimas 2 horas. "
                + "Previsão da IA para agora: " + p.nivelRotulo().toLowerCase() + " (" + p.probabilidade() + "%).";
        } else {
            texto = cabecalho + " tem " + ativos.size() + (ativos.size() == 1 ? " ocorrência ativa:\n" : " ocorrências ativas:\n")
                + ativos.stream().limit(4).map(r -> "• " + descrever(r)).collect(Collectors.joining("\n"));
        }
        return resposta(texto, "status_linha", confianca, linha,
            new AssistenteResposta.Acao("Abrir linha " + linha.getCodigo(), "/linha/" + linha.getCodigo()));
    }

    private AssistenteResposta previsao(Linha linha, Integer hora, double confianca) {
        String texto;
        if (hora != null) {
            PrevisaoRisco.Estimativa e = previsao.preverAs(linha, hora);
            int pct = (int) Math.round(e.probabilidade() * 100);
            texto = "Previsão para a linha " + linha.getCodigo() + " às " + hora + "h: "
                + PrevisaoRisco.rotuloNivel(PrevisaoRisco.nivel(e.probabilidade())).toLowerCase() + " (" + pct + "%). "
                + "Mais provável: " + StatusLinha.rotulo(e.tipoProvavel()).toLowerCase() + ". "
                + "Motivos: " + motivos(e.fatores()) + ".";
        } else {
            PrevisaoResposta p = previsao.prever(linha);
            PrevisaoResposta.Hora melhor = p.proximasHoras().stream()
                .min((a, b) -> Integer.compare(a.probabilidade(), b.probabilidade()))
                .orElseThrow();
            texto = "Previsão para a linha " + linha.getCodigo() + " agora: " + p.nivelRotulo().toLowerCase()
                + " (" + p.probabilidade() + "%). Próximas horas: "
                + p.proximasHoras().stream().map(h -> h.hora() + "h " + h.probabilidade() + "%")
                    .collect(Collectors.joining(", "))
                + ". Melhor horário para sair: " + melhor.hora() + "h. "
                + "Motivos: " + motivos(p.fatores()) + ".";
        }
        return resposta(texto, "previsao", confianca, linha,
            new AssistenteResposta.Acao("Abrir linha " + linha.getCodigo(), "/linha/" + linha.getCodigo()));
    }

    private static String motivos(List<PrevisaoResposta.Fator> fatores) {
        return fatores.stream().map(PrevisaoResposta.Fator::texto).collect(Collectors.joining("; ")).toLowerCase();
    }

    private AssistenteResposta alternativas(Linha linha, double confianca) {
        RecomendacaoResposta r = recomendador.recomendar(linha);
        if (r.alternativas().isEmpty()) {
            return resposta("Não encontrei outra linha que atenda o mesmo trajeto da " + linha.getCodigo() + ".",
                "alternativa", confianca, linha);
        }
        RecomendacaoResposta.Alternativa melhor = r.alternativas().get(0);
        String texto = "Alternativas à linha " + linha.getCodigo() + " (risco agora: " + r.probabilidadeAtual() + "%):\n"
            + r.alternativas().stream()
                .map(a -> "• " + a.codigo() + " — " + a.nome() + ": " + a.nivelRotulo().toLowerCase()
                    + " (" + a.probabilidade() + "%). " + String.join("; ", a.motivos()) + ".")
                .collect(Collectors.joining("\n"));
        return resposta(texto, "alternativa", confianca, linha,
            new AssistenteResposta.Acao("Abrir linha " + melhor.codigo(), "/linha/" + melhor.codigo()));
    }

    private AssistenteResposta alagamentos(Linha linha, double confianca) {
        List<Relato> ativos = relatos.findRecentesComLinha(RegrasNegocio.inicioJanelaAtiva()).stream()
            .filter(r -> RegrasNegocio.TIPO_ALAGAMENTO.equals(r.getTipo()))
            .filter(r -> linha == null || r.getLinha().getId().equals(linha.getId()))
            .toList();
        String onde = linha == null ? "" : " na linha " + linha.getCodigo();
        if (ativos.isEmpty()) {
            return resposta("Nenhum alagamento relatado" + onde + " nas últimas 2 horas.", "alagamento", confianca, linha);
        }
        String texto = "Alagamentos relatados" + onde + " nas últimas 2 horas:\n"
            + ativos.stream().limit(5)
                .map(r -> "• Linha " + r.getLinha().getCodigo() + ": " + descrever(r))
                .collect(Collectors.joining("\n"));
        return resposta(texto, "alagamento", confianca, linha);
    }

    private AssistenteResposta comoRelatar(Linha linha, double confianca) {
        String texto = "Abra a linha e toque em “+ Informar ocorrência” (é preciso estar logado). "
            + "Ao escrever a descrição, a IA sugere o tipo automaticamente. "
            + "Para confirmar um alerta de outra pessoa, toque no número de confirmações dele.";
        AssistenteResposta.Acao acao = linha == null ? null
            : new AssistenteResposta.Acao("Informar na linha " + linha.getCodigo(), "/linha/" + linha.getCodigo() + "/relatar");
        return resposta(texto, "relatar", confianca, linha, acao);
    }

    private AssistenteResposta listarLinhas(double confianca) {
        String texto = "Linhas cadastradas:\n" + linhas.findAllByOrderByCodigoAsc().stream()
            .map(l -> "• " + l.getCodigo() + " — " + l.getNome())
            .collect(Collectors.joining("\n"));
        return resposta(texto, "linhas", confianca, null);
    }

    private AssistenteResposta pedirLinha(String intencao, double confianca) {
        return resposta(
            "De qual linha você quer saber? Digite o número, por exemplo: “como está a 020?”.",
            intencao, confianca, null
        );
    }

    private String descrever(Relato r) {
        long minutos = Math.max(0, Duration.between(r.getCreatedAt(), OffsetDateTime.now()).toMinutes());
        String local = r.getLocal() != null ? r.getLocal() : r.getMensagem();
        String selo = RegrasNegocio.CONFIRMADO_COMUNIDADE.equals(r.getConfiabilidade())
            ? ", confirmado pela comunidade"
            : ", " + r.getConfirmacoes() + (r.getConfirmacoes() == 1 ? " confirmação" : " confirmações");
        return StatusLinha.rotulo(r.getTipo()) + (local == null ? "" : " em " + local)
            + " — há " + minutos + " min" + selo;
    }

    Optional<Linha> linhaCitada(String mensagem) {
        Matcher m = CODIGO.matcher(mensagem);
        while (m.find()) {
            Optional<Linha> linha = linhas.findByCodigoIgnoreCase(m.group(1));
            if (linha.isPresent()) return linha;
        }
        String texto = StatusLinha.semAcento(mensagem);
        List<Linha> porNome = linhas.findAllByOrderByCodigoAsc().stream()
            .filter(l -> contem(texto, l.getNome()) || contem(texto, l.getOrigem()) || contem(texto, l.getDestino()))
            .toList();
        return porNome.size() == 1 ? Optional.of(porNome.get(0)) : Optional.empty();
    }

    static Integer horaCitada(String mensagem) {
        Matcher m = HORA.matcher(StatusLinha.semAcento(mensagem));
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private static boolean contem(String texto, String trecho) {
        String alvo = StatusLinha.semAcento(trecho);
        return alvo.length() >= 5 && texto.contains(alvo);
    }

    private Optional<Linha> linhaPorCodigo(String codigo) {
        return codigo == null || codigo.isBlank() ? Optional.empty() : linhas.findByCodigoIgnoreCase(codigo.trim());
    }

    private AssistenteResposta resposta(String texto, String intencao, double confianca, Linha linha) {
        return resposta(texto, intencao, confianca, linha, null);
    }

    private AssistenteResposta resposta(
        String texto, String intencao, double confianca, Linha linha, AssistenteResposta.Acao acao
    ) {
        String codigo = linha == null ? null : linha.getCodigo();
        List<String> sugestoes = codigo == null
            ? List.of("Como está a linha 020?", "Tem alagamento agora?", "Quais linhas existem?")
            : List.of("Previsão para a " + codigo, "Vai atrasar às 18h?", "Outra linha para esse trajeto?");
        return new AssistenteResposta(texto, intencao, confianca, codigo, sugestoes, acao);
    }
}
