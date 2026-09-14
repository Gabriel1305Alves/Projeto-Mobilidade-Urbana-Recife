package br.com.embarcai.controller;

import br.com.embarcai.config.ApiException;
import br.com.embarcai.config.QrCodeGenerator;
import br.com.embarcai.dto.LinhaResposta;
import br.com.embarcai.dto.NovaLinhaRequest;
import br.com.embarcai.model.Linha;
import br.com.embarcai.model.Relato;
import br.com.embarcai.model.StatusLinha;
import br.com.embarcai.repository.LinhaRepository;
import br.com.embarcai.repository.RelatoRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/linhas")
public class LinhaController {
    private final LinhaRepository linhas;
    private final RelatoRepository relatos;
    private final QrCodeGenerator qrCodes;
    private final String adminSenha;

    public LinhaController(
        LinhaRepository linhas,
        RelatoRepository relatos,
        QrCodeGenerator qrCodes,
        @Value("${app.admin-senha}") String adminSenha
    ) {
        this.linhas = linhas;
        this.relatos = relatos;
        this.qrCodes = qrCodes;
        this.adminSenha = adminSenha;
    }

    @GetMapping
    public List<LinhaResposta> listar(@RequestParam(required = false) String q) {
        OffsetDateTime limite = OffsetDateTime.now().minusHours(2);
        Map<Long, List<Relato>> porLinha = relatos.findRecentesComLinha(limite).stream()
            .collect(Collectors.groupingBy(r -> r.getLinha().getId()));
        return linhas.findAllByOrderByCodigoAsc().stream()
            .filter(linha -> bateBusca(linha, q))
            .map(linha -> LinhaResposta.resumo(linha, porLinha.getOrDefault(linha.getId(), List.of())))
            .toList();
    }

    @GetMapping("/{codigo}")
    public LinhaResposta obter(@PathVariable String codigo) {
        Linha linha = linhas.findByCodigo(codigo)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Linha não encontrada."));
        List<Relato> daLinha = relatos.findTop40ByLinhaAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            linha, OffsetDateTime.now().minusHours(2)
        );
        return LinhaResposta.detalhe(linha, daLinha);
    }

    @GetMapping(value = "/{codigo}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrcode(@PathVariable String codigo, HttpServletRequest request) throws Exception {
        linhas.findByCodigo(codigo)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Linha não encontrada."));
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(qrCodes.png(codigo, request.getRequestURL().toString()));
    }

    @PostMapping
    public ResponseEntity<Linha> criar(
        @RequestBody NovaLinhaRequest body,
        @RequestHeader(value = "x-admin-senha", required = false) String senhaHeader
    ) {
        String senha = senhaHeader != null ? senhaHeader : body.senha();
        if (!adminSenha.equals(senha)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Senha de administração inválida.");
        }
        if (body.codigo() == null || body.nome() == null || body.origem() == null || body.destino() == null
            || body.codigo().isBlank() || body.nome().isBlank() || body.origem().isBlank() || body.destino().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Preenche código, nome, origem e destino.");
        }
        String codigo = body.codigo().trim().toUpperCase();
        if (linhas.existsByCodigo(codigo)) {
            throw new ApiException(HttpStatus.CONFLICT, "Já existe uma linha com esse código.");
        }
        Linha linha = new Linha();
        linha.setCodigo(codigo);
        linha.setNome(body.nome().trim());
        linha.setOrigem(body.origem().trim());
        linha.setDestino(body.destino().trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(linhas.save(linha));
    }

    private boolean bateBusca(Linha linha, String q) {
        if (q == null || q.isBlank()) return true;
        String termo = StatusLinha.semAcento(q);
        return StatusLinha.semAcento(linha.getCodigo()).contains(termo)
            || StatusLinha.semAcento(linha.getNome()).contains(termo)
            || StatusLinha.semAcento(linha.getOrigem()).contains(termo)
            || StatusLinha.semAcento(linha.getDestino()).contains(termo);
    }
}
