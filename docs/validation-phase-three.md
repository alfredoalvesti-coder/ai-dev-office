# Validação da Fase 3

## Verificações realizadas

- Backend Java 21: `mvnw -B -Dmaven.repo.local=.maven-home/repository -DskipTests compile` — BUILD SUCCESS.
- Angular: `npm run build` — concluído; bundle inicial 359,84 kB.

Os testes automatizados não foram executados nesta entrega. O build Angular finalizou sem avisos. O Maven compilou a aplicação, sem executar testes.

## Pendências de execução integrada

- Aplicar Flyway V3 contra PostgreSQL real.
- Instalar/iniciar Ollama e baixar manualmente o modelo com `ollama pull qwen2.5:3b`.
- Exercitar análise, timeout, cancelamento e aprovação no navegador com banco e modelo reais.
- Testar atomicidade da criação de várias tarefas e o comportamento de retomada após reinício.

Build não comprova disponibilidade do Ollama nem aplicação das migrações em um banco real.
