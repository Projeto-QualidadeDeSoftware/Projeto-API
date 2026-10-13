package com.silva.projeto.model.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record VendaRequest(

        @NotNull(message = "Data da venda é obrigatória")
        LocalDate dataVenda,

        @NotNull(message = "Cliente é obrigatório")
        Long clienteId,

        @NotNull(message = "Funcionário é obrigatório")
        Long funcionarioId,

        @NotEmpty(message = "A venda deve possuir pelo menos um item")
        List<@NotNull(message = "Item inválido") @Valid ItemVendaRequest> itens) {
}
