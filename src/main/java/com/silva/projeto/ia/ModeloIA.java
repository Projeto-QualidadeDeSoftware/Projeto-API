package com.silva.projeto.ia;

/**
 * Contrato com o provedor de IA. Para trocar a Mistral por outro provedor,
 * basta criar outra classe que implemente esta interface.
 */
public interface ModeloIA {

    /**
     * @param instrucoes regras e dados que o modelo deve usar para responder
     * @param pergunta   texto digitado pelo usuário
     * @return resposta em texto
     */
    String responder(String instrucoes, String pergunta);
}
