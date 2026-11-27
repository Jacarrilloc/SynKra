# U02-T11: imagen del core, no root

**Unidad:** U02: `Core, contrato y plugins de país`
**Historias que implementa:** H-28 (contribuye)
**Depende de:** U02-T07 (el core completo); va después de U02-T10 en el orden de olas

---

## Alcance

**Dentro** (una línea, concreta):

> Un `core/Dockerfile` y su `core/.dockerignore` que producen `synkra-core:2.1.0`: JRE 21 mínima, el bootJar del core por capas, usuario numérico `10001:10001` (C-72), `/plugins` y `/config` como volúmenes, puerto 8080 y la readiness de Spring en `/actuator/health/readiness`.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Meter plugins en la imagen: llegan por el init container (U05).
- La prueba de humo con plugins (U02-T12).
- Subir la imagen a un registro o cargarla en `kind` (U08).
- Cambios en `core/src/**`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U02-core-plugins-code-generation-plan.md` (U02-T11)
- `backend/Dockerfile` y `backend/.dockerignore` (U03-T06: el patrón aceptado; el bootJar se compila en el host porque los contenedores de este entorno no confían en la CA del proxy)
- `backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java` (`UID_CONTENEDORES`, `GID_CONTENEDORES`, `PUERTO`, `RUTA_READINESS`, rutas `/plugins` y `/config`)
- `CANDIDATAS.md` (C-72)

---

## Decisiones del orquestador

1. **Mismo patrón que el backend.** El bootJar se construye en el host (`./gradlew :core:bootJar`) y el Dockerfile solo lo empaqueta por capas, con la misma base `eclipse-temurin:21-jre-alpine` con versión fija. El plan habla de *distroless*; se acepta la base del backend para no traer otra imagen al entorno, y la bitácora lo dice. Si hay más de un `core-*.jar`, el build falla con un mensaje claro, como el del backend.
2. **Usuario.** `USER 10001:10001` numérico, creado en la imagen; `/plugins` y `/config` existen y son del usuario 10001 (Kubernetes monta encima, pero `docker run` sin volúmenes también debe arrancar).
3. **Arranque.** `ENTRYPOINT` en forma exec con el `JarLauncher`; sin argumentos de rutas: el core usa `/config` y `/plugins` por defecto (decisión 1 de U02-T04).

---

## Criterios de aceptación

- [ ] **CA-1**: imagen construida, no root (lectura de vuelta con `docker inspect` y `docker run`)
  ```bash
  ./gradlew :core:bootJar
  docker build -t synkra-core:2.1.0 core/
  docker inspect -f '{{.Config.User}} {{json .Config.Volumes}} {{json .Config.ExposedPorts}}' synkra-core:2.1.0
  docker run --rm --entrypoint id synkra-core:2.1.0
  ```
  Esperado: `10001:10001`, volúmenes `/plugins` y `/config`, puerto `8080/tcp`; `uid=10001 gid=10001`.

- [ ] **CA-2**: el contenedor arranca sin plugins y queda listo
  ```bash
  docker run -d --name core-t11 -p 18084:8080 synkra-core:2.1.0
  # espera hasta 60 s:
  curl -s -o /dev/null -w '%{http_code}\n' localhost:18084/actuator/health/readiness
  curl -s localhost:18084/plugins
  docker logs core-t11 | grep -c 'Plugins cargados'
  docker rm -f core-t11
  ```
  Esperado: `200`, `[]` y al menos `1`.

- [ ] **CA-3**: el resto del build sigue pasando
  ```bash
  ./gradlew :core:test --rerun :core:check
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <base-del-despacho>...HEAD
  ```
  Esperado: `core/Dockerfile`, `core/.dockerignore`, la bitácora y `revisiones/U02-T11/`. Se anota el sha.

---

## Notas

- Si `docker info` falla en el contenedor en la nube, `dockerd` se arranca a mano en segundo plano y la bitácora lo dice. `docker run` local no es un clúster.
