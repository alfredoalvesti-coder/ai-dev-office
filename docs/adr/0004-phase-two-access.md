# ADR 0004 — Acesso aos dados da Fase 2

Status aceito para a Fase 2 em 21/11/2026.

## Contexto

Os endpoints de projetos e tarefas persistem dados e não devem ficar abertos por padrão. O projeto ainda não possui identidade, cadastro de usuários ou regras multiusuário.

## Decisão

Usar um usuário único em memória, configurado por `APP_USERNAME` e `APP_PASSWORD`. Proteger APIs de gestão e OpenAPI com HTTP Basic, usando BCrypt para verificar a senha em memória. Health permanece público. A aplicação não cria usuários nem armazena credenciais. O backend escuta em `127.0.0.1` por padrão; acesso externo exige configuração explícita atrás de HTTPS. O frontend mantém as credenciais somente em memória e as envia às rotas `/api/` por interceptor.

## Alternativas

- Adiar APIs até implementar identidade completa: não atende ao primeiro fluxo de gestão.
- Deixar acesso anônimo no ambiente de desenvolvimento: permite expor acidentalmente dados ao executar o backend numa rede.
- Adotar agora um provedor externo ou cadastro de usuários: acrescenta serviço, gestão de segredos e decisões de identidade fora do escopo da Fase 2.

## Consequências

O produto é adequado para uso local por uma pessoa e tem proteção simples por senha. Ainda não oferece isolamento de dados por usuário, recuperação de senha ou SSO. Antes de abrir o serviço para rede ou vários usuários, será necessário substituir essa política e configurar TLS.
