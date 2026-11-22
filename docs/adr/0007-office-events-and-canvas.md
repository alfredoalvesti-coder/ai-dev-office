# ADR 0007: Eventos por WebSocket e escritório Canvas

## Status

Aceita para a Fase 5.

## Decisão

Persistir eventos de execução com cursor numérico por projeto, publicar somente após commit e recuperar eventos via REST após um cursor. Transportar eventos novos por WebSocket nativo. O browser pede um ticket aleatório, vinculado ao projeto, de uso único e válido por 60 segundos; oferece o ticket como subprotocolo, nunca em URL. O escritório 2D usa Canvas nativo e CSS para evitar runtime gráfico e assets externos.

## Limites

Tickets vivem na memória do backend, portanto o socket exige instância única neste estágio. O backend valida ticket e projeto e consome-o uma vez. O canvas é uma projeção; a timeline HTML é a alternativa acessível e ambas leem eventos persistidos. Sem animação de progresso inventado.
