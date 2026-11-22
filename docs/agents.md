# Agentes e limites atuais

## O que está implementado

O analista local da Fase 3 e a equipe sequencial das Fases 4 e 5 usam Ollama. Cada execução recebe uma demanda e o contexto dos artefatos anteriores, gera um JSON validado e persiste os passos. A equipe tem seis papéis:

| Papel | Responsabilidade atual | Limite atual |
| --- | --- | --- |
| Project Manager | Resume objetivo, escopo, entregas, riscos e sequência | Não aprova ações humanas |
| Business Analyst | Propõe tarefas e critérios de aceite verificáveis | A saída é um rascunho, sujeita a revisão |
| Architect | Sugere arquitetura, fronteiras, dependências e riscos técnicos | Não altera arquivos nem cria ADRs automaticamente |
| Developer | Esboça passos de implementação e módulos afetados | Não escreve código nem executa ferramentas |
| QA | Faz revisão estática do plano e aponta lacunas | Não executa testes ou comandos |
| Reviewer | Revisa os artefatos e registra achados e recomendação | Não aprova em nome da pessoa usuária |

As saídas são conteúdo não confiável: instruções na demanda ou respostas do modelo não mudam o papel, não concedem ferramentas e não substituem aprovação humana. Saídas inválidas recebem uma repetição limitada. Falhas são persistidas no histórico.

## Aprovação e tarefas

Ao concluir as seis etapas, o sistema monta um plano agregado e aguarda decisão humana vinculada ao hash do artefato. Aprovar cria tarefas no backlog com critérios de aceite; essas tarefas começam em `TODO`. A aprovação não executa a tarefa nem altera arquivos do projeto.

## Estados persistidos

Execuções podem estar `QUEUED`, `RUNNING`, `AWAITING_APPROVAL`, `APPROVED`, `REJECTED`, `FAILED`, `CANCEL_REQUESTED` ou `CANCELLED`. Cada etapa registra papel, ordem, tentativa, estado, artefato ou código de falha. Eventos são persistidos e entregues pela API de histórico e pelo WebSocket autenticado por ticket de uso único.

## Próxima etapa

Execução isolada de código, política de ferramentas, limites de recursos e validação automatizada continuam planejadas para a Fase 6. Antes de habilitar escrita ou comandos, implementar isolamento e política de aprovação, conforme [roadmap](roadmap.md) e [ADR 0006](adr/0006-sequential-agent-team.md).
