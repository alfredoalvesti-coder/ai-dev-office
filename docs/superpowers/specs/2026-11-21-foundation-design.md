# Design da Fase 1

## Intenção

Criar a fundação executável do AI Dev Office para aprendizado e portfólio Java, Angular e IA. O escopo é o texto fornecido pelo usuário em 21/11/2026. Fases 2 a 6 permanecem no roadmap.

## Abordagem adotada

O usuário especificou monólito modular. A execução adota um monorepositório com Maven e Angular CLI. Bibliotecas de negócio separadas acrescentariam publicação e versionamento sem benefício nesta fase. Microsserviços exigiriam coordenação e infraestrutura antes dos primeiros casos de uso. A escolha local é manter backend, frontend e infraestrutura em diretórios irmãos.

## Entregas e critérios

- Spring Boot com Web, JPA, Security, Validation, Actuator, Flyway e OpenAPI.
- Health público sem detalhes, readiness dependente de banco e demais rotas negadas.
- Angular standalone, Signals, RxJS e Material mostrando resultado de HTTP real, erro e nova tentativa.
- PostgreSQL por Compose, volume persistente, senha externa e porta em loopback.
- Testes Java com H2 e teste adicional PostgreSQL via Testcontainers, explicitamente pulado sem Docker.
- Testes Angular de carregamento, sucesso, falha e indisponibilidade do serviço.
- Documentação em português, ADRs, aprendizado, backlog das seis fases e templates GitHub.
- Licença pendente por decisão do usuário. Publicação e push exigem autorização posterior.

## Restrições e escolhas de execução

Diretório inicial vazio. Java 21, Node 24 e Git disponíveis. Maven, npm, Angular CLI e Docker ausentes do PATH. npm e Maven oficiais baixados para `.tools` no workspace pai, somente para validação. Nenhuma instalação global.

Backend usa Java 21 e Spring Boot 3.5.16. Frontend usa Angular 21 LTS. PostgreSQL usa a linha 17. Versões exatas e referências em `docs/versions.md`. Frontend usa proxy de desenvolvimento e rotas HTTP relativas. A entrega não configura credenciais de LLM nem contrata serviços. Integração de IA será decidida na Fase 3.
