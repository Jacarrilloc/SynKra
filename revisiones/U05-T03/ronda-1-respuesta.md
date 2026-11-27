F-01: Corregido en la ronda 2: imagen del init configurable con `synkra.kubernetes.imagen-init` (defecto `localhost:5001/synkra-init-descarga:1.0.0`), validada al arrancar (registro y tag o digest, nunca `:latest`), `imagePullPolicy: IfNotPresent`, constante con nombre y `application.properties`; YAML esperado actualizado y pruebas de rechazo y aceptación.
F-02: Corregido en la ronda 2: casos rechazados `../x`, `a/b`, nombre con salto de línea, versión con espacio, y aceptado `1.0.0-rc.1+b`; con `SEGURO = .+` fallan 7 pruebas (bitácora).
F-03: Corregido en la ronda 2: comentario en la prueba que explica por qué la guardia es inalcanzable con un plan válido.
F-04: Corregido en la ronda 2: `runAsUser: 10001` y `runAsGroup: 10001` como constantes con nombre en los dos contenedores; YAML actualizado.
F-05: Fuera de alcance: lo registra el orquestador como candidata.
F-06: Fuera de alcance: lo registra el orquestador como candidata.
