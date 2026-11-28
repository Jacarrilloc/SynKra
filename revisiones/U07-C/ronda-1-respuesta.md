# U07-C — respuesta a la ronda 1

F-01: Corregido en 7246352: CA-1, CA-2, CA-3 (y CA-4 y CA-5) vueltos a correr sobre 7246352, el último commit que toca código, con cabecera de fecha, sha y comando (bitacoras/U07-C-evidencia/ca1-r2.txt, ca2-r2.txt, ca3-r2.txt, ca4-r2.txt, ca5-r2.txt).
F-02: Corregido en 7246352: nueva `VencimientoPlanesTest` (2 casos) que lee el `boolean` de `vencerSiToca` (true al vencer; false si vigente, confirmado, vencido, ya vencido o inexistente) y prueba el barrido con `vence_en` exacto. Las mutaciones `== 1` a `>= 0` y `toEpochMilli() - 1` del barrido ahora caen (ca4-r2.txt, últimas dos).
F-03: Corregido en 7246352: el SQL vuelve a salir de `Plan.VENCIDO` y `Plan.PENDIENTE`; la mutación «solo pendiente» ajustada a esa forma cae en `unPlanConfirmadoDa409YNoCambia` y en `VencimientoPlanesTest`.
F-04: Corregido en 7246352: `case Plan.PENDIENTE -> plan` explícito y `default` lanza `IllegalStateException`.
F-05: Corregido en 7246352: el 400 de `abc` compara `codigo`, `mensaje` ("La solicitud es inválida o su cuerpo no se puede leer") y `detalles` vacío.
