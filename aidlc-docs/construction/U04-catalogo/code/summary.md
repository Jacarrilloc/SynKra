# U04 Catálogo: resumen de la unidad

Unidad: U04 (`backend/`, paquete `io.synkra.catalogo`, Spring Boot, SQLite). Historias: H-01 a H-06 y H-15 (según `tareas/U04-T12-resumen.md`).
Este resumen lo escribe U04-T12 sin cambiar código, pruebas, build ni scripts. Todas las salidas de abajo se produjeron en esta tarea, con `--rerun` y un `SYNKRA_DATA` temporal; el backend se arrancó en el puerto 18080 y se liberó al terminar.

## Qué expone

- Rutas HTTP (de las tareas T03, T07, T08 y T10): `POST /api/cores`, `GET /api/cores`, `GET /api/cores/{nombre}`, `POST /api/cores/{core}/plugins` (multipart, parte `archivo` y parte opcional `etiquetas`), `GET /api/cores/{core}/plugins?clave=valor&…`, `GET /api/cores/{core}/plugins/{plugin}/{version}` y `GET /api/artefactos/{sha256}`.
- API de módulo (`io.synkra.catalogo.api`, el único paquete público, atado por `FronterasModulosTest` e `InterfazCatalogoTest`, T11): `CatalogoApi` con `registrarCore`, `obtenerCore`, `subirPlugin`, `obtenerPlugin`, `listarPlugins` y `abrirArtefacto`, y la implementación de `io.synkra.verificador.Catalogo` sobre SQLite (T09) para que U07 la use.
- Persistencia: migración `V2__catalogo.sql` con `cat_core`, `cat_version_core`, `cat_plugin` y `cat_etiqueta` (T01); los PRAGMA de C-04 salen de `BaseDeDatos`.
- Configuración: `synkra.max-artefacto` (T04, 20MB por defecto) y `synkra.registro-permitido` (T13, vacío por defecto).

## Tareas

| Tarea | Qué construyó | PR | Rondas de revisión |
|---|---|---|---|
| U04-T01 | Migración `V2__catalogo.sql`, repositorios de Spring Data JDBC y PRAGMA de C-04 | #41 | 3 |
| U04-T02 | `CatalogoApi.registrarCore` y `obtenerCore` (R-C1 y R-C2); `backend` depende de `:verificador` | #43 | 2 |
| U04-T03 | Rutas de cores; traducción de `@Valid` a `detalles` (cierra C-07) | #45 | 3 |
| U04-T04 | `RecepcionArtefacto`: límite, SHA-256 al escribir, revisión de entradas del JAR, nada en disco si falla | #47 | 3 |
| U04-T05 | `RutasSegurasPropiedades` (jqwik, PBT-03): ninguna entrada aceptada sale del destino | #52 | 3 |
| U04-T06 | `subirPlugin` y `obtenerPlugin` (R-C4, R-C7 a R-C11; duplicados de C-44) | #49 | 2 |
| U04-T07 | Rutas de subida y consulta de plugins; topes de C-38 y C-50 | #54 | 2 |
| U04-T08 | `listarPlugins` y el filtro por etiquetas (R-C12); la ruta pasa a `RutasPlugins` | #56 | 3 |
| U04-T09 | `Catalogo` de U01 sobre SQLite, probado con S1 a S7 de `SuiteCombinaciones` | #58 | 2 |
| U04-T10 | `abrirArtefacto` y `GET /api/artefactos/{sha256}` (R-C13) | #60 | 2 |
| U04-T11 | `FronterasModulosTest` e `InterfazCatalogoTest`; cierra C-14 y C-36 | #64 | 3 |
| U04-T12 | Este resumen | este PR (U04-T12) | (pendiente) |
| U04-T13 | `synkra.registro-permitido` (R-C3) y los bordes de imagen de C-33 | #62 | 1 |

Los PR salen de `git log --merges` (merges de `m6r/U04-T*`). Las rondas son los informes `revisiones/U04-T<NN>/ronda-N.md` (sin contar `ronda-N-respuesta.md`), contados con `ls`. Los PR #55, #57, #59, #61, #63 y #65 son de la rama del orquestador (`orq/U04-david`), no de tareas.

## Decisiones de la unidad

Cada una sale de la sección «Decisiones del orquestador» de la tarea citada.

- **R-C2 y R-C9 (T02, T06):** una versión repetida da 409 `CONFLICTO` con el detalle `version`, sin cambiar nada en base ni en disco, aunque el hash sea otro.
- **R-C1 (T02):** se devuelven todos los detalles inválidos en un solo 422; las versiones se ordenan como `Version`, no como texto.
- **Fronteras (T02, T11):** solo `catalogo.api` es público; `io.synkra.verificador` queda como módulo compartido (`@Modulithic(sharedModules = "verificador")`, cierra C-36).
- **Recepción (T04, T05):** límite `synkra.max-artefacto` (413); cada entrada del JAR se revisa y, en todo rechazo, `artefactos/` queda sin archivos; la propiedad de T05 usa un oráculo independiente (`resolve().normalize().startsWith()`), con 1000 intentos.
- **Subida (T06, T07):** orden del diagrama de `business-logic-model.md`, el primer fallo corta; entradas duplicadas del JAR dan 422 `RUTA_NO_PERMITIDA` (C-44); etiquetas del descriptor (`origen = 'descriptor'`) y de la petición (`origen = 'subida'`) se unen; tope de 64 KB al JSON y a la parte `etiquetas` (C-38) y de 63 caracteres por valor de etiqueta (C-50).
- **Listado (T08):** una sola consulta con parámetros enlazados; orden por `nombre` y luego por versión semántica; una carpeta nunca ve plugins de otra.
- **Verificación (T09):** el `Catalogo` nunca devuelve `null` y solo las etiquetas con `origen = 'descriptor'` vuelven al `Descriptor`; la prueba de equivalencia corre S1 a S7.
- **Artefactos (T10):** el hash válido son 64 hexadecimales en minúsculas, y todo lo demás es 404; el archivo se sirve en flujo; no se recalcula el hash al servir (C-64 lo deja al init container).
- **Registro (T13):** `synkra.registro-permitido` se compara exacto, sin distinción de mayúsculas en el host; un valor inválido hace fallar el arranque; primero la forma de la imagen y después el registro.
- **C-14 (T11):** los fixtures de `plataforma.prueba` pasaron a `fixtures.plataforma`, fuera de `io.synkra`.

## CA-1: pruebas del catálogo

```
$ ./gradlew :backend:test --rerun --tests 'io.synkra.catalogo.*' 2>&1 | tail -6

For more on this, please refer to https://docs.gradle.org/9.9.2/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 14s
8 actionable tasks: 8 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.9.2/userguide/configuration_cache_enabling.html
```

Conteo por clase, de los XML de `backend/build/test-results/test/` (los 13 que dejó esa corrida):

```
$ for f in backend/build/test-results/test/TEST-io.synkra.catalogo*.xml; do echo "$(basename $f .xml | sed 's/TEST-io.synkra.//') $(grep -o -m1 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' $f)"; done
catalogo.ArtefactosPorHashTest tests="10" skipped="0" failures="0" errors="0"
catalogo.CatalogoRepositoriosTest tests="19" skipped="0" failures="0" errors="0"
catalogo.CatalogoVerificadorTest tests="14" skipped="0" failures="0" errors="0"
catalogo.CoreRegistradoTest tests="4" skipped="0" failures="0" errors="0"
catalogo.InterfazCatalogoTest tests="6" skipped="0" failures="0" errors="0"
catalogo.ListarPluginsTest tests="14" skipped="0" failures="0" errors="0"
catalogo.RecepcionArtefactoTest tests="67" skipped="0" failures="0" errors="0"
catalogo.RegistrarCoreTest tests="19" skipped="0" failures="0" errors="0"
catalogo.RegistroPermitidoTest tests="7" skipped="0" failures="0" errors="0"
catalogo.RutasCoresTest tests="15" skipped="0" failures="0" errors="0"
catalogo.RutasPluginsTest tests="28" skipped="0" failures="0" errors="0"
catalogo.RutasSegurasPropiedades tests="4" skipped="0" failures="0" errors="0"
catalogo.SubirPluginTest tests="52" skipped="0" failures="0" errors="0"
```

Suman 259 pruebas, sin saltos ni fallos. `FronterasModulosTest` (T11) está en `io.synkra` (`backend/src/test/java/io/synkra/FronterasModulosTest.java`) y no entra en este filtro. `InterfazCatalogoTest` sí entra (6 pruebas).

## CA-2: los `curl` de H-01 a H-06 y H-15

Un solo script, con el backend arrancado desde `backend/build/libs/backend-0.1.0.jar` sobre un `SYNKRA_DATA` temporal. El script no es un archivo del repositorio; queda aquí completo. H-03 y H-04 usan los casos de `aidlc-docs/inception/user-stories/stories.md` (rango `>=2.x<`, `core: otro-core`, entrada `../../etc/x` y un JAR de 25 MB con el límite en 20 MB). El JAR grande se borra al terminar el bloque. La lectura de vuelta de H-04 compara la lista de archivos y carpetas de `SYNKRA_DATA` (con `.tmp`) antes y después, cuenta los archivos de `artefactos/.tmp` y busca un `etc/x` fuera de `SYNKRA_DATA`.

```bash
#!/usr/bin/env bash
# Corre desde la raiz del worktree, tras ./gradlew :backend:bootJar
set -u
export SYNKRA_DATA=$(mktemp -d); W=$(mktemp -d)
P=localhost:18080
mk() { # nombre version core rango pais archivo-salida
  mkdir -p "$W/$6.d"
  printf 'nombre: %s\nversion: %s\ncore: %s\ncoreCompatible: %s\netiquetas:\n  pais: %s\n' "$1" "$2" "$3" "$4" "$5" > "$W/$6.d/synkra-plugin.yaml"
  (cd "$W/$6.d" && jar cf "$W/$6.jar" synkra-plugin.yaml)
}
mk kyc-pe 1.3.0 synkra-core 2.x pe kyc-pe-1.3.0
mk kyc-co 1.0.0 synkra-core 2.x co kyc-co-1.0.0
mk kyc-xx 1.0.0 otro-core 2.x pe kyc-xx-1.0.0
java -jar backend/build/libs/backend-*.jar --server.port=18080 >"$W/backend.log" 2>&1 & PID=$!
timeout 90 bash -c 'until curl -sf localhost:18080/actuator/health >/dev/null; do sleep 1; done'
D=sha256:$(printf '%064d' 0)
reg() { curl -s -o /dev/null -w '%{http_code}\n' -X POST $P/api/cores -H 'Content-Type: application/json' -d "{\"nombre\":\"$1\",\"version\":\"$2\",\"imagen\":\"localhost:5001/$1@$D\"}"; }
echo "--- H-01 (CA-2 de U04-T03)"
echo -n "POST core 2.1.0: "; reg synkra-core 2.1.0
echo -n "GET plugins: "; curl -s $P/api/cores/synkra-core/plugins; echo
echo -n "POST core 3.0.0: "; reg synkra-core 3.0.0
echo -n "GET core, versiones: "; curl -s $P/api/cores/synkra-core | python3 -I -c "import json,sys; print([v['version'] for v in json.load(sys.stdin)['versiones']])"
echo -n "POST core 3.0.0 repetida: "; reg synkra-core 3.0.0
echo -n "SQLite: "; python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select core, version from cat_version_core order by version').fetchall())" "$SYNKRA_DATA/synkra.db"
echo "--- H-02 (CA-2 de U04-T07)"
echo -n "sha256 devuelto: "; curl -s -F archivo=@"$W/kyc-pe-1.3.0.jar" $P/api/cores/synkra-core/plugins | python3 -I -c "import json,sys; print(json.load(sys.stdin)['sha256'])"
echo -n "sha256sum:       "; sha256sum "$W/kyc-pe-1.3.0.jar" | cut -d' ' -f1
echo -n "subida repetida: "; curl -s -o /dev/null -w '%{http_code}\n' -F archivo=@"$W/kyc-pe-1.3.0.jar" $P/api/cores/synkra-core/plugins
echo -n "GET plugin: "; curl -s -o /dev/null -w '%{http_code}\n' $P/api/cores/synkra-core/plugins/kyc-pe/1.3.0
echo "--- H-05 y H-06 (CA-2 de U04-T08)"
reg otro-core 1.0.0 >/dev/null
curl -s -o /dev/null -F archivo=@"$W/kyc-co-1.0.0.jar" $P/api/cores/synkra-core/plugins
curl -s -o /dev/null -F archivo=@"$W/kyc-xx-1.0.0.jar" $P/api/cores/otro-core/plugins
echo -n "?pais=pe, cantidad: "; curl -s "$P/api/cores/synkra-core/plugins?pais=pe" | python3 -I -c "import json,sys; print(len(json.load(sys.stdin)))"
echo -n "sin filtro, nombres: "; curl -s "$P/api/cores/synkra-core/plugins" | python3 -I -c "import json,sys; print(sorted(p['nombre'] for p in json.load(sys.stdin)))"
echo "--- H-15 (CA-2 de U04-T10)"
S=$(sha256sum "$W/kyc-pe-1.3.0.jar" | cut -d' ' -f1)
echo -n "sha256 descargado: "; curl -s $P/api/artefactos/$S | sha256sum | cut -d' ' -f1
echo "sha256 subido:     $S"
echo -n "hash desconocido: "; curl -s -o /dev/null -w '%{http_code}\n' $P/api/artefactos/$(printf '%064d' 0)
echo "--- H-03 y H-04 (casos de stories.md)"
mkj() { # archivo-salida coreCompatible core
  mkdir -p "$W/x.d"; printf 'nombre: p\nversion: 1.0.0\ncore: %s\ncoreCompatible: "%s"\n' "$3" "$2" > "$W/x.d/synkra-plugin.yaml"
  (cd "$W/x.d" && jar cf "$1" synkra-plugin.yaml)
}
ANTES=$(cd "$SYNKRA_DATA" && find . -type f -o -type d | sort | grep -v 'synkra.db')
mkj "$W/rango.jar" '>=2.x<' synkra-core
echo -n "H-03 rango >=2.x<: "; curl -s -w ' [%{http_code}]\n' -F archivo=@"$W/rango.jar" $P/api/cores/synkra-core/plugins | cut -c1-260
mkj "$W/otro.jar" '2.x' otro-core
echo -n "H-03 core: otro-core en la carpeta de synkra-core: "; curl -s -w ' [%{http_code}]\n' -F archivo=@"$W/otro.jar" $P/api/cores/synkra-core/plugins | cut -c1-260
python3 -I - "$W/peligro.jar" "$W/grande.jar" <<'PY'
import sys, zipfile
z = zipfile.ZipFile(sys.argv[1], 'w')
z.writestr('synkra-plugin.yaml', 'nombre: p\nversion: 1.0.0\ncore: synkra-core\ncoreCompatible: 2.x\n')
z.writestr('../../etc/x', 'x')
z.close()
z = zipfile.ZipFile(sys.argv[2], 'w', zipfile.ZIP_STORED)
z.writestr('synkra-plugin.yaml', 'nombre: g\nversion: 1.0.0\ncore: synkra-core\ncoreCompatible: 2.x\n')
z.writestr('relleno.bin', bytes(25 * 1024 * 1024))
z.close()
PY
echo -n "H-04 entrada ../../etc/x: "; curl -s -w ' [%{http_code}]\n' -F archivo=@"$W/peligro.jar" $P/api/cores/synkra-core/plugins | cut -c1-260
echo -n "H-04 JAR de 25 MB (limite 20 MB): "; ls -l "$W/grande.jar" | awk '{printf "%s bytes ", $5}'; curl -s -w ' [%{http_code}]\n' -F archivo=@"$W/grande.jar" $P/api/cores/synkra-core/plugins | cut -c1-260
DESPUES=$(cd "$SYNKRA_DATA" && find . -type f -o -type d | sort | grep -v 'synkra.db')
echo -n "H-04 diferencias en SYNKRA_DATA (archivos y carpetas, .tmp incluido, antes y despues de H-03 y H-04): "; diff <(echo "$ANTES") <(echo "$DESPUES") | wc -l
echo -n "H-04 archivos en artefactos/.tmp: "; find "$SYNKRA_DATA/artefactos/.tmp" -type f 2>/dev/null | wc -l
echo -n "H-04 etc/x fuera de SYNKRA_DATA (en /tmp y en /etc/x): "; { find /tmp -path '*/etc/x' 2>/dev/null; ls /etc/x 2>/dev/null; } | wc -l
rm -f "$W/grande.jar"
kill $PID; wait $PID 2>/dev/null
echo -n "puerto 18080 libre (0 = libre): "; ss -ltn | grep -c ':18080 ' || true
```

Salida real (el script termina con código 0):

```
--- H-01 (CA-2 de U04-T03)
POST core 2.1.0: 201
GET plugins: []
POST core 3.0.0: 201
GET core, versiones: ['2.1.0', '3.0.0']
POST core 3.0.0 repetida: 409
SQLite: [('synkra-core', '2.1.0'), ('synkra-core', '3.0.0')]
--- H-02 (CA-2 de U04-T07)
sha256 devuelto: 8f11b13ab3b1b6576638e99c119eaa37dc5c0a733e0d8489a7a80ad8a97543cc
sha256sum:       8f11b13ab3b1b6576638e99c119eaa37dc5c0a733e0d8489a7a80ad8a97543cc
subida repetida: 409
GET plugin: 200
--- H-05 y H-06 (CA-2 de U04-T08)
?pais=pe, cantidad: 1
sin filtro, nombres: ['kyc-co', 'kyc-pe']
--- H-15 (CA-2 de U04-T10)
sha256 descargado: 8f11b13ab3b1b6576638e99c119eaa37dc5c0a733e0d8489a7a80ad8a97543cc
sha256 subido:     8f11b13ab3b1b6576638e99c119eaa37dc5c0a733e0d8489a7a80ad8a97543cc
hash desconocido: 404
--- H-03 y H-04 (casos de stories.md)
H-03 rango >=2.x<: {"codigo":"ENTIDAD_NO_PROCESABLE","mensaje":"La solicitud no se puede procesar","detalles":[{"campo":"coreCompatible","motivo":"RANGO_INVALIDO: El rango '>=2.x<' no es válido: use N.x, N.M.x, N.M.P o >=N.M.P [<N.M.P] con un mínimo menor que el máximo"}]} [422]
H-03 core: otro-core en la carpeta de synkra-core: {"codigo":"CORE_EQUIVOCADO","mensaje":"La solicitud no se puede procesar","detalles":[{"campo":"core","motivo":"El descriptor es del core 'otro-core' y la carpeta es del core 'synkra-core'"}]} [422]
H-04 entrada ../../etc/x: {"codigo":"RUTA_NO_PERMITIDA","mensaje":"La solicitud no se puede procesar","detalles":[{"campo":"archivo","motivo":"La entrada '../../etc/x' tiene una ruta no permitida"}]} [422]
H-04 JAR de 25 MB (limite 20 MB): 26214695 bytes {"codigo":"DEMASIADO_GRANDE","mensaje":"El archivo supera el tamaño permitido","detalles":[]} [413]
H-04 diferencias en SYNKRA_DATA (archivos y carpetas, .tmp incluido, antes y despues de H-03 y H-04): 0
H-04 archivos en artefactos/.tmp: 0
H-04 etc/x fuera de SYNKRA_DATA (en /tmp y en /etc/x): 0
puerto 18080 libre (0 = libre): 0
```

Qué historia cubre cada bloque y contra qué criterio se comparó:

| Historia | Bloque | Esperado | Resultado |
|---|---|---|---|
| H-01 | U04-T03 | `201`, `[]`, `201`, `['2.1.0', '3.0.0']`, `409`, y las dos filas de SQLite | igual |
| H-02 | U04-T07 | dos hashes iguales, `409` y `200` (el `201` del core es el primero de H-01) | igual |
| H-03 | `stories.md` | rango `>=2.x<`: 422; `core: otro-core` en la carpeta de `synkra-core`: 422 | 422 `RANGO_INVALIDO` y 422 `CORE_EQUIVOCADO` |
| H-04 | `stories.md` | `../../etc/x`: 422 sin nada en el volumen; 25 MB con límite de 20 MB: 413 | 422 `RUTA_NO_PERMITIDA`, 413 `DEMASIADO_GRANDE`, 0 diferencias en `SYNKRA_DATA`, 0 en `.tmp`, 0 fuera |
| H-05 y H-06 | U04-T08 | `1` y `['kyc-co', 'kyc-pe']` | igual |
| H-15 | U04-T10 | los dos hashes iguales y `404` | igual |

Dos notas. La historia H-04 dice «413 con el límite configurado»; el cuerpo del 413 trae `detalles` vacío y no nombra el límite, y este resumen lo registra tal cual sin cambiar código. En la subida de H-02 el script no imprime el `201` de la subida misma (no lo pide la tarea); la repetición da 409, lo que prueba que la primera se guardó.

## Candidatas de la unidad

Los comandos de extracción, tal cual se corrieron desde la raíz del worktree:

```
$ awk -F'|' '$4 ~ /U04-T/ {gsub(/ /,"",$2); print $2}' CANDIDATAS.md | tr '\n' ' '
C-31 C-32 C-33 C-34 C-35 C-36 C-37 C-38 C-39 C-40 C-41 C-42 C-43 C-44 C-45 C-46 C-47 C-48 C-49 C-50 C-51 C-52 C-53 C-54 C-55 C-56 C-57 C-58 C-59 C-60 C-61 C-62 C-63 C-64 C-65 C-66 
```

Son 36 (C-31 a C-66): las filas de `CANDIDATAS.md` cuya columna 4 («Salió de») nombra una tarea de U04. `CANDIDATAS.md` no se modificó y ninguna se cerró aquí. La columna «Decisión» es el texto literal del archivo; el cierre lo decide el orquestador. «Trabajada en» sale de este comando (sin `U04-T12`, que solo cita la plantilla):

```
$ for c in $(awk -F'|' '$4 ~ /U04-T/ {gsub(/ /,"",$2); print $2}' CANDIDATAS.md); do echo "$c: $(grep -lw "$c" tareas/U04-T*.md | grep -v 'U04-T12' | sed 's/.*U04-\(T[0-9]*\)-.*/\1/' | paste -sd, -)"; done
C-31: T06
C-32: 
C-33: T13
C-34: 
C-35: 
C-36: T11
C-37: T06
C-38: T07
C-39: T07
C-40: 
C-41: 
C-42: 
C-43: T07
C-44: T06
C-45: T05
C-46: 
C-47: T05,T10
C-48: T10
C-49: T08
C-50: T07
C-51: 
C-52: 
C-53: 
C-54: 
C-55: 
C-56: 
C-57: 
C-58: 
C-59: 
C-60: 
C-61: 
C-62: 
C-63: 
C-64: 
C-65: 
C-66:
```

Una mención en el archivo de una tarea no prueba que la tarea la resolviera: es la lista de tareas de U04 cuyo archivo nombra la candidata, para que el orquestador la revise.

Comprobación de que todas están en este resumen:

```
$ for c in $(awk -F'|' '$4 ~ /U04-T/ {gsub(/ /,"",$2); print $2}' CANDIDATAS.md); do grep -q "^| $c " aidlc-docs/construction/U04-catalogo/code/summary.md || echo FALTA $c; done; echo fin
fin
```

| Candidata | Salió de | Qué es | Trabajada en (menciones en `tareas/U04-T*.md`) | Decisión (literal de `CANDIDATAS.md`) |
|---|---|---|---|---|
| C-31 | Revisión de U04-T01, rondas 1 a 3 | Un traductor de `SQLITE_CONSTRAINT_PRIMARYKEY`, `_FOREIGNKEY` y `_TRIGGER` (una violación de `ON DELETE RESTRICT` llega como `_TRIGGER`) a `DuplicateKeyException` y… | T06 | Aceptada como candidata por el orquestador: R-C2 (409 en una versión repetida) es de U04-T02, que la necesita. Se pide al escribir o despachar U04-T02 |
| C-32 | Revisión de U04-T01, ronda 1 | Claves compuestas reales en las entidades del catálogo: hoy `@Id` va solo en el primer campo y los repositorios son de solo lectura, porque `@Id @Embedded` genera `WHERE… | ninguna | Aceptada como candidata por el orquestador: no hay soporte en la versión actual. Se retoma si Spring Data JDBC lo añade |
| C-33 | Revisión de U04-T02, ronda 2 (F-07 y F-08, AMARILLO) | Bordes de la expresión de `imagen` sin prueba: 255/256 caracteres (M21) y tag de 128/129 (M23). Además, `REPO` rechaza `mi--repo` y `mi__repo`, que la gramática de… | T13 | Aceptada como candidata por el orquestador. El límite de 255 queda ratificado: coincide con el máximo de la gramática de referencia de distribution. Se retoma con la siguiente tarea que toque la validación de imágenes (U04-T13, R-C3) |
| C-34 | Revisión de U04-T02, ronda 2 (F-09, AMARILLO) | El `motivo` de `Version.parsear` (U01) devuelve el texto recibido sin recortar: una versión de 3000 caracteres da un motivo de unos 3200 | ninguna | Aceptada como candidata por el orquestador: U04-T02 no puede tocar `verificador/`. Corregido tras U04-T03: esa tarea **no** acota el cuerpo JSON (un POST de 3 MB vuelve como un 422 de 3 MB y uno de 30 MB se lee entero), así que el tope queda en C-38. Se recorta en U01 cuando se vuelva a tocar `Version` |
| C-35 | Revisión de U04-T02, rondas 1 y 2 | `Descriptor.NOMBRE` (U01) no limita la longitud: un nombre de 300 caracteres se registra, y Kubernetes limita los nombres a 63 | ninguna | Aceptada como candidata por el orquestador: se decide con U07 (despliegue), donde el nombre llega a Kubernetes |
| C-36 | Revisión de U04-T02, ronda 1 | `io.synkra.verificador` aparece como módulo de Modulith (`module-verificador.adoc`) porque cuelga de `io.synkra`. Decidir si se declara como módulo compartido o abierto | T11 | Aceptada como candidata por el orquestador: se decide con la siguiente tarea que toque las fronteras de módulos. **Cerrada en U04-T11**: `@Modulithic(sharedModules = "verificador")` en `SynkraApp`, sin tocar `verificador/`. `verificador` sigue apareciendo como módulo (`module-verificador.adoc` se sigue generando); es lo esperado: ahora está declarado como compartido |
| C-37 | Revisión de U04-T02, ronda 2 | Plantilla de criterios: el `grep` «sin copia» depende del orden del juego (`[a-z0-9]`) y no detecta `[0-9a-z]`. Vigilar la copia comparando con… | T06 | Aceptada por el orquestador: se aplica a las tareas que se escriban después |
| C-38 | Revisión de U04-T03, rondas 1 y 2 | Tope del cuerpo JSON de las rutas: hoy no hay ninguno (30 MB se leen enteros) y, con C-34, una versión enorme vuelve entera en el 422 | T07 | Aceptada como candidata por el orquestador: toca la configuración de plataforma. Se hace con U04-T07 (ruta de subida), que ya tiene que fijar los límites de multipart |
| C-39 | Revisión de U04-T03, ronda 1 (F-08, AMARILLO) | Rechazar campos de más y claves repetidas en el JSON: `"admin":true` da 201 y `"nombre":"dup","nombre":"otro"` registra `otro` | T07 | Aceptada como candidata por el orquestador: cambia la deserialización de toda la API. Se decide junto a C-38 |
| C-40 | Revisión de U04-T03, ronda 2 (F-11, AMARILLO) | `ConfiguracionJson` aplica la política de coerción solo hacia el texto: un `Integer` acepta `"3"` y trunca `3.7` | ninguna | Aceptada como candidata por el orquestador: hoy no hay campos numéricos. Se decide con el primero (U05 o U07) |
| C-41 | Revisión de U04-T03, rondas 2 y 3 (F-13 y F-15, AMARILLO) | Notación del `campo` en los errores: los tipos equivocados devuelven la clave tal como llega (`{"<script>":1}` en un `Map`) y solo el primer segmento en cuerpos anidados… | ninguna | Aceptada como candidata por el orquestador: hoy los DTO son planos. Se decide con el primer DTO anidado |
| C-42 | Revisión de U04-T03, ronda 3 (F-16, AMARILLO) | Restos de C-07: la traducción queda latente en producción porque `validation` solo está en pruebas; ninguna prueba ata que siga así; `CodigosError.MOTIVO_OBLIGATORIO`… | ninguna | Aceptada como candidata por el orquestador: se resuelve cuando un DTO de producción use `@Valid` |
| C-43 | Revisión de U04-T03, ronda 1 | Los 400 que corta Tomcat (`%2F`, `%00`, `..%2F` en la ruta) salen en HTML y no en `ErrorApi`; el 406 se mapea a `SOLICITUD_INVALIDA` sin código propio | T07 | Aceptada como candidata por el orquestador: es de plataforma y anterior a U04. Se retoma con U10 (seguridad) |
| C-44 | Revisión de U04-T04, ronda 3 | Entradas con nombre duplicado en el JAR: se aceptan, y `ZipFile.getEntry` y el lector en flujo pueden leer contenidos distintos para el mismo nombre (por ejemplo, dos… | T06 | Aceptada por el orquestador: se pide en U04-T06 (rechazar duplicados al leer el descriptor) |
| C-45 | Revisión de U04-T04, ronda 3 (F-13 a F-15, AMARILLO) | Pruebas sin atar en `RecepcionArtefacto`: la comparación con la lista de commons, el orden frente al conjunto, `distinct()` (C02, C03, C21), la rama `isInterrupted()` y… | T05 | Aceptada como candidata por el orquestador: se hace con U04-T05, que vuelve sobre las rutas del JAR |
| C-46 | Revisión de U04-T04, rondas 1 a 3 | Enlaces marcados solo en `AsiExtraField` y entradas FIFO o de dispositivo en el modo Unix: se aceptan (Info-ZIP las extrae como archivos regulares) | ninguna | Aceptada como candidata por el orquestador: el init container (U07) debe extraer sin conservar modos ni enlaces |
| C-47 | Revisión de U04-T06, ronda 2 (F-06, AMARILLO) | Protección contra `<core>` o `<core>/<nombre>` como enlace simbólico en `SubidaPlugin.colocar` sin prueba: `isDirectory` sin `NOFOLLOW_LINKS` (N08) o sin la comprobación… | T05,T10 | Aceptada como candidata por el orquestador: el código actual lo rechaza (500, nada fuera). Se ata con U04-T05, que vuelve sobre rutas seguras, con `Files.createSymbolicLink` |
| C-48 | Revisión de U04-T06, rondas 1 y 2 | Reconciliación de disco contra base: un JAR sin fila tras una caída entre el enlace y el commit deja esa versión en 409 para siempre (y `obtenerPlugin` da 404). Además,… | T10 | Aceptada como candidata por el orquestador: se decide con U04-T10 (artefactos por hash) |
| C-49 | Revisión de U04-T06, ronda 1 | Nombres en el catálogo y en el JAR: `a/` junto a `a`, y nombres que solo difieren en mayúsculas (choque en sistemas de archivos sin distinción) | T08 | Aceptada como candidata por el orquestador: `a/` va con C-46 en U07; las mayúsculas se deciden con U04-T08 (listado) |
| C-50 | Revisión de U04-T06, ronda 1 | Valores de etiqueta de la subida sin tope de longitud | T07 | Aceptada como candidata por el orquestador: se fija en U04-T07 junto a C-38 |
| C-51 | Revisión de U04-T05, rondas 2 y 3 | Autorreferencias aceptadas en el JAR: `.`, `./`, `./\` y `.\` pasan la comprobación de rutas (resuelven al destino mismo, sin fuga) | ninguna | Aceptada como candidata por el orquestador: no es una fuga y cambiarlo toca las reglas de U04-T04. Se decide con U07, junto a C-46, según cómo extraiga el init container |
| C-52 | Revisión de U04-T05, rondas 1 a 3 | Puntos y barras Unicode (`．．`, `／`, `∕`) se aceptan; un extractor de Windows con conversión best-fit los volvería `..` y `/` | ninguna | Aceptada como candidata por el orquestador: el init container es Linux. Se decide con U07, junto a C-46 |
| C-53 | Revisión de U04-T05, ronda 3 (F-07, AMARILLO) | Sellar `java.io.tmpdir` de la JVM de pruebas en un directorio del build: hoy la comprobación de `RutasSegurasPropiedades` mira `recepcion-*` en `/tmp`, la esquiva un… | ninguna | Aceptada como candidata por el orquestador: es configuración de `build.gradle.kts`. Se retoma con U11, junto a C-25 |
| C-54 | Revisión de U04-T05, rondas 1 a 3 | La rama `isInterrupted()` de `RecepcionArtefacto` (C11 de C-45) sigue sin prueba | ninguna | Aceptada como candidata por el orquestador: se ata con la siguiente tarea que toque `RecepcionArtefacto` |
| C-55 | Codificador de U04-T07, ronda 1 | Un nombre de core muy largo (C-35) rompe la cabecera `Location` de `POST /api/cores` y da 500 (preexistente) | ninguna | Aceptada como candidata por el orquestador: va con C-35 |
| C-56 | Codificador y revisión de U04-T07, ronda 1 | El comentario de `RutasCores.plugins` («Hasta U04-T07 y U04-T08 no hay plugins») quedó desfasado | ninguna | Resuelta en U04-T08 (la ruta pasó a `RutasPlugins` y el comentario se quitó) |
| C-57 | Revisión de U04-T07, ronda 1 | C-50 no cubre las etiquetas del descriptor: un `synkra-plugin.yaml` con un valor de 100 caracteres se acepta y se guarda entero | ninguna | Aceptada como candidata por el orquestador: U04-T07 limita C-50 a la ruta y prohíbe tocar `SubidaPlugin`. Se propone para U04-T11 o una tarea de `SubidaPlugin` Reasignada por el orquestador el 2026-11-27: U04-T11 prohíbe cambios de comportamiento, así que no cabe ahí. Queda para una tarea de pulido de U04 que se propone al responsable después de U04-T12 |
| C-58 | Revisión de U04-T07, ronda 2 | `Content-Type: */*` o `application/*` en `POST /api/cores` da 500 `ERROR_INTERNO` en vez de 415 (preexistente) | ninguna | Aceptada como candidata por el orquestador: va con la familia de C-43 |
| C-59 | Revisión de U04-T07, ronda 2 | `GET /actuator/health` con un cuerpo JSON `{"a":1}` da 422 (preexistente, menor) | ninguna | Aceptada como candidata por el orquestador: baja prioridad, las sondas no mandan cuerpo |
| C-60 | Revisión de U04-T07, ronda 2 (F-12, AMARILLO) | `LimiteCuerpoJsonAvance.afterBodyRead` drena sin tope propio; hoy no se alcanza, pero un conversor nuevo (YAML, CBOR) lo dejaría sin límite | ninguna | Aceptada como candidata por el orquestador: se fija en U04-T11 (fronteras y endurecimiento) Reasignada por el orquestador el 2026-11-27: U04-T11 prohíbe cambios de comportamiento, así que no cabe ahí. Queda para una tarea de pulido de U04 que se propone al responsable después de U04-T12 |
| C-61 | Revisión de U04-T08, ronda 3 (F-01 a F-03, AMARILLO) | Pulido de U04-T08: el motivo de un UTF-8 roto en el filtro dice «codificación de porcentaje»; la prueba de entradas rotas no fija `codigo` ni `campo`; el acoplamiento… | ninguna | Aceptada como candidata por el orquestador: no bloquea. Se recoge en U04-T11 Reasignada por el orquestador el 2026-11-27: U04-T11 prohíbe cambios de comportamiento, así que no cabe ahí. Queda para una tarea de pulido de U04 que se propone al responsable después de U04-T12 |
| C-62 | Revisión de U04-T09, rondas 1 y 2 | N+1 que nace de la forma de la interfaz `Catalogo` de U01: `Verificador.alternativas` pide `versionesPlugin` y luego un `descriptor` por versión, y con `CatalogoSqlite`… | ninguna | Aceptada como candidata por el orquestador: la caché y los métodos de lote están fuera de alcance. Se evalúa en U07, cuando la verificación en vivo mida tiempos |
| C-63 | Revisión de U04-T09, ronda 2 (F-06 a F-08, AMARILLO) | Pulido de U04-T09: los INSERT de la prueba de fila corrupta son posicionales y dejan filas en la base compartida; `io.synkra.verificador.Catalogo` va calificado en vez… | ninguna | Aceptada como candidata por el orquestador: no bloquea. Se recoge en U04-T11 Reasignada por el orquestador el 2026-11-27: U04-T11 prohíbe cambios de comportamiento, así que no cabe ahí. Queda para una tarea de pulido de U04 que se propone al responsable después de U04-T12 |
| C-64 | Revisión de U04-T10, rondas 1 y 2 | `GET /api/artefactos/{sha}` no recalcula el hash (decisión 4 de U04-T10): un archivo corrupto del mismo tamaño se sirve con un `X-Synkra-Sha256` falso. El init container… | ninguna | Aceptada como candidata por el orquestador: va con U05-T07 (`init-descarga/descargar.sh`, caso «hash alterado») |
| C-65 | Revisión de U04-T10, rondas 1 y 2 (F-02, F-06 y F-07, AMARILLO) | Pulido de U04-T10: el javadoc de `abrirArtefacto` y de `ArtefactosPorHash` dice «raíz de datos» y la raíz es `artefactos/`; `directorioIntermedioEnlazadoEs500` no… | ninguna | Aceptada como candidata por el orquestador: no bloquea. Se recoge en U04-T11; el 400 de Tomcat va con C-43 Reasignada por el orquestador el 2026-11-27: U04-T11 prohíbe cambios de comportamiento, así que no cabe ahí. Queda para una tarea de pulido de U04 que se propone al responsable después de U04-T12 |
| C-66 | Revisión de U04-T13, ronda 1 (F-01 a F-03, AMARILLO) | Pulido de U04-T13: la bitácora no separa las pruebas de caracterización de C-33 (pasan sobre la base) del rojo de R-C3; literal `100` de truncado en el mensaje de… | ninguna | Aceptada como candidata por el orquestador: no bloquea. Se recoge en U04-T11 Reasignada por el orquestador el 2026-11-27: U04-T11 prohíbe cambios de comportamiento, así que no cabe ahí. Queda para una tarea de pulido de U04 que se propone al responsable después de U04-T12 |

### Candidatas de otra unidad cuya decisión nombra U04

```
$ awk -F'|' '$4 !~ /U04-T/ && $6 ~ /U04/ {gsub(/ /,"",$2); print $2}' CANDIDATAS.md | tr '\n' ' '
C-04 C-05 C-07 C-08 C-12 C-13 C-14 C-19 C-20 
```

Son 9 (C-04, C-05, C-07, C-08, C-12, C-13, C-14, C-19 y C-20): filas cuyo origen no es de U04 y cuya decisión nombra U04. Solo C-04 (U04-T01), C-07 (U04-T03) y C-14 (U04-T11) dicen «Cerrada» en su decisión; las otras seis siguen como las dejó el orquestador y quedan para que decida si se cerraron de hecho o pasan a la tarea de pulido de U04. Los textos están en `CANDIDATAS.md`.

## Lo que queda fuera de U04

Según el resumen de U01, la integridad del artefacto por hash (S11) queda pendiente en U04 y U05 (en U01 es `FueraDeU01`); U04 sirve el hash por `X-Synkra-Sha256` y C-64 deja la verificación al init container. La verificación en vivo con el catálogo (C-62) se evalúa en U07.
