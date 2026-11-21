# Conceitos da fundação

## Spring Boot e Apex

Uma classe `@Configuration` declara objetos gerenciados pelo container. O método `@Bean` registra uma dependência. Isso permite substituir infraestrutura nos testes sem espalhar criação manual de objetos. Em Apex, serviços e seletores também separam responsabilidade, mas o gerenciamento de objetos e transações segue a plataforma Salesforce. Em Spring, esses limites precisam ser desenhados explicitamente.

`SecurityFilterChain` aplica regras antes de chegar ao controller. Health é uma entrada operacional pública, com resposta reduzida. JPA simplifica persistência, enquanto Flyway versiona alterações de banco. Essa dupla pede que o Hibernate valide o schema e que cada alteração tenha uma nova migração.

## Angular e LWC

Um standalone component declara suas dependências no próprio componente. Signals armazenam estado reativo e atualizam o template quando o valor muda, com propósito semelhante à reatividade de propriedades em LWC. RxJS trata fluxos assíncronos. Nesta fundação, HttpClient produz um Observable e o resultado atualiza um Signal. O `timeout` encerra a espera e `takeUntilDestroyed` cancela a assinatura quando o componente é destruído.

O proxy de desenvolvimento encaminha `/actuator` para o backend. Isso evita configurar CORS permissivo para uma aplicação local. Um deploy futuro precisará de proxy reverso ou política de origens definida.

## Testes e evidências

MockMvc executa requisições através do Spring e da cadeia de segurança sem abrir porta real. HttpTestingController controla respostas HTTP no teste Angular. H2 testa inicialização, mas PostgreSQL em Testcontainers verifica o banco alvo. Uma execução ignorada precisa aparecer como pendência na entrega.

## Exercícios

1. Interrompa o backend e observe a mensagem de indisponibilidade. Inicie novamente e use o botão de verificação.
2. Troque temporariamente a resposta HTTP do teste Angular para DOWN e explique por que o status muda.
3. Com Docker disponível, execute `PostgresIntegrationTest` duas vezes e confira o histórico do Flyway.
4. Explique em uma entrevista a diferença entre liveness, readiness e status de uma tarefa de negócio.
