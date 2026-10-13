package com.silva.projeto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.silva.projeto.ia.ModeloIA;
import com.silva.projeto.model.entity.Produto;
import com.silva.projeto.repository.ProdutoRepository;

class AssistenteServiceTest {

    private final ProdutoRepository produtoRepository = mock(ProdutoRepository.class);
    private final ModeloIA modeloIA = mock(ModeloIA.class);
    private final AssistenteService service = new AssistenteService(produtoRepository, modeloIA);

    @Test
    void enviaParaAIAOEstoqueAtualJuntoComAPergunta() {
        Produto notebook = new Produto("Notebook", "Notebook Dell", 3500.0, 7);
        notebook.setId(1L);

        when(produtoRepository.findAll()).thenReturn(List.of(notebook));
        when(modeloIA.responder(anyString(), anyString())).thenReturn("Há 7 notebooks.");

        String resposta = service.responder("Quantos notebooks temos?");

        ArgumentCaptor<String> instrucoes = ArgumentCaptor.forClass(String.class);
        verify(modeloIA).responder(instrucoes.capture(), eq("Quantos notebooks temos?"));

        assertThat(resposta).isEqualTo("Há 7 notebooks.");
        assertThat(instrucoes.getValue()).contains("1 | Notebook | Notebook Dell | 3500.00 | 7");
    }

    @Test
    void avisaAIAQuandoNaoHaProdutos() {
        assertThat(service.montarInstrucoes(List.of()))
                .contains("Não há nenhum produto cadastrado");
    }

    @Test
    void trataEstoqueNuloComoZero() {
        Produto semEstoque = new Produto("Teclado", "Teclado mecânico", 250.0, null);
        semEstoque.setId(3L);

        assertThat(service.montarInstrucoes(List.of(semEstoque)))
                .contains("3 | Teclado | Teclado mecânico | 250.00 | 0");
    }
}
