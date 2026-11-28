# Respuesta a la ronda 1 — U05-T07
F-01: Corregido en el commit de código de la ronda 2: temporal nuevo con `mktemp` dentro de $PLUGINS; se niega (código 7, sin escribir) si el destino es enlace o directorio, o (código 2) si $PLUGINS es enlace; un archivo regular previo se reemplaza; casos A y B del revisor y dos más en test.sh (rojo: 4 FALLA; verde: ok).
F-02: Corregido: caso `espera-creciente` con `sleep` falso al frente del PATH, base 1, exige `1 2 4 8`; el mutante de espera fija ahora falla (r2-mutantes.txt).
F-03: Corregido: decisión ratificada por el orquestador; caso `linea-vacia-en-medio` añadido.
F-04: Corregido: `SYNKRA_ESPERA_BASE` debe ser entero no negativo, si no código 2 con mensaje; 3 casos (0.5, -1, abc).
F-05: Corregido: documentado en un comentario de descargar.sh como comportamiento aceptado; caso `archivo-previo-se-reemplaza`.
F-06: Corregido: caso `set-eu-presente` (grep de la línea `set -eu`); el mutante sin `set -eu` ahora falla. Es una prueba de texto, no de comportamiento: ninguna ruta del script lo ejercita de otro modo.
F-07: Corregido: ruta del acuse en la bitácora cambiada a /home/casdav/Projects/Master/wt-orq-U04.
