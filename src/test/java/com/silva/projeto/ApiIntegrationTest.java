package com.silva.projeto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.silva.projeto.exception.AssistenteIndisponivelException;
import com.silva.projeto.ia.ModeloIA;

/**
 * Testes de ponta a ponta da API: sobem a aplicação inteira com banco H2 em
 * memória e fazem requisições HTTP simuladas, passando pela segurança.
 * A IA é substituída por um dublê, para o teste não depender da Mistral.
 */
@SpringBootTest
class ApiIntegrationTest {

    @Autowired
    private WebApplicationContext contexto;

    @MockitoBean
    private ModeloIA modeloIA;

    private MockMvc mvc;

    @BeforeEach
    void prepararMockMvc() {
        mvc = MockMvcBuilders
                .webAppContextSetup(contexto)
                .apply(springSecurity())
                .build();
    }

    // ---------- acesso sem login ----------

    @ParameterizedTest
    @ValueSource(strings = { "/cliente", "/funcionario", "/produto", "/venda", "/auth/me", "/cliente/1/vendas" })
    void rotasDeConsultaExigemLogin(String rota) throws Exception {
        mvc.perform(get(rota))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    void rotasDeAlteracaoExigemLogin() throws Exception {
        mvc.perform(json(post("/produto"), "{}")).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/funcionario"), "{}")).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/venda"), "{}")).andExpect(status().isUnauthorized());
        mvc.perform(json(put("/produto/1"), "{}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/produto/1")).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/assistente/pergunta"), "{\"pergunta\":\"oi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoERecusado() throws Exception {
        mvc.perform(get("/produto").header("Authorization", "Bearer token.que.nao.existe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verificacaoDeSaudeEPublica() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // ---------- login ----------

    @Test
    void loginComSenhaErradaERecusado() throws Exception {
        mvc.perform(json(post("/auth/login"), "{\"cpf\":\"00000000000\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("CPF ou senha inválidos"));
    }

    @Test
    void loginComCpfDesconhecidoDaAMesmaRespostaDeSenhaErrada() throws Exception {
        mvc.perform(json(post("/auth/login"), "{\"cpf\":\"99999999999\",\"senha\":\"admin123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("CPF ou senha inválidos"));
    }

    @Test
    void loginSemSenhaEInvalido() throws Exception {
        mvc.perform(json(post("/auth/login"), "{\"cpf\":\"00000000000\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginCorretoDevolveTokenQueLiberaAsRotas() throws Exception {
        mvc.perform(json(post("/auth/login"), "{\"cpf\":\"00000000000\",\"senha\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").isNotEmpty())
                .andExpect(jsonPath("$.funcionario.nome").value("Administrador"));

        String token = entrar("00000000000", "admin123");

        mvc.perform(get("/produto").header("Authorization", token))
                .andExpect(status().isOk());

        mvc.perform(get("/auth/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargo").value("Administrador"));
    }

    // ---------- funcionário ----------

    @Test
    void senhaDoFuncionarioNuncaApareceNasRespostasEFuncionaNoLogin() throws Exception {
        String token = entrar("00000000000", "admin123");

        String novo = "{\"nome\":\"Carlos\",\"cpf\":\"11122233344\",\"cargo\":\"Vendedor\","
                + "\"senha\":\"segredo1\",\"salario\":2500.00}";

        String criado = mvc.perform(json(post("/funcionario"), novo).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(criado).doesNotContain("segredo1").doesNotContain("$2a$");

        String lista = mvc.perform(get("/funcionario").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(lista).doesNotContain("senha").doesNotContain("$2a$");

        // O novo funcionário consegue entrar com a senha que foi cadastrada.
        assertThat(entrar("11122233344", "segredo1")).startsWith("Bearer ");

        // E o CPF, que é o login, não pode ser cadastrado duas vezes.
        mvc.perform(json(post("/funcionario"), novo).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    // ---------- venda e estoque ----------

    @Test
    void vendaDaBaixaNoEstoqueRecusaFaltaEDevolveAoExcluir() throws Exception {
        String token = entrar("00000000000", "admin123");

        int funcionarioId = idDe(mvc.perform(get("/auth/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        int clienteId = idDe(mvc.perform(json(post("/cliente"),
                "{\"nome\":\"Maria\",\"cpf\":\"11111111111\",\"email\":\"maria@email.com\","
                + "\"telefone\":\"18999999999\"}").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        int produtoId = idDe(mvc.perform(json(post("/produto"),
                "{\"nome\":\"Notebook\",\"descricao\":\"Notebook Dell\",\"preco\":3500.00,"
                + "\"estoque\":10}").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        // Vende 3 unidades: total calculado e estoque cai de 10 para 7.
        int vendaId = idDe(mvc.perform(json(post("/venda"),
                pedido(clienteId, funcionarioId, produtoId, 3)).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(10500.0))
                .andExpect(jsonPath("$.itens[0].produto").value("Notebook"))
                .andExpect(jsonPath("$.itens[0].quantidade").value(3))
                .andExpect(jsonPath("$.funcionario.senha").doesNotExist())
                .andReturn().getResponse().getContentAsString());

        estoqueDeveSer(produtoId, 7, token);

        // Tentar vender mais do que há é recusado e não mexe no estoque.
        mvc.perform(json(post("/venda"),
                pedido(clienteId, funcionarioId, produtoId, 8)).header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        "Estoque insuficiente para Notebook: disponível 7, solicitado 8"));

        estoqueDeveSer(produtoId, 7, token);

        // A venda aparece nas consultas.
        mvc.perform(get("/venda").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itens[0].quantidade").value(3));

        mvc.perform(get("/venda/" + vendaId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.nome").value("Maria"));

        mvc.perform(get("/cliente/" + clienteId + "/vendas").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valorTotal").value(10500.0));

        // Alterar a venda para 5 unidades: devolve as 3 e baixa 5.
        mvc.perform(json(put("/venda/" + vendaId),
                pedido(clienteId, funcionarioId, produtoId, 5)).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(17500.0));

        estoqueDeveSer(produtoId, 5, token);

        // Excluir a venda devolve tudo ao estoque.
        mvc.perform(delete("/venda/" + vendaId).header("Authorization", token))
                .andExpect(status().isOk());

        estoqueDeveSer(produtoId, 10, token);
    }

    @Test
    void vendaSemItensEInvalida() throws Exception {
        String token = entrar("00000000000", "admin123");

        mvc.perform(json(post("/venda"),
                "{\"dataVenda\":\"2026-10-11\",\"clienteId\":1,\"funcionarioId\":1,\"itens\":[]}")
                .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A venda deve possuir pelo menos um item"));
    }

    // ---------- assistente de IA ----------

    @Test
    void assistenteRespondeUsandoOEstoqueDoBanco() throws Exception {
        String token = entrar("00000000000", "admin123");

        mvc.perform(json(post("/produto"),
                "{\"nome\":\"Monitor Gamer\",\"descricao\":\"Monitor 27 polegadas\",\"preco\":1200.00,"
                + "\"estoque\":4}").header("Authorization", token))
                .andExpect(status().isOk());

        when(modeloIA.responder(anyString(), anyString())).thenReturn("Há 4 monitores em estoque.");

        mvc.perform(json(post("/assistente/pergunta"), "{\"pergunta\":\"Quantos monitores temos?\"}")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resposta").value("Há 4 monitores em estoque."));

        ArgumentCaptor<String> instrucoes = ArgumentCaptor.forClass(String.class);
        verify(modeloIA).responder(instrucoes.capture(), eq("Quantos monitores temos?"));

        assertThat(instrucoes.getValue()).contains("Monitor Gamer | Monitor 27 polegadas | 1200.00 | 4");
    }

    @Test
    void assistenteForaDoArDevolve503() throws Exception {
        String token = entrar("00000000000", "admin123");

        when(modeloIA.responder(anyString(), anyString()))
                .thenThrow(new AssistenteIndisponivelException("Assistente de IA indisponível no momento"));

        mvc.perform(json(post("/assistente/pergunta"), "{\"pergunta\":\"Quantos monitores temos?\"}")
                .header("Authorization", token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void perguntaVaziaEInvalida() throws Exception {
        String token = entrar("00000000000", "admin123");

        mvc.perform(json(post("/assistente/pergunta"), "{\"pergunta\":\"  \"}")
                .header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    // ---------- CORS (front web) ----------

    @Test
    void navegadorDeOrigemAutorizadaPodeChamarAApi() throws Exception {
        mvc.perform(options("/produto")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void navegadorDeOrigemDesconhecidaEBloqueado() throws Exception {
        mvc.perform(options("/produto")
                .header("Origin", "http://site-desconhecido.com")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    // ---------- auxiliares ----------

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requisicao, String corpo) {
        return requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    /** Faz login e devolve o valor pronto para o cabeçalho Authorization. */
    private String entrar(String cpf, String senha) throws Exception {
        String resposta = mvc.perform(json(post("/auth/login"),
                "{\"cpf\":\"" + cpf + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(resposta, "$.token");
        return "Bearer " + token;
    }

    private static int idDe(String resposta) {
        Integer id = JsonPath.read(resposta, "$.id");
        return id;
    }

    private static String pedido(int clienteId, int funcionarioId, int produtoId, int quantidade) {
        return "{\"dataVenda\":\"2026-10-11\",\"clienteId\":" + clienteId
                + ",\"funcionarioId\":" + funcionarioId
                + ",\"itens\":[{\"produtoId\":" + produtoId + ",\"quantidade\":" + quantidade + "}]}";
    }

    private void estoqueDeveSer(int produtoId, int esperado, String token) throws Exception {
        mvc.perform(get("/produto/" + produtoId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estoque").value(esperado));
    }
}
