package com.silva.projeto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.silva.projeto.exception.ConflitoException;
import com.silva.projeto.model.dto.ItemVendaRequest;
import com.silva.projeto.model.dto.VendaRequest;
import com.silva.projeto.model.entity.Cliente;
import com.silva.projeto.model.entity.Funcionario;
import com.silva.projeto.model.entity.ItemVenda;
import com.silva.projeto.model.entity.Produto;
import com.silva.projeto.model.entity.Venda;
import com.silva.projeto.repository.ClienteRepository;
import com.silva.projeto.repository.FuncionarioRepository;
import com.silva.projeto.repository.ProdutoRepository;
import com.silva.projeto.repository.VendaRepository;

@ExtendWith(MockitoExtension.class)
class VendaServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 11);

    @Mock
    private VendaRepository vendaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private VendaService service;

    private Produto notebook;
    private Produto mouse;

    @BeforeEach
    void criarProdutos() {
        notebook = new Produto("Notebook", "Notebook Dell", 3500.0, 10);
        notebook.setId(1L);

        mouse = new Produto("Mouse", "Mouse sem fio", 80.0, 5);
        mouse.setId(2L);
    }

    private void existemClienteEFuncionario() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(new Cliente()));
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(new Funcionario()));
    }

    private VendaRequest pedido(ItemVendaRequest... itens) {
        return new VendaRequest(HOJE, 1L, 1L, List.of(itens));
    }

    @Test
    void salvarCalculaTotalEDaBaixaNoEstoque() {
        existemClienteEFuncionario();
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(notebook));
        when(produtoRepository.findById(2L)).thenReturn(Optional.of(mouse));
        when(vendaRepository.save(any(Venda.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Venda venda = service.salvar(pedido(
                new ItemVendaRequest(1L, 2),
                new ItemVendaRequest(2L, 3)));

        assertThat(venda.getValorTotal()).isEqualTo(2 * 3500.0 + 3 * 80.0);
        assertThat(venda.getItens()).hasSize(2);
        assertThat(notebook.getEstoque()).isEqualTo(8);
        assertThat(mouse.getEstoque()).isEqualTo(2);
    }

    @Test
    void salvarSomaQuantidadesDoMesmoProduto() {
        existemClienteEFuncionario();
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(notebook));
        when(vendaRepository.save(any(Venda.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Venda venda = service.salvar(pedido(
                new ItemVendaRequest(1L, 4),
                new ItemVendaRequest(1L, 5)));

        assertThat(venda.getItens()).hasSize(1);
        assertThat(venda.getItens().get(0).getQuantidade()).isEqualTo(9);
        assertThat(notebook.getEstoque()).isEqualTo(1);
    }

    @Test
    void salvarRecusaVendaAcimaDoEstoque() {
        existemClienteEFuncionario();
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(notebook));

        assertThatThrownBy(() -> service.salvar(pedido(new ItemVendaRequest(1L, 11))))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("Estoque insuficiente para Notebook");

        assertThat(notebook.getEstoque()).isEqualTo(10);
        verify(vendaRepository, never()).save(any());
    }

    @Test
    void salvarPermiteVenderExatamenteTodoOEstoque() {
        existemClienteEFuncionario();
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(notebook));
        when(vendaRepository.save(any(Venda.class))).thenAnswer(chamada -> chamada.getArgument(0));

        service.salvar(pedido(new ItemVendaRequest(1L, 10)));

        assertThat(notebook.getEstoque()).isZero();
    }

    @Test
    void salvarFalhaQuandoProdutoNaoExiste() {
        existemClienteEFuncionario();
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(pedido(new ItemVendaRequest(99L, 1))))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Produto não encontrado");

        verify(vendaRepository, never()).save(any());
    }

    @Test
    void salvarFalhaQuandoClienteNaoExiste() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(pedido(new ItemVendaRequest(1L, 1))))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cliente não encontrado");
    }

    @Test
    void deletarDevolveOsProdutosAoEstoque() {
        Venda venda = vendaComItem(notebook, 3);
        when(vendaRepository.findById(7L)).thenReturn(Optional.of(venda));

        service.deletar(7L);

        assertThat(notebook.getEstoque()).isEqualTo(13);
        verify(vendaRepository).delete(venda);
    }

    @Test
    void atualizarDevolveOEstoqueAntigoAntesDeBaixarONovo() {
        // A venda antiga já tinha baixado 3 notebooks: restam 7 no estoque.
        notebook.setEstoque(7);
        Venda venda = vendaComItem(notebook, 3);

        existemClienteEFuncionario();
        when(vendaRepository.findById(7L)).thenReturn(Optional.of(venda));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(notebook));
        when(vendaRepository.save(any(Venda.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Venda atualizada = service.atualizar(7L, pedido(new ItemVendaRequest(1L, 10)));

        assertThat(notebook.getEstoque()).isZero();
        assertThat(atualizada.getItens()).hasSize(1);
        assertThat(atualizada.getValorTotal()).isEqualTo(35000.0);
    }

    private Venda vendaComItem(Produto produto, int quantidade) {
        Venda venda = new Venda();
        venda.getItens().add(new ItemVenda(
                quantidade, produto.getPreco() * quantidade, produto, venda));
        return venda;
    }
}
