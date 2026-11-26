F-01: Corregido en el commit de esta ronda: `nXIncluyeSoloSuMayor` genera el mayor como `n + desplazamiento`; con la mutación N+2 antes salían `1,2`, `0,1`, `6,7`, `10,11` y ahora 6 de 6 corridas llegan a `0, 1`.
F-02: Corregido en el commit de esta ronda: peso 6/10 a los ambiguos, muestra de 5000 y comentario con el cálculo (~6e-22); 2000 repeticiones con semillas distintas, 0 fallos.
F-03: Corregido en el commit de esta ronda: `entradas()` incluye `hostiles()` (anidación en flujo y bloque, alias, `---`, `%`, `? `, `<<`, tamaños en MAX_BYTES); con M2 la propiedad falla con StackOverflowError (3 de 3) y sin ella pasa; con la prueba vieja M2 pasaba.
F-04: No es un defecto: el orquestador lo marcó como no bloqueante; sin cambio.
F-05: Corregido en el commit de esta ronda: constante `MAX_COMPONENTE`, mínimo cerrado con `y` y `z` hasta 50 y mayor del mínimo hasta 49.
F-06: Corregido en el commit de esta ronda: 14 textos nuevos en AMBIGUOS (largo con espacios, CR, NEL, LS, BOM, control, `+.inf`, `.NAN`, `+12`, `yes`, `no`, `on`); la ida y vuelta de 1000 intentos pasa.
