# Orçamento Inteligente: API de orçamento financeiro por voz (Spring Boot + Spring AI)

API que entende **comandos de voz (ou texto)** sobre finanças pessoais, aciona **funções reais da aplicação** por *Tool Calling*, grava os dados no banco e responde em texto **e em áudio**.

> Projeto do *Desafio de Projeto: Spring AI* (DIO). Escrito do zero, seguindo o fluxo das aulas e evoluindo-o com melhorias próprias (seção 3).

---

## 1. O que o projeto faz

```
 áudio ─► Speech-to-Text ─► texto ─► ChatClient + Tool Calling ─► resposta ─► Text-to-Speech ─► áudio
                                              │
                                              └─► Serviço de domínio ─► Spring Data JPA ─► H2 / PostgreSQL
```

Exemplos de comandos que a IA entende e executa:

| Você diz | O que acontece |
|---|---|
| "Gastei 45 e 90 no mercado ontem" | grava uma despesa de R$ 45,90 na categoria *Mercado* com a data de ontem e avisa se o limite do mês está perto |
| "Recebi 5 mil de salário" | grava uma receita de R$ 5.000,00 |
| "Quanto ainda posso gastar com lazer?" | consulta o limite da categoria e diz quanto resta |
| "O que gastei esta semana?" | consulta o extrato do período, com totais calculados pelo sistema |
| "Qual é o meu saldo do mês?" | soma receitas e despesas do mês |
| "Quero gastar no máximo 500 com transporte" | define o limite mensal da categoria |

---

## 2. Como executar

**Pré-requisitos:** Java 17+, Maven 3.9+ (ou o Maven Wrapper, se você adicioná-lo) e uma chave de API da OpenAI (só os endpoints de IA precisam dela).

```bash
export OPENAI_API_KEY="sua-chave"        # Windows PowerShell: $env:OPENAI_API_KEY="sua-chave"
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080` com **banco H2 em memória** e dados de demonstração (categorias *Mercado*, *Lazer* e *Transporte* com limite, uma receita e quatro despesas).

**Sem chave de API** a aplicação também sobe: os endpoints REST de orçamento (`/api/v1/despesas`, `/extrato`...) funcionam normalmente e apenas os endpoints do assistente respondem `502`.

**Com PostgreSQL:**

```bash
docker run -d --name pg-orcamento -e POSTGRES_PASSWORD=orcamento -e POSTGRES_DB=orcamento -p 5432:5432 postgres:16
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

### Variáveis de ambiente

| Variável | Padrão | Função |
|---|---|---|
| `OPENAI_API_KEY` | (nenhum) | chave da OpenAI |
| `OPENAI_CHAT_MODEL` | `gpt-4o-mini` | modelo de chat (precisa suportar function calling) |
| `OPENAI_TRANSCRIPTION_MODEL` | `gpt-4o-mini-transcribe` | modelo de transcrição |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | localhost | conexão do perfil `postgres` |

---

## 3. Melhorias implementadas

Em vez de uma ideia grande, escolhi melhorias pequenas e completas sobre o fluxo base:

1. **Limites mensais por categoria com alertas** (`NORMAL`, `ATENCAO` a partir de 80%, `EXCEDIDO`). Toda despesa registrada devolve a situação da categoria, e a IA avisa o usuário na mesma resposta.
2. **Novas ferramentas de Tool Calling:** extrato por período/categoria, saldo da categoria, resumo do mês, listar categorias e definir limite (além de registrar despesa e receita).
3. **Validações antes de salvar:** valor positivo e com teto, data não futura, nomes de categoria normalizados (sem duplicar "Alimentação" e "alimentacao").
4. **Totais calculados pelo sistema, não pela IA.** Modelos de linguagem erram contas; o extrato devolve totais exatos de *todos* os lançamentos.
5. **Falhas viram respostas úteis:** se uma ferramenta recusa uma operação, a IA recebe o motivo e o explica; se a voz falhar, o texto ainda é devolvido (evita o usuário repetir o comando e duplicar a despesa).
6. **Erros padronizados (RFC 9457 / `ProblemDetail`)** com `@RestControllerAdvice`.
7. **Testes automatizados** das regras de negócio, do fluxo de áudio e das ferramentas, sem depender de rede ou de chave de IA.
8. **API REST sem IA** (`/api/v1/despesas` etc.), mostrando que as regras são independentes da IA.

### Arquitetura

```
api/             controllers REST, DTOs de entrada/saída, @RestControllerAdvice
aplicacao/       caso de uso (orquestra o fluxo) + portas (Transcritor, Assistente, Sintetizador)
dominio/         modelo imutável, regras de negócio, portas de repositório
infraestrutura/  adaptadores: JPA (entidades separadas do domínio), Spring AI (ChatClient, Tools, STT, TTS)
```

A dependência sempre aponta para dentro: o domínio não conhece JPA, HTTP nem Spring AI. Por isso trocar o provedor de IA ou o banco não mexe nas regras financeiras.

---

## 4. Tecnologias

Java 17 · Spring Boot 4.1 · Spring AI 2.0 (`ChatClient`, `@Tool`, transcrição e fala da OpenAI) · Spring Data JPA · H2 / PostgreSQL · Bean Validation · Lombok · JUnit 5 + AssertJ · Maven.

---

## 5. Como testar o fluxo principal

### 5.1 Testes automatizados (não precisam de chave)

```bash
mvn test
```

Os testes automatizados foram executados e **passaram sem erros**. Eles cobrem as regras de negócio (valores, datas, limites e alertas), o fluxo de áudio com dublês dos provedores e as ferramentas acionadas pela IA.

### 5.2 Sem IA: confirme que a API e o banco funcionam

```bash
# Extrato do mês (já vem com dados de demonstração)
curl http://localhost:8080/api/v1/extrato

# Registrar uma despesa e ver o alerta de limite
curl -X POST http://localhost:8080/api/v1/despesas \
  -H "Content-Type: application/json" \
  -d '{"valor": 350, "descricao": "Show", "categoria": "Lazer"}'

# Situação de uma categoria
curl http://localhost:8080/api/v1/categorias/Lazer/resumo
```

### 5.3 Com IA, por texto (simula a voz já transcrita)

```bash
curl -X POST http://localhost:8080/api/v1/assistente/texto \
  -H "Content-Type: application/json" \
  -d '{"texto": "Gastei 45 e 90 no mercado ontem", "responderEmAudio": false}'
```

Resposta (exemplo):

```json
{
  "transcricao": "Gastei 45 e 90 no mercado ontem",
  "resposta": "Pronto, registrei 45,90 reais em Mercado. Você já usou 38 por cento do limite do mês.",
  "audioBase64": null,
  "tipoMidiaAudio": null
}
```

### 5.4 Fluxo completo com áudio

Grave um comando curto (celular, gravador do sistema...) em `mp3`, `wav`, `m4a`, `webm`, `ogg`, `flac`, `mp4`, `mpeg` ou `mpga` e envie:

```bash
curl -X POST http://localhost:8080/api/v1/assistente/audio \
  -F "audio=@comando.mp3" \
  -F "responderEmAudio=true"
```

O JSON traz a `transcricao`, a `resposta` e o áudio da resposta em Base64. Para ouvir:

```bash
# Linux/macOS (requer jq)
curl -s -X POST http://localhost:8080/api/v1/assistente/audio -F "audio=@comando.mp3" -F "responderEmAudio=true" \
  | jq -r .audioBase64 | base64 -d > resposta.mp3
```

Para só converter texto em voz: `POST /api/v1/assistente/sintese` com `{"texto": "Olá!"}` devolve o MP3 direto.

> Dica: para ver a IA acionando as ferramentas, coloque `logging.level.org.springframework.ai=DEBUG` no `application.yml`.

### Endpoints

| Método | Rota | Função |
|---|---|---|
| POST | `/api/v1/assistente/audio` | comando por áudio (multipart: `audio`, `responderEmAudio`) |
| POST | `/api/v1/assistente/texto` | comando por texto |
| POST | `/api/v1/assistente/sintese` | texto → áudio MP3 |
| POST | `/api/v1/despesas` · `/api/v1/receitas` | registrar lançamento |
| GET | `/api/v1/extrato?inicio=&fim=&categoria=` | extrato com totais |
| GET | `/api/v1/resumo?mes=AAAA-MM` | resumo do mês |
| GET | `/api/v1/categorias` | listar categorias |
| GET | `/api/v1/categorias/{nome}/resumo?mes=AAAA-MM` | situação da categoria |
| PUT | `/api/v1/categorias/{nome}/limite` | definir limite mensal |

Erros seguem o padrão `ProblemDetail`: `400` entrada inválida, `404` categoria inexistente, `413` arquivo grande, `422` regra de negócio violada, `502` falha no provedor de IA.

---

## 6. O que aprendi

- **Como o Tool Calling funciona.** Quem decide chamar uma função é o modelo de linguagem, a partir das descrições que escrevi em `@Tool` e `@ToolParam`; quem executa a função é o Spring AI, que devolve o resultado ao modelo até ele montar a resposta final. Descrições vagas fazem a IA escolher a ferramenta errada, então escrevi cada uma como uma instrução.
- **IA não deve fazer contas.** Modelos de linguagem erram aritmética, por isso o extrato devolve totais calculados pelo sistema, sobre todos os lançamentos, e a IA apenas os repassa.
- **Falhas precisam ser úteis.** Quando uma regra de negócio recusa uma operação, a ferramenta devolve o motivo em vez de lançar exceção, e a IA consegue explicá-lo ao usuário. Se só a síntese de voz falhar, o texto ainda é entregue, o que evita o usuário repetir o comando e registrar a despesa duas vezes.
- **Separar as camadas compensa.** Manter domínio, entidades JPA e DTOs separados permitiu testar as regras sem banco e sem chave de IA, usando repositórios em memória, e permite trocar o provedor de IA sem mexer nas regras financeiras.
- **Validar em duas camadas.** O Bean Validation protege a borda da API com mensagens claras; o domínio valida de novo, porque as mesmas regras precisam valer para REST, para a IA e para qualquer entrada futura.
- **Datas dependem de contexto.** O modelo não sabe que dia é hoje, então a data e o fuso são enviados em cada mensagem para ele entender "ontem" ou "sexta passada".

---

## 7. Uso de IA neste projeto

Usei uma IA (Claude, da Anthropic) como **apoio** no desenvolvimento: para estruturar o projeto, gerar o código inicial e escrever os comentários e a documentação. A execução da aplicação e dos testes foi feita por mim, e o código foi estudado para que eu consiga explicar o fluxo e as decisões de arquitetura.
