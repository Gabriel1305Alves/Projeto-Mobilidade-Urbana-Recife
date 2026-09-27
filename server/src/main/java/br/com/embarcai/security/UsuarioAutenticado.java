package br.com.embarcai.security;

public record UsuarioAutenticado(Long id, String nome, String email, String role) {}
