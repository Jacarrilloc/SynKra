# Colección de Postman de SynKra

Dos archivos para probar todo el backend REST:

- `SynKra.postman_collection.json`: la colección (formato v2.1), con tests en cada petición.
- `SynKra.local.postman_environment.json`: el entorno «SynKra local» (`baseUrl`, `pais`, `usuario`,
  imagen del core y rutas a los JAR de ejemplo).

## Importar

1. En Postman: **Import** y arrastra los dos archivos JSON.
2. Elige el entorno **SynKra local** arriba a la derecha.
3. En **Settings > General > Working directory** pon la raíz del repo. Las rutas de los JAR del entorno
   (`plugins/.../build/libs/...`) son relativas a ella. Si tu versión de Postman no toma el archivo desde
   la variable, elige el JAR a mano en la pestaña **Body** de la petición.

## Contra qué backend

La variable `baseUrl` tiene dos valores posibles:

| `baseUrl` | Cómo se levanta | Kubernetes |
|---|---|---|
| `http://localhost:8080` (por defecto) | `SYNKRA_DATA=$(mktemp -d) ./gradlew :backend:bootRun` | no |
| `https://synkra.local` (copia `baseUrlKind`) | `make install` (kind con Ingress) | sí |

`SYNKRA_DATA` hace falta en local porque por defecto la base va en `/data`.

Para `https://synkra.local`:

- Agrega `127.0.0.1 synkra.local` a `/etc/hosts`. Con curl, sin tocar `hosts`:
  `curl -k --resolve synkra.local:443:127.0.0.1 https://synkra.local/actuator/health`.
- El certificado es autofirmado: desactiva **Settings > General > SSL certificate verification** en
  Postman (o usa `-k` en curl y `--insecure` en newman).
- Pon en `imagenCore` el digest real del core que dejó `make install` en el registro `localhost:5001`
  (por ejemplo `docker buildx imagetools inspect localhost:5001/synkra-core:<tag>`). Con un digest
  inventado el catálogo lo acepta, pero los pods del país no arrancan y el despliegue queda `fallido`.

## Archivos de ejemplo

Desde la raíz del repo:

```bash
./gradlew :plugins:kyc-co:jar :plugins:kyc-pe:assemble
```

| Variable | Archivo | Qué es |
|---|---|---|
| `jarKycCo` | `plugins/kyc-co/build/libs/kyc-co-1.0.0.jar` | `kyc-co` 1.0.0, core `2.x`, `pais: co` |
| `jarKycPe130` | `plugins/kyc-pe/build/libs/kyc-pe-1.3.0.jar` | `kyc-pe` 1.3.0, core `2.x`, `pais: pe` |
| `jarKycPe200` | `plugins/kyc-pe/build/libs/kyc-pe-2.0.0.jar` | `kyc-pe` 2.0.0, core `3.x`, `pais: pe` |
| `jarDescriptorRoto` | `build/postman/descriptor-roto.jar` | descriptor con versión `1.0` y rango `>=2.x<` |
| `archivoNoJar` | `postman/README.md` | cualquier archivo que no sea un ZIP |

El JAR con el descriptor roto se genera así (necesita `jar` del JDK 21):

```bash
mkdir -p build/postman/roto
printf 'nombre: kyc-roto\nversion: 1.0\ncore: synkra-core\ncoreCompatible: ">=2.x<"\n' \
  > build/postman/roto/synkra-plugin.yaml
(cd build/postman/roto && jar cf ../descriptor-roto.jar synkra-plugin.yaml)
```

## Orden de ejecución

Corre la colección entera con el **Collection Runner** (o newman), en este orden, que es el de la demo.
Cada carpeta termina con su subcarpeta de casos negativos:

1. **Salud**: `/actuator/health` y `/actuator/health/readiness`.
2. **Catálogo**: registrar el core `synkra-core` 2.1.0 y 3.0.0 (y `otro-core`), subir `kyc-co` y
   `kyc-pe` (multipart: parte de archivo `archivo` y parte opcional `etiquetas` en JSON), listar con
   filtro por etiquetas, ver un plugin y descargar el artefacto por su `sha256`.
3. **Verificación**: una combinación compatible y una incompatible (las dos dan 200), más errores.
4. **Despliegue**: crear plan, confirmar, confirmar otra vez (409), ver, esperar a `listo`, segundo
   despliegue y reversión al primero.
5. **Registro**: vista general, historial por país y detalle.

Los tests encadenan los ids con `pm.collectionVariables.set`: `sha256` (y `sha256KycCo`,
`sha256KycPe200`), `planId`, `despliegueId` (el id del despliegue es el del plan), `planId2`,
`despliegueId2`, `planReversionId` y `despliegueReversionId`.

Empieza con datos limpios. Si repites la corrida sobre la misma base, el registro de cores y la subida de
plugins responden 409 `CONFLICTO` y los tests lo aceptan (el `sha256` se lee entonces del catálogo).

Con newman, desde la raíz del repo:

```bash
newman run postman/SynKra.postman_collection.json \
  -e postman/SynKra.local.postman_environment.json --working-dir .
# contra kind: --env-var baseUrl=https://synkra.local --insecure
```

## Qué necesita Kubernetes

| Petición | Sin clúster (backend local) | Con kind |
|---|---|---|
| `POST /api/despliegues` (crear plan válido) | 503 `KUBERNETES_NO_DISPONIBLE` | 201 |
| `POST /api/despliegues/{id}/confirmacion` | se salta | 202, y 409 la segunda vez |
| `GET /api/despliegues/{id}` del flujo y las esperas | se salta | 200 |
| `POST /api/despliegues/{id}/reversion` | se salta | 201 |
| `GET /api/paises` (vista general) | 200 con `lectura: no_disponible` | 200 con `lectura: leido` |
| `GET /actuator/health/readiness` | 404 (Spring solo activa las sondas en Kubernetes) | 200 |

Cuando «Crear plan» responde 503, su test lo da por bueno y fija `sinCluster=true`; las peticiones que
dependen de un plan se saltan (`pm.execution.skipRequest`) o marcan sus tests como omitidos. Todo lo
demás corre igual sin clúster, también los casos negativos de despliegue: plugin incompatible (422
`COMBINACION_RECHAZADA`), país no permitido (422 `PAIS_NO_PERMITIDO`), usuario inválido (422) y los 404,
porque la verificación va antes de leer el clúster.

La espera a `listo` vuelve a pedir el despliegue cada 5 s, hasta 40 veces, con `postman.setNextRequest`:
solo funciona en el Collection Runner o en newman, no al enviar la petición suelta.

## Formato de error

Todo error trae el mismo cuerpo `ErrorApi`, que los tests comprueban:

```json
{"codigo": "COMBINACION_RECHAZADA", "mensaje": "La combinación no es compatible",
 "detalles": [{"campo": "plugins.kyc-pe", "motivo": "El plugin 'kyc-pe' 2.0.0 requiere el core 'synkra-core' en 3.x y se pidió 2.1.0"}]}
```

La cabecera `X-Synkra-Usuario` es opcional (sin ella queda `anonimo`): marca quién crea y quién
confirma, no es un control de acceso.
