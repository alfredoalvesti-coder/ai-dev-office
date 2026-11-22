# Escritório 3D pixelado — V1

Abra o frontend, entre com as credenciais locais e selecione um projeto. O **Escritório da equipe** aparece no painel **Equipe multiagente e escritório**.

1. Inicie uma demanda com o backend, o PostgreSQL e o Ollama ativos.
2. Selecione a execução na lista. Observe o personagem da etapa caminhar e digitar na mesa enquanto o status estiver em andamento.
3. Quando a etapa terminar, confira o retorno do personagem e compare a legenda com a **Atividade recente**.
4. Use **Pausar animação** para acompanhar as mudanças de estado com os personagens estáticos.

O navegador precisa de WebGL 2 para desenhar a sala. Os estados em texto e a timeline também funcionam sem essa capacidade.

**Exercício** — Inicie uma nova execução, pause o movimento e aguarde uma transição de etapa. Localize o evento na timeline e compare os estados dos dois personagens envolvidos.

Para reproduzir a verificação do frontend, rode `npm ci`, `npm test -- --watch=false` e `npm run build` em `frontend`. O teste Java de projeção verifica os nomes JSON consumidos pela cena usando H2 no modo PostgreSQL. [Decisão de arquitetura](../adr/0009-pixel-office-v1.md)
