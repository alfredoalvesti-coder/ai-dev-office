# Fase 6 — Demonstração e operação

## Preparar uma demonstração

Use um clone limpo e siga a configuração de JDK, Node, PostgreSQL e login do [README](../../README.md). Ollama é necessário para demonstrar a equipe. O painel de ferramentas usa Docker, uma imagem Node local e PostgreSQL.

Em uma VM Linux dedicada ao executor, prepare uma imagem Node Debian com GNU timeout. No PowerShell, configure as variáveis no mesmo terminal que inicia o backend.

```powershell
docker pull node:24-bookworm-slim
$env:APP_TOOLS_IMAGE = (docker image inspect node:24-bookworm-slim --format '{{.Id}}').Trim()
$env:APP_TOOLS_ENABLED = 'true'
```

Guarde a referência SHA-256 da imagem revisada na configuração da demonstração para reproduzir o mesmo ambiente em outro momento. Confira `docker info` e o suporte a limites antes de habilitar a fila. Se a troca de imagem alterar uma proposta existente, crie e aprove uma proposta para a imagem atual.

1. Entre com o operador local e crie o projeto sintético **Catálogo de livros**.
2. Opcionalmente, solicite à equipe via Ollama um plano para cadastrar livros e aprovar empréstimos. Mostre a timeline e a criação de tarefas em `TODO` após a aprovação.
3. Na seção **Ferramentas isoladas**, escolha **Executar JavaScript**, cole `console.log([1, 2, 3].reduce((a, b) => a + b, 0));` e crie a proposta.
4. Confira fonte, imagem e hash, aprove e abra a saída. O resultado esperado é `6`. Anote o ID, o estado e a saída observada.
5. Exercício de portfólio — proponha `while (true) {}`, aprove e observe o limite de tempo. Compare o histórico da execução com os logs que têm o mesmo `execution.id`.

Em uma entrevista, explique onde o hash impede trocar o conteúdo após revisão, como o backend decide qual execução reivindicar e por que o código passa por stdin. Mostre a evidência de um teste com PostgreSQL, do teste Docker e da restauração no CI da revisão apresentada. Use dados sintéticos. Os limites do Docker, o acesso local de operador único e a ausência de publicação externa fazem parte do escopo desta demonstração.

## Monitorar

O Actuator mantém health público sem detalhes. Métricas e Prometheus exigem a autenticação do operador e devem ficar acessíveis somente na rede administrativa. No Prometheus, adicione `infra/monitoring/alerts.yml` em `rule_files` e configure um alvo `ai-dev-office` com `metrics_path: /actuator/prometheus` e autenticação Basic usando segredo em arquivo. Configure o receptor do Alertmanager na infraestrutura de operação. Não versione as configurações que contêm credenciais.

As séries `office_tools_runs_total`, `office_tools_cleanup_failures_total` e `office_tools_duration_seconds_sum` alimentam alertas de falhas, limpeza e consumo de tempo. Ausência de métricas gera alerta de indisponibilidade do alvo configurado. O contexto de tracing consta nos logs ECS. Para exportar spans, configure `APP_TRACE_EXPORT=true`, `APP_TRACE_ENDPOINT` para um coletor Zipkin interno e `APP_TRACE_SAMPLE_RATE` conforme o volume. Use `execution.id` para ligar os registros de proposta, aprovação, worker e resultado. Fonte e saída ficam no banco, sujeitas à mesma política de acesso e retenção da aplicação.

## Backup, recuperação e entrega

Em um shell Linux, na raiz, com o PostgreSQL do Compose rodando e a configuração original disponível:

```sh
sh infra/backup-postgres.sh ../office-before-release.dump
sh infra/restore-postgres.sh ../office-before-release.dump office_restore_drill
```

O backup contém dados da aplicação. Armazene-o cifrado fora do checkout e valide a restauração antes de considerar a cópia recuperável. O script cria um banco novo e preserva o banco ativo e qualquer destino já existente. Num exercício, rode o backend contra esse banco usando `SPRING_DATASOURCE_URL`, com `APP_TOOLS_ENABLED=false`, e confirme readiness, projetos, tarefas e histórico. Para produção, mantenha também cópias fora do host, retenção definida e exercícios periódicos.

O CI compila o JAR e o frontend, valida o Compose, roda a varredura de dependências e testa o ciclo de backup e restauração com dados sintéticos. Para promover uma revisão internamente, valide todos os jobs e seus artefatos, faça backup do PostgreSQL, preserve a revisão anterior e inicie o novo backend com o executor desativado. Confira readiness, carregamento do frontend e login. Em caso de falha, pare a revisão nova. Se o schema mudou, restaure a cópia em um banco separado e aponte a versão anterior para ele. Preserve o banco atualizado para diagnóstico e avalie as gravações ocorridas desde o backup antes de trocar o tráfego.

Se a limpeza do executor ficar pendente, desative novas aprovações, confirme o daemon e use a etiqueta `dev.aidevoffice.tool-run` para localizar o contêiner correspondente. Execute a recuperação conforme a identidade registrada, sem apagar contêineres de outras aplicações.

## Validação local

Em 22/11/2026, a execução de `mvnw verify` aprovou 21 testes, incluindo as verificações de aprovação, escopo e concorrência com H2. Quatro testes de integração ficaram sem execução neste ambiente sem Docker. Os 11 testes Angular, o build Angular, a auditoria npm de produção e os testes das regras do Prometheus passaram. O CI está configurado para exigir os testes Docker e PostgreSQL, testar a restauração e executar a varredura OSV. A varredura permanece uma condição de entrega. Consulte as pendências de dependências em [versões e compatibilidade](../versions.md).

Referência da configuração de tracing. [Spring Boot e Micrometer Tracing](https://docs.spring.io/spring-boot/reference/actuator/tracing.html)
