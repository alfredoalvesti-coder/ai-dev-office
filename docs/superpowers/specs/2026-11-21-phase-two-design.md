# Design da Fase 2 — Gestão de projetos e tarefas

## Intenção

Entregar o primeiro fluxo de negócio utilizável do AI Dev Office: criar projetos, organizar tarefas com critérios de aceite e dependências, acompanhar estados e consultar o histórico de mudanças em uma interface Angular.

## Objetivos e limites

- Criar e paginar/listar projetos com nome, descrição e timestamps.
- Criar e consultar tarefas dentro de um projeto, com critérios de aceite, dependências e estado.
- Validar transições de estado, evitar ciclos e impedir sobrescrita concorrente.
- Registrar alterações de estado em histórico persistido.
- Fornecer uma interface Angular para criar projetos e tarefas, listar e atualizar estados, exibindo carregamento, vazio e erro.
- Não implementar usuários persistidos, equipes, agentes, IA, WebSocket, execução de ferramentas ou dashboard analítico.

## Acesso e segurança

Como a fase exige uma decisão antes de expor dados, a aplicação usará HTTP Basic com um único usuário configurado pelas variáveis `APP_USERNAME` e `APP_PASSWORD`. Não haverá cadastro nem armazenamento de credenciais. Os endpoints de saúde continuam públicos; documentação e APIs de gestão exigem autenticação. A tela solicita credenciais e as mantém apenas em memória durante a sessão.

## Abordagens consideradas

- Adiar endpoints até existir identidade completa: seguro, mas impede o objetivo da fase.
- Deixar a API aberta em desenvolvimento: simples, porém pode expor dados caso a aplicação seja executada em rede.
- Usar credencial única fornecida pelo ambiente: mantém a API protegida sem introduzir gestão de usuários; escolhida para esta fase.

## Arquitetura

Manter monólito modular Spring Boot, com pacotes `projects` e `tasks`, DTOs como fronteiras HTTP, validação Jakarta e migração Flyway nova. Usar `@Version` em entidades mutáveis. Tarefas terão estados `TODO`, `IN_PROGRESS` e `DONE`; atualização de estado grava evento de histórico na mesma transação. Dependências devem pertencer ao mesmo projeto e o serviço rejeitará ciclos antes de persistir.

O frontend continua Angular standalone, Material, Signals e RxJS. Um interceptor adiciona Basic Auth às chamadas de gestão. O fluxo da tela permite autenticar, criar/selecionar projeto, criar tarefa, consultar lista, alterar estado e inspecionar histórico.

## Restrições

Preservar Java 21, Spring Boot e Angular fixados. Usar Flyway sem alterar V1. Credenciais não podem ser versionadas. Não publicar nem fazer push. Testes de comportamento devem ser escritos antes da implementação e executados localmente; PostgreSQL real permanece uma validação adicional quando Docker estiver disponível.

## Critérios de aceite

Projetos e tarefas persistem no PostgreSQL; páginas e limites de entrada são validados; usuários sem credencial recebem 401; conflitos de concorrência e transições inválidas têm respostas consistentes; ciclos e dependências entre projetos são rejeitados; histórico acompanha mudanças de estado; a interface apresenta estados de carregamento, vazio, sucesso e erro e só reflete gravações confirmadas pelo backend.
