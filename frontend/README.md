# Frontend AI Dev Office

Aplicação Angular standalone da Fase 1. Consulte o README na raiz para configuração do PostgreSQL, backend e comandos completos.

- `npm ci` instala as versões do lockfile.
- `npm start` abre o servidor local em localhost:4200, com proxy para o backend em localhost:8080.
- `npm test -- --watch=false` executa Vitest.
- `npm run build` gera o build de produção.

A página mostra o resultado real do readiness do backend. A hospedagem de produção precisará de proxy reverso para a mesma rota.
