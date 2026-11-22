# Design da Fase 3 — Analista local e aprovação humana

## Intenção

Implementar um analista de requisitos que transforme uma demanda em resumo e rascunho de tarefas com critérios de aceite, usando um modelo executado localmente pelo Ollama. O resultado fica pendente de aprovação humana antes de criar tarefas.

## Objetivos

- Integrar Spring AI ao Ollama sem credencial nem cobrança de API.
- Persistir execução, tentativas, estados, eventos, consumo disponível e versão de contexto.
- Limitar texto de entrada, tokens de saída, concorrência e tentativas.
- Rejeitar saída malformada; registrar somente erro sanitizado.
- Reexecutar falha de forma idempotente; permitir cancelamento auditável.
- Vincular a decisão humana ao hash exato do rascunho e a um prazo de validade.
- Criar tarefas em transação somente depois da aprovação.
- Exibir demandas, execuções, rascunhos e controles de aprovação no dashboard Angular.

## Não objetivos e segurança

- Sem ferramentas chamadas pelo modelo, execução de código, edição automática de arquivos ou aprovação automática.
- O texto do usuário e a saída do modelo são dados não confiáveis; a aprovação é uma ação humana autenticada.
- Aprovação expira após 24 horas e fica inválida se o conteúdo salvo não corresponder ao hash aprovado.

## Abordagem

Spring AI 1.1.x com seu starter Ollama, compatível com Spring Boot 3.5. Ollama é o provedor local padrão; modelo, URL, limite de saída e concorrência são configuráveis. O domínio de execuções depende de uma porta `AnalysisGenerator`, com adaptador Spring AI na infraestrutura. Uma fila assíncrona processa cada execução fora da transação HTTP. Um pedido gera no máximo uma chamada ao modelo por tentativa. Falhas podem ser repetidas até três vezes.

## Fluxo

1. Usuário autenticado envia demanda e chave de idempotência dentro de um projeto.
2. API salva execução e primeiro evento de estado antes de agendar o analista.
3. Analista persiste tentativa, chama o modelo, valida JSON e salva rascunho, hash, modelo e uso retornado.
4. Usuário revisa rascunho; aprova ou rejeita usando o hash e a versão retornados.
5. Aprovação cria tarefas e registra decisão/evento na mesma transação; rejeição não cria tarefas.

## Limites deixados ao implementador

Manter Spring Boot 3.5, Java 21, Flyway e Angular fixados. Criar migração V3, sem alterar migrações existentes. Modelo local não será baixado automaticamente no startup. Docker/Compose não executará o modelo nem código gerado.
