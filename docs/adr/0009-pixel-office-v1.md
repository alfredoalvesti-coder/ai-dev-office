# ADR 0009 — Escritório 3D pixelado

**Estado** — Implementado na V1.

A cena usa Three.js com câmera ortográfica fixa, geometria em blocos e resolução de desenho reduzida. O módulo 3D é carregado quando a cena entra na área visível. A renderização anima somente o deslocamento e a digitação e para quando a cena sai de vista.

A execução selecionada fornece os estados da tentativa atual. Os eventos do projeto continuam disparando a atualização pelo backend, com consulta periódica como recuperação. Cada personagem usa um trajeto curto até a própria mesa quando o backend registra trabalho em andamento e volta quando a etapa se encerra. A animação representa o estado recebido e o tempo de deslocamento é decorativo.

A timeline e os estados em texto ficam disponíveis para tecnologias assistivas e para navegadores sem WebGL 2. A pessoa usuária pode pausar a animação. A preferência do sistema por movimento reduzido posiciona cada personagem diretamente no local correspondente ao estado.
