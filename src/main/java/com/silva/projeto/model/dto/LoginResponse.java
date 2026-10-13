package com.silva.projeto.model.dto;

import java.time.Instant;

public record LoginResponse(
        String token,
        String tipo,
        Instant expiraEm,
        UsuarioLogadoDTO funcionario) {
}
