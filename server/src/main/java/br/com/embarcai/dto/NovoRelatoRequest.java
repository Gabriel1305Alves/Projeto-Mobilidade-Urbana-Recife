package br.com.embarcai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NovoRelatoRequest(
    @NotBlank @Size(max = 30) String tipo,
    @Size(max = 280) String mensagem,
    @Size(max = 40) String autor,
    Double latitude,
    Double longitude,
    @Size(max = 120) String local
) {}
