# Roadmap e backlog de Issues

Fases 1 a 5 implementadas no escopo descrito nas respectivas seções. Execução de código e engenharia de produção continuam na Fase 6. Os itens abaixo são rascunhos locais prontos para virar Issues quando a publicação for autorizada. Nenhuma Issue foi criada no GitHub.

## Fase 1 — Fundação

### F1-01 — Repositório e decisões

Descrição — Preparar monorepositório, README, regras de contribuição, ADRs e exemplos de configuração.

Critérios de aceite — Estrutura clara, stack verificada, nenhum segredo, licença pendente documentada, templates de Issue e PR presentes.

Dependências — Nenhuma.

Testes sugeridos — Revisar arquivos ignorados, links locais e instruções de execução a partir de clone limpo.

### F1-02 — Backend e persistência inicial

Descrição — Inicializar Spring Boot com PostgreSQL, Flyway, health, segurança inicial, OpenAPI e logs estruturados.

Critérios de aceite — Migração inicial aplicada, readiness dependente de banco, health sem detalhes internos, rotas sem autorização negadas, JAR gerado e Compose com volume persistente.

Dependências — F1-01.

Testes sugeridos — MockMvc para health e OpenAPI, teste PostgreSQL para migrações e indisponibilidade do banco, validação de Compose.

### F1-03 — Frontend e automação de qualidade

Descrição — Inicializar Angular standalone, Signals, RxJS e Material com consulta HTTP real ao backend, testes e workflow de CI.

Critérios de aceite — Exibir carregamento, sucesso, falha e nova tentativa, aplicar timeout, build de produção e CI configurado para ambas as aplicações.

Dependências — F1-02 para integração completa.

Testes sugeridos — HTTP UP, DOWN, 503, timeout e nova tentativa. No CI exigir execução do teste PostgreSQL. Registrar verificações impossíveis no ambiente local.

## Fase 2 — Gestão

Status — Implementada. Inclui projetos e tarefas autenticados, critérios de aceite, dependências, transições validadas, controle de concorrência, histórico e dashboard Angular. Ver [ADR 0004](adr/0004-phase-two-access.md) para a decisão de acesso.

### F2-01 — Cadastro e consulta de projetos

Descrição — Entregar o primeiro caso de uso persistido com nome, descrição e timestamps.

Critérios de aceite — Criar e listar projetos, validar entrada, paginação, DTOs, transação, migração e respostas de erro consistentes. A fase usa a credencial local descrita no ADR 0004; autenticação multiusuário exige decisão própria.

Dependências — F1-02 e ADR 0004.

Testes sugeridos — Dados válidos e inválidos, limites de tamanho, paginação, acesso negado e integração PostgreSQL.

### F2-02 — Tarefas e transições

Descrição — Cadastrar tarefas por projeto com critérios de aceite, dependências e estado.

Critérios de aceite — Projeto precisa existir, transições válidas explícitas, proteção contra atualização concorrente e histórico de alterações.

Dependências — F2-01.

Testes sugeridos — Projeto ausente, transição inválida, dependência circular, conflito concorrente e persistência após reiniciar.

### F2-03 — Dashboard Angular

Descrição — Criar telas reais para projetos e tarefas com formulários e listagem.

Critérios de aceite — Estados de carregamento, vazio e erro, validação acessível e atualização após gravação confirmada pelo backend.

Dependências — F1-03, F2-01 e F2-02.

Testes sugeridos — Validação de formulário, erro HTTP, navegação por teclado e fluxo ponta a ponta de criar projeto e tarefa.

## Fase 3 — Agentes

Status — Primeiro analista local implementado com Ollama, execuções e decisões persistidas e aprovação humana antes de criar tarefas. Equipe multiagente, ferramentas e execução de código continuam futuras. Veja [ADR 0005](adr/0005-local-ai-provider.md).

### F3-01 — Escolha de SDK e primeiro agente

Descrição — Comparar Spring AI e LangChain4j e implementar o agente analista com provedor configurável.

Critérios de aceite — ADR com evidências, requisitos e critérios produzidos a partir de demanda real, credenciais externas e consumo limitado. Registrar modelo, versão de contexto e resultado.

Dependências — F2-02 e configuração autorizada do provedor.

Testes sugeridos — Resposta válida, formato inválido, timeout, rate limit, indisponibilidade e teste contratual com stub HTTP determinístico.

### F3-02 — Orquestrador e aprovação humana

Descrição — Persistir execuções, tentativas e decisões de aprovação.

Critérios de aceite — Estados auditáveis, retentativa idempotente, cancelamento, limite de passos e aprovação vinculada à operação exata. Execução de código permanece bloqueada até existir sandbox.

Dependências — F3-01.

Testes sugeridos — Reentrega, falha parcial, retomada após reinício, aprovação expirada, alteração de argumentos após aprovação e cancelamento.

## Fase 4 — Equipe multiagente

Status — Implementada equipe local sequencial com seis papéis, contexto versionado, artefatos/eventos persistidos, revisão QA textual e aprovação humana final. Nenhum papel executa código. Veja [ADR 0006](adr/0006-sequential-agent-team.md).

### F4-01 — Papéis e passagem de contexto

Descrição — Implementar PM, arquiteto, desenvolvedor, QA e revisor sobre a base do analista.

Critérios de aceite — Contratos por papel, contexto versionado, dependências respeitadas e rastreabilidade dos artefatos. As ferramentas respeitam os limites do executor disponível.

Dependências — F3-02. Escrita e execução reais dependem de F6-01.

Testes sugeridos — Handoff incompleto, contexto excessivo, instrução maliciosa em artefato, falha de um agente e limites de consumo.

### F4-02 — Ciclo de revisão com evidências

Descrição — Encadear implementação, QA, revisão e aprovação final humana.

Critérios de aceite — Resultado inclui diff, testes, achados e decisão humana, com máximo de rodadas configurável e histórico preservado.

Dependências — F4-01 e F6-01 para executar código gerado.

Testes sugeridos — QA falhando, revisão reprovada, correção insuficiente, limite de rodadas e aprovação final recusada.

## Fase 5 — Escritório virtual

Status — Implementados eventos persistidos com cursor, recuperação REST, entrega WebSocket autenticada por ticket de uso único e sala 2D Canvas com timeline acessível, usando estados reais. Veja [ADR 0007](adr/0007-office-events-and-canvas.md).

### F5-01 — Eventos em tempo real

Descrição — Criar transporte WebSocket para estados reais e histórico.

Critérios de aceite — Eventos ordenados por execução, autorização por projeto, recuperação por cursor, reconexão e deduplicação.

Dependências — F3-02 e F4-01.

Testes sugeridos — Conexão interrompida, evento duplicado, evento fora de ordem e cliente sem autorização.

### F5-02 — Escritório 2D Phaser

Descrição — Representar agentes e atividades usando o fluxo real de eventos.

Critérios de aceite — Estados documentados representados visualmente, histórico consultável, alternativa textual acessível e uso de recursos controlado.

Dependências — F5-01.

Testes sugeridos — Cada estado real, reconexão, baixa taxa de quadros, viewport reduzido e coerência entre histórico e animação.

## Fase 6 — Engenharia de produção

### F6-01 — Execução isolada e política de ferramentas

Descrição — Implementar ambientes efêmeros com limites de CPU, memória, disco, tempo, arquivos, rede e credenciais.

Critérios de aceite — Host e credenciais protegidos, allowlist de ferramentas, aprovação obrigatória, artefatos rastreáveis e limpeza segura após falha. Implementar esta Issue antes de habilitar execução real nas Fases 3 e 4.

Dependências — Contratos de execução e aprovação de F3-02. Pode ser antecipada sem implementar o restante da Fase 6.

Testes sugeridos — Processo infinito, excesso de memória, path traversal, acesso à rede negado, tentativa de ler segredos e cancelamento durante escrita.

### F6-02 — Observabilidade, segurança e entrega

Descrição — Evoluir logs ECS e CI inicial para métricas, tracing, hardening e entrega de demonstração.

Critérios de aceite — Correlação por execução, alertas de falhas e consumo, análise de dependências, política de acesso, backup e procedimento de recuperação. Publicação depende de autorização humana.

Dependências — F6-01 e funcionalidades escolhidas para a demonstração.

Testes sugeridos — Restauração de backup, falha de rollout, segredo ausente, autorização entre projetos e falha de observabilidade.

### F6-03 — Portfólio e documentação de demonstração

Descrição — Preparar tutorial, roteiro de entrevista e demonstração com evidências.

Critérios de aceite — Diferenciar implementado e planejado, explicar escolhas e limites, incluir comandos reprodutíveis e dados sintéticos.

Dependências — Entregas selecionadas das fases anteriores, licença e autorização de publicação.

Testes sugeridos — Executar tutorial a partir de clone limpo e revisar exposição de dados antes de publicar.
