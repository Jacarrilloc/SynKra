# Ronda 1 — respuesta del codificador (U01-T04)

F-01: Corregido en el commit de esta ronda: la regex `INTERVALO` usa el espacio literal (` *`) en lugar de `\s*`, según el arbitraje (U+0020). Rojo: 3 fallos (`>=2.1.0\t<3.0.0`, `>=\n2.1.0`, `>=2.1.0\r\n<3.0.0`); verde: 36 pruebas, 0 fallos.
F-02: Corregido en el commit de esta ronda: casos ` >=2.1.0`, `>=2.1.0 ` y `>=2.1.0 <3.0.0 ` añadidos a los inválidos (ya pasaban; ahora quedan fijados).
F-03: No es un defecto: sigue el precedente aprobado de `Version.parsear` (campo `"version"`). Se deja sin cambio y se propone como tarea candidata que `LectorDescriptor` (U01) asigne el campo.
