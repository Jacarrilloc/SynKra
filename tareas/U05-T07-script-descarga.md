# U05-T07: script `init-descarga/descargar.sh`

**Unidad:** U05: `Pod del país`
**Historias que implementa:** H-15
**Depende de:** U05-T03 (contrato del Deployment) y U04-T10 (`GET /api/artefactos/{sha}`)

---

## Alcance

**Dentro** (una línea, concreta):

> Un script POSIX `init-descarga/descargar.sh` que lee `/config/artefactos.txt`, descarga cada JAR por su hash desde `$SYNKRA_URL/api/artefactos/<sha256>`, comprueba el sha256 de lo descargado (C-64), reintenta 5 veces con espera creciente, escribe solo en `/plugins` y termina con código distinto de 0 y un mensaje claro ante cualquier fallo; más su prueba `init-descarga/test.sh` con un servidor HTTP local.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- El Dockerfile y la imagen (U05-T08).
- Cambios en `backend/` (ni el endpoint de artefactos ni el constructor).
- Autenticación contra el catálogo, TLS o caché de descargas.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U05-pod-pais-code-generation-plan.md` (U05-T07)
- `aidlc-docs/inception/user-stories/stories.md` (H-15)
- `aidlc-docs/construction/U08-instalacion-limite/infrastructure-design/infrastructure-design.md` (init container y URL del catálogo)
- `backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java` (formato de `artefactos.txt`: `<sha256> <nombre>-<version>.jar` por línea; `COMANDO_INIT`, `VAR_URL`, rutas `/plugins` y `/config`)
- `backend/src/main/java/io/synkra/catalogo/RutasArtefactos.java` (`GET /api/artefactos/{sha}` y su cabecera `X-Synkra-Sha256`)
- `CANDIDATAS.md` (C-64 y C-72)

---

## Decisiones del orquestador

1. **Contrato con el Deployment (C-72).** La imagen de U05-T08 instalará el script en `/usr/local/bin/descargar.sh`; aquí el script vive en `init-descarga/descargar.sh`. Las rutas se pueden cambiar solo con variables para las pruebas: `SYNKRA_CONFIG` (por defecto `/config`) y `SYNKRA_PLUGINS` (por defecto `/plugins`). `SYNKRA_URL` es obligatoria: si falta, sale con error.
2. **Shell.** `#!/bin/sh` con `set -eu`, sin bashismos (debe correr en la imagen mínima de T08). Herramientas: `curl` y `sha256sum` (coreutils). Se comprueba con `sh -n` y, si está instalado, `shellcheck -s sh`.
3. **Validación de cada línea antes de descargar.** Formato exacto `^[0-9a-f]{64} [A-Za-z0-9][A-Za-z0-9._+-]*\.jar$`. Una línea que no cumple (por ejemplo, con `../` o `/`) se rechaza y el script termina sin descargar nada. Un archivo vacío deja `/plugins` vacío y termina con 0.
4. **Descarga atómica.** Se descarga a un archivo temporal **dentro** de `/plugins` (por ejemplo `.<nombre>.parcial`), se comprueba el sha256 calculado con `sha256sum` contra el de la lista (no contra la cabecera), y solo entonces se renombra. Si el hash no cuadra, se borra el temporal, se registra `hash distinto para <nombre>: esperado <x>, recibido <y>` y se sale con error. Si un JAR falla, se borran también los que ya se habían descargado en esta corrida: `/plugins` queda vacío.
5. **Reintentos.** 5 intentos por archivo ante error de red o HTTP 5xx, con espera creciente (1, 2, 4, 8 s); un 404 no se reintenta. La espera base se puede bajar con `SYNKRA_ESPERA_BASE` para las pruebas. Tras el quinto fallo: `catálogo no responde tras 5 intentos: <url>`.
6. **curl.** `--fail`, `--silent --show-error`, `--max-time` con valor en una variable con nombre, sin `-k`.

---

## Criterios de aceptación

- [ ] **CA-1**: los cuatro casos del plan, más los bordes
  ```bash
  sh -n init-descarga/descargar.sh && echo sintaxis-ok
  command -v shellcheck >/dev/null && shellcheck -s sh init-descarga/descargar.sh; echo "shellcheck=$?"
  init-descarga/test.sh; echo "salida=$?"
  ```
  Esperado: `sintaxis-ok`, `shellcheck=0` (o shellcheck ausente, dicho en la bitácora) y `salida=0`. `test.sh` levanta un servidor HTTP local (por ejemplo `python3 -m http.server` sobre un directorio con `api/artefactos/<sha>`, o un pequeño servidor en Python que pueda devolver 500), usa directorios temporales para `/config` y `/plugins`, y comprueba, **listando de vuelta el directorio de plugins** en cada caso:
  - hash correcto: el JAR queda en plugins con su nombre, y su sha256 coincide;
  - hash alterado (el servidor sirve otro contenido): código ≠ 0, plugins vacío, sin temporales, mensaje con «hash distinto»;
  - catálogo caído (puerto cerrado): exactamente 5 intentos (contados en el log del script) y código ≠ 0;
  - línea con `../` o `/` en el nombre: rechazada, código ≠ 0, ninguna petición al servidor;
  - dos JAR, el segundo alterado: plugins vacío al final;
  - `artefactos.txt` vacío: código 0, plugins vacío;
  - sin `SYNKRA_URL`: código ≠ 0.
  Cada caso imprime `ok <caso>` o `FALLA <caso>`, y `test.sh` termina con ≠ 0 si alguno falla.

- [ ] **CA-2**: el rojo muerde
  En una copia, con la comprobación de sha256 quitada del script, `test.sh` falla al menos en «hash alterado». Se pega la salida.

- [ ] **CA-3**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <base-del-despacho>...HEAD
  ```
  Esperado: `init-descarga/descargar.sh`, `init-descarga/test.sh`, la bitácora y `revisiones/U05-T07/`. Los dos scripts con permiso de ejecución en git (`git ls-files -s init-descarga` muestra `100755`). Se anota el sha.

---

## Plan de pruebas

- `test.sh` en `sh` o `bash`, sin red externa: todo contra `127.0.0.1`. Debe limpiar sus temporales y matar su servidor con un `trap`.

**Rojo primero:** `test.sh` antes que `descargar.sh` (con un script vacío, debe fallar en todos los casos).
