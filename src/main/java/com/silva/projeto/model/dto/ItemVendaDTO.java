package com.silva.projeto.model.dto;

public record ItemVendaDTO(
        Long produtoId,
        String produto,
        Integer quantidade,
        Double subtotal) {
}
