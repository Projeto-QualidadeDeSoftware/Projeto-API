package com.silva.projeto.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.silva.projeto.ia.ModeloIA;
import com.silva.projeto.model.entity.Produto;
import com.silva.projeto.repository.ProdutoRepository;

/**
 * Responde perguntas sobre o estoque. A cada pergunta, a situação atual dos
 * produtos é lida do banco e enviada junto para a IA, que só redige a resposta:
 * os números vêm sempre do banco, nunca da "memória" do modelo.
 */
@Service
public class AssistenteService {

    private final ProdutoRepository produtoRepository;
    private final ModeloIA modeloIA;

    public AssistenteService(ProdutoRepository produtoRepository, ModeloIA modeloIA) {
        this.produtoRepository = produtoRepository;
        this.modeloIA = modeloIA;
    }

    public String responder(String pergunta) {

        List<Produto> produtos = produtoRepository.findAll();

        return modeloIA.responder(montarInstrucoes(produtos), pergunta);
    }

    String montarInstrucoes(List<Produto> produtos) {

        StringBuilder texto = new StringBuilder();

        texto.append("Você é o assistente de estoque de um sistema de vendas.\n");
        texto.append("Responda em português, de forma curta e direta.\n");
        texto.append("Use SOMENTE os dados da lista abaixo; não invente produtos, preços nem quantidades.\n");
        texto.append("Se a pergunta não for sobre produtos ou estoque, ou se a resposta não estiver nos dados, ");
        texto.append("diga que não tem essa informação.\n");
        texto.append("Data de hoje: ").append(LocalDate.now()).append("\n\n");

        if (produtos.isEmpty()) {
            texto.append("Não há nenhum produto cadastrado.");
            return texto.toString();
        }

        texto.append("Produtos cadastrados (id | nome | descrição | preço em reais | unidades em estoque):\n");

        for (Produto produto : produtos) {
            texto.append(produto.getId()).append(" | ")
                    .append(produto.getNome()).append(" | ")
                    .append(produto.getDescricao()).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", produto.getPreco())).append(" | ")
                    .append(produto.getEstoque() == null ? 0 : produto.getEstoque())
                    .append("\n");
        }

        return texto.toString();
    }
}
