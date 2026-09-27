package br.com.embarcai.dto;

import br.com.embarcai.model.Usuario;

public record UsuarioResposta(Long id, String nome, String email, String role) {
    public static UsuarioResposta de(Usuario usuario) {
        return new UsuarioResposta(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getRole());
    }
}
