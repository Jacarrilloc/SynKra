# Plan de tareas: U03 Plataforma del backend

> **Parte 1 de Code Generation.** No se ejecuta en el módulo 5; la Parte 2 requiere aprobación y
> aviso previo a Julian. Sin Functional Design (no tiene lógica de negocio).

Raíz del proyecto. Crea `backend/` con el paquete `plataforma`. Resumen en
`aidlc-docs/construction/U03-plataforma-backend/code/`.

## Contexto

- **Historia dueña:** H-29 (Should, tarea final). **Contribuye a:** H-18 (SQLite en `/data`),
  H-24 (formato de error), H-28 (imagen).
- **Depende de:** ninguna unidad. **Expone:** la aplicación `SynkraApp`, la configuración de
  datos, `ErrorApi` y la prueba de fronteras que los módulos U04 a U07 y U10 deben pasar.
- **Entidades de base de datos:** ninguna propia; crea la base y el mecanismo de migraciones.

## Tareas

- [x] **U03-T01 · Proyecto `backend/` (Spring Boot 3, Java 21)**
  Incluido en `settings.gradle.kts` (necesita la raíz de Gradle de U01-T01); dependencias de web, actuator, Spring Data JDBC, `sqlite-jdbc`,
  Flyway y Spring Modulith (pruebas).
  **Criterio:** `./gradlew :backend:bootJar` pasa. · H-24

- [x] **U03-T02 · SQLite en `/data` con Flyway**
  Ruta configurable (`SYNKRA_DATA=/data`); `synkra.db` se crea al arrancar; migración `V1__base.sql`
  vacía; una sola conexión de escritura (SQLite, RNF-07).
  **Criterio:** `./gradlew :backend:test --tests '*BaseDeDatosTest'` pasa y, tras arrancar en local, `sqlite3 $SYNKRA_DATA/synkra.db 'select version from flyway_schema_history'` devuelve `1`. · H-18

- [x] **U03-T03 · Formato de error único**
  `ErrorApi {codigo, mensaje, detalles:[{campo, motivo}]}` y el manejador global para 400, 404,
  409, 413 y 422; los controladores lanzan `ExcepcionApi` y cada módulo traduce a ella su `Resultado`.
  **Criterio:** `./gradlew :backend:test --tests '*ErrorApiTest'` pasa con un controlador de prueba que produce cada código. · H-24

- [x] **U03-T04 · Salud**
  `/actuator/health` con la base incluida.
  **Criterio:** `curl -s localhost:8080/actuator/health | jq -r .status` = `UP` con la aplicación local. · H-24

- [x] **U03-T05 · Prueba de fronteras del monolito modular (D14)**
  `ApplicationModules.of(SynkraApp.class).verify()` y la documentación de módulos generada.
  **Criterio:** `./gradlew :backend:test --tests '*FronterasModulosTest'` pasa; con una clase de prueba que importa un paquete interno de otro módulo, falla. · H-24

- [x] **U03-T06 · Imagen del backend multi-stage y no root**
  `/data` como volumen.
  **Criterio:** `docker build -t synkra-backend backend/` y `docker inspect -f '{{.Config.User}}' synkra-backend` no vacío ni `root`/`0`. · H-28

- [x] **U03-T07 · Resumen de la unidad**
  **Criterio:** `aidlc-docs/construction/U03-plataforma-backend/code/summary.md` contiene la salida real de `./gradlew :backend:check`. · H-24

- [ ] **U03-T08 · *(Should)* OpenAPI** _(no se hizo: pendiente opcional, ver `aidlc-docs/construction/U03-plataforma-backend/code/summary.md`)_
  springdoc en `/api/v3/api-docs`; cada módulo anota sus rutas al construirse.
  **Criterio:** `curl -s localhost:8080/api/v3/api-docs | jq -r .openapi` empieza por `3.`. · H-29

## Trazabilidad

| Historia | Tareas |
|---|---|
| H-24 (contribuye) Los cinco casos sin el front | T01, T03, T04, T05, T07 |
| H-18 (contribuye) Sobrevive al reinicio | T02 |
| H-28 (contribuye) Imágenes no root | T06 |
| H-29 API documentada (Should) | T08 |

**Terminada cuando:** `./gradlew :backend:check` pasa (con `FronterasModulosTest`) y
`curl localhost:8080/actuator/health` responde `UP`.
