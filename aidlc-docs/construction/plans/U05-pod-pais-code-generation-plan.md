# Plan de tareas: U05 Pod del país

> **Parte 1 de Code Generation.** No se ejecuta en el módulo 5; la Parte 2 requiere aprobación y aviso previo a Julian. Sin Functional Design (adaptador sin reglas de negocio; los objetos están fijados en el
Infrastructure Design de U08).

Raíz del proyecto. Añade el paquete `kubernetes` a `backend/` (con `kubernetes.api` público) y
crea `init-descarga/`. Resumen en `aidlc-docs/construction/U05-pod-pais/code/`.

## Contexto

- **Historia dueña:** H-15. **Contribuye a:** H-12, H-13 (segunda capa), H-14, H-16, H-19, H-28.
- **Depende de:** U03. **Expone:** `AdaptadorKubernetes` (`construir`, `aplicar`, `estado`,
  `contextoFallo`, `leerPais`) y la imagen `synkra-init-descarga`.
- **Pruebas:** servidor simulado de Fabric8 (`KubernetesMockServer`) y un servidor HTTP local para el
  script; **ninguna tarea aplica nada a un clúster real** (AUTONOMIA-01).

## Tareas

- [x] **U05-T01 · Cliente Fabric8 configurado con la cuenta del pod**
  **Criterio:** `./gradlew :backend:test --tests '*ClienteKubernetesTest'` pasa contra el servidor simulado. · H-12

- [x] **U05-T02 · Guardia de namespace (segunda capa, D13)**
  Todo método rechaza un namespace que no sea `synkra-<país>` antes de llamar a la API.
  **Criterio:** `./gradlew :backend:test --tests '*GuardiaNamespaceTest'` pasa con `default`, `kube-system` y `synkra` rechazados y **cero** peticiones registradas en el servidor simulado. · H-13

- [x] **U05-T03 · `construir`: objetos del país**
  Deployment (imagen por digest, init container, `emptyDir` `/plugins`, ConfigMap en `/config`, `automountServiceAccountToken: false`, recursos de U08, readiness del core), ConfigMap (`plugins-activos.yaml` y `artefactos.txt` con nombre y sha256) y Service.
  **Criterio:** `./gradlew :backend:test --tests '*ConstruirObjetosPaisTest'` pasa comparando con un YAML esperado en `src/test/resources`. · H-12, H-15

- [x] **U05-T04 · `aplicar` con server-side apply**
  **Criterio:** `./gradlew :backend:test --tests '*AplicarTest'` pasa: tras `aplicar`, el servidor simulado tiene los tres objetos en `synkra-pe`, con el *field manager* `synkra`. · H-12

- [x] **U05-T05 · `estado` y `contextoFallo`**
  Réplicas listas, reinicios, `CrashLoopBackOff`/`Init:Error`; últimas 200 líneas del core, log del init y eventos.
  **Criterio:** `./gradlew :backend:test --tests '*EstadoYContextoTest'` pasa con un pod listo, uno en `CrashLoopBackOff` y uno en `Init:Error`. · H-14, H-16

- [x] **U05-T06 · `leerPais`**
  Core, digest, plugins con versión y hash leídos del Deployment y el ConfigMap.
  **Criterio:** `./gradlew :backend:test --tests '*LeerPaisTest'` pasa con un país desplegado y uno vacío. · H-19

- [x] **U05-T07 · Script `init-descarga/descargar.sh`**
  Lee `/config/artefactos.txt`; descarga por hash de `$SYNKRA_URL`; comprueba con `sha256sum`; 5 intentos con espera creciente; escribe solo en `/plugins`; `set -eu`.
  **Criterio:** `init-descarga/test.sh` pasa los casos: hash correcto (archivo en `/plugins`), hash alterado (código ≠ 0, nada en `/plugins`), catálogo caído (5 intentos y código ≠ 0), nombre con `../` en la lista (rechazado). · H-15

- [x] **U05-T08 · Imagen del init container**
  Base mínima con `curl` y `coreutils`, usuario no root.
  **Criterio:** `docker build -t synkra-init-descarga init-descarga/` y `docker inspect -f '{{.Config.User}}' synkra-init-descarga` no vacío ni `root`/`0`. · H-28

- [x] **U05-T09 · Fronteras del módulo**
  **Criterio:** `./gradlew :backend:test --tests '*FronterasModulosTest'` pasa con `kubernetes`; `grep -rn "io.fabric8" backend/src/main/java --include=*.java | grep -v "/kubernetes/"` no devuelve nada. · H-12

- [x] **U05-T10 · Resumen**
  **Criterio:** `aidlc-docs/construction/U05-pod-pais/code/summary.md` contiene la salida real de `./gradlew :backend:test --tests '*kubernetes*'` y de `init-descarga/test.sh`. · H-15

## Trazabilidad

| Historia | Tareas |
|---|---|
| H-15 | T03, T07, T10 |
| H-12 (contribuye) | T01, T03, T04, T09 |
| H-13 (contribuye) | T02 |
| H-14, H-16 (contribuye) | T05 |
| H-19 (contribuye) | T06 |
| H-28 (contribuye) | T08 |

**Terminada cuando:** `./gradlew :backend:test --tests '*kubernetes*'` pasa e `init-descarga/test.sh` termina con código 0.
