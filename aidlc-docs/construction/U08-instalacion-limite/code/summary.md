# Resumen de U08: Instalación y límite

**Unidad:** U08, tareas de código U08-A a U08-D (tareas del plan U08-T01 a U08-T11) y esta tarea, U08-E (U08-T12, resumen, y la guía `GUIA.md`).
**Historias:** dueñas H-24, H-26, H-27 y H-28; contribuye a H-13 (primera capa del límite).
**Fecha de las corridas:** 2026-11-28. **sha de las corridas:** c1ab806. Es `main` en `4881428` (con U08-A a U08-D, U09-A a U09-C y la colección de Postman fusionadas) más los guiones de U08-E; ni el `Makefile` ni `deploy/`, `scripts/`, `backend/`, `core/`, `front/` o `postman/` cambian entre los dos (`git diff --stat 4881428 c1ab806 -- Makefile scripts deploy backend core front postman` sale vacío).
**Lo único que tocó U08-E:** este resumen, `GUIA.md`, la evidencia y los guiones de `bitacoras/U08-E-evidencia/`. Ningún código.
**Entorno:** máquina x86_64 (`docker-servidor-arquitectura: x86_64` en la cabecera de cada archivo de evidencia), `JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2` (Java 21). Clúster solo `kind-synkra` con su kubeconfig `.kube/kind-synkra.yaml`.

## Qué se construyó

Una instalación local completa de SynKra en `kind`, con un límite de permisos comprobado y un recorrido de punta a punta por la API. Por tarea (el «Dentro» de cada tarea, resumido):

- **U08-A, clúster, guardia y RBAC (U08-T01 a T04).** `deploy/kind/cluster.yaml` (control plane y worker; `80` y `443` solo en `127.0.0.1`), el registro `kind-registry` en `127.0.0.1:5001`, el kubeconfig propio `.kube/kind-synkra.yaml` (ignorado por git), la guardia `scripts/guardia-contexto.sh` en todo objetivo del `Makefile` que toque el clúster, y `deploy/base/` con los namespaces `synkra`, `synkra-co` y `synkra-pe` y el RBAC de `synkra/deployer`: un `Role` y un `RoleBinding` por país, ningún `ClusterRole` ni `ClusterRoleBinding`.
- **U08-B, backend, Ingress e imágenes (U08-T05 a T07).** `deploy/base/backend/` (PVC `synkra-datos` de 1 Gi, Deployment `synkra-backend` con `Recreate`, sin root, sondas y recursos, y Service), `deploy/overlays/kind/` (imagen de `localhost:5001` e Ingress `synkra.local` con TLS), `scripts/certificado-tls.sh` (autofirmado) y las tres imágenes (`synkra-backend:0.1.0`, `synkra-core-ejemplo:2.1.0`, `synkra-init-descarga:1.0.0`) con `scripts/imagenes.sh`, multi-stage, base por digest y `USER 10001:10001`, atadas a `ConstructorObjetosPais` por `scripts/verificar-imagenes.sh`.
- **U08-C, `make install` y el límite (U08-T08 y T09).** La unión de A y B; Ingress NGINX `controller-v1.15.1` copiado a `deploy/ingress-nginx/` (fijado al nodo `ingress-ready`); el secreto TLS `synkra/synkra-local-tls`; `make imagenes`, `make desplegar` (valida en el servidor, aplica, espera el `rollout` y comprueba que el pod corre el digest recién subido), `make verificar-limite` (seis preguntas `kubectl auth can-i` como `system:serviceaccount:synkra:deployer`) y `make install` que los encadena.
- **U08-D, recorrido por la API (U08-T10 y T11).** `scripts/e2e-api.sh` recorre CU1 a CU5 con `curl` y `jq`, lee cada paso de vuelta por la API y por `kubectl`, y comprueba que cada 4xx trae `codigo`, `mensaje` y `detalles`. Descubrió un defecto del backend (C-138: un plan que solo cambia plugins no reiniciaba el core) y lo arregló con la anotación `synkra.io/config-sha256` en la plantilla del pod de `ConstructorObjetosPais`.
- **U08-E, esta tarea.** Este resumen con una corrida fresca, y `GUIA.md`, la guía para quien instala y usa SynKra por primera vez.

## Cómo se prueba

Todo corre sobre un clúster `kind` recién creado, nunca sobre otro (AUTONOMIA-01), con el kubeconfig del proyecto:

```bash
make cluster-delete          # borra el clúster synkra y su registro
make install                 # clúster, registro, base, Ingress, TLS, imágenes, backend y límite
make verificar-limite        # las seis respuestas del límite
./gradlew :plugins:kyc-co:jar :plugins:kyc-pe:assemble   # los JAR de ejemplo que sube el e2e
./scripts/e2e-api.sh         # CU1 a CU5 de punta a punta
```

`bash bitacoras/U08-E-evidencia/corrida.sh` corre `make cluster-delete`, `make install`, `make verificar-limite` y `./scripts/e2e-api.sh` en ese orden y escribe cada salida completa, con cabecera de fecha, sha, comando, arquitectura del servidor de Docker y `JAVA_HOME`, en `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/` (`install.txt`, `limite.txt`, `e2e.txt`; el borrado va a `bitacoras/U08-E-evidencia/borrar.txt`). Las rutas del home de la máquina aparecen como `$HOME` (el guion las sustituye al guardar).

Cómo se probó cada tarea de código (sus bitácoras y sus informes de revisión están en `bitacoras/U08-*` y `revisiones/U08-*`): un guion por criterio (`bitacoras/U08-X-evidencia/caN.sh`) que escribe su `caN.txt`, y mutaciones en copias para comprobar que cada regla muerde (por ejemplo, la guardia de contexto, el RBAC sin `ClusterRole`, `kubectl` sin `--kubeconfig`, el e2e sin la vuelta atrás). El único código de Java que cambió en U08 es el de C-138 (`ConstruirObjetosPaisTest`, con tres pruebas nuevas, en U08-D).

La guía se prueba aparte: `bash bitacoras/U08-E-evidencia/guia.sh` ejecuta cada bloque de `GUIA.md` marcado como ejecutable sobre una instalación limpia y guarda su salida en `bitacoras/U08-E-evidencia/guia.txt`; `bash bitacoras/U08-E-evidencia/cifras.sh` comprueba con `grep -F` cada cifra de este resumen y cada fragmento de respuesta de la guía contra su archivo.

## Salidas reales

Las tres salidas completas están en `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/`. Aquí van las líneas que sostienen el resultado, copiadas tal cual (el número es el de su línea en el archivo).

**`make install`** (`install.txt`, 388 líneas): una línea por paso y el cierre.

```text
12:Creating cluster "synkra" ...
45:registro listo en 127.0.0.1:5001
51:namespace/synkra created
85:deployment "ingress-nginx-controller" successfully rolled out
92:secret/synkra-local-tls created
341:OK imagenes subidas
372:deployment "synkra-backend" successfully rolled out
373:OK desplegar: localhost:5001/synkra-backend:0.1.0 sha256:48b554d0a4fa144ecf93cb5403209c06cacd26985f629824920b0facbc5e1baa
385:OK limite: 6 respuestas esperadas
387:salida=0
388:segundos=84
```

**`make verificar-limite`** (`limite.txt`, sus últimas 14 líneas, que son las de la corrida):

```text
JAVA_HOME: $HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
nota: la ruta personal de la maquina aparece como $HOME
---
contexto permitido: kind-synkra (https://127.0.0.1:38973)
scripts/verificar-limite.sh
ok create deployments.apps -n synkra-co: yes
ok create deployments.apps -n synkra-pe: yes
ok create deployments.apps -n default: no
ok create deployments.apps -n kube-system: no
ok create deployments.apps -n synkra: no
ok create namespaces: no
OK limite: 6 respuestas esperadas
salida=0
segundos=1
```

**`./scripts/e2e-api.sh`** (`e2e.txt`, 96 líneas): el cierre de cada caso, las diez respuestas 4xx y la línea final.

```text
10:4xx 409 CONFLICTO POST /api/cores
13:4xx 422 ENTIDAD_NO_PROCESABLE POST /api/cores
15:4xx 409 CONFLICTO POST /api/cores/synkra-core/plugins
22:OK CU1
23:tiempo CU1 2s
27:4xx 422 PAIS_NO_PERMITIDO POST /api/despliegues
28:4xx 422 USUARIO_INVALIDO POST /api/despliegues
30:4xx 409 PLAN_NO_PENDIENTE POST /api/despliegues/1/confirmacion
42:OK CU2
43:tiempo CU2 22s
56:OK CU3
57:tiempo CU3 16s
60:4xx 422 COMBINACION_RECHAZADA POST /api/despliegues
67:OK CU4
68:tiempo CU4 1s
71:4xx 409 DESPLIEGUE_EN_CURSO POST /api/despliegues/4/confirmacion
73:D4 tardó 185s en fallar
79:4xx 422 ORIGEN_NO_LISTO POST /api/despliegues/3/reversion
80:4xx 404 NO_ENCONTRADO POST /api/despliegues/0/reversion
92:OK CU5
93:tiempo CU5 188s
94:OK e2e: CU1 a CU5, 10 respuestas 4xx con codigo, mensaje y detalles
95:salida=0
96:segundos=229
```

## Desviaciones del diseño

Cada desviación dice qué decía el diseño, el plan o la tarea; qué se hizo; quién la anotó (con su cita literal) y si el revisor la aceptó (cita literal o «el informe no la comenta»).

1. **Sin `yq` ni `kubeconform` (U08-B).** El plan (U08-T05, T06) comprueba el Deployment con `yq` y el overlay con `kubeconform -strict`; ninguno está instalado. U08-B usa `scripts/verificar-backend.py` y `kubectl apply --dry-run=server`. Plan: «yq 'select(.kind=="Deployment" and .metadata.name=="synkra-backend") | .spec.strategy.type'» (`aidlc-docs/construction/plans/U08-instalacion-limite-code-generation-plan.md`). Bitácora: «sustituye a `kubeconform -strict`, que no está» (`bitacoras/U08-B.md`). El informe no la comenta.
2. **`--tmpfs /tmp` con `exec` en CA-6 de U08-B.** Con el comando literal de la tarea el backend no arranca (`sqlite-jdbc` carga su biblioteca nativa desde `/tmp`, y Docker lo monta `noexec`); en Kubernetes el `emptyDir` no lo es, así que el Deployment no cambia. Bitácora: «Con el comando literal de la tarea el backend no arranca» (`bitacoras/U08-B.md`). Revisor: «CA-6, `tmpfs` con `exec`: aceptada.» (`revisiones/U08-B/ronda-1.md`).
3. **RBAC con `get` sobre `namespaces` (U08-A).** `infrastructure-design.md` no lo nombra; se añadió `get` con `resourceNames` para la comprobación de existencia de un país. Bitácora: «el RBAC añade `get` sobre `namespaces` con `resourceNames` por `existeNamespace`» (`bitacoras/U08-A.md`). El informe no la comenta.
4. **`aplicar-base` aplica con `-f` (U08-C).** Con `- backend` en la base, `apply -k deploy/base` crearía un Deployment cuya imagen aún no tiene registro. Bitácora: «pasa a `apply -f namespaces.yaml -f rbac.yaml` (decisión 7)» (`bitacoras/U08-C.md`). El informe no la comenta.
5. **`imagePullPolicy: Always` solo en el overlay de `kind` (U08-C).** Para que el pod tome siempre el digest recién subido; la base sigue con `IfNotPresent`. Bitácora: «`imagePullPolicy: Always` solo en el overlay de `kind`» (`bitacoras/U08-C.md`). El informe no la comenta.
6. **`docker build --provenance=false --sbom=false` (U08-C).** Con las atestaciones por defecto el mismo contenido daba dos digests y la comparación del pod con el registro no era fiable. Bitácora: «`docker build --provenance=false --sbom=false`» (`bitacoras/U08-C.md`). Revisor: «aceptada. `rojo-digest.txt` muestra que el mismo contenido daba dos digests distintos» (`revisiones/U08-C/ronda-1.md`).
7. **`make` sale con 2 cuando el script sale con 1 (U08-C).** Es la especificación (CA-7 y H-27) la que pedía 1. Bitácora: «`make verificar-limite` sale con 2, no con 1, cuando el script sale con 1» (`bitacoras/U08-C.md`). Revisor: «GNU make devuelve 2 siempre que falla una receta» (`revisiones/U08-C/ronda-1.md`). La guía lo dice: léelo como «distinto de 0».
8. **Ingress NGINX `controller-v1.15.1` está retirado (U08-C).** Es la última versión del proyecto y no tendrá parches; cambiar de controlador no era de esta tarea. Bitácora: «es la última versión del proyecto `kubernetes/ingress-nginx`, retirado después: no habrá parches nuevos» (`bitacoras/U08-C.md`). Revisor: «Ingress NGINX `controller-v1.15.1` está retirado y no tendrá parches: plan de cambio de controlador.» (`revisiones/U08-C/ronda-1.md`).
9. **C-138 corregida dentro de U08-D.** El e2e descubrió que un plan que solo cambia plugins no recreaba el pod (el core lee los plugins al arrancar y `concuerda` decía `true` con los plugins viejos). Se arregló con la anotación `synkra.io/config-sha256` en la plantilla del pod. Bitácora: «Arreglada aquí (`c202f7c`).» (`bitacoras/U08-D.md`). Revisor: «El arreglo de C-138 es correcto y está probado.» (`revisiones/U08-D/ronda-1.md`).
10. **`kube-root-ca.crt` en `synkra-pe` (U08-D).** El criterio esperaba `0` objetos en `synkra-pe`; Kubernetes crea ese ConfigMap en todo namespace. Bitácora: «es `configmap/kube-root-ca.crt`, que Kubernetes crea en **todo** namespace» (`bitacoras/U08-D.md`). Revisor: «La acepto. Lo único que hay es `configmap/kube-root-ca.crt`» (`revisiones/U08-D/ronda-1.md`).
11. **Imágenes del namespace comparadas ordenadas (U08-D, CU4).** El orden de `{..image}` no es estable. Bitácora: «`kubectl get deploy -n synkra-co -o jsonpath='{..image}'` **no tiene orden estable**» (`bitacoras/U08-D.md`). El informe no la comenta.

## Trazabilidad

Del plan de U08 (`aidlc-docs/construction/plans/U08-instalacion-limite-code-generation-plan.md`, sección «Trazabilidad»), con la tarea de código que implementó cada tarea del plan:

| Historia | Tareas del plan | Tareas de código |
|---|---|---|
| H-24 (despliegue y registro por API, de punta a punta) | T10, T11 | U08-D |
| H-26 (instalar SynKra en un clúster local) | T01, T02, T03, T05, T06, T08, T12 | U08-A (T01 a T03), U08-B (T05, T06), U08-C (T08), U08-E (T12) |
| H-27 (el límite del despliegue) | T04, T09 | U08-A (T04), U08-C (T09) |
| H-28 (imágenes sin root) | T07 | U08-B |
| H-13 (contribuye: la primera capa del límite) | T09 | U08-C; en U08-D, CU4 comprueba que nada incompatible llega al clúster |

H-26 también se apoya en `GUIA.md`: la guía que lleva a una persona de una máquina vacía a SynKra instalado y usado.

## Tareas y rondas

| Tarea | PR | Rondas del revisor | Veredictos |
|---|---|---|---|
| U08-A | #111 | 2 | ronda 1 «VEREDICTO: NO-VERDE» (`revisiones/U08-A/ronda-1.md`); ronda 2 «VEREDICTO: VERDE» (`revisiones/U08-A/ronda-2.md`) |
| U08-B | #112 | 3 | ronda 1 «VEREDICTO: NO-VERDE» (`revisiones/U08-B/ronda-1.md`); ronda 2 «VEREDICTO: NO-VERDE» (`revisiones/U08-B/ronda-2.md`); ronda 3 «VEREDICTO: VERDE» (`revisiones/U08-B/ronda-3.md`) |
| U08-C | #113 | 3 | ronda 1 «VEREDICTO: NO-VERDE» (`revisiones/U08-C/ronda-1.md`); ronda 2 «VEREDICTO: NO-VERDE» (`revisiones/U08-C/ronda-2.md`); ronda 3 «VEREDICTO: VERDE» (`revisiones/U08-C/ronda-3.md`) |
| U08-D | #116 | 2 | ronda 1 «VEREDICTO: NO-VERDE» (`revisiones/U08-D/ronda-1.md`); ronda 2 «VEREDICTO: VERDE» (`revisiones/U08-D/ronda-2.md`) |
| U08-E | pendiente | 1 (esta) | por el revisor |

Los PR salen de la tarea U08-E: «U08-A a U08-D (fusionadas en `main`: #111, #112, #113, #116)» (`orq:tareas/U08-E-resumen-guia.md`). En U08-B y U08-C hubo una ronda más porque el arreglo de un hallazgo dejó otro (el informe de la ronda 2 de U08-B dice de su F-06: «lo provocó el arreglo de F-03» (`revisiones/U08-B/ronda-2.md`)); ninguna ronda cambió el diseño.

## Candidatas abiertas

Las de la lista de la tarea, con su estado según `CANDIDATAS.md` (el del árbol del orquestador, que es el que tiene C-130 en adelante) y las bitácoras:

- **Atendidas por U08-B** (su bitácora dice qué hizo): «bases por digest (C-101)», «(C-98, C-95)» y «sin líneas muertas (C-103)» (`bitacoras/U08-B.md`). Queda de C-98 que `curl` no lleva versión fija: «Alpine solo guarda la última versión de cada rama» (`bitacoras/U08-B.md`).
- **C-130**, el límite de inotify: «`kind create cluster` necesita `fs.inotify.max_user_instances` ≥ 512» (`orq:CANDIDATAS.md`). Atendida por la sección «Requisitos» de `GUIA.md`.
- **C-131**: «`core/Dockerfile:2` cita C-98 para el digest de la base» (`orq:CANDIDATAS.md`). Abierta (comentario a corregir).
- **C-132**: «Ninguna tarea debe comparar `Id` entre corridas» (`orq:CANDIDATAS.md`). Vigente; esta tarea no compara `Id`.
- **C-136**: «Cada `kubectl` de las recetas del `Makefile` escribe la caché de descubrimiento» (`orq:CANDIDATAS.md`). Abierta.
- **C-137**: «`bitacoras/U08-B-evidencia/ronda3-verde.txt` se cita como prueba de `FALLA versiones`» (`orq:CANDIDATAS.md`). Abierta (pulido de evidencia).
- **C-138**: corregida en U08-D (desviación 9).
- **C-139**: «Especificación de U08-C: CA-7 y H-27 piden que `make verificar-limite` salga con 1» (`orq:CANDIDATAS.md`). Abierta; la guía lo explica (desviación 7).
- **C-140**: «Pulido de scripts de U08-C» (`orq:CANDIDATAS.md`). Abierta.
- **C-143**: «un core en CrashLoopBackOff se cierra en `fallido` solo al vencer el plazo» (`orq:CANDIDATAS.md`). Abierta; se nota en el e2e (CU5 tarda 188 s) y en la guía («unos 3 minutos»).
- **C-144**: «Pulido de U08-D» (`orq:CANDIDATAS.md`). Abierta.

Las propuestas nuevas que salieron de esta tarea (sin número, para el orquestador) están en la bitácora `bitacoras/U08-E.md`, sección «Propuestas de candidata».

## Cifras y su fuente

Cada fila se comprueba con `bash bitacoras/U08-E-evidencia/cifras.sh` (`grep -F` del texto de la última columna contra el archivo de la tercera). Una fuente que empieza por `orq:` está en el árbol del orquestador, en solo lectura.

| Cifra | Valor | Fuente | Texto buscado |
|---|---|---|---|
| `make install` termina con 0 | 0 | `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/install.txt` | `salida=0` |
| Tiempo de `make install` | 84 s | `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/install.txt` | `segundos=84` |
| Cierre de `make install` | límite | `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/install.txt` | `OK limite: 6 respuestas esperadas` |
| Respuestas del límite | 6 | `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/limite.txt` | `OK limite: 6 respuestas esperadas` |
| Primera pregunta del límite | yes | `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/limite.txt` | `ok create deployments.apps -n synkra-co: yes` |
| Última pregunta del límite | no | `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/limite.txt` | `ok create namespaces: no` |
| Resultado del e2e | OK | `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/e2e.txt` | `OK e2e: CU1 a CU5, 10 respuestas 4xx con codigo, mensaje y detalles` |
| Tiempo del e2e | 229 s | `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/e2e.txt` | `segundos=229` |
| Tiempo de CU5 | 188 s | `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/e2e.txt` | `tiempo CU5 188s` |
| Tiempo del despliegue fallido | 185 s | `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/e2e.txt` | `D4 tardó 185s en fallar` |
| Última respuesta 4xx del e2e (la décima) | 404 | `aidlc-docs/construction/U08-instalacion-limite/code/evidencia/e2e.txt` | `4xx 404 NO_ENCONTRADO POST /api/despliegues/0/reversion` |
| Criterios de U08-A | 10 | `bitacoras/U08-A.md` | `criterios de aceptación: 10 (CA-1 a CA-10)` |
| Criterios de U08-B | 9 | `bitacoras/U08-B.md` | `criterios de aceptación: 9 (CA-1 a CA-9)` |
| Criterios de U08-C | 10 | `bitacoras/U08-C.md` | `criterios de aceptación: 10 (CA-1 a CA-10)` |
| Criterios de U08-D | 8 | `bitacoras/U08-D.md` | `criterios de aceptación: 8 (CA-1 a CA-8)` |
| Veredicto final de U08-A | VERDE | `revisiones/U08-A/ronda-2.md` | `VEREDICTO: VERDE` |
| Veredicto final de U08-B | VERDE | `revisiones/U08-B/ronda-3.md` | `VEREDICTO: VERDE` |
| Veredicto final de U08-C | VERDE | `revisiones/U08-C/ronda-3.md` | `VEREDICTO: VERDE` |
| Veredicto final de U08-D | VERDE | `revisiones/U08-D/ronda-2.md` | `VEREDICTO: VERDE` |
| Pruebas nuevas por C-138 | 3 | `bitacoras/U08-D.md` | `laPlantillaDelPodLlevaElSha256DeLaConfiguracionDelConfigMap` |
| Candidata C-130 | aceptada | `orq:CANDIDATAS.md` | `| C-130` |
| Candidata C-144 | aceptada | `orq:CANDIDATAS.md` | `| C-144` |
| Tarea de esta unidad | U08-E | `orq:tareas/U08-E-resumen-guia.md` | `# U08-E: resumen de U08 y guía para instalar y usar SynKra` |
