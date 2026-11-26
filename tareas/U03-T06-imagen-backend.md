# U03-T06: Imagen del backend multi-stage y no root

**Unidad:** U03: `Plataforma del backend`
**Historias que implementa:** H-28
**Depende de:** U03-T04

---

## Alcance

**Dentro** (una línea, concreta):

> `backend/Dockerfile` multi-stage que toma el JAR de `./gradlew :backend:bootJar`, lo separa en capas con el modo `tools` de Spring Boot en una etapa y arma la imagen final sobre un JRE 21 con un usuario sin root (UID fijo distinto de 0), `/data` como volumen escribible por ese usuario y el puerto 8080; más `backend/.dockerignore`.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Compilar con Gradle **dentro** de `docker build`: en este entorno los contenedores no confían en la CA del proxy y no pueden bajar dependencias. El JAR se compila en el host y la imagen solo lo empaqueta (ver Notas).
- Imágenes del front, del core o del init container (U09, U02, U05).
- Manifiestos de Kubernetes, `securityContext`, sondas o Helm (U08).
- Publicar la imagen en un registro, firmarla o escanearla.
- Cambios en `src/main`, `src/test`, `application.properties` o `build.gradle.kts`. Si hace falta uno (por ejemplo, para el nombre del JAR), se registra en la bitácora y se para.
- C-11 (literal `synkra.db` en `SaludBaseDeDatos`): queda para la siguiente tarea que toque la salud.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U03-plataforma-backend-code-generation-plan.md`
- `aidlc-docs/inception/user-stories/stories.md` (H-28)
- `backend/build.gradle.kts` (versión `0.1.0`; el JAR ejecutable es `backend/build/libs/backend-0.1.0.jar` y el `-plain.jar` no sirve)
- `backend/src/main/resources/application.properties` (`SYNKRA_DATA`, por defecto `/data`; salud con componentes)

---

## Criterios de aceptación

Antes de los criterios, en este contenedor: `docker info` debe responder. Si no, se arranca el
daemon con `dockerd > /tmp/dockerd.log 2>&1 &` y se espera a que `docker info` responda.

- [ ] **CA-1**: la imagen se construye desde el JAR recién compilado
  ```bash
  ./gradlew :backend:bootJar --rerun && docker build -t synkra-backend backend/
  ```
  Esperado: las dos partes terminan bien (`BUILD SUCCESSFUL` y la imagen etiquetada `synkra-backend`).

- [ ] **CA-2**: la imagen corre sin root (H-28)
  ```bash
  docker inspect -f '{{.Config.User}}' synkra-backend
  docker run --rm --entrypoint id synkra-backend
  ```
  Esperado: el primero no vacío ni `root` ni `0` (y si trae grupo, tampoco `0`); el segundo muestra
  un `uid` distinto de 0.

- [ ] **CA-3**: es multi-stage de verdad y la imagen final no trae herramientas de construcción
  ```bash
  grep -c '^FROM ' backend/Dockerfile
  docker run --rm --entrypoint sh synkra-backend -c 'command -v javac || echo sin-javac; find / -xdev -name "backend-*.jar" 2>/dev/null | wc -l'
  docker inspect -f '{{json .Config.Volumes}} {{json .Config.ExposedPorts}}' synkra-backend
  ```
  Esperado: `FROM` ≥ 2; `sin-javac` y `0` (el JAR gordo se quedó en la primera etapa: la final solo
  tiene las capas extraídas); el volumen `/data` y el puerto `8080/tcp` declarados.

- [ ] **CA-4**: el contenedor arranca, escribe su base en el volumen y responde la salud
  ```bash
  docker volume create synkra-datos-prueba
  docker run -d --name synkra-prueba -p 18080:8080 -v synkra-datos-prueba:/data synkra-backend
  for i in $(seq 1 60); do curl -fs localhost:18080/actuator/health && break; sleep 1; done; echo
  docker exec synkra-prueba sh -c 'ls -ln /data'
  docker rm -f synkra-prueba && docker volume rm synkra-datos-prueba
  ```
  Esperado: la salud responde `{"status":"UP",...}` con el componente `db` en `UP`; `/data/synkra.db`
  existe y su dueño es el UID del CA-2.

- [ ] **CA-5**: entrada hostil: sin permiso de escritura en `/data`, la salud lo dice
  ```bash
  docker run -d --name synkra-ro -p 18081:8080 --read-only --tmpfs /tmp -v "$(mktemp -d)":/data:ro synkra-backend
  sleep 20; docker logs synkra-ro 2>&1 | tail -20; curl -s localhost:18081/actuator/health; echo
  docker rm -f synkra-ro
  ```
  Esperado: no queda un contenedor que diga `UP` con una base que no puede escribir. Vale cualquiera de
  los dos: el proceso termina con un error legible en el log (Flyway o SQLite nombran el problema), o la
  salud responde `DOWN` con el componente `db` en `DOWN`. Se pega en la bitácora cuál de los dos pasó.

- [ ] **CA-6**: el contexto de la imagen no arrastra lo que no debe
  ```bash
  cat backend/.dockerignore
  docker build --no-cache -t synkra-backend backend/ 2>&1 | grep -i 'transferring context'
  ```
  Esperado: `.dockerignore` excluye al menos `src/`, `build/` salvo `build/libs/backend-*.jar` sin
  `-plain`, `*.db` y `.gradle/`; el contexto transferido es del orden del JAR (unos MB), no del árbol
  completo.

- [ ] **CA-7**: el backend sigue pasando, sin caché
  ```bash
  ./gradlew :backend:check --rerun
  ```
  Esperado: `BUILD SUCCESSFUL`.

---

## Plan de pruebas

- Esta tarea no agrega pruebas de JUnit: sus pruebas son los comandos de los criterios, y cada salida
  se pega completa en la bitácora.
- Rojo primero: antes de escribir el `Dockerfile`, CA-1 falla (no hay `Dockerfile`); se registra. Luego,
  con una versión que corre como root (sin `USER`), CA-2 falla; se registra y se corrige. Así queda el
  par falla-pasa del criterio que importa (H-28).

---

## Notas

- Base: `eclipse-temurin:21-jre-alpine` para la imagen final y `eclipse-temurin:21-jre-alpine` (o
  `21-jdk-alpine`) para la etapa de capas; ya se pudo bajar en este contenedor. Se fija la etiqueta,
  no `latest`.
- Capas: `java -Djarmode=tools -jar app.jar extract --layers --launcher --destination /extraido` en la
  primera etapa, y un `COPY` por capa en la final (`dependencies`, `spring-boot-loader`,
  `snapshot-dependencies`, `application`) para que el caché de Docker funcione.
- Usuario: un UID y GID fijos (por ejemplo `10001`), creados con `addgroup`/`adduser -S`; `USER 10001:10001`
  en número, para que Kubernetes pueda comprobar `runAsNonRoot` sin resolver nombres (U08). `/data`
  se crea en la imagen con ese dueño antes de declarar el `VOLUME`.
- `ENTRYPOINT` en forma exec (`["java", ...]`), sin `sh -c`, para que el proceso reciba las señales.
- Puede que `docker build` en este contenedor no tenga red hacia el proxy: la imagen no descarga nada
  en `RUN`, solo copia.
