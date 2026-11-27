F-01: Corregido en dfbcd7a: todos los casos arrancan con `vivo()` (assertDoesNotThrow); el mutante «sin la captura» ahora falla por AssertionFailedError (línea 73), ver `bitacoras/U02-T06-evidencia/ronda2-mutante-sin-captura.txt`.
F-02: Corregido en dfbcd7a: casos nuevos de tres plugins (orden inverso) y de `detener()` que lanza; el mutante de orden directo muere (2 fallos) y el de detener sin try/catch muere (1 fallo), ver `ronda2-mutante-orden-directo.txt` y `ronda2-mutante-detener-sin-captura.txt`.
F-03: Corregido en dfbcd7a: elegí `PluginsHealthIndicator` en el grupo readiness; eliminado `vigilar` y la publicación de REFUSING_TRAFFIC. El mutante con indicador siempre UP muere (4 fallos), `ronda2-mutante-indicador-up.txt`.
F-04: Corregido en dfbcd7a: constante `PREFIJO_FALLO_DETENER`, usada también en la prueba.
F-05: Corregido en el commit de evidencia: `ca1.txt` y `ca3.txt` regenerados sobre el código final.
