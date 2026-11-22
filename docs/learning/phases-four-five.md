# Fases 4 e 5: equipe e escritório

## Fase 4 — Handoff entre papéis

PM define escopo; analista transforma a demanda em tarefas e critérios; arquiteto revisa fronteiras e riscos; desenvolvedor descreve módulos e plano de mudanças sem patches; QA avalia critérios sem executar testes; revisor recomenda ajustes. Cada passo recebe o contexto acumulado e grava artefato/evento. As três tentativas máximas reiniciam a equipe completa. O usuário revisa o pacote final e aprova o hash; só então as tarefas são criadas.

## Fase 5 — Eventos e visualização

Mudanças de estado geram eventos transacionais com cursor. WebSocket entrega eventos pós-commit; REST recupera o histórico após reconexão. A conexão usa ticket temporário, de uso único. Canvas mostra os seis papéis com status do backend; a lista de atividade oferece o mesmo conteúdo a leitores de tela e navegação sem canvas.

## Usar

Em um projeto, abra “Equipe multiagente e escritório”, envie uma demanda e observe a timeline enquanto a equipe processa. Ao final, revise as seis saídas e aprove ou rejeite. O modelo Ollama deve estar ativo; veja [guia da Fase 3](phase-three.md).
