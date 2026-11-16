package br.com.embarcai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final long JANELA_MS = 10 * 60 * 1000;
    private static final int MAXIMO = 8;

    private final Map<String, List<Long>> relatos = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> logins = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> solicitacoes = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) return true;
        String uri = request.getRequestURI();
        return !"/api/login".equals(uri)
            && !"/api/solicitacoes-linha".equals(uri)
            && !uri.matches("/api/linhas/[^/]+/relatos");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean login = "/api/login".equals(uri);
        boolean pedidoLinha = "/api/solicitacoes-linha".equals(uri);
        Map<String, List<Long>> balde = login ? logins : (pedidoLinha ? solicitacoes : relatos);
        if (estourou(balde, request.getRemoteAddr())) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(login
                ? "{\"erro\":\"Muitas tentativas de login. Espera alguns minutos e tenta de novo.\"}"
                : pedidoLinha
                    ? "{\"erro\":\"Muitos pedidos de linha seguidos. Espera alguns minutos e tenta de novo.\"}"
                    : "{\"erro\":\"Muitos relatos seguidos deste aparelho. Espera alguns minutos e tenta de novo.\"}"
            );
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean estourou(Map<String, List<Long>> balde, String ip) {
        long agora = Instant.now().toEpochMilli();
        List<Long> lista = balde.computeIfAbsent(ip, k -> new ArrayList<>());
        synchronized (lista) {
            lista.removeIf(t -> agora - t > JANELA_MS);
            if (lista.size() >= MAXIMO) return true;
            lista.add(agora);
            return false;
        }
    }
}
