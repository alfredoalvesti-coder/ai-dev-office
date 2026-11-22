# Versões e compatibilidade

Verificação em 22/11/2026. As versões abaixo foram consultadas em registros oficiais. As versões exatas ficam no POM e no lockfile.

| Componente | Escolha | Justificativa |
| --- | --- | --- |
| Java | 21 LTS | Compatível com Spring Boot 4.2 |
| Spring Boot | 4.2.0 | Gerencia Spring Framework 7.1.0, Tomcat 11 e Jackson 3 |
| Spring AI | 2.1.0 | Linha para Spring Boot 4.2 com as correções do SDK MCP |
| Maven | 3.9.11 | Distribuição oficial fixada no Wrapper |
| Springdoc | 3.2.1 | Linha compatível com Spring Boot 4.2 e Jackson 3 |
| PostgreSQL | 17.12-alpine | Tag oficial confirmada no Docker Hub, mesma imagem no Compose e Testcontainers |
| Node | Linha 24 a partir de 24.15 | Versão recomendada para desenvolvimento e CI do projeto |
| Angular, Material e CDK | 22.3.0 | Mesma versão do framework, CLI e bibliotecas de interface |
| TypeScript | 6.0.3 | Compatível com Angular 22 |
| RxJS | Linha 7.8 | Compatível com Angular 22 |
| Vitest | Linha 4 | Runner gerado pelo Angular CLI, versão exata no lockfile |
| JUnit 5, Mockito, Flyway e Testcontainers | BOM do Spring Boot | Manter a matriz de dependências gerenciada pelo Boot |

## Correções de segurança

Na Fase 6, a primeira atualização de patches reduziu a varredura OSV de 30 para 7 apontamentos. A atualização seguinte migrou o backend para Spring Boot 4.2 e Spring AI 2.1, que resolvem os apontamentos anteriores em Spring MVC/WebFlux e no SDK MCP. Os adaptadores Ollama usam a API atual de opções. O Jackson 3 foi configurado com padrões compatíveis com Spring Boot 3 para preservar os formatos da API e dos artefatos armazenados.

No frontend, a migração oficial para Angular CLI 22 retirou a árvore transitiva que continha `pacote`, `glob` e `minimatch`. O CI mantém a varredura OSV dos dois manifests e a auditoria npm de produção.

Na verificação final, o OSV-Scanner 2.7.1 retornou zero apontamentos para o POM e o lockfile. O `npm audit` completo também retornou zero. Passaram 22 testes do backend, incluindo o contrato HTTP dos dois adaptadores Ollama, e 11 testes Angular. Os dois builds passaram. A resolução do lockfile para Linux x64 glibc passou em modo dry-run.

## Pendências conhecidas

- Docker e PostgreSQL real ainda dependem de ambiente com Docker disponível.
- Validar integração completa após clone limpo e no GitHub Actions.
- O teste de contrato usa um servidor Ollama simulado. A integração com o modelo real continua sendo um teste de ambiente.

## Fontes

- [Spring Boot e Java/Maven](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring AI 2.1 e dependências](https://spring.io/blog/2026/11/20/spring-ai-2-1-0-GA-available-now/)
- [Maven Central, Boot](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml)
- [Springdoc e compatibilidade](https://springdoc.org/)
- [Maven Central, Springdoc](https://repo.maven.apache.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-ui/maven-metadata.xml)
- [Angular, compatibilidade](https://angular.dev/reference/versions)
- [Angular, suporte e releases](https://angular.dev/reference/releases)
- [Registro npm, Angular](https://registry.npmjs.org/@angular/core)
- [PostgreSQL, tags oficiais](https://hub.docker.com/_/postgres)
- [Advisory minimatch](https://github.com/advisories/GHSA-g3hv-75wg-m249)
