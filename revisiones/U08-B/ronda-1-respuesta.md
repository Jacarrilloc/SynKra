# U08-B: respuesta a la ronda 1

F-01: Corregido en 8532284: `--pull=never` en los dos `docker run` de `scripts/verificar-imagenes.sh` (los únicos `docker run` de mis scripts); par falla-pasa en `bitacoras/U08-B-evidencia/ronda2-rojo.txt` (intenta `https://rojo-revisor.local:1/v2/…`) y `ronda2-verde.txt` (`No such image`, sin red, y los `FALLA … existe`).
F-02: Corregido en 8532284: `grep -iE '^[[:space:]]*FROM[[:space:]]'`; la mutación `from alpine:3.20 AS revision` pasaba (`OK imagenes`) y ahora cae con `FALLA init-descarga/Dockerfile FROM` y salida 1.
F-03: Corregido en 8532284: las tres versiones salen de `build.gradle.kts` e `IMAGEN_INIT_POR_DEFECTO` en los dos scripts, sin literales; `imagenes.sh` falla con `FALLA versiones` si una sale vacía.
F-04: Corregido en 8532284 y fb00045: el diagnóstico de openssl se muestra si falla (solo entonces, para no ensuciar el éxito con su `-----`), y el `trap` borra `tls.key`/`tls.crt` si el script no llegó al final; con un `mv` que falla ya no queda `tls.key` huérfano (`ronda2-verde.txt`).
F-05: Corregido en la bitácora: aclara que `ca8.txt` de la ronda 1 salió sobre `40c992e` (mismo código que `0ae9306`) y que en la ronda 2 todo se corrió sobre `fb00045`.
Candidatas del revisor (comentario de `core/Dockerfile:2`, `Id` con containerd): no se tocan, las registra el orquestador.
