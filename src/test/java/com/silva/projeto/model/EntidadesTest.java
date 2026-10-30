package com.silva.projeto.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.silva.projeto.model.entity.Cliente;
import com.silva.projeto.model.entity.Funcionario;
import com.silva.projeto.model.entity.ItemVenda;
import com.silva.projeto.model.entity.Produto;
import com.silva.projeto.model.entity.Venda;

/** Garante que as entidades guardam e devolvem os valores que o Hibernate e o Jackson usam. */
class EntidadesTest {

    @Test
    void vendaComItens() {
        Cliente cliente = new Cliente("Maria", "52998224725", "maria@email.com", "18999999999");
        Funcionario funcionario = new Funcionario("Carlos", "98765432100", "Gerente", "hash", 3000.0);
        Produto produto = new Produto("Água", "Água mineral 500ml", 3.5, 10);

        Venda venda = new Venda(LocalDate.of(2026, 10, 29), 7.0);
        venda.setId(5L);
        venda.setCliente(cliente);
        venda.setFuncionario(funcionario);

        ItemVenda item = new ItemVenda();
        item.setId(8L);
        item.setQuantidade(2);
        item.setSubtotal(7.0);
        item.setProduto(produto);
        item.setVenda(venda);

        List<ItemVenda> itens = new ArrayList<>();
        itens.add(item);
        venda.setItens(itens);
        venda.setDataVenda(LocalDate.of(2026, 10, 28));
        venda.setValorTotal(7.0);

        assertThat(venda.getId()).isEqualTo(5L);
        assertThat(venda.getDataVenda()).isEqualTo(LocalDate.of(2026, 10, 28));
        assertThat(venda.getValorTotal()).isEqualTo(7.0);
        assertThat(venda.getCliente()).isSameAs(cliente);
        assertThat(venda.getFuncionario()).isSameAs(funcionario);
        assertThat(venda.getItens()).containsExactly(item);

        assertThat(item.getId()).isEqualTo(8L);
        assertThat(item.getQuantidade()).isEqualTo(2);
        assertThat(item.getSubtotal()).isEqualTo(7.0);
        assertThat(item.getProduto()).isSameAs(produto);
        assertThat(item.getVenda()).isSameAs(venda);
    }

    @Test
    void clienteEFuncionarioListamSuasVendas() {
        Venda venda = new Venda(LocalDate.of(2026, 10, 29), 10.0);

        Cliente cliente = new Cliente();
        cliente.setVendas(List.of(venda));

        Funcionario funcionario = new Funcionario();
        funcionario.setVendas(List.of(venda));

        assertThat(cliente.getVendas()).containsExactly(venda);
        assertThat(funcionario.getVendas()).containsExactly(venda);
    }
}
