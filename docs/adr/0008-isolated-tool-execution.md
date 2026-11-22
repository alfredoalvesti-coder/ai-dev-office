# ADR 0008 — Ferramentas isoladas e auditáveis

**Estado** — Aceito para a entrega local da Fase 6.

O backend aceita propostas de JavaScript para as ferramentas `NODE_CHECK` e `NODE_RUN`. A pessoa operadora revisa o código e aprova explicitamente o hash SHA-256, que inclui a versão da política, projeto, execução, ferramenta, imagem imutável e código UTF-8. Cada proposta expira em 24 horas. A aprovação do plano multiagente continua criando tarefas. Para executar um trecho vindo desse plano, a pessoa operadora o submete e aprova no painel de ferramentas.

O worker usa o CLI Docker com argumentos fixos e passa o código por stdin. A imagem local deve ser confiável, identificada por SHA-256 e sem volumes declarados. O executor exige Linux com cgroups v2 e suporte a cotas de CPU, memória, swap e PIDs. Cada execução usa 0,5 CPU, 128 MiB de RAM sem swap adicional, 32 PIDs, sistema de arquivos de imagem somente leitura, um `/tmp` de 8 MiB e `/dev/shm` de 4 MiB. O contêiner roda sem rede externa, portas, volumes de host ou credenciais da aplicação, com UID/GID 1000, sem capacidades adicionais e com seccomp. O worker limita o comando a 10 segundos. O GNU timeout da imagem mantém um limite interno de 15 segundos caso o backend desapareça.

A tentativa de execução fica persistida com identidade da proposta, autor, aprovação, imagem, status, saída combinada limitada a 8 KiB e indicação de truncamento. A fila reivindica cada execução por atualização condicional. Cancelamento vence uma conclusão concorrente se já persistido. O worker tenta remover o contêiner pelo nome da execução. Após perda do processo, a próxima instância tenta limpar execuções com lease expirado. Uma interrupção deixa evidência e exige uma nova proposta para repetir.

Docker partilha o kernel do servidor que o hospeda. O escopo de operação é uma instância do backend em ambiente privado, com um daemon Docker dedicado, preferencialmente rootless, em VM isolada de serviços e dados de produção. O daemon e a imagem são componentes de confiança. Um processo com acesso ao daemon pode atuar sobre a VM que o hospeda. O executor começa desativado e requer configuração explícita. A autenticação permanece a do operador único das fases anteriores.

Referência dos limites aplicados pelo Docker Engine. [Opções de docker run](https://docs.docker.com/reference/cli/docker/container/run/)
