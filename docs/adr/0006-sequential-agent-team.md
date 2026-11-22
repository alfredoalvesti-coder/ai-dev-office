# ADR 0006: Equipe multiagente sequencial

## Status

Aceita para a Fase 4.

## Decisão

Executar seis papéis Ollama em sequência: gerente de projeto, analista de negócio, arquiteto, desenvolvedor, QA e revisor. Cada passo recebe a demanda e os artefatos anteriores e grava sua saída antes do próximo passo. Limitar a seis papéis fixos, 800 tokens por papel, três tentativas completas e concorrência local de uma equipe. O rascunho final agrega as tarefas do analista e as revisões dos demais papéis. Aprovação humana pelo hash final persiste tarefas em uma transação.

## Limites

O desenvolvedor só descreve um plano de alterações, sem produzir patches. QA faz apenas análise estática textual. Nenhum papel executa comandos, testes, ferramentas, altera arquivos ou aprova o próprio trabalho. A Fase 6 continua necessária antes de execução de código.
