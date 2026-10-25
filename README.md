# sistema-vendas-api
API REST desenvolvida com Spring Boot para gerenciamento de vendas.

# Sistema de Gerenciamento de Vendas API

## Descrição do Projeto
Este projeto consiste em uma API REST desenvolvida em Java utilizando Spring Boot para gerenciamento de um sistema de vendas.

O sistema permite:
- Cadastro de clientes
- Cadastro de funcionários
- Cadastro de produtos
- Realização de vendas
- Relacionamento entre entidades
- Validações de dados
- Tratamento de exceções

# Tecnologias Utilizadas
* Java 25
* Spring Boot
* Spring Web
* Spring Data JPA
* Hibernate
* MySQL
* Bean Validation
* Spring Security (login com token JWT)
* Mistral AI (assistente de estoque)
* JUnit 5, Mockito e JaCoCo (testes automatizados)
* GitHub Actions (CI/CD)
* Springdoc OpenAPI (Swagger)
* Docker, Docker Compose e Caddy (HTTPS)
* MariaDB com plugin de auditoria
* Prometheus e Grafana (monitoramento)
* Maven

# Estrutura do Projeto
- Controller
- Service
- Repository
- Model (Entity)
- DTO

# Banco de Dados
- MySQL

# Como Executar o Projeto

## 1. Clonar o repositório e Abrir o projeto

## 2. Criar banco de dados 'projeto' MySQL

## 3. Configurar application.properties

Configurando:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/projeto
spring.datasource.username=root
spring.datasource.password=sua_senha
spring.jpa.hibernate.ddl-auto=update
```

## 4. Executar o projeto

Ao iniciar com o banco vazio, a API cria um usuário inicial para o primeiro login:
CPF `00000000000`, senha `admin123` (somente em desenvolvimento; em homologação e produção
esses valores vêm de variáveis de ambiente).

## 5. Variáveis de ambiente

| Variável | Para que serve | Padrão em desenvolvimento |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Conexão com o MySQL | `localhost:3306/projeto`, `root`, sem senha |
| `JWT_SECRET` | Chave que assina os tokens de login (mínimo 32 caracteres) | chave de desenvolvimento |
| `JWT_EXPIRACAO_MINUTOS` | Validade do token | `480` (8 horas) |
| `ADMIN_CPF`, `ADMIN_SENHA` | Usuário criado quando não existe nenhum funcionário | `00000000000` / `admin123` |
| `CORS_ORIGENS` | Endereços do front web autorizados, separados por vírgula | `localhost:5173`, `:3000` e `:8081` |
| `MISTRAL_API_KEY` | Chave da Mistral para o assistente de IA | vazio (assistente responde 503) |
| `MISTRAL_MODELO` | Modelo usado | `ministral-8b-2512` |
| `LOG_ARQUIVO` | Arquivo de log | `logs/projeto.log` |
| `SPRING_PROFILES_ACTIVE` | `homologacao` ou `producao` nos ambientes publicados | sem perfil |

Com o perfil `homologacao` ou `producao`, `JWT_SECRET`, `ADMIN_CPF`, `ADMIN_SENHA` e `CORS_ORIGENS`
são obrigatórias: se faltar alguma, a aplicação não inicia.

# Documentação da API (Swagger)

Com a API rodando, a documentação interativa fica em:

- `http://localhost:8080/swagger-ui.html` (tela)
- `http://localhost:8080/v3/api-docs` (especificação OpenAPI em JSON)

Para testar as rotas pela tela: execute `POST /auth/login`, copie o `token` da resposta,
clique em **Authorize** e cole o token.

# Auditoria e Logs

- **Auditoria da aplicação:** toda operação que altera dados (inclusão, alteração, exclusão,
  login, login recusado e pergunta à IA) é gravada na tabela `auditoria` com data e hora,
  funcionário, ação, rota, status da resposta, IP e duração. Consultas (`GET`) não são gravadas.
  A senha nunca é registrada e o CPF de logins recusados aparece mascarado (`123.***.***-01`).
  Consulta: `GET /auditoria?pagina=0&tamanho=50`, somente para o cargo `Administrador`.
- **Logs da aplicação:** além do console, ficam em `logs/projeto.log`, com rotação a cada 10 MB
  e 30 arquivos guardados. Cada requisição gera uma linha de acesso
  (`POST /produto 200 35ms funcionario=1 ip=...`) e os logins recusados geram um aviso.
- **Auditoria do banco:** na VPS o MariaDB usa o plugin `server_audit`, que registra conexões
  e todos os comandos que alteram estrutura, dados ou permissões.

# Monitoramento

- `GET /actuator/health`: saúde da API (usado pelo deploy).
- `GET /actuator/prometheus`: métricas da API (requisições, tempo de resposta, erros, memória),
  lidas pelo Prometheus. Na VPS esse endereço não fica acessível de fora.
- Grafana com o painel **Prontê - Visão geral**: disponibilidade da API e do banco, requisições,
  tempo de resposta, erros, memória da JVM, conexões do banco e CPU, memória e disco do servidor.

# Testes Automatizados

```
./mvnw verify
```

Executa os testes unitários (JUnit 5 + Mockito) e os testes de integração da API
(MockMvc com banco H2 em memória, sem precisar de MySQL nem da Mistral), gera o
relatório de cobertura em `target/site/jacoco/index.html` e falha se a cobertura
de linhas ficar abaixo de 60%.

# CI/CD

- **CI** (`.github/workflows/ci.yml`): em todo push e pull request para `main` e `develop`, roda `./mvnw verify`.
- **CD** (`.github/workflows/cd.yml`): a cada alteração na `main`, testa, gera a imagem Docker, publica em
  **homologação** automaticamente e publica em **produção** somente depois que um integrante do grupo aprovar.

# Infraestrutura (VPS)

Tudo roda em containers Docker numa VPS, descritos em `deploy/docker-compose.yml`:

| Container | Função |
| --- | --- |
| `proxy` | Caddy: entrada pela internet com HTTPS automático; serve o front web de produção (`deploy/web`) e o de homologação (`deploy/web-homologacao`), publicados pelo CD do Projeto-FrontWeb |
| `api-homologacao` | API no ambiente de homologação (banco `projeto_homologacao`) |
| `api-producao` | API no ambiente de produção (banco `projeto_producao`) |
| `banco` | MariaDB com auditoria; cada ambiente tem banco e usuário próprios |
| `prometheus` | Coleta as métricas da API, do banco e do servidor |
| `grafana` | Painel de monitoramento |
| `monitor-banco`, `monitor-servidor` | Exportadores de métricas do MariaDB e da VPS |

Primeira instalação na VPS (com Docker instalado):

```
git clone https://github.com/Projeto-QualidadeDeSoftware/Projeto-API.git
cd Projeto-API/deploy
cp .env.exemplo .env      # preencher senhas, chaves e endereços
docker compose up -d
```

Depois disso as atualizações da API são feitas pelo CD, que executa `deploy/atualizar.sh` na VPS.

# Funcionalidades
- CRUD completo de Clientes
- CRUD completo de Funcionários
- CRUD completo de Produtos
- CRUD completo de Vendas, com quantidade por produto e baixa automática de estoque
- Login com CPF e senha; nenhuma rota funciona sem estar logado
- Assistente de IA que responde perguntas sobre o estoque
- Validações utilizando Bean Validation

# Endpoints da API

Todas as rotas exigem login, exceto `POST /auth/login`, `GET /actuator/health`,
`GET /actuator/prometheus` e a documentação (`/swagger-ui.html` e `/v3/api-docs`).
Sem token válido a resposta é `401`.

# Autenticação

## Login

```
POST /auth/login
```

```json
{
  "cpf": "00000000000",
  "senha": "admin123"
}
```

Resposta:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEm": "2026-10-11T23:30:00Z",
  "funcionario": { "id": 1, "nome": "Administrador", "cargo": "Administrador" }
}
```

O token deve ser enviado em todas as outras requisições, no cabeçalho:

```
Authorization: Bearer <token>
```

Quando o token expira a API volta a responder `401` e é preciso fazer login de novo.

## Usuário logado

```
GET /auth/me
```

Devolve `id`, `nome` e `cargo` do funcionário dono do token.

## Formato dos erros

```json
{ "status": 401, "mensagem": "CPF ou senha inválidos", "dataHora": "2026-10-11T13:30:00" }
```

| Status | Quando |
| --- | --- |
| 400 | Dados inválidos |
| 401 | Sem login, token inválido/expirado ou CPF/senha errados |
| 403 | Funcionário sem permissão (auditoria) |
| 404 | Registro não encontrado |
| 409 | Estoque insuficiente ou CPF de funcionário já cadastrado |
| 503 | Assistente de IA indisponível |

# Cliente
## Criar Cliente
```
POST /cliente
```

```
{
  "nome": "Maria",
  "cpf": "11111111111",
  "email": "maria@email.com",
  "telefone": "18999999999"
}
```

## Listar Clientes
```
GET /cliente
```

## Buscar Cliente por ID
```
GET /cliente/1
```

## Atualizar Cliente
```
PUT /cliente/1
```

## Deletar Cliente
```http
DELETE /cliente/1
```

# Funcionário

## Criar Funcionário
```
POST /funcionario
```

```
{
  "nome": "Carlos",
  "cpf": "98765432100",
  "cargo": "Vendedor",
  "senha": "123456",
  "salario": 2500.00
}
```

O CPF é o login do funcionário e não pode se repetir. A senha é gravada como hash
e nunca é devolvida nas respostas.

O CPF (de funcionários e clientes) pode ser enviado com ou sem máscara (`111.111.111-11` ou
`11111111111`), no cadastro e no login, mas é sempre gravado e devolvido só com números.
CPF com quantidade de dígitos diferente de 11 é recusado com `400`.

## Listar Funcionários
```
GET /funcionario
```

## Buscar Funcionário por ID
```
GET /funcionario/1
```

## Atualizar Funcionário
```
PUT /funcionario/1
```

## Deletar Funcionário
```
DELETE /funcionario/1
```

# Produto

## Criar Produto
```
POST /produto
```

```
{
  "nome": "Notebook",
  "descricao": "Notebook Dell",
  "preco": 3500.00,
  "estoque": 10
}
```

## Listar Produtos
```
GET /produto
```

## Buscar Produto por ID
```
GET /produto/1
```

## Atualizar Produto
```
PUT /produto/1
```

## Deletar Produto
```
DELETE /produto/1
```

# Venda

## Criar Venda
```
POST /venda
```

```json
{
  "dataVenda": "2026-05-13",
  "clienteId": 1,
  "funcionarioId": 1,
  "itens": [
    { "produtoId": 1, "quantidade": 2 },
    { "produtoId": 2, "quantidade": 1 }
  ]
}
```

O valor total é calculado automaticamente e a quantidade vendida sai do estoque de cada produto.
Se não houver estoque suficiente de algum item, a venda inteira é recusada com `409`.

Resposta:

```json
{
  "id": 1,
  "dataVenda": "2026-05-13",
  "valorTotal": 7080.0,
  "cliente": { "id": 1, "nome": "Maria", "cpf": "11111111111", "email": "maria@email.com", "telefone": "18999999999" },
  "funcionario": { "id": 1, "nome": "Carlos", "cpf": "98765432100", "cargo": "Vendedor", "salario": 2500.0 },
  "itens": [
    { "produtoId": 1, "produto": "Notebook", "quantidade": 2, "subtotal": 7000.0 },
    { "produtoId": 2, "produto": "Mouse", "quantidade": 1, "subtotal": 80.0 }
  ]
}
```

## Listar Vendas
```
GET /venda
```

## Buscar Venda por ID
```
GET /venda/1
```

## Atualizar Venda
```
PUT /venda/1
```

Mesmo corpo da criação. O estoque da venda antiga é devolvido antes de dar baixa nos novos itens.

## Deletar Venda
```
DELETE /venda/1
```

Os produtos da venda excluída voltam para o estoque.

# Assistente de IA

```
POST /assistente/pergunta
```

```json
{
  "pergunta": "Quais produtos estão com menos de 5 unidades?"
}
```

Resposta:

```json
{
  "resposta": "Mouse (2 unidades) e Teclado (4 unidades)."
}
```

A cada pergunta a API lê os produtos do banco e envia esses dados junto com a pergunta para a
Mistral, então a resposta reflete o estoque do momento. A pergunta aceita até 500 caracteres.
Sem `MISTRAL_API_KEY`, ou se a Mistral estiver fora do ar ou no limite do plano gratuito, a resposta é `503`.

# Validações
O projeto utiliza Bean Validation para validar os dados recebidos pela API.
