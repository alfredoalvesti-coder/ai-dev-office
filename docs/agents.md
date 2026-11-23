# Agentes e limites atuais

## O que está implementado

O analista local da Fase 3 e a equipe sequencial das Fases 4 e 5 usam Ollama. Cada execução recebe uma demanda e o contexto dos artefatos anteriores, gera um JSON validado e persiste os passos. A equipe tem seis papéis:

| Papel | Responsabilidade atual | Limite atual |
| --- | --- | --- |
| Project Manager | Resume objetivo, escopo, entregas, riscos e sequência | Não aprova ações humanas |
| Business Analyst | Propõe tarefas e critérios de aceite verificáveis | A saída é um rascunho, sujeita a revisão |
| Architect | Sugere arquitetura, fronteiras, dependências e riscos técnicos | Não altera arquivos nem cria ADRs automaticamente |
| Developer | Esboça passos de implementação e módulos afetados | No fluxo do plano ainda não grava arquivos |
| QA | Faz revisão estática do plano e aponta lacunas | Não executa testes ou comandos |
| Reviewer | Revisa os artefatos e registra achados e recomendação | Não aprova em nome da pessoa usuária |
| Implementação (Fase 7) | Após o plano aprovado, propõe arquivos concretos | Só grava no workspace depois de nova aprovação humana |

As saídas são conteúdo não confiável: instruções na demanda ou respostas do modelo não mudam o papel, não concedem ferramentas e não substituem aprovação humana. Saídas inválidas recebem uma repetição limitada. Falhas são persistidas no histórico.

## Aprovação e tarefas

Ao concluir as seis etapas, o sistema monta um plano agregado e aguarda decisão humana vinculada ao hash do artefato. Aprovar cria tarefas no backlog com critérios de aceite; essas tarefas começam em `TODO`. A aprovação do plano não altera arquivos.

## Implementação em workspace (Fase 7)

Com o plano `APPROVED`, o operador pode solicitar uma `implementation_run`. O gerador produz arquivos propostos; o operador revisa o hash e decide. Aprovar aplica os arquivos em `workspaces/{projectId}/` com caminhos sanitizados. Rejeitar encerra sem escrita. Veja [ADR 0010](adr/0010-implementation-workspace.md) e o [guia da Fase 7](learning/phase-seven.md).

## Estados persistidos

Execuções podem estar `QUEUED`, `RUNNING`, `AWAITING_APPROVAL`, `APPROVED`, `REJECTED`, `FAILED`, `CANCEL_REQUESTED` ou `CANCELLED`. Cada etapa registra papel, ordem, tentativa, estado, artefato ou código de falha. Eventos são persistidos e entregues pela API de histórico e pelo WebSocket autenticado por ticket de uso único.

## Próxima etapa

A V1 da Fase 7 grava arquivos aprovados no workspace. Próximos passos: verificação automatizada (build/teste), ciclo de correção com QA e eventual animação dedicada no escritório 3D para `IMPLEMENTATION_*`.
