F-01: Corregido: `AppFronteras` pasó de `@SpringBootApplication` a `@Modulithic`; rojo (bean `appFronteras` en el contexto real) y verde en la bitácora, ronda 2; la lectura de beans quedó como prueba `SynkraAppTest.laAppDePruebaDeFronterasNoEntraAlContextoReal`.
F-02: Corregido: la bitácora ronda 2 describe la mutación real (quitar import y campo) con el mensaje `Expecting code to raise a throwable.`
F-03: Corregido: comentario movido encima de la declaración de `modulos`.
F-04: Corregido a medias: se mantiene la ruta `build/spring-modulith-docs` (es el valor por defecto de Documenter y Gradle fija el directorio en `backend/`), pero se documentó la dependencia con un comentario en el test.
