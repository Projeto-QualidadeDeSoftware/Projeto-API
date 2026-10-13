package com.silva.projeto.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.silva.projeto.exception.AssistenteIndisponivelException;

/** Testa a integração com a Mistral usando um servidor simulado (sem internet e sem chave real). */
class MistralModeloIATest {

    private static final String URL = "https://api.mistral.ai/v1/chat/completions";

    private MockRestServiceServer mistral;
    private RestClient cliente;

    @BeforeEach
    void criarServidorSimulado() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.mistral.ai");
        mistral = MockRestServiceServer.bindTo(builder).build();
        cliente = builder.build();
    }

    @Test
    void enviaChaveModeloEMensagensEDevolveOTextoDaResposta() {
        mistral.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer chave-de-teste"))
                .andExpect(jsonPath("$.model").value("mistral-small-latest"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value("dados do estoque"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("Quantos notebooks temos?"))
                .andRespond(withSuccess(
                        "{\"id\":\"abc\",\"object\":\"chat.completion\",\"choices\":[{\"index\":0,"
                        + "\"message\":{\"role\":\"assistant\",\"content\":\" Há 7 notebooks. \"},"
                        + "\"finish_reason\":\"stop\"}],\"usage\":{\"total_tokens\":42}}",
                        MediaType.APPLICATION_JSON));

        ModeloIA modelo = new MistralModeloIA(cliente, "chave-de-teste", "mistral-small-latest");

        String resposta = modelo.responder("dados do estoque", "Quantos notebooks temos?");

        assertThat(resposta).isEqualTo("Há 7 notebooks.");
        mistral.verify();
    }

    @Test
    void semChaveNaoChamaAMistral() {
        ModeloIA modelo = new MistralModeloIA(cliente, "", "mistral-small-latest");

        assertThatThrownBy(() -> modelo.responder("dados", "pergunta"))
                .isInstanceOf(AssistenteIndisponivelException.class)
                .hasMessageContaining("MISTRAL_API_KEY");

        mistral.verify();
    }

    @Test
    void limiteDoPlanoGratuitoViraAssistenteIndisponivel() {
        mistral.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        ModeloIA modelo = new MistralModeloIA(cliente, "chave-de-teste", "mistral-small-latest");

        assertThatThrownBy(() -> modelo.responder("dados", "pergunta"))
                .isInstanceOf(AssistenteIndisponivelException.class)
                .hasMessageContaining("indisponível");
    }

    @Test
    void respostaSemTextoViraAssistenteIndisponivel() {
        mistral.expect(requestTo(URL))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        ModeloIA modelo = new MistralModeloIA(cliente, "chave-de-teste", "mistral-small-latest");

        assertThatThrownBy(() -> modelo.responder("dados", "pergunta"))
                .isInstanceOf(AssistenteIndisponivelException.class);
    }
}
