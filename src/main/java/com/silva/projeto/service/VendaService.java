package com.silva.projeto.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.silva.projeto.model.entity.Cliente;
import com.silva.projeto.model.entity.Funcionario;
import com.silva.projeto.model.entity.Produto;
import com.silva.projeto.model.entity.Venda;
import com.silva.projeto.repository.ClienteRepository;
import com.silva.projeto.repository.FuncionarioRepository;
import com.silva.projeto.repository.ProdutoRepository;
import com.silva.projeto.repository.VendaRepository;
import com.silva.projeto.model.dto.ClienteDTO;
import com.silva.projeto.model.dto.FuncionarioDTO;
import com.silva.projeto.model.dto.ProdutoDTO;
import com.silva.projeto.model.dto.VendaDTO;

@Service
public class VendaService {

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
                .orElseThrow(() -> new RuntimeException("Venda não encontrada"));
    }

    public Venda salvar(Venda venda, Long clienteId, Long funcionarioId, List<Long> produtosIds) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        List<Produto> produtos = produtoRepository.findAllById(produtosIds);

        venda.setCliente(cliente);
        venda.setFuncionario(funcionario);
        venda.setProdutos(produtos);

        double total = produtos.stream()
                .mapToDouble(Produto::getPreco)
                .sum();

        venda.setValorTotal(total);

        return vendaRepository.save(venda);
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

        List<ProdutoDTO> produtosDTO = venda.getProdutos()
                .stream()
                .map(produto -> new ProdutoDTO(
                        produto.getId(),
                        produto.getNome(),
                        produto.getPreco(),
                        produto.getEstoque()
                ))
                .toList();

        return new VendaDTO(
                venda.getId(),
                venda.getDataVenda(),
                venda.getValorTotal(),
                clienteDTO,
                funcionarioDTO,
                produtosDTO
        );
    }
    
    public Venda atualizar(
            Long id,
            Venda vendaAtualizada,
            Long clienteId,
            Long funcionarioId,
            List<Long> produtosIds) {

        Venda venda = buscarPorId(id);

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new RuntimeException("Funcionário não encontrado"));

        List<Produto> produtos = produtoRepository.findAllById(produtosIds);

        venda.setDataVenda(vendaAtualizada.getDataVenda());
        venda.setCliente(cliente);
        venda.setFuncionario(funcionario);
        venda.setProdutos(produtos);

        double total = produtos.stream()
                .mapToDouble(Produto::getPreco)
                .sum();

        venda.setValorTotal(total);

        return vendaRepository.save(venda);
    }

    public void deletar(Long id) {
        vendaRepository.deleteById(id);
    }
}