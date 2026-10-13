package com.silva.projeto.ia;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.silva.projeto.exception.AssistenteIndisponivelException;

/** Implementação do {@link ModeloIA} usando a API de chat da Mistral. */
@Component
public class MistralModeloIA implements ModeloIA {

    private final RestClient cliente;
    private final String apiKey;
    private final String modelo;

    @Autowired
    public MistralModeloIA(
            @Value("${mistral.url}") String url,
            @Value("${mistral.api-key}") String apiKey,
            @Value("${mistral.modelo}") String modelo) {

        this(criarCliente(url), apiKey, modelo);
    }

    /** Usado pelos testes, que passam um cliente HTTP simulado. */
    MistralModeloIA(RestClient cliente, String apiKey, String modelo) {
        this.cliente = cliente;
        this.apiKey = apiKey;
        this.modelo = modelo;
    }

    private static RestClient criarCliente(String url) {

        // Sem limite de tempo, uma lentidão da Mistral travaria a requisição do usuário.
        SimpleClientHttpRequestFactory http = new SimpleClientHttpRequestFactory();
        http.setConnectTimeout(Duration.ofSeconds(5));
        http.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(http)
                .build();
    }

    @Override
    public String responder(String instrucoes, String pergunta) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new AssistenteIndisponivelException(
                    "Assistente de IA não configurado: defina a variável MISTRAL_API_KEY");
        }

        Map<String, Object> corpo = Map.of(
                "model", modelo,
                "temperature", 0.2,
                "max_tokens", 600,
                "messages", List.of(
                        Map.of("role", "system", "content", instrucoes),
                        Map.of("role", "user", "content", pergunta)));

        RespostaChat resposta;

        try {
            resposta = cliente.post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(RespostaChat.class);
        } catch (RestClientException erro) {
            // Inclui limite do plano gratuito (HTTP 429), chave inválida e falha de rede.
            throw new AssistenteIndisponivelException(
                    "Assistente de IA indisponível no momento, tente novamente em instantes", erro);
        }

        if (resposta == null
                || resposta.choices() == null
                || resposta.choices().isEmpty()
                || resposta.choices().get(0).message() == null
                || resposta.choices().get(0).message().content() == null
                || resposta.choices().get(0).message().content().isBlank()) {

            throw new AssistenteIndisponivelException(
                    "Assistente de IA não retornou uma resposta, tente novamente");
        }

        return resposta.choices().get(0).message().content().trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaChat(List<Escolha> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Escolha(Mensagem message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Mensagem(String content) {
    }
}
