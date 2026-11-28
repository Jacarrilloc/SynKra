# Respuesta a la ronda 1 — U02-T09
F-01: Corregido: `rechazaConMotivo` se separó en `rechazaPorLongitud`, `rechazaPorNoAscii` y `rechazaSolicitudNulaSinLanzar`, cada una con `assertEquals` contra la constante de motivo de `PluginKycCo`; `losMotivosSonDistintos` evita constantes iguales. Par falla-pasa: `bitacoras/U02-T09-evidencia/rojo-r2.txt` (copia con stub de motivo fijo en mktemp: 17 tests, 13 fallan, incluidos todos los de rechazo) y `ca1.txt` (17/0/0/0 con la regla).
F-02: Corregido: motivos como constantes con nombre (`MOTIVO_LONGITUD`, `MOTIVO_NO_DIGITOS`, `MOTIVO_SOLICITUD_NULA`, `MOTIVO_DOCUMENTO_NULO`, `MOTIVO_VALIDO`).
F-03: Corregido: comentario en la rama del documento nulo que dice que es defensa porque `SolicitudKyc` ya lo prohíbe.
F-04: Corregido: la prueba del descriptor comprueba `coreCompatible().texto()` igual a `2.x`.
F-05: Corregido: `ca4.txt` lleva el sha tomado tras el último commit de código.
