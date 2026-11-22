# ADR 0005: Provedor local de IA

## Status

Aceita para a Fase 3.

## Decisão

Usar Spring AI 1.1.8 com Ollama local, mantendo Spring Boot 3.5. O modelo padrão é `qwen2.5:3b`, configurável por `OLLAMA_MODEL`. O endereço padrão é `http://localhost:11434`, configurável por `OLLAMA_BASE_URL`. O startup não baixa modelos; a pessoa desenvolvedora instala Ollama e baixa explicitamente o modelo desejado.

## Motivos

- Evita chave de API e custo por chamada para este fluxo de desenvolvimento.
- Mantém uma porta interna para permitir trocar o adaptador depois.
- A versão 1.1.x do Spring AI mantém compatibilidade com Spring Boot 3.5.

## Limites

O modelo roda na máquina local e consome seus recursos; os termos/licença do modelo escolhido se aplicam. A demanda é limitada a 5.000 caracteres, a saída a 1.200 tokens, a fila a uma execução simultânea e as tentativas a três. A IA apenas sugere requisitos e tarefas. Nenhuma tarefa é criada antes de aprovação humana do hash SHA-256 do artefato, válida por 24 horas. Não há tool calling, execução de código nem download automático.
