F-01: Corregido en la ronda 2: CA-4 se corrió al final, después de la última edición, y la salida se pegó tal cual en la bitácora (sección «CA-4, ronda 2»), con el aviso de que los conteos no incluyen el propio bloque pegado.
F-02: Corregido en la ronda 2: quité la columna «Estado»; el resumen trae la «Decisión» literal de `CANDIDATAS.md` y «Trabajada en», sacada con `grep -lw` sobre `tareas/U04-T*.md`. El cierre lo decide el orquestador.
F-03: Corregido en la ronda 2: los comandos `awk`, el `grep -lw` y la comprobación van literales con su salida, en el resumen y en la bitácora.
F-04: Corregido en la ronda 2: H-03 y H-04 usan los casos de `stories.md` (`>=2.x<`, `core: otro-core`, `../../etc/x`, 25 MB con límite de 20 MB: 422, 422, 422 y 413); la lectura de vuelta cubre todo `SYNKRA_DATA` incluido `.tmp` y busca `etc/x` fuera; el JAR grande se borra. El cuerpo del 413 no nombra el límite: queda anotado en el resumen, sin tocar código.
F-05: Corregido en la ronda 2: la última línea del script lleva `|| true`; el script sale con rc=0.
F-06: Corregido en la ronda 2: el título es «Candidatas de otra unidad cuya decisión nombra U04».
