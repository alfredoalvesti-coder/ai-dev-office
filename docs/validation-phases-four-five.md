# Validação das Fases 4 e 5

## Builds

- Backend Java 21: `mvnw -B -Dmaven.repo.local=.maven-home/repository -DskipTests compile` — compilação aprovada.
- Angular 21: `npm run build` — compilação aprovada sem avisos; bundle inicial de 380,59 kB.

Os testes automatizados não foram executados. Não foi iniciado PostgreSQL nem Ollama neste ambiente; portanto Flyway V4, seis chamadas reais, aprovação, tickets/WebSocket e recuperação de eventos precisam ser exercitados com os serviços locais ativos. O build não substitui validação integrada.

## Atualização integrada — 22/11/2026

O registro acima é da validação inicial de build. Depois, o fluxo foi executado com PostgreSQL e Ollama locais:

- Backend respondeu `UP` no Actuator e as migrações Flyway foram aplicadas usando o schema `public` como histórico.
- Uma execução real com `qwen2.5:3b` concluiu os seis papéis (`PROJECT_MANAGER`, `BUSINESS_ANALYST`, `ARCHITECT`, `DEVELOPER`, `QA` e `REVIEWER`) e aguardou aprovação humana.
- Após aprovação, a execução ficou `APPROVED` e criou duas tarefas no backlog, ambas `TODO`, com critérios de aceite. Nenhum código foi executado ou escrito, conforme o limite desta versão.
- A primeira saída do analista falhou na validação. A correção limita a uma nova saída por etapa para JSON inválido ou artefato rejeitado pela validação; a repetição integrada completou as seis etapas.
- Corrigida a configuração dos origins de WebSocket para `localhost:4200` e `127.0.0.1:4200`, configurável por `APP_WEBSOCKET_ALLOWED_ORIGINS`.

Verificação automatizada mais recente: `mvnw.cmd -Dmaven.repo.local=..\\.tools\\maven-repo verify` — BUILD SUCCESS, 16 testes no total, 15 aprovados e 1 ignorado. O teste ignorado depende do Docker/Testcontainers. O browser não foi exercitado novamente nesta atualização.

## Procedimento para ver

1. Inicie PostgreSQL e Ollama; confirme que `qwen2.5:3b` está disponível.
2. Inicie backend e frontend conforme o README e entre com as credenciais locais.
3. Selecione um projeto e abra “Equipe multiagente e escritório”.
4. Envie uma demanda. Observe estados reais do PM, analista, arquiteto, desenvolvedor, QA e revisor no canvas e na timeline.
5. Revise o pacote final e aprove/rejeite usando os controles. Só aprovação cria as tarefas no backlog; elas aparecem inicialmente em `TODO` e precisam ser executadas por uma pessoa. O agente Developer apenas propõe um plano nesta versão.

Se o WebSocket cair, a interface tenta reconectar com ticket novo e consulta o histórico persistido após o último cursor.
