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
    @ValueSource(strings = { "/cliente", "/funcionario", "/produto", "/venda", "/auth/me", "/cliente/1/vendas", "/auditoria" })
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

    @Test
    void documentacaoDaApiEPublicaEDescreveOLogin() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Prontê Conveniência - API"))
                .andExpect(jsonPath("$.paths['/produto']").exists())
                .andExpect(jsonPath("$.paths['/auth/login'].post").exists())
                .andExpect(jsonPath("$.components.securitySchemes.token.scheme").value("bearer"));
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

        String novo = "{\"nome\":\"Carlos\",\"cpf\":\"11122233396\",\"cargo\":\"Vendedor\","
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
        assertThat(entrar("11122233396", "segredo1")).startsWith("Bearer ");

        // E o CPF, que é o login, não pode ser cadastrado duas vezes.
        mvc.perform(json(post("/funcionario"), novo).header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    void loginAceitaCpfComOuSemMascara() throws Exception {
        assertThat(entrar("000.000.000-00", "admin123")).startsWith("Bearer ");
        assertThat(entrar("00000000000", "admin123")).startsWith("Bearer ");
    }

    @Test
    void cpfCadastradoComMascaraFicaSoComNumerosEValeNoLogin() throws Exception {
        String token = entrar("00000000000", "admin123");

        mvc.perform(json(post("/funcionario"),
                "{\"nome\":\"Ana\",\"cpf\":\"444.555.666-19\",\"cargo\":\"Gerente\","
                + "\"senha\":\"segredo2\",\"salario\":3000.00}").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("44455566619"));

        assertThat(entrar("44455566619", "segredo2")).startsWith("Bearer ");
        assertThat(entrar("444.555.666-19", "segredo2")).startsWith("Bearer ");

        // O mesmo CPF digitado sem máscara é reconhecido como repetido.
        mvc.perform(json(post("/funcionario"),
                "{\"nome\":\"Ana 2\",\"cpf\":\"44455566619\",\"cargo\":\"Gerente\","
                + "\"senha\":\"segredo3\",\"salario\":3000.00}").header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    void clienteComCpfInvalidoERecusado() throws Exception {
        String token = entrar("00000000000", "admin123");

        mvc.perform(json(post("/cliente"),
                "{\"nome\":\"Joana\",\"cpf\":\"123\",\"email\":\"joana@email.com\","
                + "\"telefone\":\"18999999999\"}").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("CPF deve ter 11 dígitos"));
    }

    @Test
    void clienteComDigitoVerificadorErradoERecusado() throws Exception {
        String token = entrar("00000000000", "admin123");

        mvc.perform(json(post("/cliente"),
                "{\"nome\":\"Joana\",\"cpf\":\"111.111.111-11\",\"email\":\"joana@email.com\","
                + "\"telefone\":\"18999999999\"}").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("CPF inválido"));
    }

    @Test
    void clienteComCpfRepetidoERecusado() throws Exception {
        String token = entrar("00000000000", "admin123");

        int primeiro = idDe(mvc.perform(json(post("/cliente"),
                "{\"nome\":\"Paulo\",\"cpf\":\"321.321.321-78\",\"email\":\"paulo@email.com\","
                + "\"telefone\":\"18999999999\"}").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        // O mesmo CPF sem máscara é reconhecido como repetido.
        mvc.perform(json(post("/cliente"),
                "{\"nome\":\"Paulo 2\",\"cpf\":\"32132132178\",\"email\":\"paulo2@email.com\","
                + "\"telefone\":\"18999999999\"}").header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Já existe um cliente com este CPF"));

        // Atualizar o próprio cliente mantendo o CPF continua permitido.
        mvc.perform(json(put("/cliente/" + primeiro),
                "{\"nome\":\"Paulo Souza\",\"cpf\":\"32132132178\",\"email\":\"paulo@email.com\","
                + "\"telefone\":\"18999999999\"}").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Paulo Souza"));
    }

    @Test
    void excluirRegistroInexistenteDevolve404() throws Exception {
        String token = entrar("00000000000", "admin123");

        mvc.perform(delete("/cliente/999999").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Cliente não encontrado"));
        mvc.perform(delete("/produto/999999").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Produto não encontrado"));
        mvc.perform(delete("/funcionario/999999").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Funcionário não encontrado"));
    }

    @Test
    void requisicaoMalFormadaDevolve400() throws Exception {
        String token = entrar("00000000000", "admin123");

        mvc.perform(json(post("/produto"), "{ isto não é json").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Dados enviados em formato inválido"));

        mvc.perform(get("/produto/abc").header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultarEAlterarCadastrosPeloId() throws Exception {
        String token = entrar("00000000000", "admin123");

        int clienteId = idDe(mvc.perform(json(post("/cliente"),
                "{\"nome\":\"Rita\",\"cpf\":\"24681357928\",\"email\":\"rita@email.com\","
                + "\"telefone\":\"18999999999\"}").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        mvc.perform(get("/cliente").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nome == 'Rita')]").isNotEmpty());

        mvc.perform(get("/cliente/" + clienteId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("24681357928"))
                .andExpect(jsonPath("$.email").value("rita@email.com"));

        int funcionarioId = idDe(mvc.perform(json(post("/funcionario"),
                "{\"nome\":\"Davi\",\"cpf\":\"369.258.147-55\",\"cargo\":\"Estoquista\","
                + "\"senha\":\"segredo5\",\"salario\":1900.00}").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        mvc.perform(json(put("/funcionario/" + funcionarioId),
                "{\"nome\":\"Davi Lima\",\"cpf\":\"36925814755\",\"cargo\":\"Repositor\","
                + "\"senha\":\"segredo6\",\"salario\":2100.00}").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senha").doesNotExist());

        mvc.perform(get("/funcionario/" + funcionarioId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Davi Lima"))
                .andExpect(jsonPath("$.cargo").value("Repositor"))
                .andExpect(jsonPath("$.salario").value(2100.0));

        mvc.perform(get("/funcionario/" + funcionarioId + "/vendas").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // A senha nova passa a valer no login.
        assertThat(entrar("36925814755", "segredo6")).startsWith("Bearer ");

        int produtoId = idDe(mvc.perform(json(post("/produto"),
                "{\"nome\":\"Suco\",\"descricao\":\"Suco de uva 1L\",\"preco\":9.0,"
                + "\"estoque\":12}").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        mvc.perform(json(put("/produto/" + produtoId),
                "{\"nome\":\"Suco Integral\",\"descricao\":\"Suco de uva integral 1L\",\"preco\":11.0,"
                + "\"estoque\":15}").header("Authorization", token))
                .andExpect(status().isOk());

        mvc.perform(get("/produto/" + produtoId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Suco Integral"))
                .andExpect(jsonPath("$.preco").value(11.0))
                .andExpect(jsonPath("$.estoque").value(15));

        mvc.perform(get("/funcionario/999999").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Funcionário não encontrado"));
    }

    // ---------- venda e estoque ----------

    @Test
    void vendaComDataNoFuturoERecusada() throws Exception {
        String token = entrar("00000000000", "admin123");

        mvc.perform(json(post("/venda"),
                "{\"dataVenda\":\"2030-01-01\",\"clienteId\":1,\"funcionarioId\":1,"
                + "\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}")
                .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A data da venda não pode ser no futuro"));
    }

    @Test
    void cadastrosComVendasNaoPodemSerExcluidos() throws Exception {
        String token = entrar("00000000000", "admin123");

        int funcionarioId = idDe(mvc.perform(json(post("/funcionario"),
                "{\"nome\":\"Bruno\",\"cpf\":\"74185296355\",\"cargo\":\"Operador de Caixa\","
                + "\"senha\":\"segredo4\",\"salario\":2000.00}").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        int clienteId = idDe(mvc.perform(json(post("/cliente"),
                "{\"nome\":\"Lucia\",\"cpf\":\"15935748606\",\"email\":\"lucia@email.com\","
                + "\"telefone\":\"18999999999\"}").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        int produtoId = idDe(mvc.perform(json(post("/produto"),
                "{\"nome\":\"Refrigerante\",\"descricao\":\"Lata 350ml\",\"preco\":6.0,"
                + "\"estoque\":20}").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        int vendaId = idDe(mvc.perform(json(post("/venda"),
                pedido(clienteId, funcionarioId, produtoId, 2)).header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        mvc.perform(delete("/cliente/" + clienteId).header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        "Não é possível excluir o cliente porque ele tem vendas registradas"));

        mvc.perform(delete("/produto/" + produtoId).header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        "Não é possível excluir o produto porque ele aparece em vendas registradas"));

        mvc.perform(delete("/funcionario/" + funcionarioId).header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        "Não é possível excluir o funcionário porque ele tem vendas registradas"));

        // Sem a venda, os três cadastros podem ser excluídos.
        mvc.perform(delete("/venda/" + vendaId).header("Authorization", token))
                .andExpect(status().isOk());

        mvc.perform(delete("/cliente/" + clienteId).header("Authorization", token))
                .andExpect(status().isOk());
        mvc.perform(delete("/produto/" + produtoId).header("Authorization", token))
                .andExpect(status().isOk());
        mvc.perform(delete("/funcionario/" + funcionarioId).header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    void vendaDaBaixaNoEstoqueRecusaFaltaEDevolveAoExcluir() throws Exception {
        String token = entrar("00000000000", "admin123");

        int funcionarioId = idDe(mvc.perform(get("/auth/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        int clienteId = idDe(mvc.perform(json(post("/cliente"),
                "{\"nome\":\"Maria\",\"cpf\":\"12312312387\",\"email\":\"maria@email.com\","
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

    // ---------- auditoria ----------

    @Test
    void alteracoesFicamRegistradasNaAuditoriaEConsultasNao() throws Exception {
        String token = entrar("00000000000", "admin123");

        mvc.perform(json(post("/produto"),
                "{\"nome\":\"Caderno\",\"descricao\":\"Caderno 96 folhas\",\"preco\":20.0,"
                + "\"estoque\":5}").header("Authorization", token))
                .andExpect(status().isOk());

        mvc.perform(get("/auditoria").param("tamanho", "200").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.acao == 'INCLUSAO' && @.recurso == '/produto' "
                        + "&& @.usuario == 'Administrador' && @.status == 200)]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.metodo == 'GET')]").isEmpty());
    }

    @Test
    void loginsFicamNaAuditoriaSemSenhaEComCpfMascarado() throws Exception {
        mvc.perform(json(post("/auth/login"), "{\"cpf\":\"123.456.789-01\",\"senha\":\"senhaSecreta9\"}"))
                .andExpect(status().isUnauthorized());

        String token = entrar("00000000000", "admin123");

        String auditoria = mvc.perform(get("/auditoria").param("tamanho", "200").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.acao == 'LOGIN_RECUSADO' && @.usuario == '123.***.***-01')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.acao == 'LOGIN' && @.usuario == 'Administrador')]").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        assertThat(auditoria).doesNotContain("senhaSecreta9").doesNotContain("12345678901");
    }

    @Test
    void auditoriaSoPodeSerConsultadaPorAdministrador() throws Exception {
        String admin = entrar("00000000000", "admin123");

        mvc.perform(json(post("/funcionario"),
                "{\"nome\":\"Vera\",\"cpf\":\"52998224725\",\"cargo\":\"Vendedor\","
                + "\"senha\":\"1234\",\"salario\":1800.0}").header("Authorization", admin))
                .andExpect(status().isOk());

        String vendedora = entrar("52998224725", "1234");

        mvc.perform(get("/auditoria").header("Authorization", vendedora))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

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
