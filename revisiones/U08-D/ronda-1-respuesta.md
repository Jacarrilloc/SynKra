# U08-D — respuesta a la ronda 1

Código de la ronda 2: `7289d41` (último commit que toca código: `scripts/e2e-api.sh`). El Java no cambió: CA-1 y CA-5 siguen vigentes.

- F-01: Corregido en 7289d41: las líneas 77 (`sed`), 91 (`grep|head|sed` de la versión del core), 210 (`jq -r .estado`, ahora `jq_c`), 237, 251 y 311 (tuberías), y las sustituciones de `jq` en las líneas 157, 213, 419 y 509 pasan por `ejecutar`/`jq_c` o llevan `|| falla <caso> …`. Evidencia en `ca9.txt` (con su `ca9.sh`): la mutación del revisor (`version` → `versión` en una copia de `core/build.gradle.kts`) da `FALLA preparacion: no se pudo leer la version de core/build.gradle.kts (grep, head o sed salió con 1)` y `salida=1`; con `4.0.0`, `FALLA preparacion: la versión del core … '4.0.0' y no cae en 2.x`; mi mutación propia (el cuerpo de un 200 pasa a ser HTML en `esperar_estado`) da `FALLA CU2: jq .estado salió con 5: jq: parse error …` y `salida=1`; después `aplicando=0` y `restaurado=0`. Rehechos con ese código: `ca2`, `ca3` (kind nuevo, install, límite, e2e), `ca4`, `ca6`, `ca7`, `ca8`.
- F-02: Corregido en 7289d41 y su commit siguiente: `ca5-mensajes.sh` versionado y `ca5-mensajes.txt` regenerado con `corre.sh` (cabecera con fecha, sha, arquitectura, `JAVA_HOME` y el comando); `ca3-pe.txt` regenerado con `JAVA_HOME` en la cabecera.
- F-03: Fuera de alcance: queda como C-143 (lo indica el orquestador); no se toca.
- F-04: Fuera de alcance: queda como C-143/C-144 (lo indica el orquestador); no se toca.
- F-05: Fuera de alcance: queda como C-143/C-144 (lo indica el orquestador); no se toca.
- F-06: Fuera de alcance: queda como C-143/C-144 (lo indica el orquestador); no se toca.
