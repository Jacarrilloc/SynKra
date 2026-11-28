# Ronda 2 — U07-D

VEREDICTO: VERDE

Las cinco respuestas (F-01 a F-05) se sostienen con mis propias corridas. Mis mutaciones M1 a M5 de la ronda 1, aplicadas con mis mismos `sed`, ahora caen todas, y también cuatro sondas nuevas (M6 a M9). Volví a correr los ocho criterios completos y todos pasan. Solo quedan dos AMARILLO, que no bloquean.

Comprobaciones previas:
- **Hash de la tarea:** `2d710b026e2a23ba49b60987b5997abe1ab41e80`, coincide.
- **Rama:** punta `11372b8`. El último commit que toca `backend` es `45b4248`.
- **Unión:** `ade68f6`, con padres `208d5f9` y `5b471bc`.
- **`Plan.java`:** `git diff ade68f6 -- backend/src/main/java/io/synkra/despliegue/Plan.java | wc -l` da **0**.
- **Worktree:** limpio al terminar (`git status --short` = 0). Ningún JAR quedó vivo y el puerto 18093 quedó libre.
- **Cómo corrí:** en copias hechas con `git archive` más `git init -q`, con JAVA_HOME de Adoptium 21, HOME y KUBECONFIG falsos, `python3 -I` y sin `git stash`. Las copias se borraron al terminar.
- **Edad de la evidencia:** toda la del codificador (`ca0-r2`, `ca1` a `ca6`, `ca5-r2`) cita `45b4248`, el último sha de código. `ca7.txt` se tomó sobre `b264443` y su commit solo trae `ca7.txt` y la bitácora. Ninguna evidencia es anterior al diff.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 0 | Unión limpia y en verde | Órdenes git de CA-0 sobre `ade68f6`, y `./gradlew :backend:test --rerun :backend:check` en una copia de `ade68f6` | pasa: `0 0 0 0`. El `grep -cx` desde archivo da `1`. `Clock.system` solo en `Reloj.java` y `RegistroServicio.java`, y `Clock reloj()` solo en `Reloj.java:12`. BUILD SUCCESSFUL, 48 XML con 0/0/0 y las 7 clases nombradas presentes |
| 1 | Confirmar contra el simulador | `--rerun --tests '*ConfirmarTest'` y el Python del criterio | pasa: tests=10 y 0/0/0, con tiempos por caso entre 0,012 y 1,33 s. Las seis salidas de Python coinciden letra por letra |
| 2 | P4 | `--rerun --tests '*CarpetaEquivocadaPlanTest'` y Python | pasa: tests=3 y 0/0/0; `[('co','otro-core','pendiente','[{"nombre":"kyc-pe","version":"1.3.0"}]')]` y `(0,)` |
| 3 | Sin clúster, con doble y con el JAR real | La prueba (`--rerun`) y las líneas 183 a 221 de la tarea, literales, en el puerto 18093 | pasa: tests=2 y 0/0/0. Del JAR salen `201 201 201`, 422 USUARIO_INVALIDO, 503, 410, 409, 422 `['plugins.kyc-pe']`, 422 PLAN_DESACTUALIZADO, 404, `[] 200` y `404`, más las cinco filas esperadas y `(0,)` |
| 4 | R-D13, sin 501 | `--rerun` de PlanPendienteTest y RelojTest, Python, `grep` del 501 y diff | pasa: 9 y 1, con 0/0/0. La lista es idéntica a la de U07-C. `501=1`. El diff solo trae `@Autowired guarda` y los casos `py` y `uy` |
| 5 | Rojo primero y mutaciones | M1 a M9, una copia por mutación, con las tres clases | pasa: **9 de 9 dan BUILD FAILED** (detalle abajo). El rojo original está registrado desde la ronda 1 |
| 6 | Suite y fronteras | `--rerun :backend:check` y los `grep` de CA-6 | BUILD SUCCESSFUL, 51 XML con 0/0/0 (`FronterasModulosTest`, `CrearPlanTest`, `VerificacionVivoTest`, `VistaGeneralTest` y `HistorialTest` incluidas). `registro-interno=1`. `aplicar` y `construir` solo en `ConfirmacionServicio.java`, con `2`. `vigilancia=1`, `programado=1`, `confirmar-tx=1`. `tablas-reg=0` y `requiresNew=0` salen de comentarios previos a la base (`Plan.java:10` y `RegistroServicio.java:60`). Según el arbitraje (C-122) valen los greps de uso real y de literales SQL, y los dos dan 1: `uso-real=1` y `tablas-reg-sql=1` |
| 7 | Diff acotado | Las tres órdenes de CA-7 en el worktree | pasa: `A` para las tres pruebas y `HttpPlanes`, `M` solo para `PlanPendienteTest` y `SimuladorKubernetes`, `0` líneas quitadas. Nada en resources, registro, plataforma, kubernetes ni catalogo, y `Plan.java` ya no aparece |

## Respuestas a la ronda 1, verificadas
- **F-01: cerrado.** Mis mutaciones de la ronda 1, con los mismos `sed`, dan todas BUILD FAILED con 15 pruebas:
  - M1 (sin `fallido` ante otra RuntimeException): cae `ConfirmarSinClusterTest.unaExcepcionInesperada…`, con «expected FALLIDO but was APLICANDO».
  - M2 (sin `vence_en` en el WHERE): cae `ConfirmarTest.ar…`, con «Expecting code to raise a throwable».
  - M3 (el 410 de `reservar` pasa a 409 PLAN_NO_PENDIENTE): cae `ar`, con «expected 410 GONE but was 409».
  - M4 (se registra sin plugins) y M5 (etiquetas vacías): cae `cl`, con la lectura de vuelta de `plugins` y `etiquetas`.
  - Sondas nuevas de esta ronda, que también caen:
    - M6 (`>=` en lugar de `>`): `ar`; el borde está bien fijado.
    - M7 (`eventos` fijo en `"x"`): `unaExcepcionInesperada…`.
    - M8 (las etiquetas de `kyc-pe` para todos los plugins): `cl`.
    - M9 (`estado` en mayúsculas): `pe` y `cl`.
  - Además leí `reg_plugin` con Python: `cl` tiene `kyc-ee` con `{"cliente":"acme","pais":"cl"}` y `kyc-pe` con `{}`. Los sha256 difieren de los de `ca1.txt` porque los JAR de prueba se generan en cada corrida (hay sellos de tiempo en el zip); no es un defecto.
- **F-02: cerrado.** Desde un archivo, el `grep -cx` da `1`. Dentro de mi propio arnés (que pasa por `eval`) dio `0`, lo que confirma el diagnóstico corregido.
- **F-03: cerrado.** El diff de `Plan.java` está vacío.
- **F-04: cerrado.** `ec` compara con `tresPatch("ec")`.
- **F-05: cerrado.** `RutasConfirmacion` usa `EstadoDespliegue.APLICANDO` y `RUTA_DESPLIEGUES`. M9 demuestra que la forma en minúsculas está fijada por las pruebas.

## Hallazgos
### F-06 · AMARILLO · `bitacoras/U07-D.md:108` · «el `2` de `git status`» no coincide con `ca7.txt`
La línea de la ronda 2 copia la explicación de la ronda 1 («el `2` son `ca7.sh` y `ca7.txt`»). Pero `ca7.txt` de la ronda 2 muestra `1`, porque `ca7.sh` ya estaba en el repositorio y solo cambió `ca7.txt`. La evidencia es correcta; el texto de la bitácora no.

### F-07 · AMARILLO · `ConfirmarTest.java:111,246` y `ConfirmarSinClusterTest.java:88-108` · Datos de prueba que se apartan de las decisiones 8 y 11, declarados
- El caso `cl` ya no usa solo S2: añade un tercer plugin propio, `kyc-ee` con etiquetas. La decisión 8 nombra solo `kyc-zz` y `kyc-yy`.
- `ConfirmarSinClusterTest` trae ahora un caso con `disponible()=true`, lo que contradice el nombre de la clase.
- Las dos cosas están declaradas en la bitácora, no cambian ninguna salida de CA-1 ni de CA-3, y son la forma más barata de cerrar F-01. Las anoto para que el orquestador lo sepa, no para que se cambien.

## Tareas candidatas (defectos reales fuera de alcance)
- Ya registradas por el orquestador: C-122 (los greps `REQUIRES_NEW` y `reg_` de CA-6 casan con comentarios) y C-123 (en el `catch` se pierde la excepción original si `actualizarResultado` lanza).
- Nueva: el `grep -cx` de CA-0 da `0` si el arnés pasa el comando por `eval`. Conviene usar `grep -Fcx` o un archivo de patrón en la plantilla de tareas.

## Rutas de transcripciones largas
En `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-D-r2/`:
- `cas.txt`: CA-1, CA-2, CA-4, CA-6, la prueba de CA-3, los greps de uso real y la lectura de `reg_plugin`.
- `mutaciones.txt` (M1 a M9), con `muts.sh` y `mut.sh`.
- `ca3-jar.txt` y `ca3-jar.sh`, `todo.txt`.
- `ca0-gradle.txt`, `ca0-xml.txt` y `ca4-diff.txt`.

```
VEREDICTO: VERDE
AMARILLO|bitacoras/U07-D.md:108|«el 2 de git status» copiado de la ronda 1; ca7.txt de la ronda 2 da 1
AMARILLO|ConfirmarTest.java:111,246 y ConfirmarSinClusterTest.java:88-108|kyc-ee en cl y un caso con disponible()=true en la clase «SinCluster»: se apartan de las decisiones 8 y 11, declarados y sin efecto en los criterios
INFORME: revisiones/U07-D/ronda-2.md
```
