# Design: Fases 4 e 5 — Equipe coordenada e escritório visual

## Intenção

Permitir que uma demanda percorra PM, analista, arquiteto, desenvolvedor, QA e revisor local, com artefatos/eventos persistidos, revisão humana final e visualização de atividades reais em um escritório 2D. O usuário já autorizou a implementação; escolhas de escopo abaixo são defaults de implementação.

## Objetivos

- Coordenar PM, analista, arquiteto, desenvolvedor, QA e revisor em uma ordem explícita com contexto versionado.
- Persistir cada passo, saída, falha e transição; limitar cada tentativa a seis chamadas Ollama sequenciais e saída total controlada.
- QA e revisor produzem avaliação textual estruturada; não iniciam builds, testes, comandos, ferramentas ou alterações de arquivos.
- Vincular aprovação final humana ao hash do plano e criar tarefas em uma transação.
- Publicar eventos reais com cursor recuperável e deduplicação via WebSocket autenticado.
- Apresentar um escritório 2D acessível, com alternativa textual e sem progresso simulado.

## Fora do escopo

- Execução, edição de código, ferramentas, sandbox, testes disparados pelos agentes.
- Autonomia para criar tarefas sem decisão humana.
- Vários provedores, colaboração multiusuário e autorização granular por membro.
- Phaser ou assets externos obrigatórios; preferir canvas nativo leve e sprites CSS feitos com formas, para manter bundle controlado e sem licença/asset adicional.

## Abordagens consideradas

1. Encadear seis passos Ollama com contratos pequenos, persistência própria e saída final imutável. Escolhida: oferece rastreabilidade, falhas localizadas e fronteira clara para futuras ferramentas.
2. Fazer um único prompt pedir cinco personas ao mesmo tempo. Descartada: não permite auditar handoffs nem apontar qual papel falhou.
3. Importar Phaser e conectar a eventos por STOMP/SockJS. Descartada para esta entrega: peso e dependências maiores sem benefício para uma única sala; Canvas 2D mais WebSocket nativo cobre a cena proposta.

## Fluxo e contratos

POST recebe `{demand,idempotencyKey}` e deduplica pela chave única dentro do projeto. Worker serial processa seis papéis; cada saída JSON é validada e armazenada; falha interrompe passos posteriores; sucesso agrega backlog, critérios, arquitetura, plano de implementação, avaliação QA e revisão. A execução aguarda aprovação do hash SHA-256 por 24 horas, igual à Fase 3; depois passa a `EXPIRED` e não pode gerar tarefas. Aprovação cria tarefas sem dependências automáticas; usuário gerencia dependências depois. Rejeição não cria tarefas. WebSocket envia envelope `{eventId, cursor, projectId, executionId, type, occurredAt, payload}`. REST oferece histórico por cursor e emissão de ticket WebSocket de uso único, com expiração curta, para que o navegador não coloque credenciais na URL.

## Estado da cena

Canvas mostra seis agentes posicionados em estações; o agente ativo usa status RUNNING, passos concluídos SUCCEEDED, ainda não iniciados IDLE, e falhas FAILED. Renderização deriva apenas de eventos e snapshot REST. O painel paralelo lista o mesmo histórico em HTML acessível. Redimensionamento, pausa quando aba oculta e fallback se Canvas não estiver disponível.

## Persistência e limites

Adicionar somente V4. Tabelas para execuções da equipe, passos e decisões reutilizam project/task APIs, sem alterar migrations existentes. Concorrência total da equipe local = 1. Limites por papel: até 800 tokens; passos fixos seis; contexto contém demanda e saídas anteriores; timeout configurável; no máximo 10 tarefas propostas. Eventos persistidos antes da publicação. Reentrega não duplica mudança de estado; REST recupera cursor após reconexão. Ticket é aleatório, uso único e validade de 60 s.

## UX

Dentro do projeto, nova aba/seção “Equipe e escritório”: demanda, execuções recentes, canvas, estado por papel, timeline, artefato final, hash/expiração e ações aprovar/rejeitar/repetir/cancelar quando válidas. Sala é uma projeção; formulários e controles continuam utilizáveis por teclado/leitor de tela fora do canvas.

## Riscos

Cinco chamadas locais aumentam latência e uso de memória; exibir progresso somente ao concluir cada passo. Interrupção por reinício não deve deixar estado RUNNING eternamente: na inicialização, marcar execução ativa como FAILED/INTERRUPTED, preservando passos concluídos e permitindo retentativa explícita. WebSocket deve validar projeto no ticket e limitar clientes/eventos; não aceitar ticket em query string.
