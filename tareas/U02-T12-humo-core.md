# U02-T12: prueba de humo del contenedor del core

**Unidad:** U02: `Core, contrato y plugins de país`
**Historias que implementa:** H-21
**Depende de:** U02-T10 (JAR de `kyc-pe`) y U02-T11 (imagen del core)

---

## Alcance

**Dentro** (una línea, concreta):

> Un script `core/smoke.sh` que, con `docker run` local y montando `/plugins` y `/config`, comprueba que la imagen `synkra-core:2.1.0` arranca con `kyc-pe 1.3.0` (y `GET /plugins` lo lista) y termina con código distinto de 0 con `kyc-pe 2.0.0` (`VERSION_INCOMPATIBLE`).

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cambiar el core, los plugins o el Dockerfile. Si el humo falla, se reporta.
- Cualquier clúster, `kind` o `kubectl` (AUTONOMIA-01).
- El init container: aquí `/plugins` se monta desde el host.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U02-core-plugins-code-generation-plan.md` (U02-T12)
- `core/Dockerfile` (U02-T11), `plugins/kyc-pe/` (U02-T10)
- `backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java` (formato real de `plugins-activos.yaml` después de C-81: lista en la raíz)
- `CANDIDATAS.md` (C-81 y C-84)

---

## Decisiones del orquestador

1. **Mismo formato que en el pod (C-81).** El `plugins-activos.yaml` que monta el humo es el que generaría `construir`: lista en la raíz de `{nombre, version}`.
2. **Script.** `bash` con `set -euo pipefail`, contenedores con nombre único, `trap` que los borra y borra sus temporales. Espera de readiness con tope (60 s) en una variable con nombre. Imprime `ok <caso>` o `FALLA <caso>` y termina con ≠ 0 si un caso falla.
3. **Los dos casos se leen de vuelta.** Caso 1: readiness `200` y `GET /plugins` con `kyc-pe` y `1.3.0`. Caso 2: `docker wait` devuelve un código distinto de 0 y `docker logs` contiene `VERSION_INCOMPATIBLE plugin=kyc-pe`.
4. **Permisos.** Los directorios montados se preparan legibles por el usuario 10001 del contenedor.

---

## Criterios de aceptación

- [ ] **CA-1**: el humo pasa
  ```bash
  ./gradlew :core:bootJar :plugins:kyc-pe:assemble
  docker build -t synkra-core:2.1.0 core/
  core/smoke.sh; echo "salida=$?"
  docker ps -a --format '{{.Names}}' | grep -c smoke
  ```
  Esperado: `ok` en los dos casos, `salida=0`, y `0` contenedores del humo restantes.

- [ ] **CA-2**: el humo muerde
  En una copia, con `plugins-activos.yaml` del caso 2 apuntando a `kyc-pe 1.3.0` (el caso 2 deja de ser incompatible), `core/smoke.sh` termina con ≠ 0 y dice `FALLA` en el caso 2. Se pega la salida.

- [ ] **CA-3**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <base-del-despacho>...HEAD
  git ls-files -s core/smoke.sh
  ```
  Esperado: `core/smoke.sh` (`100755`), la bitácora y `revisiones/U02-T12/`. Se anota el sha.
