# Ronda 1 — U05-T08

VEREDICTO: VERDE

Todos los criterios los comprobé yo, con mi propia construcción de la imagen (`docker build --no-cache`, aarch64) y con mi propio servidor de prueba. Pasan los cuatro. No queda ningún hallazgo ROJO ni NARANJA. Hay cuatro AMARILLOS, que no bloquean.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Imagen construida y no root | `docker build --no-cache -t synkra-init-descarga:1.0.0 init-descarga/` (build=0); `docker inspect -f '{{.Config.User}} {{json .Config.Entrypoint}} cmd={{json .Config.Cmd}}'`; `docker run --rm --entrypoint id`; `docker run --rm --entrypoint sh … -c 'command -v curl sha256sum; command -v sha256sum; ls -l …; test -w …'` | pasa: `10001:10001 ["/usr/local/bin/descargar.sh"] cmd=null`; `uid=10001(synkra) gid=10001(synkra)`; `/usr/bin/curl` y `/usr/bin/sha256sum`; `-r-xr-xr-x root root … descargar.sh`; el usuario 10001 no puede escribir el script; el sha256 del script dentro de la imagen es igual al del árbol (`a323a6dc…`); `/plugins` es de 10001 y se puede escribir; `/config` es root 0755 |
| 1b | `.dockerignore`: el contexto solo lleva `descargar.sh` | build desechable desde stdin (`COPY . /ctx; RUN find /ctx`) sobre `init-descarga/`; la imagen temporal ya está borrada | pasa: solo `/ctx/descargar.sh` |
| 2 | El script corre en la imagen contra un servidor local | mi propio arnés `scratchpad/bin/ca2.sh` (servidor `python3 -I` en 127.0.0.1 con puerto efímero, espera con `timeout 10`, JAR aleatorio de 5000 B, `--network host`, `/config:ro`) | pasa: con el hash correcto, `salida=0` y el JAR queda en plugins (10001:10001, 0644, sha igual). Con el hash alterado (se sirve el contenido real bajo el hash falso), `salida=6`, `hash distinto` y plugins vacío (`total 0`). Corrida extra con `--read-only --cap-drop ALL --security-opt no-new-privileges --user 10001:10001`: `salida=0` |
| 3 | `test.sh` sigue pasando | `init-descarga/test.sh; echo salida=$?` | pasa: `todos los casos ok`, `salida=0`. Tarda 0,84 s, pero la suite no depende de estado externo (curl falso, espera 0), así que el tiempo es coherente |
| 4 | Diff acotado | `git rev-parse --short HEAD` → `830997c`; `git diff --stat 696eecc...HEAD`; `git diff --name-only … -- init-descarga/descargar.sh init-descarga/test.sh backend/` | pasa: 9 archivos (Dockerfile, .dockerignore, bitácora y evidencia). No toca script, `test.sh` ni `backend/`. No hay desborde |

También comprobé el contrato con `ConstructorObjetosPais` (C-72): `withCommand("/usr/local/bin/descargar.sh")`, `runAsUser`/`runAsGroup` 10001 y `runAsNonRoot` coinciden con el `USER 10001:10001` numérico de la imagen. Con `ENTRYPOINT` se anula el `CMD ["/bin/sh"]` heredado de alpine (`cmd=null`). La base es `alpine:3.20` con versión fija (3.20.10) y TLS no se desactivó.

## Hallazgos
### F-01 · AMARILLO · init-descarga/Dockerfile:2-3 · Base y paquete sin fijar del todo
`FROM alpine:3.20` no lleva digest (hoy resuelve a `sha256:d9e853e8…`) y `apk add curl` no fija versión (hoy da 8.14.1-r2). Cumple la decisión 1, que pide versión fija y no `:latest`, pero dos builds de la misma etiqueta `1.0.0` pueden salir distintas.

### F-02 · AMARILLO · init-descarga/Dockerfile:3 · «Sin nada más» es relativo
La imagen conserva `apk-tools`, `ssl_client`, `scanelf` y los applets de busybox (`wget`, `nc`…). La tarea permite alpine, así que no bloquea. Quitar `apk` reduciría la superficie.

### F-03 · AMARILLO · init-descarga/Dockerfile:4-7 · UID/GID 10001 repetido sin enlace con el backend
El valor aparece cuatro veces (`addgroup`, `adduser`, `chown`, `USER`) y además está en `UID_CONTENEDORES`/`GID_CONTENEDORES` de Java. Ninguna prueba ata el Dockerfile a la constante. Es el valor que fija el contrato y no es visible para el usuario, por eso no es NARANJA. Un `ARG` único y una comprobación cruzada evitarían que se desincronicen.

### F-04 · AMARILLO · Evidencia del codificador · Detalles menores
- `ca2.sh` espera al servidor con `while … sleep 0.1` sin `timeout`: si el servidor no arranca, la espera no termina nunca. Además llama a `python3` sin `-I`.
- `ca4.txt` y el anexo de CA-4 muestran el stat de 111939a (8 archivos, bitácora con 41 líneas), no el de HEAD 830997c (9 archivos, 54 líneas). Está rotulado con su sha y es correcto para ese sha, así que no es evidencia falsa. El stat de HEAD que da CA-4 lo corrí yo.
- Ojo de especificación: en busybox ash, `command -v curl sha256sum` imprime solo la primera ruta. El codificador lo detectó y lo comprobó aparte; yo también.

## Tareas candidatas (defectos reales fuera de alcance)
- Ya está registrada como C-97 y la confirmé: el arnés tuvo que hacer `chmod 777` en el directorio de plugins del host para que 10001 pudiera escribir. En el pod real hace falta `fsGroup: 10001` o algo equivalente en el `emptyDir`.
- Ninguna nueva.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/build.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca2-revisor.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca3.txt
- Arnés propio: /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/bin/ca2.sh y srv.py

## Estado al terminar
- El worktree /home/julian/Escritorio/Proyectos/wt-m6r-U05-T08 está limpio (`git status --short` vacío).
- El temporal de mktemp está borrado (comprobado).
- No quedó ningún contenedor mío: todos los que corrí llevaban `--rm`, y los que lista `docker ps -a` son de otros proyectos.
- La imagen `synkra-init-descarga:1.0.0` queda en el daemon a propósito, porque es el artefacto de la tarea.
- No usé kubectl, kind ni registros.
- No escribí `revisiones/U05-T08/ronda-1.md` porque mis herramientas son de solo lectura. El contenido de este informe es el que va en ese archivo.

```
VEREDICTO: VERDE
AMARILLO|init-descarga/Dockerfile:2-3|Base y paquete sin fijar del todo (sin digest, curl sin versión)
AMARILLO|init-descarga/Dockerfile:3|apk-tools y applets de busybox siguen en la imagen
AMARILLO|init-descarga/Dockerfile:4-7|UID/GID 10001 repetido sin enlace con UID_CONTENEDORES
AMARILLO|bitacoras/U05-T08-evidencia/ca2.sh y ca4.txt|Espera sin timeout, python sin -I, stat de CA-4 del sha previo
INFORME: revisiones/U05-T08/ronda-1.md
```
