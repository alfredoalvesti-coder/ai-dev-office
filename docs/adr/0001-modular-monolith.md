# ADR 0001 — Monólito modular em monorepositório

Status aceito para a Fase 1 em 21/11/2026, conforme requisito do projeto.

## Contexto

Uma pessoa está aprendendo Java, Angular e orquestração de agentes. O primeiro incremento precisa ser executável e compreensível.

## Decisão

Um deploy de backend Spring Boot com organização futura por negócio, frontend Angular separado no mesmo repositório e PostgreSQL. Usar chamadas internas para colaborar entre módulos. Publicar DTOs em suas fronteiras. Separar aplicação, domínio e infraestrutura quando os casos de uso exigirem.

## Alternativas

Microsserviços introduziriam rede, autenticação entre serviços, rastreamento e consistência distribuída. Um único pacote técnico dificultaria identificar os donos dos casos de uso. Bibliotecas Maven por domínio acrescentariam builds antes de existir código de negócio.

## Consequências

O deploy inicial é simples. Limites dos módulos precisam ser revisados nos PRs. Extração de serviço depende de evidências futuras de escala ou isolamento.
