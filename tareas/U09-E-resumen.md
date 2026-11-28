# U09-E: recorrido de la demo por el front y resumen de la unidad U09

**Unidad:** U09: `Front`
**Tarea del plan que agrupa:** U09-T09
**Historias que implementa:** H-25
**Revisor:** `sonnet`
**Depende de:** U09-A a U09-D (VERDES y fusionadas) y U08-D (fusionada: trae `scripts/e2e-api.sh`, que deja el catálogo de la demo). Sale de `<main con U09-D>` (rama `m6r/U09-E`, worktree `../wt-m6r-U09-E`). Usa el `kind-synkra` que dejó CA-7 de U09-D, con el kubeconfig del proyecto; si no está arriba, se instala con `make install` (decisión 3). Nadie más usa el clúster mientras corre el recorrido.

---

## Alcance

**Dentro** (una línea, concreta):

> El recorrido de CU2, CU4 y CU5 hecho **en el front**, en `https://synkra.local`, en el país `pe`, paso por paso, con la parte del log del front que muestra cada llamada a la API y la lectura de vuelta de la API (y de `kubectl` en CU4) después de cada paso; la salida fresca de `dotnet test front.Tests`; y `aidlc-docs/construction/U09-front/code/summary.md` con lo que construyó la unidad, sus desviaciones, la trazabilidad, las tareas y las candidatas, con cada cifra atada a su fuente.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cualquier cambio de código, de pruebas, de `deploy/`, del `Makefile` o de los scripts de U08 y U09-D. Si algo falla en el recorrido o en las pruebas, se para y se reporta; no se arregla.
- Todo clúster que no sea `kind-synkra`; `~/.kube/config`; `gitops2`, `m08-gitops1` y sus contenedores (ninguna orden los lista ni los toca). `sudo` y `kubectl port-forward` (decisión 2: la resolución de `synkra.local` en el navegador la pone el responsable del proyecto, no el agente).
- `synkra-co`: el recorrido del front es en `pe`. `co` es el país de `scripts/e2e-api.sh` (U08-D); el front no despliega en él.
- La subida de JAR por el front (CU1): el catálogo de la demo lo deja `scripts/e2e-api.sh`. La pantalla «Catálogo» se mira, no se usa para subir.
- Tocar `CANDIDATAS.md`, el plan de U09, `unidades-y-tareas.md`, `aidlc-docs/aidlc-state.md` o `aidlc-docs/audit.md`; cerrar o reabrir candidatas.
- Inventar lo que no está en los archivos: las cifras de las rondas salen de `revisiones/` y `bitacoras/`; las del recorrido, de `evidencia/`.

---

## Archivos de contexto

- Plantillas de resumen: `aidlc-docs/construction/U07-despliegue/code/summary.md` y `aidlc-docs/construction/U06-registro/code/summary.md` (formato, `evidencia/`, «Cifras y su fuente»), y las tareas que los pidieron: `tareas/U06-D-resumen.md` y `tareas/U07-G-fronteras-resumen.md`. Si `main` ya tiene el de U08 (`aidlc-docs/construction/U08-instalacion-limite/code/summary.md`), también.
- `aidlc-docs/construction/plans/U09-front-code-generation-plan.md` y `unidades-y-tareas.md` (sección U09); `Modulo 3/specs/prd.md` §9.4, CU2, CU4 y CU5, y «Sesión 16: qué se demuestra».
- `tareas/U09-A-proyecto-cliente-vista.md`, `tareas/U09-B-catalogo-detalle.md`, `tareas/U09-C-desplegar.md`, `tareas/U09-D-imagen-manifiesto.md`.
- `bitacoras/U09-A.md` a `bitacoras/U09-D.md` y sus carpetas `bitacoras/U09-*-evidencia/`; `revisiones/U09-A/` a `revisiones/U09-D/` (todas las rondas y respuestas).
- `scripts/e2e-api.sh` (qué deja en el catálogo y en `co`) y `tareas/U08-D-e2e-api.md` (decisiones 4 y 5: la versión «mentirosa» `3.0.0` y el fallo de CU5).
- **Candidatas:** léelas de `/home/casdav/Projects/Master/wt-orq-U04/CANDIDATAS.md` (la del orquestador, solo lectura) y de `CANDIDATAS.md` de `main`. No las copies al repositorio.

---

## Decisiones del orquestador

1. **Tres fases, en este orden.**
   - **Fase 1 (codificador):** escribe y commitea `bitacoras/U09-E-evidencia/paso.sh` (decisión 4) y `bitacoras/U09-E-evidencia/recorrido.md`, la lista de los pasos de la decisión 5 con lo que se hace en la pantalla y lo que se espera ver; corre la preparación (decisión 3) y el paso 0.
   - **Fase 2 (recorrido):** una persona con navegador (el responsable del proyecto, o el orquestador con el navegador integrado) hace cada paso de `recorrido.md` en `https://synkra.local` y, al terminar cada uno, corre `bash bitacoras/U09-E-evidencia/paso.sh <n> <nombre>` desde la raíz del worktree. Quien hace el recorrido no cambia nada del repositorio salvo lo que escribe `paso.sh`. Si la pantalla no muestra lo esperado, se para ahí y se anota el paso en la bitácora.
   - **Fase 3 (codificador):** con los archivos de la fase 2, la corrida fresca de las pruebas (decisión 7) y las fuentes, escribe `summary.md`.
2. **El navegador y `synkra.local`.** El agente no usa `sudo` ni toca `/etc/hosts`. Antes de la fase 2, el responsable del proyecto deja `synkra.local` resolviendo a `127.0.0.1` en su máquina (o abre el navegador con una regla de resolución equivalente) y acepta o instala `.tls/tls.crt` del worktree. Se anota en la bitácora cómo se hizo, sin pegar la clave. Si nadie puede hacerlo, la fase 2 no se hace y la tarea se para (el corte R8 del plan es la demo por `./scripts/e2e-api.sh`, que ya existe; se reporta y lo decide el orquestador).
3. **Preparación** (fase 1; todo `kubectl` con `--kubeconfig "$PWD/.kube/kind-synkra.yaml"`, todo `curl` con `--resolve synkra.local:443:127.0.0.1 --cacert .tls/tls.crt`):
   - El clúster: `make status` y `curl … https://synkra.local/actuator/health` (`UP`); el front `1/1` en `synkra`. Si no está, `make install` con `JAVA_HOME` como en U08-C, y se pega su salida.
   - El catálogo de la demo: `./scripts/e2e-api.sh` una vez (sube, o encuentra ya subidos, `synkra-core` `VC` y `3.0.0`, `kyc-co` `1.0.0`, `kyc-pe` `1.3.0` y `2.0.0`, y deja `co` en un `listo` concordante); se pega su última línea `OK e2e: …`.
   - El punto de partida de `pe`: `GET /api/paises/pe/despliegues` y `GET /api/paises` (lo que diga `pe` se pega tal cual; el recorrido no supone que esté vacío, y su `cambios` de CU2 dice lo que había).
   - `VC` es la versión del core de `core/build.gradle.kts` (no se escribe a mano en `recorrido.md`: se lee y se anota).
4. **`paso.sh <n> <nombre>`** (`bash`, `set -euo pipefail`; solo lee; `<n>` con dos cifras, `00` a `11`, y `<nombre>` el de la tabla de la decisión 5): escribe `aidlc-docs/construction/U09-front/code/evidencia/recorrido/paso-<n>-<nombre>.txt` con:
   - fecha (UTC), sha de `HEAD`, `<n>` y `<nombre>`;
   - **el log de la API visto desde el front**: las líneas del log de `synkra-front` desde el paso anterior (`kubectl logs -n synkra deploy/synkra-front --since-time=<marca del paso anterior>`), filtradas a las de `System.Net.Http.HttpClient.ApiSynkra` (una petición `Sending HTTP request <MÉTODO> <URL>` y su respuesta `Received HTTP response … <código>`), y debajo, la lista corta `MÉTODO ruta código` que sale de ellas; la marca de tiempo se guarda en `aidlc-docs/construction/U09-front/code/evidencia/recorrido/.marca` (se versiona al final);
   - la lectura de vuelta de la API: `GET /api/paises` (con `jq -c '[.[] | {pais, lectura, concuerda, ultimoDespliegueId}]'`) y `GET /api/paises/pe/despliegues` (con `jq -c '[.[] | {id, tipo, origenId, estado, versionCore, plugins: [.plugins[] | .nombre + " " + .version], creadoPor, confirmadoPor}]'`);
   - de `kubectl`: la imagen y los pods del país (`kubectl -n synkra-pe get deploy -o jsonpath='{..image}'`, `get pods -o wide`) y el `resourceVersion` del Deployment `synkra-core` y del ConfigMap `synkra-core-config` de `synkra-pe` (o `no hay` si no existen todavía).
   Si una orden falla, escribe `FALLA paso <n>: <orden> salió con <código>` en el archivo y sale con 1. Las rutas con consulta pueden salir redactadas en el log del front (`?*`, .NET 10); se anota, no se cambia la configuración de logs.
5. **Los pasos del recorrido** (todos en `pe`; usuario `ana` para preparar y `luis` para confirmar, escritos en el campo «Usuario» de la pantalla; las ids son las que da la API, nunca números fijos):
   | n | Nombre | En el front | Se espera en la pantalla | Llamadas esperadas en el log del front |
   |---|---|---|---|---|
   | 0 | inicio | (ninguna; corre la fase 1) | — | — |
   | 1 | vista | abrir `/` | un renglón por país; `co` `Leído`, `Sí` | `GET /api/paises` |
   | 2 | catalogo | abrir `Catálogo` | `synkra-core` con `VC` y `3.0.0`; `kyc-pe` `1.3.0` (`2.x`) y `2.0.0` (`3.x`) | `GET /api/cores`, un `GET /api/cores/<core>/plugins` por core |
   | 3 | cu2-verifica | `Desplegar`: país `pe`, core `synkra-core`, versión `VC`, filtro `pais=pe`, marcar `kyc-pe 1.3.0` | veredicto `APROBADA`, «Preparar plan» habilitado | `GET /api/cores/synkra-core/plugins?pais=pe`, `POST /api/verificaciones` (una por cambio) |
   | 4 | cu2-plan | usuario `ana`, «Preparar plan» | el plan con `synkra-pe`, la imagen por digest y `kyc-pe 1.3.0` en añadidos | `POST /api/despliegues` `201` |
   | 5 | cu2-confirma | usuario `luis`, «Confirmar» | «Ver el despliegue #D1» | `POST /api/despliegues/<D1>/confirmacion` `202` |
   | 6 | cu2-listo | abrir el detalle de D1 y pulsar «Actualizar» hasta `Listo` | `Listo`; en «Vista general», `pe` `Leído`, `Sí`, `#D1` | `GET /api/despliegues/<D1>`, `GET /api/paises/pe/despliegues` |
   | 7 | cu4-rechazo | `Desplegar`: `pe`, `synkra-core`, `VC`, marcar `kyc-pe 2.0.0` (y desmarcar `1.3.0`) | `RECHAZADA`, el motivo `VERSION_INCOMPATIBLE: …` y las alternativas; «Preparar plan» y «Confirmar» deshabilitados | `POST /api/verificaciones` `200`, **ningún** `POST /api/despliegues` |
   | 8 | cu5-fallo | `Desplegar`: `pe`, `synkra-core`, `3.0.0`, `kyc-pe 2.0.0`; preparar (`ana`) y confirmar (`luis`) | `APROBADA` (el catálogo dice `3.0.0`), plan y confirmación de D2 | `POST /api/verificaciones`, `POST /api/despliegues` `201`, `POST …/<D2>/confirmacion` `202` |
   | 9 | cu5-fallido | detalle de D2, «Actualizar» hasta `Fallido` | `Fallido` y la sección de contexto con `logCore` que nombra `kyc-pe`; en «Vista general», `pe` con `No` en «Concuerda» y `#D1` | `GET /api/despliegues/<D2>`, `GET /api/paises/pe/despliegues` |
   | 10 | cu5-vuelta | en el historial, «Volver a este» en D1; usuario `ana`, «Preparar la vuelta»; usuario `luis`, «Confirmar la vuelta» | el plan de `Reversión` con origen `#D1`; «Ver el despliegue #D3» | `GET /api/despliegues/<D1>`, `POST /api/despliegues/<D1>/reversion` `201`, `POST …/<D3>/confirmacion` `202` |
   | 11 | cu5-listo | detalle de D3, «Actualizar» hasta `Listo`; volver a «Vista general» | `Listo`; `pe` `Sí`, `#D3`; el historial termina en D3 `reversion` `listo`, D2 `fallido`, D1 `listo` | `GET /api/despliegues/<D3>`, `GET /api/paises/pe/despliegues`, `GET /api/paises` |
   Comprobaciones entre pasos (las hace el codificador en la fase 3, sobre los archivos): en el paso 7 la imagen y los dos `resourceVersion` de `synkra-pe` son los del paso 6 y el historial de `pe` tiene el mismo largo (CU4: nada llegó al clúster); en los pasos 3 a 11, cada `POST` del log tiene el `X-Synkra-Usuario` que dice el registro (`creadoPor` `ana`, `confirmadoPor` `luis`); el paso 9 tiene `concuerda` `false` y el 11 `true`. El front llama dos veces a algunas lecturas al abrir una página (el prerender y el circuito interactivo de Blazor Server): se anota y no se cuenta como fallo.
6. **Archivo y secciones.** `aidlc-docs/construction/U09-front/code/summary.md`, con estos encabezados de nivel 2, en este orden: `## Qué expone`, `## Cómo se prueba`, `## Recorrido de la demo`, `## Salidas reales`, `## Desviaciones del diseño`, `## Trazabilidad`, `## Tareas`, `## Candidatas abiertas`, `## Cifras y su fuente`. La cabecera (antes de la primera sección) dice la unidad, la historia, la fecha, el sha de las corridas (`git log -1 --format=%h -- front front.Tests deploy scripts`), la máquina (`uname -m`, la de David) y que esta tarea no tocó código.
   - **Qué expone:** los archivos de `front/` (salida de `git ls-files front | grep -v '^front/wwwroot/'`, pegada), las rutas de las pantallas (salida de `grep -rhn '@page' front/Components/Pages`), los métodos públicos de `ApiSynkra` por archivo `partial` (de `grep -n 'public Task' front/Api/ApiSynkra*.cs`), la imagen (`docker image inspect` de `localhost:5001/synkra-front:<versión>`: `RepoTags`, `Config.User`, `ExposedPorts`, `Entrypoint`) y los objetos del manifiesto (de `kubectl kustomize deploy/overlays/kind`, solo los de `synkra-front`: `kind`, `namespace` y `name`). Una tabla «Pantalla del PRD §9.4 → página → clase de prueba» (Vista general, Catálogo y subida, Desplegar, Detalle y vuelta atrás), con cada nombre de clase comprobado con `ls front.Tests`.
   - **Cómo se prueba:** bUnit con el `ApiSynkra` real sobre `ManejadorHttpSimulado` (sin servidor ni puertos), los filtros del plan por contenido del nombre (`CatalogoTest` toma también `ApiSynkraCatalogoTest` y `SubidaCatalogoTest`; `DetalleTest` toma `ReversionDetalleTest`), la prueba de arranque como proceso aparte, todo `dotnet` en `mcr.microsoft.com/dotnet/sdk:10.0.404` vía `scripts/dotnet-docker.sh`, las mutaciones en copias, y la imagen y el manifiesto con los verificadores de U09-D.
   - **Recorrido de la demo:** una subsección por paso de la decisión 5 con lo que se hizo, lo que mostró la pantalla (dicho por quien hizo el recorrido, citado de la bitácora), la lista `MÉTODO ruta código` y la lectura de vuelta, **pegadas tal cual** de su `paso-<n>-*.txt`; y al final las comprobaciones entre pasos de la decisión 5, cada una con su resultado y el par de archivos que la sostiene.
7. **Salidas reales.** Se corren en la fase 3, sobre la punta de la rama, y cada una se guarda completa (fecha, sha, comando y salida) en `aidlc-docs/construction/U09-front/code/evidencia/<nombre>.txt`:
   1. `scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=todo.trx" --results-directory front.Tests/TestResults; echo "salida=$?"` y `python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/todo.trx`, con el conteo por clase que sale de ese `.trx` (un guion `python3 -I` que cuenta `UnitTestResult` por la clase del `testName`; se versiona en la evidencia).
   2. Los tres filtros del plan (`CatalogoTest`, `DesplegarTest`, `DetalleTest`) y el `grep` de U09-T06 corrido después de compilar.
   3. `kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-front.py` y `bash scripts/verificar-imagenes.sh`.
   En el resumen se pega solo la línea `Passed!`, la primera línea de `trx.py`, el conteo por clase y las líneas `OK …`, copiadas tal cual y **sin elipsis `[...]`**; lo demás se cita por archivo. Las cifras de las revisiones (29, 64, 77, 112…) se citan en «Cifras y su fuente» y **no** se presentan como las de hoy: el resumen dice las de la corrida fresca; si no coinciden con las de U09-D (CA-1), se anota la diferencia con su causa y no se ajusta nada.
8. **Desviaciones.** Lista numerada; cada una con qué decía el plan o el diseño, qué se hizo, quién lo anotó (bitácora o revisión, con ruta) y si el revisor la aceptó. Como mínimo, todas ya anotadas en las bitácoras, tareas o revisiones:
   - U09-A: .NET 10 en vez de .NET 8 (el plan dice 8; .NET 8 salió de soporte).
   - U09-A: `HttpMessageHandler` simulado en vez de un servidor HTTP con puerto.
   - U09-A: «estado del último despliegue» de U09-T02 se lee como la lectura del clúster y la concordancia de `GET /api/paises`.
   - U09-A: `HistorialEscapaElPais` reforzada con `a/b?c` (C-133); los criterios enmendados de CA-1 (arquitectura, C-135) y CA-2 (uso en vez de texto, C-134), con su anotación en `aidlc-docs/audit.md`.
   - U09-A, F-04: el arranque de la aplicación se dejó para U09-C, que lo probó como proceso, y U09-D en la imagen.
   - U09-B y U09-C: la interpretación de U09-T06 con los campos `coreCompatible` y `compatible` de la API.
   - U09-B y U09-C: el reparto en paralelo con tres archivos comunes idénticos; la subida de JAR (U09-T03) y la vuelta atrás (U09-T05) en U09-C, y sus criterios del plan cumplidos en la unión (CA-1 de U09-D); el enlace a «Desplegar» antes de que existiera la página.
   - U09-C: la interpretación de «la reversión llama a la API solo después de confirmar»; el tope de 25 MB de la subida; el handler simulado ampliado.
   - U09-D: la restauración con red de `Microsoft.AspNetCore.App.Internal.Assets`; `kubeconform` sustituido; las sondas sobre `/app.css`; las claves de Data Protection en memoria; el cambio de `desplegar.sh` (cada Deployment por nombre).
   Si en las bitácoras o revisiones hay otra, se añade con su fuente; ninguna de arriba se omite.
9. **Trazabilidad.** Tabla `Historia | Tareas del plan | Tarea del loop que la entregó`: H-25 (T01 a T06 y T09), H-28 (contribuye: T07), H-26 (contribuye: T08), como en el plan, con la tarea U09-A a U09-E de cada una (T03 y T05 en U09-B y U09-C).
10. **Tareas.** Tabla de U09-A a U09-D con qué construyó cada una, las tareas del plan que cubre, el PR de fusión (de `git log --merges --oneline` y `gh pr list --state merged`; no se inventan) y las rondas (los `ronda-N.md` de `revisiones/U09-*/`, contados con `ls`, sin los `-respuesta`), con el veredicto de cada ronda copiado de su informe.
11. **Candidatas abiertas.** Se sacan con `grep`, no a mano: las filas de las dos `CANDIDATAS.md` cuyo «Salió de» nombre `U09`, con su estado y a quién van (al menos C-133, C-134 y C-135, y las que hayan abierto U09-B a U09-D). Las filas que salen de la del orquestador llevan su ruta absoluta.
12. **Cifras y su fuente.** La última sección es una tabla `| cifra | ruta | cadena |`, una fila por cada número de pruebas, mutaciones, rondas, objetos o tiempos que el resumen cite fuera de los bloques pegados: `ruta` entre acentos graves, relativa a la raíz, a un archivo de `revisiones/`, `bitacoras/` o `evidencia/`; `cadena` entre acentos graves, un texto literal de ese archivo que contiene la cifra (sin `|`). Si la revisión y la bitácora dan cifras distintas para lo mismo, se citan las dos y no se reconcilian.
13. **Formato y evidencia.** Español, sin emojis, sin código nuevo (salvo `paso.sh` y el guion de conteo, que son evidencia). Bitácora en `bitacoras/U09-E.md` (con quién hizo la fase 2, cómo resolvió `synkra.local` y lo que vio en cada paso), evidencia en `bitacoras/U09-E-evidencia/` y en `aidlc-docs/construction/U09-front/code/evidencia/`. Si pones un diagrama de Mermaid, se valida (`common/content-validation.md`); no hace falta ninguno.

---

## Criterios de aceptación

- [ ] **CA-1**: el archivo existe con las secciones, en orden
  ```bash
  f=aidlc-docs/construction/U09-front/code/summary.md
  test -f "$f" && echo existe
  grep -n '^## ' "$f"
  for s in 'Qué expone' 'Cómo se prueba' 'Recorrido de la demo' 'Salidas reales' 'Desviaciones del diseño' 'Trazabilidad' 'Tareas' 'Candidatas abiertas' 'Cifras y su fuente'; do printf '%s ' "$s"; grep -c "^## $s\$" "$f"; done
  for h in H-25 H-26 H-28; do printf '%s ' $h; grep -c "$h" "$f"; done
  for c in C-133 C-134 C-135; do printf '%s ' $c; grep -c "$c" "$f"; done
  for t in U09-T01 U09-T02 U09-T03 U09-T04 U09-T05 U09-T06 U09-T07 U09-T08 U09-T09; do printf '%s ' $t; grep -c "$t" "$f"; done
  ```
  Esperado: `existe`; los nueve encabezados en ese orden; cada sección `1`; cada historia, candidata y tarea del plan al menos `1`. En «Desviaciones del diseño» están, buscables con `grep -n -i`, `.NET 10`, `HttpMessageHandler`, `coreCompatible`, `reversión`, `Internal.Assets` y `kubeconform`.

- [ ] **CA-2**: el recorrido está entero, paso por paso, y coincide con la API
  Guardar como `bitacoras/U09-E-evidencia/ca2.sh`:
  ```bash
  d=aidlc-docs/construction/U09-front/code/evidencia/recorrido
  ls "$d" | grep -c '^paso-'
  grep -l '^FALLA' "$d"/paso-*.txt; echo "fallas=$?"
  head -3 "$d"/paso-*.txt
  grep -h -E '^(GET|POST) ' "$d"/paso-07-*.txt | grep -c '^POST /api/despliegues'
  for n in 06 07; do grep -h -A1 'resourceVersion' "$d"/paso-$n-*.txt; done
  grep -h -E '^POST /api/despliegues/[0-9]+/(confirmacion|reversion) ' "$d"/paso-*.txt
  python3 -I - "$d" <<'EOF'
  import glob, json, re, sys
  ult = sorted(glob.glob(sys.argv[1] + "/paso-11-*.txt"))[0]
  texto = open(ult, encoding="utf-8").read()
  hist = json.loads(re.findall(r"^\[\{\"id\".*$", texto, re.M)[-1])
  print([(h["tipo"], h["estado"], h["creadoPor"], h["confirmadoPor"]) for h in hist[:3]])
  EOF
  ```
  Esperado: `12` (pasos 0 a 11); `fallas=1`; cada archivo con fecha, sha y su número; `0` (en CU4 el front no pidió ningún plan); las dos lecturas de `resourceVersion` del paso 6 y del 7 iguales (CU4: nada llegó al clúster); tres confirmaciones (`202`) y una reversión (`201`); `[('reversion', 'listo', 'ana', 'luis'), ('despliegue', 'fallido', 'ana', 'luis'), ('despliegue', 'listo', 'ana', 'luis')]`. El revisor lee la sección «Recorrido de la demo» contra los archivos y contra la tabla de la decisión 5: cada paso tiene sus llamadas esperadas en el log, y la pantalla que se describe es la que corresponde a la API de ese paso.

- [ ] **CA-3**: las salidas reales son frescas y de la punta
  ```bash
  rm -rf front.Tests/TestResults
  scripts/dotnet-docker.sh test front.Tests --logger "trx;LogFileName=todo.trx" --results-directory front.Tests/TestResults; echo "salida=$?"
  python3 -I bitacoras/U09-A-evidencia/trx.py front.Tests/TestResults/todo.trx | head -1
  f=aidlc-docs/construction/U09-front/code/summary.md
  grep -c 'Passed!' "$f"; grep -c 'salida=0' "$f"
  ls aidlc-docs/construction/U09-front/code/evidencia/
  head -3 aidlc-docs/construction/U09-front/code/evidencia/*.txt
  ```
  Esperado: `Passed!` con `Failed: 0` y `Skipped: 0`, `salida=0`, y `trx.py` con `passed` = `total`; el resumen contiene al menos una línea `Passed!` y un `salida=0`; cada archivo de `evidencia/` trae fecha, sha y comando en sus primeras líneas, con el sha de la punta (o de su padre, con la nota «tomada justo antes de su propio commit»). El revisor reproduce el mismo total y el mismo conteo por clase.

- [ ] **CA-4**: toda cifra citada sale de un archivo real
  Guardar como `bitacoras/U09-E-evidencia/ca4.sh` y correrlo desde la raíz:
  ```bash
  f=aidlc-docs/construction/U09-front/code/summary.md
  sed -n '/^## Cifras y su fuente/,$p' "$f" | grep '^| ' | tail -n +3 | while IFS='|' read -r _ c r s _; do
    r=$(echo "$r" | tr -d ' `'); s=$(echo "$s" | sed 's/^ *`//; s/` *$//')
    if grep -qF -- "$s" "$r"; then echo "ok $r"; else echo "FALTA $r :: $s"; fi
  done | sort | uniq -c
  ```
  Esperado: ninguna línea `FALTA`, y al menos una fila `ok` de cada una de `revisiones/U09-A/`, `revisiones/U09-B/`, `revisiones/U09-C/` y `revisiones/U09-D/`. Para cada número del texto que no está en la tabla ni en un bloque pegado, el revisor lo busca con `grep -rnF '<cifra>' revisiones/U09-* bitacoras/U09-* aidlc-docs/construction/U09-front/code/evidencia`; si no aparece, es hallazgo.

- [ ] **CA-5**: toda salida pegada es copia literal de su archivo
  ```bash
  d=aidlc-docs/construction/U09-front/code
  awk '/^```/{b=!b;next} b' "$d/summary.md" | grep -v '^\$ ' | while IFS= read -r l; do
    [ -z "$l" ] && continue
    grep -rqxF -- "$l" "$d/evidencia" bitacoras/U09-A-evidencia bitacoras/U09-B-evidencia bitacoras/U09-C-evidencia bitacoras/U09-D-evidencia bitacoras/U09-E-evidencia revisiones/U09-A revisiones/U09-B revisiones/U09-C revisiones/U09-D || echo "NO LITERAL: $l"
  done
  grep -c '\[\.\.\.\]' "$d/summary.md"
  ```
  Esperado: el bucle no imprime nada y el último `grep -c` da `0`. Las líneas `$ comando` quedan fuera del bucle; su comando se compara a mano con el de la primera línea del archivo de `evidencia/` correspondiente.

- [ ] **CA-6**: nada de código cambió
  ```bash
  B=$(git merge-base HEAD main)
  git diff --stat "$B"...HEAD -- front front.Tests backend core verificador plugin-api plugins init-descarga deploy scripts Makefile gradle build.gradle.kts settings.gradle.kts | tail -1
  ```
  Esperado: no imprime nada.

- [ ] **CA-7**: el diff queda acotado
  Guardar como `bitacoras/U09-E-evidencia/ca7.sh`; `ca7.txt` se genera justo antes de su propio commit, con el árbol limpio:
  ```bash
  B=$(git merge-base HEAD main)
  git rev-parse --short HEAD; git status --porcelain | wc -l
  git diff --name-only "$B"...HEAD | grep -v -E '^(aidlc-docs/construction/U09-front/code/|bitacoras/U09-E\.md$|bitacoras/U09-E-evidencia/|revisiones/U09-E/)'; echo "fuera=$?"
  git ls-files .kube .tls | wc -l
  ```
  Esperado: `fuera=1` (solo esas rutas) y `0` (ni kubeconfig ni certificado en git). El sha impreso es el del padre del commit de `ca7.txt`. Si hace falta evidencia que no cae en esas rutas, se para y se pregunta.

---

**Al terminar:** `kind-synkra` queda arriba con `pe` en D3 (`reversion`, `listo`) y `co` como lo dejó `scripts/e2e-api.sh`. El resumen no se presenta como cerrado hasta que el revisor (`sonnet`) dé VERDE; el orquestador cierra la unidad.
