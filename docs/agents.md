# Especificação dos agentes planejados

Documento de planejamento das Fases 3 e 4. Nenhum agente está implementado.

| Agente | Entrada | Saída esperada | Responsabilidade e limite |
| --- | --- | --- | --- |
| Project Manager | Demanda e backlog atual | Plano de tarefas, dependências e acompanhamento | Distribuir e acompanhar, sem aprovar suas próprias ações sensíveis |
| Business Analyst | Demanda e perguntas respondidas | Histórias, requisitos e critérios de aceite | Tornar ambiguidades explícitas e rastrear requisitos |
| Software Architect | Requisitos e restrições | ADRs, limites e contratos | Justificar escolhas sem impor abstrações sem uso |
| Developer | Tarefa aprovada, contratos e workspace isolado | Patch versionado e registro de validação | Alterar somente os arquivos e ferramentas autorizados |
| QA | Critérios e implementação | Testes, resultados e evidências | Executar somente em ambiente isolado com limites |
| Code Reviewer | Diff, contratos e evidências | Achados com severidade e referência ao código | Avaliar qualidade e segurança, encaminhar para aprovação humana |

## Estados

`IDLE` indica agente disponível. `PLANNING` prepara a execução. `WORKING` executa uma tarefa. `WAITING` aguarda dependência, esclarecimento ou aprovação. `REVIEWING` avalia um artefato. `FAILED` encerra uma tentativa com erro. `COMPLETED` encerra uma tentativa com resultado registrado. O agente poderá retornar a IDLE após conclusão ou reconhecimento de falha. O estado da tentativa permanece no histórico.

Transições serão validadas pelo orquestrador. Registrar agente, tarefa, tentativa, estado anterior, novo estado, instante, motivo e correlação. Falha não deve gerar conclusão automática. Retentativa cria nova tentativa, mantendo evidências anteriores.

## Ferramentas e contexto

Conceder leitura por padrão dentro do workspace aprovado. Escrita, execução, rede, publicação e alteração de credenciais exigem política explícita. Ações sensíveis aguardam aprovação humana vinculada aos argumentos e à versão do artefato. Limitar tempo, consumo de tokens, número de ferramentas e tamanho de contexto. Persistir referência aos artefatos e resumo de decisão sem armazenar segredos.

A passagem de contexto inclui requisitos, critérios, contratos, alterações, validações e pendências. Dados externos e código gerado são conteúdo não confiável. O histórico deve permitir reconstruir o que cada agente fez e qual humano aprovou a entrega.
