package br.com.embarcai.config;

import br.com.embarcai.model.Linha;
import br.com.embarcai.model.Relato;
import br.com.embarcai.model.Usuario;
import br.com.embarcai.negocio.RegrasNegocio;
import br.com.embarcai.repository.LinhaRepository;
import br.com.embarcai.repository.RelatoRepository;
import br.com.embarcai.repository.UsuarioRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {
    private final LinhaRepository linhas;
    private final RelatoRepository relatos;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public DataSeeder(
        LinhaRepository linhas,
        RelatoRepository relatos,
        UsuarioRepository usuarios,
        PasswordEncoder encoder
    ) {
        this.linhas = linhas;
        this.relatos = relatos;
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUsuario("Administrador", "admin@embarcai.com", "admin123", RegrasNegocio.PAPEL_ADMIN);
        seedUsuario("Maria Passageira", "maria@embarcai.com", "123456", RegrasNegocio.PAPEL_PASSAGEIRO);

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

        posicionarLinhas();

        Linha linha020 = linhas.findByCodigo("020").orElseGet(() ->
            linhas.save(nova("020", "Barra de Jangada / Rio Doce", "Barra de Jangada", "Rio Doce"))
        );

        if (!relatos.existsByLinhaAndMensagemContainingIgnoreCase(linha020, "Agamenon")) {
            relatos.save(ocorrencia(linha020, "transito", "Av. Agamenon Magalhães, sentido centro", 8, 14,
                -8.0478, -34.8996, "Av. Agamenon Magalhães"));
            relatos.save(ocorrencia(linha020, "alagamento", "Rua da Aurora, próximo ao Terminal", 5, 32,
                -8.0588, -34.8785, "Rua da Aurora"));
        } else {
            relatos.findAll().stream()
                .filter(r -> r.getMensagem() != null)
                .forEach(r -> {
                    if (r.getMensagem().contains("Agamenon")) {
                        r.setCreatedAt(OffsetDateTime.now().minusMinutes(14));
                        r.setConfiabilidade(RegrasNegocio.confiabilidadeDe(r.getTipo(), r.getConfirmacoes()));
                        if (r.getLatitude() == null) {
                            r.setLatitude(-8.0478);
                            r.setLongitude(-34.8996);
                            r.setLocal("Av. Agamenon Magalhães");
                        }
                        relatos.save(r);
                    } else if (r.getMensagem().contains("Aurora")) {
                        r.setCreatedAt(OffsetDateTime.now().minusMinutes(32));
                        r.setConfirmacoes(Math.max(r.getConfirmacoes(), 5));
                        r.setConfiabilidade(RegrasNegocio.confiabilidadeDe(r.getTipo(), r.getConfirmacoes()));
                        if (r.getLatitude() == null) {
                            r.setLatitude(-8.0588);
                            r.setLongitude(-34.8785);
                            r.setLocal("Rua da Aurora");
                        }
                        relatos.save(r);
                    }
                });
        }
    }

    private void seedUsuario(String nome, String email, String senha, String role) {
        if (usuarios.existsByEmailIgnoreCase(email)) return;
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(encoder.encode(senha));
        usuario.setRole(role);
        usuarios.save(usuario);
    }

    private Linha nova(String codigo, String nome, String origem, String destino) {
        Linha linha = new Linha();
        linha.setCodigo(codigo);
        linha.setNome(nome);
        linha.setOrigem(origem);
        linha.setDestino(destino);
        return linha;
    }

    private Relato ocorrencia(
        Linha linha, String tipo, String mensagem, int confirmacoes, int minutos,
        double lat, double lng, String local
    ) {
        Relato relato = new Relato();
        relato.setLinha(linha);
        relato.setTipo(tipo);
        relato.setMensagem(mensagem);
        relato.setAutor("Comunidade");
        relato.setConfirmacoes(confirmacoes);
        relato.setConfiabilidade(RegrasNegocio.confiabilidadeDe(tipo, confirmacoes));
        relato.setLatitude(lat);
        relato.setLongitude(lng);
        relato.setLocal(local);
        relato.setCreatedAt(OffsetDateTime.now().minusMinutes(minutos));
        return relato;
    }

    private void posicionarLinhas() {
        posicionar("020", -8.2275, -34.9378, -7.9667, -34.8392);
        posicionar("101", -8.0632, -34.8711, -8.1220, -34.8989);
        posicionar("040", -8.0270, -34.9550, -8.0735, -34.8950);
        posicionar("052", -8.0010, -34.9170, -8.0540, -34.8990);
        posicionar("163", -8.1210, -34.9360, -8.0570, -34.8850);
        posicionar("190", -8.1680, -34.9260, -8.0575, -34.8820);
        posicionar("201", -8.0080, -34.9290, -8.0270, -34.9550);
        posicionar("321", -7.9780, -34.8580, -8.0690, -34.8740);
        posicionar("411", -8.0490, -34.9510, -8.0570, -34.8850);
        posicionar("513", -8.0890, -34.9390, -7.9667, -34.8392);
        posicionar("1900", -8.0638, -34.8730, -8.0665, -34.8775);
    }

    private void posicionar(String codigo, double latO, double lngO, double latD, double lngD) {
        linhas.findByCodigo(codigo).ifPresent(linha -> {
            linha.setLatOrigem(latO);
            linha.setLngOrigem(lngO);
            linha.setLatDestino(latD);
            linha.setLngDestino(lngD);
            linhas.save(linha);
        });
    }
}
