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
    private final Map<String, List<Long>> envios = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(request.getMethod().equals("POST") && request.getRequestURI().matches("/api/linhas/[^/]+/relatos"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        long agora = Instant.now().toEpochMilli();
        List<Long> lista = envios.computeIfAbsent(ip, k -> new ArrayList<>());
        synchronized (lista) {
            lista.removeIf(t -> agora - t > 10 * 60 * 1000);
            if (lista.size() >= 8) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                    "{\"erro\":\"Muitos relatos seguidos deste aparelho. Espera alguns minutos e tenta de novo.\"}"
                );
                return;
            }
            lista.add(agora);
        }
        filterChain.doFilter(request, response);
    }
}
