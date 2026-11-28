# U08-D: script de punta a punta por la API (CU1 a CU5) y formato único de error

**Unidad:** U08: `Instalación y límite`
**Tareas del plan que agrupa:** U08-T10 y U08-T11
**Historias que implementa:** H-24 (T10, T11); contribuye a H-13 (CU4: nada incompatible llega al clúster) y cierra C-75 (server-side apply contra un servidor real)
**Depende de:** U08-C (VERDE). Sale de `<punta VERDE de U08-C>`: rama `m6r/U08-D`, worktree `../wt-m6r-U08-D`. Es la primera tarea que despliega países de verdad en `kind-synkra`. Empieza **después** de que el revisor de U08-C haya terminado: CA-3 borra y vuelve a crear el clúster.

---

## Alcance

**Dentro** (una línea, concreta):

> `scripts/e2e-api.sh` (`bash`, `set -euo pipefail`, `curl --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt`, `jq`) que recorre CU1 subir core y plugins, CU2 planear y confirmar en `synkra-co`, CU3 cambiar plugins, CU4 combinación incompatible rechazada sin que nada llegue al clúster, y CU5 fallo y vuelta atrás, leyendo de vuelta cada paso con la API y con `kubectl` real, y comprobando que cada 4xx que provoca trae `codigo`, `mensaje` y `detalles`; más el arreglo mínimo de `ConstructorObjetosPais` sin el que CU3 y CU5 no pueden pasar (decisión 1).

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cualquier clúster que no sea `kind-synkra` (AUTONOMIA-01). En las máquinas del equipo hay otros clústeres `kind` de otros cursos y sus contenedores: **ninguna orden los lista, los nombra, los toca, los para ni los borra**. Prohibidos `kind get clusters`, `kind delete clusters`, `docker ps` sin un filtro de nombre propio de SynKra, todo `kubectl` sin `--kubeconfig` y leer o escribir `~/.kube/config`.
- `sudo`, `/etc/hosts`, `kubectl port-forward`, `curl -k` o `--insecure`. Lo que corre dentro del core se lee por el proxy de la API de Kubernetes (`kubectl get --raw …/services/synkra-core:http/proxy/plugins`), con el kubeconfig del proyecto.
- Cambios en el `Makefile`, `deploy/`, los Dockerfile y los scripts de U08-A, U08-B y U08-C. Si `make install` falla, se para y se reporta.
- Cambios en el código Java **fuera** de la decisión 1: ni `LectorPais`, ni la vigilancia, ni los códigos de error, ni el formato de error (si un 4xx no trae el formato único, es un hallazgo: se para y se reporta, no se arregla aquí). Tampoco en `plugins/`, `core/` ni `init-descarga/`.
- El resumen de U08 (U08-E: `aidlc-docs/construction/U08-instalacion-limite/code/summary.md`), `aidlc-docs/`, `CANDIDATAS.md`, `unidades-y-tareas.md`, el plan de U08 y `loop/`.
- El front (U09) y la colección de Postman (`origin/m6r/postman`, solo de referencia).
- Construir los JAR desde el script: `scripts/e2e-api.sh` no corre Gradle (decisión 4).
- Cualquier descarga que no sea la de `make install` (decisión 4 de U08-C) o las dependencias Maven ya declaradas, si su caché no las tiene.

---

## Archivos de contexto

- `loop/CONTINUAR.md` («Reglas del entorno» y «Lecciones de esta sesión para los despachos»)
- `aidlc-docs/construction/plans/U08-instalacion-limite-code-generation-plan.md` (U08-T10 y U08-T11) y `unidades-y-tareas.md` (sección U08, «Terminada cuando»)
- `Modulo 3/specs/prd.md` (CU1 a CU5, y la demo: «Rechazar una combinación incompatible» y «Vista general y reversión»)
- `tareas/U08-C-install-limite.md` y `bitacoras/U08-C.md` (`make install`, `.tls/`, el registro y el `Accept` para leer digests)
- Rutas de la API en `backend/src/main/java/io/synkra/`: `catalogo/RutasCores.java` (`POST /api/cores` con `nombre`, `version`, `imagen`; la imagen tiene que ser `registro/repo[:tag]@sha256:<64 hex>` del registro permitido), `catalogo/RutasPlugins.java` (`POST /api/cores/{core}/plugins` multipart con la parte `archivo` y la parte opcional `etiquetas`; `GET` con filtro por etiquetas en la consulta), `despliegue/RutasPlanes.java` (`POST /api/despliegues` con `pais`, `core`, `versionCore`, `plugins: [{nombre, version}]`; 201), `despliegue/RutasConfirmacion.java` (`POST /api/despliegues/{id}/confirmacion`; 202 con `estado: aplicando`), `despliegue/RutasReversion.java` (`POST /api/despliegues/{id}/reversion`; 201, plan `tipo: reversion`), `registro/RutasRegistro.java` (`GET /api/despliegues/{id}`, `GET /api/paises/{pais}/despliegues`), `registro/RutasVistaGeneral.java` (`GET /api/paises`), `despliegue/Usuario.java` (cabecera `X-Synkra-Usuario`)
- Códigos y estados HTTP: `plataforma/CodigosError.java`, `plataforma/ErrorApi.java` (`codigo`, `mensaje`, `detalles: [{campo, motivo}]`), `despliegue/CodigosDespliegue.java`, `despliegue/PlanServicio.java`, `despliegue/GuardaConfirmacion.java`, `despliegue/ReservaConfirmacion.java`, `despliegue/PaisPermitido.java`, `catalogo/CatalogoServicio.java` y `catalogo/SubidaPlugin.java`
- `despliegue/VigilanciaDespliegues.java` y `kubernetes/LectorPais.java` (cuándo un despliegue queda `listo` o `fallido`; C-82) y `application.properties` (`synkra.vigilancia.intervalo` `PT5S`, `synkra.vigilancia.plazo` `PT3M`)
- `kubernetes/ConstructorObjetosPais.java` (Deployment `synkra-core`, ConfigMap `synkra-core-config`, Service `synkra-core` puerto `http`, contenedor `core`), `kubernetes/LectorConfigMapPais.java`, y las pruebas `ConstruirObjetosPaisTest.java`, `YamlObjetosPaisTest.java` y `src/test/resources/kubernetes/pais-pe-esperado.yaml`
- Los ejemplos: `core/` (core `synkra-core`, versión de `core/build.gradle.kts`; `EndpointsCore.java` sirve `GET /plugins`; `ArranquePlugins.java` rechaza el arranque si un plugin no es compatible con la versión compilada), `plugins/kyc-co` (`kyc-co` 1.0.0, `coreCompatible: 2.x`, `pais: co`) y `plugins/kyc-pe` (`kyc-pe` 1.3.0 con `2.x` y 2.0.0 con `3.x`, `pais: pe`), y `core/smoke.sh` (estilo de `trap` y de `FALLA`)
- `CANDIDATAS.md`: C-75, C-82, C-97 (el `emptyDir` de `/plugins` escribible por 10001 en el pod real: se observa aquí), C-132
- Plantillas de estilo: `tareas/U08-C-install-limite.md` y `tareas/U07-G-fronteras-resumen.md`

---

## Decisiones del orquestador

1. **El arreglo previo: cambiar solo los plugins tiene que recrear el pod.** Hoy `ConstructorObjetosPais` pone la lista de plugins solo en el ConfigMap; la plantilla del pod no cambia, así que un plan que cambia plugins sin cambiar la imagen actualiza el ConfigMap y **no** reinicia el core. El core lee `plugins-activos.yaml` y el init descarga los JAR solo al arrancar, y `LectorPais` lee lo que corre del ConfigMap: el despliegue queda `listo` y la vista general dice que concuerda, mientras el core sigue con los plugins viejos. CU3 y CU5 lo destapan, y es justo lo que la North Star no permite. El arreglo, dentro de esta tarea y con su prueba roja primero:
   - `ConstructorObjetosPais` añade a los metadatos de la **plantilla del pod** del Deployment la anotación `synkra.io/config-sha256`, con el `sha256` en hexadecimal (minúsculas) de los bytes UTF-8 de `plugins-activos.yaml` + `"\n"` + `artefactos.txt`, los mismos textos que van al ConfigMap (constante nueva para el nombre de la anotación, en el mismo archivo).
   - Pruebas nuevas en `ConstruirObjetosPaisTest`: (a) la anotación es el `sha256` calculado en la prueba a partir del `data` del ConfigMap construido; (b) dos planes que solo difieren en un plugin dan anotaciones distintas; (c) el mismo plan con los plugins en otro orden da la misma anotación. Se actualiza `pais-pe-esperado.yaml` con el valor que salga.
   - Nada más cambia en Java (ni `LectorPais`, ni la vigilancia, ni los demás módulos). La candidata nueva («un cambio solo de plugins no reiniciaba el pod») la numera el orquestador; el codificador la describe en la bitácora.
2. **Archivos.** Esta tarea escribe solo: `scripts/e2e-api.sh`; `backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java`, `backend/src/test/java/io/synkra/kubernetes/ConstruirObjetosPaisTest.java` y `backend/src/test/resources/kubernetes/pais-pe-esperado.yaml` (decisión 1); `bitacoras/U08-D.md`, `bitacoras/U08-D-evidencia/` y `revisiones/U08-D/`.
3. **Cómo habla el script.**
   - Se corre desde cualquier sitio: `RAIZ` es la raíz del repositorio (por la ruta del script). `KC=${KUBECONFIG_SYNKRA:-$RAIZ/.kube/kind-synkra.yaml}`. Lo primero que hace con el clúster es `KUBECONFIG_SYNKRA="$KC" "$RAIZ/scripts/guardia-contexto.sh"`; si falla, `FALLA guardia` y 1. Todo `kubectl` lleva `--kubeconfig "$KC"`.
   - Una sola función para la API, `pedir <metodo> <ruta> [<archivo JSON> | <partes multipart>]`, que siempre usa `curl -sS --max-time 60 --resolve synkra.local:443:127.0.0.1 --cacert "$RAIZ/.tls/tls.crt" -H 'X-Synkra-Usuario: e2e' -o <temporal> -w '%{http_code}' https://synkra.local<ruta>` y deja el estado y el cuerpo (una variable opcional cambia el valor de la cabecera solo para la petición 5 de la decisión 6). Si `curl` sale con un código distinto de 0: `FALLA <caso>: curl <metodo> <ruta> salió con <código>`. Los cuerpos JSON se arman con `jq -n --arg …` (nunca concatenando texto). La subida usa `--form "archivo=@<jar>;type=application/java-archive"` y, para etiquetas, `--form-string 'etiquetas=…'`.
   - Cada respuesta se compara con el estado esperado exacto. **Toda** respuesta `4xx`, esperada o no, pasa por `formato_error` (U08-T11): `jq -e 'has("codigo") and has("mensaje") and has("detalles") and (.codigo|type)=="string" and (.mensaje|type)=="string" and (.detalles|type)=="array"'`; si no cumple, `FALLA formato: <metodo> <ruta> <estado>`. Una `4xx` que no estaba en la lista de la decisión 6 es `FALLA <caso>: <estado> no esperado` aunque tenga buen formato.
   - Toda espera es explícita y acotada: `esperar_estado <id> <estado>` sondea `GET /api/despliegues/<id>` cada 3 s hasta `E2E_ESPERA` segundos (300 por defecto); si llega a otro estado final o vence, `FALLA <caso>: el despliegue <id> quedó en <estado> (esperado <estado>)` con el `contexto` del detalle. `kubectl rollout status` siempre con `--timeout`.
   - Al primer `FALLA` el script sale con 1 (los casos dependen unos de otros). Sin errores mudos de `set -e`: todo comando externo (`curl`, `kubectl`, `jq`, `python3`, `sha256sum`) que puede fallar se envuelve y da `FALLA <caso>: <orden> salió con <código>`.
   - `trap` registrado al principio, antes del primer `mktemp -d`, que borra el temporal. El script no crea nada más fuera de la API (los planes y despliegues que crea son el estado que se quiere dejar).
   - Salida: una línea `ok <caso> <comprobación>` por comprobación, `OK CUn` al cerrar cada caso, una línea `4xx <estado> <codigo> <metodo> <ruta>` por cada error provocado, y al final `OK e2e: CU1 a CU5, <n> respuestas 4xx con codigo, mensaje y detalles` y 0.
4. **Preparación, sin literales sueltos** (lección de F-03 y F-06 de U08-B: cada dato se lee de su origen y se comprueba).
   - Herramientas: `curl`, `jq`, `kubectl`, `python3`, `sha256sum`; si falta una, `FALLA preparacion: falta <herramienta>`.
   - Los JAR: `plugins/kyc-co/build/libs/kyc-co-1.0.0.jar`, `plugins/kyc-pe/build/libs/kyc-pe-1.3.0.jar` y `plugins/kyc-pe/build/libs/kyc-pe-2.0.0.jar` (los de `core/smoke.sh` y de la colección de Postman). Si falta uno: `FALLA preparacion: falta <ruta> (corre ./gradlew :plugins:kyc-co:jar :plugins:kyc-pe:assemble)`. De cada JAR se lee `synkra-plugin.yaml` con `python3 -I` (`zipfile` y PyYAML del sistema) y se exige que `nombre`, `version`, `core` y `coreCompatible` sean los que el guion asume (`kyc-co 1.0.0 synkra-core 2.x`, `kyc-pe 1.3.0 synkra-core 2.x`, `kyc-pe 2.0.0 synkra-core 3.x`); si no, `FALLA preparacion: <jar> dice <campo> <valor>`.
   - El core: nombre `synkra-core` (el `core` de los descriptores); versión `VC` = la de `core/build.gradle.kts`, que tiene que caer en `2.x`. El repositorio de la imagen es `synkra-core-ejemplo` con la etiqueta `VC` (el de `scripts/imagenes.sh`); el digest se lee del registro (`HEAD http://127.0.0.1:5001/v2/synkra-core-ejemplo/manifests/<VC>`, cabecera `Docker-Content-Digest`, con el `Accept` de la decisión 8 de U08-C). La imagen que se registra es `localhost:5001/synkra-core-ejemplo:<VC>@<digest>`.
   - La versión «mentirosa» de CU5 es `3.0.0`: el guion comprueba que cae en el `coreCompatible` de `kyc-pe 2.0.0` (`3.x`) y que **no** es `VC`.
   - Antes de CU1: `GET /actuator/health` da 200 y `status` `UP`; si no, `FALLA preparacion: el backend no responde en https://synkra.local`.
5. **Los cinco casos** (todo en `synkra-co`; `D2`, `D3`, `D4` y `D5` son los ids que devuelve la API, nunca números escritos):
   - **CU1, subir core y plugins.** `POST /api/cores` con `synkra-core` `VC` y la imagen por digest: 201, o 409 `CONFLICTO` si ya estaba (segunda corrida); con 409, `GET /api/cores/synkra-core` tiene que traer `VC` con una imagen cuyo digest el registro sirve (`HEAD …/manifests/<digest>` da 200) y desde ahí se usa **esa** imagen. Igual para `3.0.0` con la misma imagen (es el core «mentiroso» de CU5: misma imagen, compilada como `VC`). Los tres JAR: 201 con el `sha256` de la respuesta igual al `sha256sum` del JAR local, o 409 `CONFLICTO` si ya estaban (entonces `GET` del plugin trae su `version` y `coreCompatible`; el `sha256` no se compara con el JAR local, que Gradle puede reconstruir con otro). `kyc-co` sube con `etiquetas={"cliente":"e2e"}`. Lectura de vuelta: `GET /api/cores/synkra-core` lista `VC` y `3.0.0`; `GET /api/cores/synkra-core/plugins?pais=co` trae `kyc-co` 1.0.0 (con `pais: co` y `cliente: e2e` en la primera corrida); `GET …?pais=pe` trae `kyc-pe` 1.3.0 y 2.0.0.
   - **CU2, planear y confirmar en `synkra-co`.** Plan `{pais: co, core: synkra-core, versionCore: VC, plugins: [kyc-co 1.0.0]}`: 201, `estado` `pendiente`, `creadoPor` `e2e`, y `cambios` coherente con lo que corría antes (vacío en la primera corrida: se pega). Confirmar: 202 `aplicando`, `confirmadoPor` `e2e`. `esperar_estado D2 listo`. Lecturas de vuelta con `kubectl`: `rollout status deployment/synkra-core -n synkra-co --timeout=300s`; la imagen del contenedor `core` es la registrada; el ConfigMap `synkra-core-config` lista solo `kyc-co` 1.0.0; **lo que el core cargó** (`GET …/proxy/plugins`) es exactamente `[kyc-co 1.0.0]`; los `managedFields` del Deployment, del ConfigMap y del Service traen `manager: synkra` con `operation: Apply` (C-75). Con la API: `GET /api/despliegues/D2` en `listo` con su imagen y plugins; `GET /api/paises` da `co` con `concuerda: true` y `ultimoDespliegueId: D2`.
   - **CU3, cambiar plugins.** Plan `[kyc-co 1.0.0, kyc-pe 1.3.0]` sobre `VC` (misma imagen: solo cambian los plugins): `cambios` dice que `kyc-pe` 1.3.0 se añade y nada más; confirmar; `esperar_estado D3 listo`. Lecturas de vuelta: la anotación `synkra.io/config-sha256` de la plantilla cambió respecto de CU2 y hubo un pod nuevo (el nombre del pod del core no es el de CU2); el ConfigMap lista los dos; **lo que el core cargó** es `[kyc-co 1.0.0, kyc-pe 1.3.0]`; `GET /api/paises`: `concuerda: true`, `ultimoDespliegueId: D3`.
   - **CU4, combinación incompatible.** Antes: `I0` = `kubectl get deploy -n synkra-co -o jsonpath='{..image}'`, los `resourceVersion` del Deployment `synkra-core` y del ConfigMap, y el largo del historial (`GET /api/paises/co/despliegues`). Plan `[kyc-co 1.0.0, kyc-pe 2.0.0]` sobre `VC`: 422 `COMBINACION_RECHAZADA` con al menos un detalle de `campo` `plugins.kyc-pe` (se pega su `motivo`, que dice qué versión sirve). Después: la misma orden `jsonpath` da exactamente `I0` (criterio del plan), los dos `resourceVersion` no cambiaron, el historial tiene el mismo largo y lo que el core cargó sigue siendo lo de D3. El script imprime `CU4 imagenes antes=<I0> despues=<I1>`.
   - **CU5, fallo y vuelta atrás.** Plan `{versionCore: 3.0.0, plugins: [kyc-pe 2.0.0]}`: SynKra lo aprueba (el catálogo dice que el core es 3.0.0), confirmar da 202 (`D4`). Mientras `D4` sigue `aplicando` (se comprueba con `GET` justo antes), un plan nuevo `[kyc-co 1.0.0]` sobre `VC` y su confirmación dan 409 `DESPLIEGUE_EN_CURSO` (ese plan queda pendiente y vence solo). `esperar_estado D4 fallido`: el core, compilado como `VC`, rechaza `kyc-pe 2.0.0` al arrancar (es la discrepancia motor-core que la demo quiere ver). Lecturas de vuelta: `GET /api/despliegues/D4` trae `contexto` con `logCore` no vacío que nombra `kyc-pe`; con `kubectl`, un pod del core en `CrashLoopBackOff` o `Error` y el pod viejo sigue `Ready`; lo que el core **sirve** por el Service sigue siendo lo de D3 (el país no se cayó); `GET /api/paises`: `concuerda: false` y `ultimoDespliegueId: D3`. Revertir `D4` da 422 `ORIGEN_NO_LISTO`; revertir `0` da 404 `NO_ENCONTRADO`. Vuelta atrás: `POST /api/despliegues/D3/reversion` da 201 con `tipo: reversion` y `origenId: D3`, confirmar, `esperar_estado D5 listo`. Lecturas de vuelta: `rollout status` termina; todos los pods del core `Ready` y ninguno en `CrashLoopBackOff`; la imagen (`jsonpath`) y el ConfigMap son los de D3; lo que el core cargó es `[kyc-co 1.0.0, kyc-pe 1.3.0]`; el historial de `co` termina en `D5 reversion listo`, `D4 despliegue fallido`, `D3 despliegue listo`; `GET /api/paises`: `concuerda: true`, `ultimoDespliegueId: D5`.
6. **Las diez respuestas 4xx que el script provoca** (U08-T11; en este orden, cada una con su estado y su `codigo` exactos, y todas por `formato_error`):
   | # | Caso | Petición | Estado | `codigo` |
   |---|---|---|---|---|
   | 1 | CU1 | `POST /api/cores` repetido (`VC`) | 409 | `CONFLICTO` |
   | 2 | CU1 | `POST /api/cores` con `nombre: "Core Malo"`, `version: "2.1"`, `imagen: "nginx:latest"` | 422 | `ENTIDAD_NO_PROCESABLE` (con detalles de `nombre`, `version` e `imagen`) |
   | 3 | CU1 | `POST /api/cores/synkra-core/plugins` con `kyc-co` repetido | 409 | `CONFLICTO` |
   | 4 | CU2 | plan con `pais: mx` | 422 | `PAIS_NO_PERMITIDO` |
   | 5 | CU2 | plan con `X-Synkra-Usuario: No Valido` | 422 | `USUARIO_INVALIDO` |
   | 6 | CU2 | confirmar `D2` otra vez | 409 | `PLAN_NO_PENDIENTE` |
   | 7 | CU4 | plan con `kyc-pe 2.0.0` sobre `VC` | 422 | `COMBINACION_RECHAZADA` |
   | 8 | CU5 | confirmar otro plan mientras `D4` aplica | 409 | `DESPLIEGUE_EN_CURSO` |
   | 9 | CU5 | revertir `D4` (fallido) | 422 | `ORIGEN_NO_LISTO` |
   | 10 | CU5 | revertir `0` | 404 | `NO_ENCONTRADO` |
   - El 1 y el 3 salen en todas las corridas (se repite a propósito lo que ya se subió). El 8 depende de que `D4` siga `aplicando` entre el `GET` y la confirmación: si `D4` ya cerró antes del `GET`, es `FALLA CU5: D4 cerró antes de provocar DESPLIEGUE_EN_CURSO` (no se confirma nada); se anota cuánto tardó `D4` en fallar.
7. **Segunda corrida.** El script compara siempre con lo que había antes de cada paso (no con números fijos), así que corre otra vez sobre la misma instalación: CU1 da 409 en lo ya subido y sigue; CU2 parte de lo que dejó CU5 y su `cambios` lo dice. CA-4 lo demuestra.
8. **Las imágenes que corren son las del último commit.** El backend con el arreglo de la decisión 1 se instala con `make cluster-delete` y `make install` desde el worktree, después del commit del arreglo (el overlay de U08-C pide `imagePullPolicy: Always` y `make desplegar` comprueba el digest, pero se usa un clúster nuevo igual, como pide el «Terminada cuando» de U08). Toda la evidencia de CA-3 a CA-7 sale de ese clúster y del último commit que toca código; si el código cambia después, se reinstala y se vuelve a correr.
9. **Entorno y portabilidad.** Como en U08-C (decisión 5): nada escrito para una arquitectura; la cabecera de cada `caN.txt` trae fecha, sha, `uname -m`, `docker version -f '{{.Server.Arch}}'` y `JAVA_HOME` (el de Java 21, resuelto como en U08-C). Criterios de Gradle como `./gradlew :backend:test --rerun :backend:check` (C-69).
10. **Qué queda arriba al terminar.** `kind-synkra` con SynKra instalado y `synkra-co` en `D5` (`listo`), para U08-E. Las copias de `mktemp -d` se borran; la anotación de la mutación «cu4-llega» se quita en su `trap`.
11. **Bitácora en `bitacoras/U08-D.md`**, evidencia en `bitacoras/U08-D-evidencia/` (un `caN.sh` versionado y su `caN.txt` con la cabecera de la decisión 9, el comando y la salida completa), y el revisor en `revisiones/U08-D/`. Las mutaciones se hacen en copias (`git archive HEAD | tar -x -C "$(mktemp -d)"` y `git init -q` dentro), nunca con `git stash`. En los `grep` de patrones con `${...}` se usa `grep -F` con comillas simples. La bitácora anota cuánto tardó cada caso y la C-97 (si el init escribió en el `emptyDir` de `/plugins` sin `fsGroup`).

---

## Criterios de aceptación

Desde la raíz del worktree: `KC=$PWD/.kube/kind-synkra.yaml`; todo `kubectl` lleva `--kubeconfig "$KC"`, nunca el global.

- [ ] **CA-1**: la decisión 1. La plantilla del pod cambia cuando cambian solo los plugins
  ```bash
  ./gradlew :backend:test --rerun :backend:check; echo "gradle=$?"
  python3 -I -c "import sys,xml.etree.ElementTree as E; r=E.parse(sys.argv[1]).getroot(); print(r.get('tests'), r.get('failures'), r.get('errors'), r.get('skipped')); [print(t.get('name')) for t in r.iter('testcase')]" backend/build/test-results/test/TEST-io.synkra.kubernetes.ConstruirObjetosPaisTest.xml
  grep -n 'synkra.io/config-sha256' backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java backend/src/test/resources/kubernetes/pais-pe-esperado.yaml
  git diff --name-only "<punta VERDE de U08-C>"...HEAD -- backend | sort
  ```
  Esperado: `gradle=0`; `0 0 0` en fallos, errores y saltos, y los nombres de las tres pruebas nuevas de la decisión 1 entre los `testcase`; la constante en el `.java` y la anotación en el YAML esperado; solo los tres archivos de la decisión 2.

- [ ] **CA-2**: el script está escrito como piden las reglas (sin clúster)
  ```bash
  bash -n scripts/e2e-api.sh; echo "sintaxis=$?"
  grep -n 'set -euo pipefail' scripts/e2e-api.sh
  grep -c -- '--resolve synkra.local:443:127.0.0.1' scripts/e2e-api.sh
  grep -cF -- '--cacert' scripts/e2e-api.sh
  grep -nE -- '(^|[[:space:]])-k([[:space:]]|$)|--insecure|/etc/hosts|sudo|port-forward|kind get clusters|\.kube/config' scripts/e2e-api.sh; echo "prohibidos=$?"
  grep -nE '(^|[^-])kubectl ' scripts/e2e-api.sh | grep -vF -e '--kubeconfig' | grep -v '^[0-9]*:[[:space:]]*#'; echo "sin-kubeconfig=$?"
  grep -nE 'amd64|arm64|x86_64|aarch64' scripts/e2e-api.sh; echo "arquitectura=$?"
  grep -nF 'has("codigo") and has("mensaje") and has("detalles")' scripts/e2e-api.sh
  grep -n -e 'trap' -e 'mktemp' scripts/e2e-api.sh | head -3
  ```
  Esperado: `sintaxis=0`; la línea de `set -euo pipefail`; `1` o más en `--resolve` y en `--cacert` (la única función que llama a `curl` contra `synkra.local`); `prohibidos=1`; `sin-kubeconfig=1`; `arquitectura=1`; la línea del chequeo de formato; el `trap` en una línea anterior al primer `mktemp`.

- [ ] **CA-3**: U08-T10 y U08-T11. En un `kind` nuevo, `make install && make verificar-limite && ./scripts/e2e-api.sh` termina con 0
  Guardar como `bitacoras/U08-D-evidencia/ca3.sh`:
  ```bash
  KC=$PWD/.kube/kind-synkra.yaml
  ./gradlew :plugins:kyc-co:jar :plugins:kyc-pe:assemble; echo "jars=$?"
  make cluster-delete; echo "borrar=$?"
  make install; echo "install=$?"
  make verificar-limite; echo "limite=$?"
  time ./scripts/e2e-api.sh; echo "e2e=$?"
  kubectl --kubeconfig "$KC" get deploy,cm,svc,pods -n synkra-co -o wide
  kubectl --kubeconfig "$KC" get deploy synkra-core -n synkra-co -o jsonpath='{range .metadata.managedFields[*]}{.manager} {.operation}{"\n"}{end}'
  kubectl --kubeconfig "$KC" get deploy -n synkra-co -o jsonpath='{..image}'; echo
  kubectl --kubeconfig "$KC" get --raw /api/v1/namespaces/synkra-co/services/synkra-core:http/proxy/plugins; echo
  curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt https://synkra.local/api/paises/co/despliegues | jq -c '[.[] | {id, tipo, origenId, estado, creadoPor, confirmadoPor}]'
  curl -s --resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt https://synkra.local/api/paises | jq -c '.[] | select(.pais=="co") | {concuerda, ultimoDespliegueId, plugins}'
  kubectl --kubeconfig "$KC" get deploy,cm,svc -n synkra-pe --no-headers 2>&1 | wc -l
  ```
  Esperado: `jars=0`, `borrar=0`, `install=0`, `limite=0` y `e2e=0` con el tiempo real; la salida completa del script en `ca3.txt`, con todas las líneas `ok`, `OK CU1` a `OK CU5`, `CU4 imagenes antes=… despues=…` con los dos lados iguales, las diez líneas `4xx` de la decisión 6 en su orden y `OK e2e: CU1 a CU5, 10 respuestas 4xx con codigo, mensaje y detalles`; el Deployment `synkra-core` `1/1` y su pod `Running`; una línea `synkra Apply` entre los `managedFields` (C-75: server-side apply contra un servidor real); la imagen registrada del core y la del init; los plugins de D3 cargados; el historial con `D5 reversion` (`origenId` D3) `listo`, `D4 despliegue fallido`, `D3`, `D2` `listo`, todos de `e2e`; `concuerda: true` con `ultimoDespliegueId` de D5; `0` en `synkra-pe` (nada se desplegó fuera de `synkra-co`).

- [ ] **CA-4**: la segunda corrida, sobre la misma instalación, también termina con 0
  ```bash
  ./scripts/e2e-api.sh; echo "e2e-2=$?"
  ```
  Esperado: `e2e-2=0`; en CU1 aparecen los 409 de lo que ya estaba (y siguen las líneas `4xx` 1 y 3); el `cambios` de CU2 dice que `kyc-pe` 1.3.0 se quita; las diez líneas `4xx` y `OK e2e: …`. Se pega la salida completa.

- [ ] **CA-5**: rojo primero. Sin el arreglo, el script destapa que el core no cargó los plugins nuevos
  El primer commit de la rama trae **solo** `scripts/e2e-api.sh` y las pruebas nuevas de `ConstruirObjetosPaisTest` (sin tocar `ConstructorObjetosPais.java` ni `pais-pe-esperado.yaml`); la bitácora pega su sha y su `git show --stat`. Con el código de ese commit:
  ```bash
  ./gradlew :backend:test --rerun --tests 'io.synkra.kubernetes.ConstruirObjetosPaisTest'; echo "gradle=$?"
  make cluster-delete && make install > /dev/null 2>&1; echo "install=$?"
  ./scripts/e2e-api.sh; echo "e2e=$?"
  ```
  Esperado: `gradle=` distinto de 0, con las tres pruebas nuevas en rojo (se pegan sus nombres y el mensaje de la primera); `install=0`; el script pasa CU1 y CU2 y cae en CU3 con `FALLA CU3: …` que muestra que la plantilla no cambió o que el core sigue cargando `[kyc-co 1.0.0]` mientras el plan confirmó `[kyc-co 1.0.0, kyc-pe 1.3.0]`, y `e2e=1`. Después del commit del arreglo, CA-3 se corre en un clúster nuevo (decisión 8).

- [ ] **CA-6**: cada comprobación del script muerde (en copias, contra el clúster real)
  ```bash
  W=$PWD; KC=$W/.kube/kind-synkra.yaml
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q)
  cp -r .tls "$D/"; ln -s "$W/plugins/kyc-co/build" "$D/plugins/kyc-co/build"; ln -s "$W/plugins/kyc-pe/build" "$D/plugins/kyc-pe/build"; mkdir -p "$D/.kube"; ln -s "$KC" "$D/.kube/kind-synkra.yaml"
  # Mutación anotada literalmente en la bitácora; una por copia:
  #   cu4-llega:       justo después del 422 de CU4, una línea que hace `kubectl --kubeconfig "$KC" -n synkra-co annotate configmap synkra-core-config synkra.io/e2e-mutacion=1 --overwrite` (y su `trap` la quita con `synkra.io/e2e-mutacion-`, registrado antes)
  #   cuerpo-ajeno:    CU1 gana una petición esperada `GET /noexiste` con 404 (la contesta NGINX con HTML, sin el formato único)
  #   sin-vuelta:      CU5 no confirma el plan de reversión
  #   codigo-otro:     la petición 7 de la decisión 6 espera el código OTRO_CODIGO en vez de COMBINACION_RECHAZADA
  sed -i '<mutación>' "$D/scripts/e2e-api.sh"
  (cd "$D" && KUBECONFIG_SYNKRA="$KC" bash scripts/e2e-api.sh); echo "salida=$?"
  rm -rf "$D"
  ./scripts/e2e-api.sh; echo "restaurado=$?"
  ```
  Esperado: `cu4-llega` → `FALLA CU4: …` que nombra el `resourceVersion` del ConfigMap y `salida=1` (y la anotación ya no está: `kubectl get cm synkra-core-config -n synkra-co -o jsonpath='{.metadata.annotations}'` se pega); `cuerpo-ajeno` → `FALLA formato: GET /noexiste 404` y `salida=1` (el chequeo de formato rechaza un cuerpo que no es el único); `sin-vuelta` → `FALLA CU5: …` (el despliegue no llega a `listo` o la vista general no concuerda) y `salida=1`; `codigo-otro` → `FALLA CU4: …` que muestra `COMBINACION_RECHAZADA` frente a `OTRO_CODIGO` y `salida=1`, sin que CU4 cambie nada en el clúster. Después de cada mutación, `restaurado=0` (el script real vuelve a dejar `synkra-co` en un `listo` concordante). La bitácora pega cada mutación y la línea que cayó. Sin `git stash` ni cambios en el worktree.

- [ ] **CA-7**: el script falla con mensaje claro antes de tocar nada
  ```bash
  D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
  git archive HEAD | tar -x -C "$D"
  (cd "$D" && bash scripts/e2e-api.sh); echo "sin-jars=$?"
  KUBECONFIG_SYNKRA="$D/no-existe.yaml" ./scripts/e2e-api.sh; echo "sin-kubeconfig=$?"
  grep -n 'command -v' scripts/e2e-api.sh | head -3
  ```
  Esperado: `FALLA preparacion: falta plugins/kyc-co/build/libs/kyc-co-1.0.0.jar (corre ./gradlew …)` y `sin-jars=1`; la línea de la guardia (`falta el kubeconfig …`), `FALLA guardia` y `sin-kubeconfig=1`; la comprobación de herramientas (decisión 4) antes de los JAR. En los dos casos no hubo ninguna petición a la API ni a Kubernetes (el historial de `co` tiene el mismo largo antes y después: se pega).

- [ ] **CA-8**: el diff queda acotado
  Guardar como `bitacoras/U08-D-evidencia/ca8.sh`; `ca8.txt` se genera justo antes de su propio commit, con el árbol limpio:
  ```bash
  git rev-parse --short HEAD; git status --porcelain | wc -l
  git diff --stat "<punta VERDE de U08-C>"...HEAD
  git diff --name-only "<punta VERDE de U08-C>"...HEAD | grep -v -E '^(scripts/e2e-api\.sh|backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais\.java|backend/src/test/java/io/synkra/kubernetes/ConstruirObjetosPaisTest\.java|backend/src/test/resources/kubernetes/pais-pe-esperado\.yaml|bitacoras/U08-D\.md|bitacoras/U08-D-evidencia/.*|revisiones/U08-D/.*)$'; echo "fuera=$?"
  git diff --name-only "<punta VERDE de U08-C>"...HEAD -- Makefile deploy scripts/guardia-contexto.sh scripts/registro-local.sh scripts/imagenes.sh scripts/desplegar.sh scripts/secreto-tls.sh scripts/verificar-limite.sh | wc -l
  git ls-files .kube .tls | wc -l
  ```
  Esperado: `fuera=1` (solo esas rutas); `0`; `0`. El sha impreso es el del padre del commit de `ca8.txt`.

---

**Rojo primero:** `scripts/e2e-api.sh` y las pruebas nuevas de `ConstruirObjetosPaisTest` se commitean **antes** que el arreglo de `ConstructorObjetosPais` (CA-5: ese commit no trae la implementación, y el script cae en CU3 sobre un clúster instalado con ese código). Después del arreglo se instala en un `kind` nuevo y CA-3 a CA-7 se corren con el código final. Al cerrar, `synkra-co` queda en `D5` `listo` para U08-E (decisión 10).
