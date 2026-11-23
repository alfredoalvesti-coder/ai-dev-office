# Fase 7 — Equipe trabalhar

## Conceito

Até a Fase 6 a equipe planejava. A Fase 7 acrescenta o elo de implementação: proposta de arquivos, segunda aprovação humana e aplicação em um workspace por projeto.

Compare com Apex/LWC: um plano de sprint não altera a org até alguém implantar. Aqui o hash da proposta é o contrato da implantação local.

## Fluxo

1. Aprove um `team_run`.
2. Peça a implementação no painel.
3. Aguarde a proposta (`AWAITING_APPROVAL`).
4. Revise arquivos e hash; aprove ou rejeite.
5. Em caso de aprovação, confira `workspaces/<projectId>/` e os eventos na timeline.

## Limites

No máximo oito arquivos, 16 KiB cada, extensões controladas, caminhos relativos. Sem build automático nesta V1.

## Exercício

Peça um CRUD mínimo de “notas” (título e texto). Aprove o plano, peça a implementação, leia os arquivos gerados e diga o que ainda faltaria para o código compilar e rodar.
