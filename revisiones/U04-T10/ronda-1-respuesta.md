F-01: Corregido en la ronda 2: la raíz es `$SYNKRA_DATA/artefactos`, definida una sola vez en `SubidaPlugin.raizArtefactos` y reutilizada; prueba nueva con `ruta='synkra.db'` y tamaño cuadrado (rojo: 200; verde: 500 sin servir nada).
F-02: No es un defecto del código: Tomcat rechaza `%2f` y `%00` con 400 antes del controlador, sin fuga ni acceso a disco; documentado en la bitácora.
F-03: Corregido en la ronda 2: prueba renombrada a `hashMalFormadoEs404` y nueva `directorioIntermedioEnlazadoEs500`.
F-04: Corregido en la ronda 2: se abre el canal con NOFOLLOW_LINKS y el tamaño se comprueba sobre el archivo abierto, que es el que se sirve.
F-05: Corregido en la ronda 2: la cuenta es 21 XML; este archivo sustituye al de ruido.
