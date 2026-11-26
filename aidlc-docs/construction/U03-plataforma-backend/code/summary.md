# U03 Plataforma del backend: resumen de la unidad

Unidad: U03 (`backend/`, Spring Boot 3, Java 21). Historias: H-24 (cinco casos sin el front), H-18 (contribuye, T02) y H-28 (T06).
Este resumen lo escribe U03-T07 sin cambiar código. La unidad queda lista para que el orquestador la cierre.

## Tareas

| Tarea | Qué construyó | PR | Rondas de revisión |
|---|---|---|---|
| U03-T01 | Proyecto `backend/` Gradle con Spring Boot 3 y Java 21 | #10 | 2 |
| U03-T02 | SQLite en `SYNKRA_DATA` con Flyway y dialecto propio | #12 | 4 |
| U03-T03 | Formato de error único (`ErrorApi`) y manejador de errores | #14 | 2 |
| U03-T04 | Salud: indicador de la base de datos | #19 | 2 |
| U03-T05 | Prueba de fronteras del monolito modular (D14) | #21 | 3 |
| U03-T06 | Imagen multi-stage, usuario no root | #23 | 2 |
| U03-T07 | Este resumen | (pendiente de PR) | (pendiente) |
| U03-T08 | OpenAPI (Should, H-29): pendiente opcional, no se hizo | n/a | n/a |

Los PR #10, #12 y #14 (T01 a T03) se fusionaron en `modulo-6-rehecho`. Esa rama llegó a `main` con el PR #16, cuya rama de origen era otra (`claude/project-thread-ti5y5k`) y contenía `modulo-6-rehecho`.

## Archivos

Código (`backend/`, más una línea en la raíz):

- `settings.gradle.kts` (raíz): U03-T01 le añadió `backend` a los módulos del proyecto
- `backend/build.gradle.kts`, `backend/Dockerfile`, `backend/.dockerignore`
- `backend/src/main/java/io/synkra/SynkraApp.java`
- `backend/src/main/java/io/synkra/plataforma/BaseDeDatos.java`
- `backend/src/main/java/io/synkra/plataforma/CodigosError.java`
- `backend/src/main/java/io/synkra/plataforma/DetalleError.java`
- `backend/src/main/java/io/synkra/plataforma/DialectoSqlite.java`
- `backend/src/main/java/io/synkra/plataforma/ErrorApi.java`
- `backend/src/main/java/io/synkra/plataforma/ExcepcionApi.java`
- `backend/src/main/java/io/synkra/plataforma/ManejadorErrores.java`
- `backend/src/main/java/io/synkra/plataforma/SaludBaseDeDatos.java`
- `backend/src/main/java/io/synkra/plataforma/package-info.java`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/db/migration/V1__base.sql`

Pruebas (`backend/src/test/java/io/synkra/`):

- `FronterasModulosTest.java`, `SynkraAppTest.java`
- `fronteras/AppFronteras.java`, `fronteras/uno/Uno.java`, `fronteras/dos/interno/Algo.java` (fixtures de la prueba de fronteras)
- `plataforma/BaseDeDatosTest.java`, `plataforma/DialectoSqliteTest.java`, `plataforma/ErrorApiTest.java`
- `plataforma/RepositorioSqliteTest.java`, `plataforma/RutaDatosPorDefectoTest.java`, `plataforma/SaludTest.java`
- `plataforma/prueba/ControladorDePrueba.java`, `plataforma/prueba/Elemento.java`, `plataforma/prueba/ElementoRepositorio.java` (fixtures)

## Cómo construir, probar y empaquetar

Desde la raíz del repositorio:

```bash
./gradlew :backend:test --rerun :backend:check   # pruebas y verificaciones
./gradlew :backend:bootJar                       # JAR ejecutable
docker build -t synkra-backend backend/          # imagen
docker inspect -f '{{.Config.User}}' synkra-backend
```

Si `docker info` no responde, se arranca el daemon con `dockerd > /tmp/dockerd.log 2>&1 &` (como en U03-T06).

## Evidencia

### `:backend:check` (corrida del 2026-11-26, 17:29 UTC)

Cola de `./gradlew :backend:test --rerun :backend:check`, sin editar:

```
For more on this, please refer to https://docs.gradle.org/9.9.2/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 9s
4 actionable tasks: 1 executed, 3 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.9.2/userguide/configuration_cache_enabling.html
```

### Conteos de pruebas (XML de `backend/build/test-results/test/`)

| Clase | tests | skipped | failures | errors |
|---|---|---|---|---|
| io.synkra.FronterasModulosTest | 3 | 0 | 0 | 0 |
| io.synkra.SynkraAppTest | 2 | 0 | 0 | 0 |
| io.synkra.plataforma.BaseDeDatosTest | 3 | 0 | 0 | 0 |
| io.synkra.plataforma.DialectoSqliteTest | 6 | 0 | 0 | 0 |
| io.synkra.plataforma.ErrorApiTest | 13 | 0 | 0 | 0 |
| io.synkra.plataforma.RepositorioSqliteTest | 4 | 0 | 0 | 0 |
| io.synkra.plataforma.RutaDatosPorDefectoTest | 1 | 0 | 0 | 0 |
| io.synkra.plataforma.SaludTest | 3 | 0 | 0 | 0 |
| **Total** | **35** | **0** | **0** | **0** |

### Imagen

`./gradlew :backend:bootJar && docker build -t synkra-backend backend/ && docker inspect -f '{{.Config.User}}' synkra-backend` terminó bien. Cola del build y usuario de la imagen:

```
#14 naming to docker.io/library/synkra-backend:latest done
#14 unpacking to docker.io/library/synkra-backend:latest done
#14 DONE 0.1s
10001:10001
```

`docker run --rm --entrypoint id synkra-backend` devuelve `uid=10001(synkra) gid=10001(synkra) groups=10001(synkra)`, el mismo UID que dejó U03-T06.

## Candidatas pendientes

Fuente: `CANDIDATAS.md`. Regla: están todas las candidatas cuya columna de origen es una revisión o una ronda de una tarea de U03. U03 no las resuelve.

| Candidata | Una línea | Unidad que la recibe |
|---|---|---|
| C-03 | Centralizar versión y repositorios en `settings.gradle.kts` (`dependencyResolutionManagement`) | Una tarea que pueda tocar la raíz |
| C-04 | PRAGMA de SQLite (`busy_timeout`, WAL, `foreign_keys`) | U04-T01 |
| C-05 | Convertidores de fecha de SQLite y regla de columnas `INTEGER` para fechas | U04-T01 |
| C-07 | Traducir `@Valid` (`MethodArgumentNotValidException`, `HandlerMethodValidationException`) a `detalles` | U04 (primeros DTO) |
| C-08 | Prueba con contenedor real del `413 DEMASIADO_GRANDE` en formato `ErrorApi` | U04-T04 |
| C-11 | Detalle `error` de `SaludBaseDeDatos` con `BaseDeDatos.archivo(...)` | La siguiente tarea que toque salud o `BaseDeDatos` |
| C-12 | Afirmar el contenido real de `components.puml` | U04 (primer módulo de negocio) |
| C-14 | Decidir si los fixtures de `plataforma.prueba` entran a todos los `@SpringBootTest` | U04 (cuando traiga sus controladores) |
| C-17 | UID arbitrario con `fsGroup` o permisos de grupo en `/data` | U08, con el `securityContext` del Deployment |

C-03 y C-17 no estaban en la lista de la tarea, pero salen de U03 (T01 y T06). C-18 (plantilla de criterios) sale de U03-T06 y ya está aplicada en U01-T08 y U03-T07: no queda pendiente.

## Lo que queda fuera

- U03-T08 (OpenAPI, H-29, Should): no se hizo; es un pendiente opcional.
- Marcar las casillas del plan de U03 y actualizar `aidlc-state.md`: lo hace el orquestador al cerrar la unidad.
- Las candidatas de arriba: se resuelven en las unidades indicadas.
- Despliegue de la imagen en el clúster: es de U08.
