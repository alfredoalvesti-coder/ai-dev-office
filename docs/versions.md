# Versões e compatibilidade

Verificação em 21/11/2026. As versões abaixo foram consultadas em registros oficiais. Compatibilidade final também depende dos testes do projeto. Esta fundação usa linhas estáveis confirmadas, com atualização de patches por PR e repetição dos testes.

| Componente | Escolha | Justificativa |
| --- | --- | --- |
| Java | 21 LTS | Já disponível e compatível com Spring Boot 3.5 |
| Spring Boot | 3.5.16 | Release estável confirmada no Maven Central e documentação, integra com Spring Framework 6 e ecossistema 3.x |
| Maven | 3.9.11 | Distribuição oficial fixada no Wrapper |
| Springdoc | 2.8.17 | Linha 2.8 para Spring Boot 3.5, confirmada no Maven Central |
| PostgreSQL | 17.12-alpine | Tag oficial confirmada no Docker Hub, mesma imagem no Compose e Testcontainers |
| Node | Linha 24 | Runtime local 24.22.1, compatível com Angular 21 |
| Angular | 21 LTS | Linha suportada confirmada, versões exatas em package.json e lockfile |
| Angular Material/CDK | 21.2.14 | Mesma versão principal do Angular |
| TypeScript | Linha 5.9 | Compatível com Angular 21 |
| RxJS | Linha 7.8 | Compatível com Angular 21 |
| Vitest | Linha 4 | Runner gerado pelo Angular CLI, versão exata no lockfile |
| JUnit 5, Mockito, Flyway, JDBC e Testcontainers | BOM do Spring Boot | Evitar sobrepor a matriz de dependências gerenciada pelo Boot |

Escolhi a linha Spring Boot 3.5 para começar com uma base confirmada e estudar integração Spring de forma gradual. A escolha tem custo de atualização futura. Reavaliar suporte e migração para a linha Boot 4 antes da Fase 3, junto da decisão do SDK de IA. Angular 22 já existe no registro, mas a fundação usa a linha 21 LTS verificada. Nenhuma dependência usa versão SNAPSHOT ou release candidate.

## Pendências conhecidas

- Docker e PostgreSQL real ainda dependem de ambiente com Docker disponível.
- Validar integração completa após clone limpo e no GitHub Actions.
- A auditoria npm completa apontou 16 entradas moderadas na cadeia de desenvolvimento do Angular CLI, relacionadas a minimatch (GHSA-g3hv-75wg-m249). A correção automática oferecida exige mudança principal para CLI 22. Não aplicar `audit fix --force` sem atualizar o conjunto Angular e testar. A auditoria de dependências de produção apresentou zero vulnerabilidades na verificação local. Reavaliar avisos a cada atualização.
- Confirmar compatibilidade e releases de Spring AI/LangChain4j na Fase 3.

## Fontes

- [Spring Boot 3.5 e Java/Maven](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [Maven Central, Boot](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml)
- [Springdoc e compatibilidade](https://springdoc.org/)
- [Maven Central, Springdoc](https://repo.maven.apache.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-ui/maven-metadata.xml)
- [Angular, compatibilidade](https://angular.dev/reference/versions)
- [Angular, suporte e releases](https://angular.dev/reference/releases)
- [Registro npm, Angular](https://registry.npmjs.org/@angular/core)
- [PostgreSQL, tags oficiais](https://hub.docker.com/_/postgres)
- [Advisory minimatch](https://github.com/advisories/GHSA-g3hv-75wg-m249)
