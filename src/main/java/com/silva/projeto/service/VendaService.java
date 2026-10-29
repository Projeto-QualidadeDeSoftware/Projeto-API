package com.silva.projeto.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.silva.projeto.exception.ConflitoException;
import com.silva.projeto.exception.DadoInvalidoException;
import com.silva.projeto.exception.RecursoNaoEncontradoException;
import com.silva.projeto.model.entity.Cliente;
import com.silva.projeto.model.entity.Funcionario;
import com.silva.projeto.model.entity.ItemVenda;
import com.silva.projeto.model.entity.Produto;
import com.silva.projeto.model.entity.Venda;
import com.silva.projeto.repository.ClienteRepository;
import com.silva.projeto.repository.FuncionarioRepository;
import com.silva.projeto.repository.ProdutoRepository;
import com.silva.projeto.repository.VendaRepository;
import com.silva.projeto.model.dto.ClienteDTO;
import com.silva.projeto.model.dto.FuncionarioDTO;
import com.silva.projeto.model.dto.ItemVendaDTO;
import com.silva.projeto.model.dto.ItemVendaRequest;
import com.silva.projeto.model.dto.VendaDTO;
import com.silva.projeto.model.dto.VendaRequest;

@Service
public class VendaService {

    /** A loja fica no Brasil: "hoje" é o dia em Brasília, mesmo com o servidor em UTC. */
    private static final ZoneId FUSO_DA_LOJA = ZoneId.of("America/Sao_Paulo");

    private final VendaRepository vendaRepository;
    private final ClienteRepository clienteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ProdutoRepository produtoRepository;

    public VendaService(
            VendaRepository vendaRepository,
            ClienteRepository clienteRepository,
            FuncionarioRepository funcionarioRepository,
            ProdutoRepository produtoRepository) {

        this.vendaRepository = vendaRepository;
        this.clienteRepository = clienteRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.produtoRepository = produtoRepository;
    }

    public List<Venda> listarTodos() {
        return vendaRepository.findAll();
    }

    public Venda buscarPorId(Long id) {
        return vendaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Venda não encontrada"));
    }

    @Transactional
    public Venda salvar(VendaRequest dados) {

        Venda venda = new Venda();

        preencher(venda, dados);

        return vendaRepository.save(venda);
    }

    @Transactional
    public Venda atualizar(Long id, VendaRequest dados) {

        Venda venda = buscarPorId(id);

        devolverEstoque(venda);
        venda.getItens().clear();

        preencher(venda, dados);

        return vendaRepository.save(venda);
    }

    /** Excluir uma venda equivale a cancelá-la: os produtos voltam para o estoque. */
    @Transactional
    public void deletar(Long id) {

        Venda venda = buscarPorId(id);

        devolverEstoque(venda);

        vendaRepository.delete(venda);
    }

    /**
     * Monta a venda a partir do pedido, dando baixa no estoque de cada produto.
     * Se faltar estoque de qualquer item, nada é gravado (a transação é desfeita).
     */
    private void preencher(Venda venda, VendaRequest dados) {

        if (dados.dataVenda().isAfter(LocalDate.now(FUSO_DA_LOJA))) {
            throw new DadoInvalidoException("A data da venda não pode ser no futuro");
        }

        Cliente cliente = clienteRepository.findById(dados.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));

        Funcionario funcionario = funcionarioRepository.findById(dados.funcionarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário não encontrado"));

        venda.setDataVenda(dados.dataVenda());
        venda.setCliente(cliente);
        venda.setFuncionario(funcionario);

        // O mesmo produto enviado em duas linhas vira um item só.
        Map<Long, Integer> quantidadePorProduto = new LinkedHashMap<>();

        for (ItemVendaRequest item : dados.itens()) {
            quantidadePorProduto.merge(item.produtoId(), item.quantidade(), Integer::sum);
        }

        double total = 0;

        for (Map.Entry<Long, Integer> entrada : quantidadePorProduto.entrySet()) {

            Long produtoId = entrada.getKey();
            int quantidade = entrada.getValue();

            Produto produto = produtoRepository.findById(produtoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Produto não encontrado: id " + produtoId));

            int estoque = produto.getEstoque() == null ? 0 : produto.getEstoque();

            if (quantidade > estoque) {
                throw new ConflitoException(
                        "Estoque insuficiente para " + produto.getNome()
                        + ": disponível " + estoque
                        + ", solicitado " + quantidade);
            }

            produto.setEstoque(estoque - quantidade);

            double subtotal = produto.getPreco() * quantidade;

            venda.getItens().add(new ItemVenda(quantidade, subtotal, produto, venda));

            total += subtotal;
        }

        venda.setValorTotal(total);
    }

    private void devolverEstoque(Venda venda) {

        for (ItemVenda item : venda.getItens()) {

            Produto produto = item.getProduto();
            int estoque = produto.getEstoque() == null ? 0 : produto.getEstoque();

            produto.setEstoque(estoque + item.getQuantidade());
        }
    }

    public VendaDTO converterParaDTO(Venda venda) {

        ClienteDTO clienteDTO = new ClienteDTO(
                venda.getCliente().getId(),
                venda.getCliente().getNome(),
                venda.getCliente().getCpf(),
                venda.getCliente().getEmail(),
                venda.getCliente().getTelefone()
        );

        FuncionarioDTO funcionarioDTO = new FuncionarioDTO(
                venda.getFuncionario().getId(),
                venda.getFuncionario().getNome(),
                venda.getFuncionario().getCpf(),
                venda.getFuncionario().getCargo(),
                venda.getFuncionario().getSalario()
        );

        List<ItemVendaDTO> itensDTO = venda.getItens()
                .stream()
                .map(item -> new ItemVendaDTO(
                        item.getProduto().getId(),
                        item.getProduto().getNome(),
                        item.getQuantidade(),
                        item.getSubtotal()
                ))
                .toList();

        return new VendaDTO(
                venda.getId(),
                venda.getDataVenda(),
                venda.getValorTotal(),
                clienteDTO,
                funcionarioDTO,
                itensDTO
        );
    }
}
