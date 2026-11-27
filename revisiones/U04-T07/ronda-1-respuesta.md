# U04-T07 — respuesta a la ronda 1

F-01: Corregido en el commit de la ronda 2: `LimiteCuerpoJson` acota todo cuerpo JSON sin mirar la ruta; prueba con `;x=1`, `/api/%63ores` y `/api/cores/` (con y sin relleno), y bootJar con 3 MB da 413 en las tres.
F-02: Corregido en el commit de la ronda 2: `LimiteCuerpoJsonAvance` consume tras el objeto, por el flujo acotado, lo que falte hasta tope+1; prueba en chunked con relleno final (413, no se registra) y de exactamente 64 KB (201).
F-03: Corregido en el commit de la ronda 2: `MARGEN_CABECERAS` con nombre; max-request = max-file + `synkra.max-cuerpo-json` + margen.
F-04: Corregido en el commit de la ronda 2: `losLimitesDeMultipartSalenDeMaxArtefacto` ata el bean del contexto a `synkra.max-artefacto` (con los valores de Boot, 1 MB, fallaría) y `unArchivoPorEncimaDelArtefactoYDebajoDeTomcatDa413DeRC5` mira el motivo de R-C5.
F-05: Corregido en el commit de la ronda 2: se cuentan puntos de código; prueba con 63 y 64 emojis.
F-06: Corregido en el commit de la ronda 2: `FAIL_ON_TRAILING_TOKENS` en la parte `etiquetas`; da 422 `etiquetas`.
F-07: Corregido en el commit de la ronda 2: `MSG_CUERPO_DEMASIADO_GRANDE` para el 413 de JSON y de la parte `etiquetas`; prueba del mensaje.
F-08: Corregido en el commit de la ronda 2: el motivo usa `RutasPlugins.MAX_VALOR_ETIQUETA`; la prueba de texto sigue fijando «63».
F-09: No es un defecto de esta tarea: anotado en la bitácora (Tomcat escribe la parte a disco antes del tope de la ruta; acotarlo exige un parser propio).
Candidatas: C-55, C-56 como en el informe; las etiquetas del descriptor de más de 63 caracteres las deja el orquestador, sin tocar `SubidaPlugin`.
