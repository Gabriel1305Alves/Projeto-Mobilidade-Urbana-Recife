package br.com.embarcai.controller;

import br.com.embarcai.config.ApiException;
import br.com.embarcai.config.CoordenadasLinhas;
import br.com.embarcai.dto.LinhaResposta;
import br.com.embarcai.dto.NovaSolicitacaoLinhaRequest;
import br.com.embarcai.dto.SolicitacaoLinhaResposta;
import br.com.embarcai.model.Linha;
import br.com.embarcai.model.SolicitacaoLinha;
import br.com.embarcai.model.Usuario;
import br.com.embarcai.negocio.RegrasNegocio;
import br.com.embarcai.repository.LinhaRepository;
import br.com.embarcai.repository.SolicitacaoLinhaRepository;
import br.com.embarcai.repository.UsuarioRepository;
import br.com.embarcai.security.UsuarioAutenticado;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitacoes-linha")
public class SolicitacaoLinhaController {
    private final SolicitacaoLinhaRepository solicitacoes;
    private final LinhaRepository linhas;
    private final UsuarioRepository usuarios;
    private final CoordenadasLinhas coordenadas;

    public SolicitacaoLinhaController(
        SolicitacaoLinhaRepository solicitacoes,
        LinhaRepository linhas,
        UsuarioRepository usuarios,
        CoordenadasLinhas coordenadas
    ) {
        this.solicitacoes = solicitacoes;
        this.linhas = linhas;
        this.usuarios = usuarios;
        this.coordenadas = coordenadas;
    }

    @PostMapping
    public ResponseEntity<SolicitacaoLinhaResposta> solicitar(
        @RequestBody NovaSolicitacaoLinhaRequest body,
        Authentication authentication
    ) {
        Usuario usuario = usuarioLogado(authentication);
        validarCampos(body);
        String codigo = body.codigo().trim().toUpperCase();
        if (linhas.existsByCodigoIgnoreCase(codigo)) {
            throw new ApiException(HttpStatus.CONFLICT, "Essa linha já está cadastrada. Busque pelo código na tela inicial.");
        }
        if (solicitacoes.existsByUsuarioIdAndCodigoIgnoreCaseAndStatus(
            usuario.getId(), codigo, RegrasNegocio.SOLICITACAO_PENDENTE
        )) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Você já pediu o cadastro desta linha. Aguarde a análise da equipe.");
        }
        if (solicitacoes.existsByCodigoIgnoreCaseAndStatus(codigo, RegrasNegocio.SOLICITACAO_PENDENTE)) {
            throw new ApiException(HttpStatus.CONFLICT, "Já existe um pedido pendente para este código. A equipe vai analisar.");
        }

        SolicitacaoLinha pedido = new SolicitacaoLinha();
        pedido.setCodigo(codigo);
        pedido.setNome(body.nome().trim());
        pedido.setOrigem(body.origem().trim());
        pedido.setDestino(body.destino().trim());
        pedido.setStatus(RegrasNegocio.SOLICITACAO_PENDENTE);
        pedido.setUsuario(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(SolicitacaoLinhaResposta.de(solicitacoes.save(pedido)));
    }

    @GetMapping
    public List<SolicitacaoLinhaResposta> listarPendentes() {
        return solicitacoes.findByStatusComUsuario(RegrasNegocio.SOLICITACAO_PENDENTE).stream()
            .map(SolicitacaoLinhaResposta::de)
            .toList();
    }

    @PostMapping("/{id}/aprovar")
    public LinhaResposta aprovar(@PathVariable Long id) {
        SolicitacaoLinha pedido = pendente(id);
        if (linhas.existsByCodigoIgnoreCase(pedido.getCodigo())) {
            pedido.setStatus(RegrasNegocio.SOLICITACAO_RECUSADA);
            solicitacoes.save(pedido);
            throw new ApiException(HttpStatus.CONFLICT, "Já existe uma linha com esse código. O pedido foi encerrado.");
        }
        Linha linha = new Linha();
        linha.setCodigo(pedido.getCodigo());
        linha.setNome(pedido.getNome());
        linha.setOrigem(pedido.getOrigem());
        linha.setDestino(pedido.getDestino());
        coordenadas.preencher(linha);
        Linha salva = linhas.save(linha);
        pedido.setStatus(RegrasNegocio.SOLICITACAO_APROVADA);
        solicitacoes.save(pedido);
        return LinhaResposta.resumo(salva, List.of());
    }

    @PostMapping("/{id}/recusar")
    public Map<String, Object> recusar(@PathVariable Long id) {
        SolicitacaoLinha pedido = pendente(id);
        pedido.setStatus(RegrasNegocio.SOLICITACAO_RECUSADA);
        solicitacoes.save(pedido);
        return Map.of("id", pedido.getId(), "status", pedido.getStatus());
    }

    private SolicitacaoLinha pendente(Long id) {
        SolicitacaoLinha pedido = solicitacoes.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
        if (!RegrasNegocio.SOLICITACAO_PENDENTE.equals(pedido.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Este pedido já foi analisado.");
        }
        return pedido;
    }

    private void validarCampos(NovaSolicitacaoLinhaRequest body) {
        if (body == null || body.codigo() == null || body.nome() == null || body.origem() == null || body.destino() == null
            || body.codigo().isBlank() || body.nome().isBlank() || body.origem().isBlank() || body.destino().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Preenche código, nome, origem e destino.");
        }
        String codigo = body.codigo().trim();
        if (codigo.length() > 20) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "O código da linha está longo demais.");
        }
        if (body.nome().trim().length() > 160 || body.origem().trim().length() > 120 || body.destino().trim().length() > 120) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Algum campo passou do tamanho máximo.");
        }
    }

    private Usuario usuarioLogado(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioAutenticado atual)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Faça login para solicitar o cadastro de uma linha.");
        }
        return usuarios.findById(atual.id())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Faça login para continuar."));
    }
}
