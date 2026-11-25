# Respuesta a la ronda 1 — U03-T02
F-01: Corregido en el commit de esta ronda: DialectoSqlite propio (LIMIT n OFFSET m, sin bloqueo de filas) reemplaza al ANSI; DialectoSqliteTest falló en rojo con "OFFSET 10 ROWS FETCH FIRST 5 ROWS ONLY" y "FOR UPDATE" y ahora ejecuta el SQL en SQLite y pasa (2/0/0/0).
F-02: Corregido en la bitácora (Ronda 2): la salida real del rojo de F-01 está pegada. El rojo de la ronda 1 no se puede reconstruir, solo queda el commit único ac12a5b; la salida de la ronda 2 es fresca.
F-03: Corregido en el commit de esta ronda: se quitó spring.flyway.locations (era el valor por defecto).
F-04: Corregido en el commit de esta ronda: RutaDatosPorDefectoTest usa ConfigDataEnvironmentPostProcessor.applyTo sobre un entorno sin fuentes de sistema; pasa también con SYNKRA_DATA exportado.
Candidata C-04 (PRAGMA de SQLite): no se hace, queda para el orquestador.
