# Respuesta a la ronda 2 — U01-T11

F-06: Corregido en el commit de la ronda 3 (ver SHA en el informe): `catalogosDeTexto` añade una carpeta «core-b» con los mismos nombres de plugin y un rango que sirve (fuera de `cores`, así las selecciones siguen sobre `core-a`), pesos de catálogo 3/6/1 y umbrales de `generadorCubreLosCodigos` en 15 (plugin con texto distinto), 6 (core con texto distinto) y 10 (solo en otra carpeta). Medido en 100 lotes de 1000: medias 54,3 / 26,9 / 39,5, mínimos 38 / 14 / 26, varianzas 72,6 / 31,1 / 34,0; cada media es 3,6 a 4,5 veces su umbral. Ver la bitácora, sección «Ronda 3».
F-07: Corregido en el commit de la ronda 3: `determinista` usa `edgeCases = EdgeCasesMode.NONE`; sigue en `tries = 1000` y `checks = 1000` y ahora tarda ~1,2 s.
F-08: Corregido en el commit de la ronda 3: `CORE_EQUIVOCADO` sube a 315,6 por 1000 (antes 143) y «solo en otra carpeta» a 39,5 (antes 6,6).
