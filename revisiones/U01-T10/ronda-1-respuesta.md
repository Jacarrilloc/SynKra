# Respuesta a la ronda 1 — U01-T10

- F-01: Corregido en la ronda 2: `masAltaYMasBajaDeVerdad` se reemplazó por `masAltaYMasBajaDeVerdadSinImportarElOrden`, que carga el catálogo en orden directo e inverso (plugin y core), de modo que la respuesta esperada nunca es la primera compatible en ambos órdenes. Las tres variantes de `findFirst` (plugin, core, ambos) en una copia `git archive` fallan en esa prueba; evidencia en la bitácora, «Ronda 2».
- F-02: Corregido en la ronda 2: `alternativas` hace `requireNonNull` con mensaje sobre `versionesPlugin`, `versionesCore` y `descriptor`; prueba nueva `catalogoQueDevuelveNullEsErrorConMensaje`.
- F-03: Corregido en la ronda 2: R-V5 dice solo «plugin **pedido**».
- F-04: Corregido en la ronda 2: la salida del `sed` de CA-4 y la vuelta a verde de cada mutación están pegadas en la bitácora.
