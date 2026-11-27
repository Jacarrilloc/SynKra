# Respuesta a la ronda 2 — U05-T05
F-08: Corregido en el commit de la ronda 3: la lista de eventos se sirve con una expectativa; e0 llega sin ninguna marca y `ebasura` con una marca ilegible, y ambos deben ir al final. El mutante nullsFirst (copia temporal) ahora falla: `bitacoras/U05-T05-evidencia/r3-mutante-nullsFirst.txt` (`losEventosSeOrdenanPor...` FAILED, 1 de 20). Con el código correcto pasan los 20 (`r3-verde-estado.txt`).
F-09: Corregido en el commit de la ronda 3: RUTA_PODS se construye con URLEncoder desde ETIQUETA_NOMBRE y NOMBRE_CORE.
F-10: No se cambia, por arbitraje del orquestador: la suite corre sin reintentos y lo declara; el orquestador amplía la candidata C-76 (U07 debe fijar la configuración de reintentos de producción con una prueba).
F-11: Corregido en el commit de la ronda 3: import de Supplier en LectorPais y imports de la prueba ordenados.
Verde completo: `r3-verde-backend.txt` (BUILD SUCCESSFUL, 28 de 28 XML con 0 saltos, fallos y errores).
