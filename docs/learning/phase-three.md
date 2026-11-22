# Fase 3: analista local e aprovação

## Fluxo

1. O frontend envia uma demanda e uma chave de idempotência.
2. O backend persiste a execução como `QUEUED`; após a transação, o worker serial processa a fila.
3. Spring AI chama Ollama pela porta `AnalysisGenerator`. A resposta JSON é validada e persistida como rascunho com hash SHA-256.
4. A pessoa revisa o resumo e as tarefas propostas. A aprovação ou rejeição precisa enviar o hash que foi exibido.
5. A aprovação cria todas as tarefas na mesma transação da decisão. Rejeição não cria tarefa.

## Estados e auditoria

Estados: `QUEUED`, `RUNNING`, `AWAITING_APPROVAL`, `APPROVED`, `REJECTED`, `FAILED`, `CANCEL_REQUESTED`, `CANCELLED` e `EXPIRED`. Tentativas e eventos são persistidos em tabelas dedicadas. O estado persistido é a fonte de verdade; a interface atualiza a lista periodicamente.

## API

- `POST /api/projects/{projectId}/analysis-runs` — `{ "demand": "...", "idempotencyKey": "..." }`
- `GET /api/projects/{projectId}/analysis-runs` — execuções recentes.
- `GET /api/analysis-runs/{id}` — artefato, hash, tentativas e eventos.
- `POST /api/analysis-runs/{id}/decision` — `{ "decision": "APPROVED", "artifactHash": "..." }` ou `REJECTED`.
- `POST /api/analysis-runs/{id}/retry` e `/cancel` — retentativa e cancelamento.

Todas as rotas exigem a autenticação Basic local configurada para o app.

## Executar localmente

Instale o Ollama conforme a documentação oficial e execute `ollama pull qwen2.5:3b`. Inicie o banco e o backend normalmente. Para outro modelo/endereço, defina `OLLAMA_MODEL` e `OLLAMA_BASE_URL` no ambiente do processo. O projeto não baixa modelo por conta própria. Execuções reais ainda dependem de backend, PostgreSQL, Ollama e modelo disponíveis.
