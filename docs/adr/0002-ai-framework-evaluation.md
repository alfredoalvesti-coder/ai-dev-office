# ADR 0002 — Avaliação de Spring AI e LangChain4j

Status proposto. Implementação e decisão final previstas para a Fase 3.

## Critérios

Integração com Spring Boot, tool calling, MCP, portabilidade de provedores, teste determinístico, observabilidade e clareza para estudo.

| Aspecto | Spring AI | LangChain4j |
| --- | --- | --- |
| Integração | Configuração e abstrações alinhadas ao Spring | Starters Spring e APIs independentes do framework |
| Modelos | Abstrações de chat e adaptadores de provedores | Modelos e AI Services com interfaces Java |
| Ferramentas | Tool calling e integração MCP documentados | Tools e cliente MCP documentados |
| Orquestração | Workflow e persistência precisam de desenho da aplicação | AI Services facilitam composição, mas execução durável também precisa de desenho próprio |
| Escolha inicial | Candidato preferido pelo foco no ecossistema Spring | Alternativa para avaliar em uma prova de conceito equivalente |

## Proposta

Comparar uma tarefa de análise de requisitos, uma ferramenta de leitura aprovada, timeout, falha do provedor e registro de resultado nos dois frameworks. Reconfirmar versões estáveis e matriz Spring Boot no início da Fase 3. A dependência será escolhida após essa prova. Nenhum SDK de LLM foi instalado nesta fundação.

## Consequências

O domínio de tarefas e execuções deve permanecer independente do SDK. Não criar uma abstração de provedor antes do primeiro fluxo concreto. Aprovação, política de ferramentas e controle transacional pertencem à aplicação.

Fontes consultadas em 21/11/2026.

- [Spring AI](https://docs.spring.io/spring-ai/reference/)
- [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html)
- [LangChain4j com Spring Boot](https://docs.langchain4j.dev/tutorials/spring-boot-integration/)
- [LangChain4j Tools](https://docs.langchain4j.dev/tutorials/tools/)
