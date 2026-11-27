# U02-T02: Proyecto `core/` (Spring Boot) con la versión compilada

**Unidad:** U02: `Core, contrato y plugins de país`
**Historias que implementa:** H-21
**Depende de:** U02-T01

---

## Alcance

**Dentro** (una línea, concreta):

> El subproyecto `core/` (Spring Boot 3.5, Java 21, paquete `io.synkra.core`) que depende de `:plugin-api`, arranca vacío y lleva la versión `2.1.0` escrita en tiempo de build en `synkra-core.properties` dentro del JAR (D4), expuesta por un bean `VersionCore`.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cargar plugins (`CargadorPlugins`, U02-T03) y el arranque verificado (U02-T04).
- Endpoints `GET /plugins` y `POST /kyc/{plugin}` (U02-T07). Solo actuator viene de serie.
- La imagen Docker del core (U02-T11).
- Leer la versión de variables de entorno o propiedades externas: D4 lo prohíbe.
- Cambios en `backend/`, `verificador/` o `plugin-api/`. Fuera de `core/`, solo la línea `include` de `settings.gradle.kts`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U02-core-plugins-code-generation-plan.md` (U02-T02)
- `aidlc-docs/inception/application-design/application-design.md` (D4)
- `backend/build.gradle.kts` (patrón de Spring Boot del proyecto: versión, BOM, repositorios)
- `tareas/U02-T01-plugin-api.md`

---

## Decisiones del orquestador

1. **Origen de la versión.** `version = "2.1.0"` en `core/build.gradle.kts`, y un `processResources` que la escribe en `synkra-core.properties` (`version=2.1.0`). Se lee con `getResourceAsStream`, no con `@Value` ni `Environment`, para que ninguna variable de entorno ni argumento pueda cambiarla.
2. **`VersionCore`.** Devuelve la versión como `io.synkra.verificador.Version` si `core` depende de `:verificador`; si esa dependencia aún no hace falta, como `String` validado con el mismo formato. Lo que se elija se dice en la bitácora (U02-T04 la necesitará para verificar).
3. **Puerto.** El core escucha en `8080` por defecto, igual que el backend, pero las pruebas usan `RANDOM_PORT` y los criterios el `18081`.

---

## Criterios de aceptación

- [ ] **CA-1**: el JAR lleva la versión compilada (lectura de vuelta del artefacto)
  ```bash
  ./gradlew :core:bootJar --rerun-tasks
  unzip -p core/build/libs/core-*.jar BOOT-INF/classes/synkra-core.properties
  ```
  Esperado: `BUILD SUCCESSFUL` y la línea `version=2.1.0`.

- [ ] **CA-2**: la versión no se puede cambiar desde fuera
  ```bash
  ./gradlew :core:test --rerun --tests '*VersionCoreTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' core/build/test-results/test/TEST-*VersionCoreTest.xml
  ```
  Esperado: `BUILD SUCCESSFUL`, `tests` ≥ 3, sin saltos ni fallos. Las pruebas afirman que `VersionCore` da `2.1.0` y que sigue dando `2.1.0` con la propiedad `version=9.9.9` y con una variable de entorno `SYNKRA_CORE_VERSION=9.9.9` en el contexto de prueba.

- [ ] **CA-3**: el core arranca y responde salud
  ```bash
  java -jar core/build/libs/core-*.jar --server.port=18081 & PID=$!
  timeout 90 bash -c 'until curl -sf localhost:18081/actuator/health >/dev/null; do sleep 1; done'
  curl -s localhost:18081/actuator/health | python3 -I -c "import json,sys; print(json.load(sys.stdin)['status'])"
  kill $PID; wait $PID 2>/dev/null
  ```
  Esperado: `UP`.

- [ ] **CA-4**: el resto del build sigue pasando y el diff queda acotado
  ```bash
  ./gradlew :verificador:check :plugin-api:check :backend:check :core:check --rerun
  git diff --stat origin/main...HEAD
  ```
  Esperado: `BUILD SUCCESSFUL`; el diff toca `core/**`, una línea de `settings.gradle.kts`, la bitácora y `revisiones/U02-T02/`.

---

## Plan de pruebas

- `VersionCoreTest` (`@SpringBootTest`) y una prueba de humo del contexto.

**Rojo primero:** las pruebas antes que `VersionCore`, en una copia o con las pruebas solas, sin `git stash` en el worktree.
