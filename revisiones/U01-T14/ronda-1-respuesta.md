# U01-T14: respuesta a la ronda 1

F-01: Corregido: añadida la fila de `VerificadorTest` (15) a la tabla de conteos. Un diff de las 12 filas contra el bucle de CA-2 no da diferencias (bitácora, «Ronda 2», F-01); suman 178.
F-02: Corregido: `CodigoMotivo` pasa a «10 códigos; el último, `DESCRIPTOR_INVALIDO`», y la fila de U01-T03 aclara que U01-T06 añadió `DESCRIPTOR_INVALIDO`. Barrido de cantidades y nombres contra el código con `grep`, pegado en la bitácora.
F-03: Corregido: U04 ya no figura como consumidor de `CatalogoEnMemoria`; su línea dice que implementa su propio `Catalogo` sobre SQLite y prueba S1 a S7 de `SuiteCombinaciones`, con S11 pendiente en U04 y U05, y cada afirmación de «Quién lo usa» lleva su fuente. La frase rota se reescribió.
F-04: Corregido: «qué expone» dice que los fallos de datos vuelven como `Resultado.Fallo` y que las precondiciones de programación lanzan `IllegalArgumentException` (constructores y `Verificador.alternativas` con un veredicto ajeno).
F-05: No es un defecto del resumen: el criterio CA-1 es contradictorio (la línea de `verificarSuite` no está en `tail -6`). Se deja la solución y queda anotado en la bitácora para el orquestador.
F-06: Corregido: el stat de CA-5 se rehízo al final, tras la última edición (bitácora, «CA-5 ronda 2»).
