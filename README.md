# Sistema de Estoque

API REST simples de controle de estoque, feita para servir de base a um trabalho de testes de software.

**Tecnologias:** Java 25, Spring Boot 4.1, Spring Data JPA, banco H2 em memória, JUnit 5, AssertJ, Mockito e MockMvc.

## Como rodar

Pré-requisitos: JDK 25 e Maven 3.9 ou superior (ou uma IDE como IntelliJ, que já traz o Maven).

```bash
mvn spring-boot:run   # sobe a API em http://localhost:8080
mvn test              # roda todos os testes
```

O banco é em memória: os dados somem quando a aplicação para.

## Estrutura

```
src/main/java/com/estoque
├── dominio/       Entidades (Categoria, Produto, Movimentacao) e o validador Ean13
├── repositorio/   Interfaces Spring Data JPA
├── servico/       Regras de negócio (é aqui que cada RN é verificada)
├── api/           Controllers REST, DTOs e tratamento de erros
└── excecao/       RegraNegocioException e RecursoNaoEncontradoException

src/test/java/com/estoque
├── dominio/Ean13Test.java        Exemplo de teste unitário
└── api/FluxoEstoqueTest.java     Exemplo de teste de integração com MockMvc
```

## Regras de negócio

### Categoria

| Regra | Descrição |
|---|---|
| RN01 | O nome da categoria é obrigatório e único, com 3 a 50 caracteres. A comparação ignora maiúsculas e minúsculas. |
| RN02 | Uma categoria que tem produtos vinculados não pode ser excluída. |

### Produto

| Regra | Descrição |
|---|---|
| RN03 | O código do produto é um EAN-13 obrigatório, válido (13 dígitos e dígito verificador correto) e único. |
| RN04 | O nome é obrigatório, com 3 a 100 caracteres. |
| RN05 | O preço deve ser maior que zero, com no máximo 2 casas decimais. |
| RN06 | O estoque mínimo é obrigatório e deve ser maior ou igual a zero. |
| RN07 | Todo produto pertence a exatamente uma categoria, que deve existir. |
| RN08 | Todo produto é cadastrado com saldo zero. O saldo só muda por meio de movimentações. |
| RN09 | Nome, preço, estoque mínimo e categoria podem ser alterados. O código EAN é imutável. |
| RN10 | Um produto que já tem movimentações não pode ser excluído, apenas inativado. |
| RN11 | Um produto inativo não aceita novas movimentações. |

### Movimentações

| Regra | Descrição |
|---|---|
| RN12 | A quantidade de uma movimentação deve ser um número inteiro entre 1 e 10.000. |
| RN13 | Uma saída não pode ser maior que o saldo atual. Sair exatamente o saldo é permitido. |
| RN14 | Saídas simultâneas do mesmo produto nunca podem deixar o saldo negativo. Se duas competirem pelo mesmo saldo, uma é recusada por saldo insuficiente (RN13). |
| RN15 | Cada movimentação registra tipo, quantidade, data e hora e saldo resultante. |
| RN16 | Movimentações não podem ser editadas nem excluídas. Erros são corrigidos com uma movimentação no sentido oposto. |

### Alertas e relatórios

| Regra | Descrição |
|---|---|
| RN17 | Um produto está abaixo do mínimo quando seu saldo é **menor** que o estoque mínimo. Saldo igual ao mínimo não gera alerta. |
| RN18 | O relatório de reposição lista os produtos ativos abaixo do mínimo, ordenados pela quantidade que falta (maior primeiro), com filtro opcional por categoria. |

### Detalhes de implementação que afetam os testes

- **Espaços nas pontas** dos nomes (categoria e produto) são removidos antes de contar os caracteres. `"  Ab  "` tem 2 caracteres e é recusado.
- **Ordem das validações** no cadastro e na alteração de produto: RN03 (ou RN09) → RN04 → RN05 → RN06 → RN07. A primeira regra violada interrompe a operação.
- **Ordem nas movimentações:** produto inexistente (404) → RN11 → RN12 → RN13.
- **Preço:** zeros à direita não contam como casa decimal. `10.50` e `10.500` são aceitos, `10.505` é recusado.
- **Quantidade decimal**, como `2.5`, é recusada pela RN12 em vez de ser arredondada.
- **Desempate no relatório (RN18):** quando dois produtos têm a mesma quantidade faltante, a ordem é pelo nome.
- **Concorrência (RN14):** a saída trava a linha do produto no banco (`PESSIMISTIC_WRITE`) até o fim da transação.
- **Data e hora (RN15)** vêm de um bean `Clock`. Nos testes, ele pode ser substituído por `Clock.fixed(...)` para ter horários previsíveis.
- **Gerar EANs válidos** para os dados de teste: `Ean13.gerar("789123456789")` devolve `"7891234567895"`.

## Endpoints

| Método | Caminho | Descrição |
|---|---|---|
| POST | `/categorias` | Cria categoria |
| GET | `/categorias` | Lista categorias |
| GET | `/categorias/{id}` | Busca categoria |
| DELETE | `/categorias/{id}` | Exclui categoria |
| POST | `/produtos` | Cadastra produto |
| GET | `/produtos` | Lista produtos |
| GET | `/produtos/{id}` | Busca produto |
| PUT | `/produtos/{id}` | Altera produto |
| PATCH | `/produtos/{id}/inativar` | Inativa produto |
| DELETE | `/produtos/{id}` | Exclui produto |
| POST | `/produtos/{id}/entradas` | Registra entrada |
| POST | `/produtos/{id}/saidas` | Registra saída |
| GET | `/produtos/{id}/movimentacoes` | Histórico do produto |
| GET | `/relatorios/reposicao?categoriaId=` | Relatório de reposição (filtro opcional) |

### Exemplos de corpo

```json
// POST /categorias
{ "nome": "Bebidas" }

// POST /produtos  (no PUT, o "ean" é opcional e, se enviado, deve ser igual ao atual)
{ "ean": "7891234567895", "nome": "Suco de uva", "preco": 8.90, "estoqueMinimo": 10, "categoriaId": 1 }

// POST /produtos/1/entradas  ou  /produtos/1/saidas
{ "quantidade": 20 }
```

### Respostas de erro

Todos os erros seguem o formato `{ "codigo": "...", "mensagem": "..." }`.

| Status | Quando | `codigo` |
|---|---|---|
| 422 | Regra de negócio violada | Número da regra, ex.: `RN13` |
| 404 | Categoria ou produto inexistente | `NAO_ENCONTRADO` |
| 400 | JSON malformado ou parâmetro com tipo errado | `REQUISICAO_INVALIDA` |
| 409 | Conflito no banco (ex.: dois cadastros simultâneos com o mesmo EAN) | `CONFLITO` |
| 405 | Tentativa de editar ou excluir movimentação (RN16) | — |
