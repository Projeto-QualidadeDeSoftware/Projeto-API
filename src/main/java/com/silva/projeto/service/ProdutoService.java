package com.silva.projeto.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.silva.projeto.exception.ConflitoException;
import com.silva.projeto.exception.RecursoNaoEncontradoException;
import com.silva.projeto.model.entity.Produto;
import com.silva.projeto.repository.ItemVendaRepository;
import com.silva.projeto.repository.ProdutoRepository;
import com.silva.projeto.model.dto.ProdutoDTO;

@Service
public class ProdutoService {

    private final ProdutoRepository repository;
    private final ItemVendaRepository itemVendaRepository;

    public ProdutoService(ProdutoRepository repository, ItemVendaRepository itemVendaRepository) {
        this.repository = repository;
        this.itemVendaRepository = itemVendaRepository;
    }

    public List<Produto> listarTodos() {
        return repository.findAll();
    }

    public Produto buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado"));
    }

    public Produto salvar(Produto produto) {
        return repository.save(produto);
    }

    public Produto atualizar(Long id, Produto produtoAtualizado) {

        Produto produto = buscarPorId(id);

        produto.setNome(produtoAtualizado.getNome());
        produto.setDescricao(produtoAtualizado.getDescricao());
        produto.setPreco(produtoAtualizado.getPreco());
        produto.setEstoque(produtoAtualizado.getEstoque());

        return repository.save(produto);
    }

    public void deletar(Long id) {

        Produto produto = buscarPorId(id);

        if (itemVendaRepository.existsByProdutoId(id)) {
            throw new ConflitoException(
                    "Não é possível excluir o produto porque ele aparece em vendas registradas");
        }

        repository.delete(produto);
    }
    
    public ProdutoDTO converterParaDTO(Produto produto) {

        return new ProdutoDTO(
                produto.getId(),
                produto.getNome(),
                produto.getPreco(),
                produto.getEstoque()
        );
    }
}