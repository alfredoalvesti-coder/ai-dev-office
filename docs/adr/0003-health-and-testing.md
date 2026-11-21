# ADR 0003 — Saúde real e testes em duas camadas

Status aceito como escolha de implementação da Fase 1.

## Decisão

Usar Actuator para liveness, readiness com banco e saúde geral. Frontend consulta readiness por proxy HTTP local. Os testes rápidos executam Spring, Security, Flyway e H2 em modo PostgreSQL. Um teste Testcontainers adicional usa a mesma imagem PostgreSQL do Compose, valida schema e reexecução de migrações.

## Consequências

H2 permite feedback sem Docker e tem diferenças de SQL e comportamento. Sucesso nessa suíte não comprova compatibilidade com PostgreSQL. Sem Docker, a integração é marcada como ignorada. O workflow verifica que a integração PostgreSQL não foi ignorada no CI. O timeout do frontend evita estado de carregamento permanente.

Não há endpoints de negócio nesta fase. DTOs, Bean Validation de domínio e tratamento centralizado de erros serão implementados junto da primeira API de gestão, com testes de contrato.
