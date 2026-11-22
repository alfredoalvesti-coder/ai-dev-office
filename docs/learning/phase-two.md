# Conceitos da Fase 2

## Contratos HTTP e DTOs

O controller recebe DTOs de entrada e valida limites antes do caso de uso. DTOs de saída evitam expor entidades JPA e seus relacionamentos. A lista de projetos usa `Page` para oferecer conteúdo e metadados de paginação. O servidor fixa uma ordenação estável e limita a página a 100 itens.

## Transações e histórico

Criar uma tarefa e gravar seu estado inicial no histórico ocorre em uma transação. Alterar o estado também atualiza a tarefa e insere um registro de histórico na mesma transação. Se uma etapa falhar, ambas são desfeitas.

## Concorrência otimista

Cada tarefa tem uma versão. A tela envia a versão que leu junto da mudança de estado ou dependências. O serviço recusa versões desatualizadas; `@Version` do JPA protege também contra duas gravações que cheguem ao banco ao mesmo tempo.

## Dependências como grafo

Uma dependência representa uma aresta da tarefa dependente para a tarefa que precisa terminar primeiro. A API exige que as duas tarefas pertençam ao mesmo projeto e rejeita ciclos. Uma tarefa não pode começar enquanto suas dependências não estiverem concluídas.

## Exercícios

1. Crie duas tarefas e configure a segunda para depender da primeira; tente iniciá-la antes de concluir a primeira.
2. Consulte o endpoint de histórico e confira o registro de criação e cada transição.
3. Abra dois clientes com a mesma tarefa; atualize o estado no primeiro e envie no segundo uma versão antiga.
4. Tente criar um projeto sem nome e uma página maior que o limite. Observe as respostas 400.
