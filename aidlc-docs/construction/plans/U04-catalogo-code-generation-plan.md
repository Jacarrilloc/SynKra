# Plan de tareas: U04 Catálogo

> **Parte 1 de Code Generation.** No se ejecuta en el módulo 5; la Parte 2 requiere aprobación y aviso previo a Julian.

Raíz del proyecto. Añade el paquete `catalogo` a `backend/` (con `catalogo.api` público) y la
migración `V2__catalogo.sql`. Resumen en `aidlc-docs/construction/U04-catalogo/code/`.

## Contexto

- **Historias dueñas:** H-01 a H-06, H-31 (Should). **Contribuye a:** H-15 (descarga por hash), H-24.
- **Depende de:** U01 (`LectorDescriptor`, `Catalogo`), U03 (aplicación, SQLite, errores, fronteras).
- **Diseño:** `../U04-catalogo/functional-design/` (R-C1 a R-C14). **Tablas:** `cat_core`,
  `cat_version_core`, `cat_plugin`, `cat_etiqueta`.
- **Expone:** `CatalogoApi` (registrar, obtener, subir, listar, abrir por hash) e implementación de `Catalogo` de U01.

## Tareas

- [x] **U04-T01 · Migración y repositorios**
  `V2__catalogo.sql` con las cuatro tablas e índice por `sha256`; repositorios Spring Data JDBC.
  **Criterio:** `./gradlew :backend:test --tests '*CatalogoRepositoriosTest'` pasa y `sqlite3 $SYNKRA_DATA/synkra.db '.tables'` lista las cuatro `cat_*`. · H-01

- [x] **U04-T02 · Registrar un core (R-C1, R-C2)**
  **Criterio:** `./gradlew :backend:test --tests '*RegistrarCoreTest'` pasa con: primera versión (201 y carpeta vacía), segunda versión (mismo core), imagen sin digest (422), versión repetida (409). · H-01

- [x] **U04-T03 · Rutas de cores**
  `POST/GET /api/cores`, `GET /api/cores/{nombre}`, `GET /api/cores/{nombre}/plugins`.
  **Criterio:** con el backend local, `curl -s -o /dev/null -w '%{http_code}' -X POST localhost:8080/api/cores -d @core.json -H 'Content-Type: application/json'` = `201` y `curl -s localhost:8080/api/cores/synkra-core/plugins` = `[]`. · H-01

- [x] **U04-T04 · Recepción segura del JAR (R-C5, R-C6, R-C11)**
  Lectura a un temporal con límite de tamaño y SHA-256 en flujo; revisión de entradas del JAR antes de guardar.
  **Criterio:** `./gradlew :backend:test --tests '*RecepcionArtefactoTest'` pasa con: JAR de 25 MB (413, nada en disco), entrada `../../etc/x` (422 `RUTA_NO_PERMITIDA`, nada en disco), JAR válido (hash igual a `sha256sum`). · H-04, H-02

- [x] **U04-T05 · Propiedad de rutas seguras (PBT-03)**
  **Criterio:** `./gradlew :backend:test --tests '*RutasSegurasPropiedades'` pasa: ninguna entrada aceptada resuelve fuera del directorio de destino. · H-04

- [x] **U04-T06 · Subir un plugin (R-C4, R-C7 a R-C10)**
  Descriptor con U01, core de la carpeta, inmutabilidad, etiquetas sin choque; mover a la ruta final y guardar.
  **Criterio:** `./gradlew :backend:test --tests '*SubirPluginTest'` pasa con: válido (201 con descriptor y sha256), sin descriptor, campo desconocido, `core` de otra carpeta (422 `CORE_EQUIVOCADO`), versión repetida (409), etiqueta en conflicto (422). · H-02, H-03

- [x] **U04-T07 · Ruta de subida**
  `POST /api/cores/{nombre}/plugins` (multipart: `archivo` y `etiquetas`), `GET …/plugins/{plugin}/{version}`.
  **Criterio:** `curl -F archivo=@plugins/kyc-pe/build/libs/kyc-pe-1.3.0.jar localhost:8080/api/cores/synkra-core/plugins | jq -r .sha256` = `sha256sum` del mismo archivo. · H-02

- [x] **U04-T08 · Filtrar por etiqueta y aislamiento de carpetas (R-C12)**
  **Criterio:** `./gradlew :backend:test --tests '*ListarPluginsTest'` pasa; `curl -s "localhost:8080/api/cores/synkra-core/plugins?pais=pe" | jq length` = `1` y la carpeta de `otro-core` no muestra `kyc-pe`. · H-05, H-06

- [x] **U04-T09 · `Catalogo` de U01 implementado por el módulo**
  **Criterio:** `./gradlew :backend:test --tests '*CatalogoVerificadorTest'` pasa los casos S1 a S7 de la suite de U01 usando datos guardados en SQLite. · H-06

- [x] **U04-T10 · Servir artefactos por hash (R-C13)**
  `GET /api/artefactos/{sha256}`.
  **Criterio:** `curl -s localhost:8080/api/artefactos/<sha> | sha256sum` coincide con `<sha>`; un hash inexistente da 404. · H-15

- [x] **U04-T11 · Fronteras del módulo**
  **Criterio:** `./gradlew :backend:test --tests '*FronterasModulosTest'` sigue pasando con `catalogo` y solo `catalogo.api` es público. · H-01

- [x] **U04-T12 · Resumen**
  **Criterio:** `aidlc-docs/construction/U04-catalogo/code/summary.md` contiene la salida real de `./gradlew :backend:test --tests '*catalogo*'` y de los `curl` de H-01 a H-06. · H-01

- [x] **U04-T13 · *(Should)* Registro de imágenes permitido (R-C3)**
  **Criterio:** con `SYNKRA_REGISTRO_PERMITIDO=localhost:5001`, registrar `docker.io/x@sha256:…` da 422 (`./gradlew :backend:test --tests '*RegistroPermitidoTest'`). · H-31

## Trazabilidad

| Historia | Tareas |
|---|---|
| H-01 | T01, T02, T03, T11, T12 |
| H-02 | T04, T06, T07 |
| H-03 | T06 |
| H-04 | T04, T05 |
| H-05 | T08 |
| H-06 | T08, T09 |
| H-15 (contribuye) | T10 |
| H-31 (Should) | T13 |

**Terminada cuando:** `./gradlew :backend:test --tests '*catalogo*'` pasa y los criterios de H-01 a H-06 se cumplen con `curl` contra el backend local.
