# Plan de tareas: U02 Core, contrato y plugins de país

> **Parte 1 de Code Generation.** No se ejecuta en el módulo 5; la Parte 2 requiere aprobación y
> aviso previo a Julian. Functional Design se omitió para esta unidad (`execution-plan.md`): las
> reglas que aplica son las de U01, y el arranque está definido en S8 de `services.md`.

Raíz del proyecto. Crea `plugin-api/`, `core/`, `plugins/kyc-co/`, `plugins/kyc-pe/` y el
*fixture* `plugins/fixture-falla-al-iniciar/` (solo pruebas). Resumen en
`aidlc-docs/construction/U02-core-plugins/code/`.

## Contexto

- **Historias dueñas:** H-21, H-22, H-23. **Contribuye a:** H-10 (corre la suite), H-28 (imagen).
- **Depende de:** U01 (`verificador` y su `testFixtures`).
- **Expone:** `SynkraPlugin`, `PuntoKyc`; la imagen `synkra-core:2.1.0` con `GET /plugins`,
  `/actuator/health/readiness` y `POST /kyc/{plugin}`; los JAR de `kyc-co 1.0.0`, `kyc-pe 1.3.0`
  y `kyc-pe 2.0.0`.

## Tareas

- [x] **U02-T01 · `plugin-api/` (contrato)**
  `SynkraPlugin` (`nombre`, `iniciar`, `detener`), `ContextoPlugin`, `PuntoKyc` con
  `SolicitudKyc`/`ResultadoKyc`. Sin dependencias.
  **Criterio:** `./gradlew :plugin-api:build` pasa y `./gradlew :plugin-api:dependencies --configuration runtimeClasspath` no lista dependencias. · H-21

- [x] **U02-T02 · Proyecto `core/` (Spring Boot) con la versión compilada**
  La versión `2.1.0` se escribe en tiempo de build en un recurso del JAR; no se lee de variables
  de entorno (D4).
  **Criterio:** `./gradlew :core:bootJar && unzip -p core/build/libs/core-*.jar BOOT-INF/classes/synkra-core.properties` muestra `version=2.1.0`. · H-21

- [x] **U02-T03 · `CargadorPlugins`: un `URLClassLoader` por plugin**
  Carga con `ServiceLoader` dentro del cargador propio de cada JAR.
  **Criterio:** `./gradlew :core:test --tests '*CargadorPluginsTest'` pasa y afirma que dos plugins tienen cargadores distintos. · H-22

- [x] **U02-T04 · `ArranquePlugins`: leer, verificar con U01 y activar**
  Lee `/config/plugins-activos.yaml` y `/plugins`; arma un `Catalogo` con lo presente y llama a
  `Verificador.verificar` con la versión compilada; si el veredicto es `RECHAZADA`, el proceso
  termina con código ≠ 0 y el motivo en el log; los plugins presentes y no activos no se cargan.
  **Criterio:** `./gradlew :core:test --tests '*ArranquePluginsTest'` pasa con: válido (S1), incompatible `3.x` (S4), activo ausente (S6), presente y no activo (S12). · H-21

- [x] **U02-T05 · La suite de U01 aplicada al arranque**
  Prueba parametrizada que toma `SuiteCombinaciones.casos()` y comprueba que el arranque del core
  coincide con el veredicto esperado (concordancia motor-core, R1).
  **Criterio:** `./gradlew :core:test --tests '*ConcordanciaSuiteTest'` pasa todos los casos ejecutables. · H-21, H-10

- [x] **U02-T06 · Fallo al iniciar un plugin y readiness**
  Una excepción en `iniciar` impide que la readiness pase; el log nombra el plugin. *Fixture*
  `fixture-falla-al-iniciar`.
  **Criterio:** `./gradlew :core:test --tests '*FalloInicioPluginTest'` pasa: `/actuator/health/readiness` responde `503` y el log contiene el nombre del plugin. · H-22

- [x] **U02-T07 · Endpoints del core**
  `GET /plugins` (nombre y versión de los activos) y `POST /kyc/{plugin}` que delega en el `PuntoKyc`.
  **Criterio:** `./gradlew :core:test --tests '*EndpointsCoreTest'` pasa; en local, `curl localhost:8080/plugins` lista solo los activos. · H-21

- [x] **U02-T08 · El core no conoce países (P3)**
  **Criterio:** `grep -rniE "colombia|peru|\"co\"|\"pe\"" core/src/main` no devuelve nada. · H-21

- [x] **U02-T09 · Plugin `kyc-co 1.0.0`**
  Regla simulada (por ejemplo, documento numérico de 6 a 10 dígitos); `synkra-plugin.yaml` con
  `core: synkra-core`, `coreCompatible: 2.x`, `etiquetas: {pais: co}`.
  **Criterio:** `./gradlew :plugins:kyc-co:test` pasa y `unzip -p plugins/kyc-co/build/libs/kyc-co-1.0.0.jar synkra-plugin.yaml` muestra `pais: co` y `2.x`. · H-23

- [x] **U02-T10 · Plugin `kyc-pe 1.3.0` y `kyc-pe 2.0.0`**
  Regla simulada de Perú (por ejemplo, DNI de 8 dígitos); la `2.0.0` declara `coreCompatible: 3.x`
  para la demo de CU4.
  **Criterio:** `./gradlew :plugins:kyc-pe:test` pasa y los dos JAR muestran `2.x` y `3.x` en su descriptor. · H-23

- [x] **U02-T11 · Imagen del core multi-stage y no root**
  Dockerfile: build con Gradle, ejecución en imagen mínima (distroless de Java 21), usuario no
  root, `/plugins` y `/config` como volúmenes.
  **Criterio:** `docker build -t synkra-core:2.1.0 core/` y `docker inspect -f '{{.Config.User}}' synkra-core:2.1.0` no vacío ni `root`/`0`. · H-28

- [x] **U02-T12 · Prueba de humo del contenedor**
  Con `docker run`, montando `/plugins` y `/config`: con `kyc-pe 1.3.0` arranca; con `kyc-pe 2.0.0` termina.
  **Criterio:** `core/smoke.sh` termina con código 0 (comprueba `curl /plugins` en el primer caso y código de salida ≠ 0 en el segundo). · H-21

- [x] **U02-T13 · Resumen de la unidad**
  **Criterio:** `aidlc-docs/construction/U02-core-plugins/code/summary.md` contiene la salida real de `./gradlew :core:check` y de `core/smoke.sh`. · H-21

## Trazabilidad

| Historia | Tareas |
|---|---|
| H-21 El core verifica al arrancar | T01, T02, T04, T05, T07, T08, T12, T13 |
| H-22 Un plugin que falla no deja el core a medias | T03, T06 |
| H-23 Plugins de Colombia y Perú | T09, T10 |
| H-28 (contribuye) Imágenes no root | T11 |
| H-10 (contribuye) La suite bloquea el build | T05 |

**Terminada cuando:** `./gradlew :core:check :plugins:kyc-co:check :plugins:kyc-pe:check` pasa y
`core/smoke.sh` termina con código 0.

## Notas

- Ninguna tarea aplica cambios a un clúster: el humo se hace con `docker run` local (AUTONOMIA-01).
