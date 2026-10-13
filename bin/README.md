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

# Funcionalidades
- CRUD completo de Clientes
- CRUD completo de Funcionários
- CRUD completo de Produtos
- CRUD completo de Vendas
- Validações utilizando Bean Validation

# Endpoints da API

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
POST /venda?clienteId=1&funcionarioId=1&produtosIds=1,2
```

```
{
  "dataVenda": "2026-05-13"
}
```

O valor total da venda é calculado automaticamente com base nos produtos informados.

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
PUT /venda/1?clienteId=1&funcionarioId=1&produtosIds=2,3
```

## Deletar Venda
```
DELETE /venda/1
```

# Validações
O projeto utiliza Bean Validation para validar os dados recebidos pela API.
