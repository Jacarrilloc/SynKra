# U03-T03 — respuesta a la ronda 1
F-01: Corregido en el commit de la ronda 2: nuevo `plataforma/CodigosError` (códigos y mensajes de plataforma como constantes, un solo sitio) usado por `ManejadorErrores` y por `ControladorDePrueba`; el mensaje del 500 vive una sola vez (`MSG_ERROR_INTERNO`, vía `errorInterno()`). Rojo: 4 de 13 pruebas fallaban con el manejador anterior; verde: 13 de 13.
F-02: Corregido en el commit de la ronda 2: 405 afirma código y mensaje exactos; nueva prueba del 415; el mensaje del 500 se afirma. El multipart real que supera el límite no se añade: MockMvc no aplica el límite del contenedor, así que solo se puede simular lanzando la excepción (el revisor lo verificó a mano contra Tomcat real).
F-03: Corregido en el commit de la ronda 2: mensajes con tildes y eñe; prueba `losMensajesDePlataformaLlevanTildes`.
F-04: Corregido en el commit de la ronda 2: `HttpStatus.resolve` con respaldo (400/500) en vez de `valueOf`; 405 y 415 ya tienen código propio (`METODO_NO_PERMITIDO`, `TIPO_NO_SOPORTADO`).
Candidata C-07 (`@Valid` a `detalles`): no se hace.
