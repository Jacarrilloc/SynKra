# U05-T08: imagen del init container

**Unidad:** U05: `Pod del país`
**Historias que implementa:** H-28 (contribuye), H-15
**Depende de:** U05-T07

---

## Alcance

**Dentro** (una línea, concreta):

> Un `init-descarga/Dockerfile` que produce la imagen `synkra-init-descarga:1.0.0`: base mínima con `curl`, `sha256sum` y `sh`, el script de U05-T07 instalado en `/usr/local/bin/descargar.sh` (C-72), usuario numérico no root `10001:10001`, y sin nada más.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cambiar `descargar.sh` o `test.sh` (si la imagen necesita un cambio en el script, se reporta).
- Subir la imagen a un registro, o cargarla en `kind` (U08).
- Cambios en `backend/`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U05-pod-pais-code-generation-plan.md` (U05-T08)
- `aidlc-docs/construction/U08-instalacion-limite/infrastructure-design/infrastructure-design.md` (registro local `localhost:5001`, recursos del init)
- `backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java` (`COMANDO_INIT`, `UID_CONTENEDORES`, `GID_CONTENEDORES`, `IMAGEN_INIT_POR_DEFECTO`)
- `backend/Dockerfile` y `backend/.dockerignore` (U03-T06: cómo se resolvió la CA del proxy en este entorno)
- `init-descarga/descargar.sh` e `init-descarga/test.sh` (U05-T07) y `CANDIDATAS.md` (C-72)

---

## Decisiones del orquestador

1. **Base.** Una imagen mínima con versión fija (por ejemplo `alpine:3.20` o `busybox` con curl estático; la bitácora dice cuál y por qué). Nada de `:latest`.
2. **Contrato con el Deployment.** `COPY --chmod=0555 descargar.sh /usr/local/bin/descargar.sh`; `USER 10001:10001` numérico (el Deployment fija `runAsUser`/`runAsGroup` 10001 y `runAsNonRoot`); `/plugins` y `/config` como puntos de montaje que el usuario 10001 puede escribir y leer respectivamente cuando Kubernetes monta los volúmenes. Sin `ENTRYPOINT` de shell interactivo: `ENTRYPOINT ["/usr/local/bin/descargar.sh"]`.
3. **`.dockerignore`.** El contexto de build solo incluye `descargar.sh`.
4. **Docker en este entorno.** Si `docker info` falla, `dockerd` se arranca a mano en segundo plano (lo dice la bitácora). Si el build necesita descargar paquetes y la CA del proxy no es de confianza dentro del contenedor, se elige una base que ya traiga `curl` en lugar de desactivar TLS.

---

## Criterios de aceptación

- [ ] **CA-1**: la imagen se construye y es no root (lectura de vuelta con `docker inspect`)
  ```bash
  docker build -t synkra-init-descarga:1.0.0 init-descarga/
  docker inspect -f '{{.Config.User}} {{json .Config.Entrypoint}}' synkra-init-descarga:1.0.0
  docker run --rm --entrypoint id synkra-init-descarga:1.0.0
  docker run --rm --entrypoint sh synkra-init-descarga:1.0.0 -c 'command -v curl sha256sum && ls -l /usr/local/bin/descargar.sh'
  ```
  Esperado: `10001:10001 ["/usr/local/bin/descargar.sh"]`; `uid=10001 gid=10001`; las dos herramientas encontradas y el script ejecutable y no escribible por el usuario.

- [ ] **CA-2**: el script corre dentro de la imagen contra un servidor local
  ```bash
  # el codificador pega cómo levanta el servidor de prueba en el host y monta config y plugins
  docker run --rm --network host -e SYNKRA_URL=http://127.0.0.1:<puerto> -v "$D/config:/config:ro" -v "$D/plugins:/plugins" synkra-init-descarga:1.0.0; echo "salida=$?"
  ls -l "$D/plugins"
  ```
  Esperado: con un hash correcto, `salida=0` y el JAR en `$D/plugins`; con un hash alterado, `salida` distinta de 0 y `$D/plugins` vacío. Se pegan las dos corridas.

- [ ] **CA-3**: la prueba del script sigue pasando
  ```bash
  init-descarga/test.sh; echo "salida=$?"
  ```
  Esperado: `salida=0`.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <base-del-despacho>...HEAD
  ```
  Esperado: `init-descarga/Dockerfile`, `init-descarga/.dockerignore`, la bitácora y `revisiones/U05-T08/`. Se anota el sha.

---

## Notas

- Ninguna prueba usa un clúster. `docker run` local no es un clúster.
