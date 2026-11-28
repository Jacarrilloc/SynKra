# Ronda 2 — U07-B

VEREDICTO: VERDE

Corrí yo los siete criterios con su comando literal y todos pasan. Los tres hallazgos que se corrigieron (F-01, F-03 y F-04) quedan comprobados con ejecuciones mías, incluida la mutación M8, que en la ronda 1 sobrevivía. Esta ronda no tiene hallazgos nuevos. F-02 (candidata C-108) y F-05 (convención del orquestador) se quedan como los arbitró el orquestador y no los reabro: no apareció ningún defecto nuevo.

Datos de partida:
- **Hash de la tarea:** `git hash-object` da `b4a557ba82a3eafeaf3924dbfd04a11d90c3962f`. Coincide.
- **Punta revisada:** 9d80cba. La base 8182b9d es la punta VERDE de U07-A y es ancestro de la punta.
- **Código de producción:** entre ba47422 (punta de la ronda 1) y HEAD no cambia nada en él. Cambian solo `CrearPlanTest.java` (+9/−2), `VerificacionVivoTest.java` (+6/−2) y bitácoras.
- **Edad de la evidencia:** entre c9d0814 (el sha de la evidencia de `ronda2/`) y HEAD no cambia nada en `backend/`. Esa evidencia sí pudo salir de este diff.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Verificación en vivo (R-D2) | literal de CA-1 | pasa: `BUILD SUCCESSFUL`, `tests="15" skipped="0" failures="0" errors="0"`, suite de 4,27 s (contexto Spring real); Python `(0,)` y `(4,) (3,)` |
| 2 | Crear plan contra el simulador | literal de CA-2 | pasa: `tests="9"`, sin saltos, 4,66 s; las tres salidas de Python son idénticas a las esperadas. Además leí `dep_plan` completa: en `co` la columna `plugins` sale `[kyc-co 1.0.0, kyc-pe 1.3.0]` y `resumen.plugins` en el mismo orden, aunque el plan los pidió al revés; `creado_por` vale `anonimo` en las tres filas |
| 3 | PBT-03 | literal de CA-3 | pasa: `tests="4"` y cuatro `tries = 1000`. Coberturas: 36 % (mismo nombre, otra versión o hash), 15 % (país vacío), 22 % (sin cambios). La suite tarda 0,44 s, y es creíble: es una función pura, con 1000 llamadas y 1000 comprobaciones en el reporte y una marca de tiempo fresca |
| 4 | JAR real, puerto 18090 (libre), sin clúster | literal de CA-4 | pasa: `201` ×4; `200 RECHAZADA [PLUGIN kyc-pe 1.3.0, CORE synkra-core 3.0.0]`; `422 COMBINACION_RECHAZADA`; `422 PAIS_NO_PERMITIDO`; `503 KUBERNETES_NO_DISPONIBLE`; `(0,)` |
| 5 | Mutaciones en copias de `git archive` | `corre.sh`, una copia por mutación | pasa: la copia de control sin mutar da 28 de 28 en verde, y las diez mutaciones dan `BUILD FAILED` (detalle abajo) |
| 6 | Suite completa y fronteras | literal de CA-6 | pasa: `BUILD SUCCESSFUL`, 38 XML con 485 pruebas, ninguno con `skipped`, `failures` o `errors` distinto de 0; `registro=1`, `internos=1`, `fabric8=1`, `escrituras=1` |
| 7 | Diff acotado | literal de CA-7 sobre 8182b9d | pasa: 47 archivos. Fuera de `despliegue/**`, `bitacoras/U07-B*` y `revisiones/U07-B/` solo está `SimuladorKubernetes.java`, con el `A` exacto que pide el criterio. En las pruebas de `despliegue` solo hay altas (`A`) de las tres clases nuevas: las de U07-A no cambian |

**Detalle de CA-5.** Cada mutación, con la prueba que cae en la clase dueña de la regla:

- M1 (sin alternativas) cae en `VerificacionVivoTest`: `s4DaLasDosAlternativasEnOrden`, S4 y S5.
- M2 (sin buscar en otras carpetas) cae en `VerificacionVivoTest`: `s7DaCoreEquivocadoYS6PluginAusente` y S7.
- M3 (sin cortar con `RECHAZADA`) cae en `CrearPlanTest`: `unaCombinacionRechazadaDa422…`.
- M4 (`PaisPermitido` después de verificar) cae en `CrearPlanTest`: `unPaisNoPermitidoDa422…`.
- M5 (`leerPais` antes de verificar) tumba cinco pruebas de `CrearPlanTest`.
- M6 (comparar solo por nombre) tumba dos propiedades de `CambiosPropiedades`.
- M7 (`sinCambios` sin `core.igual`) tumba dos propiedades.
- M9 (no insertar el plan) tumba dos pruebas de `CrearPlanTest`.
- M10 (cobertura de país vacío al 50 %) hace caer `aplicarLosCambios…`.
- **M8 (quitar el `.sorted` del resumen en `PlanServicio:65`) ahora cae:** 28 pruebas y 1 fallo, `CrearPlanTest.cambiaUnPluginYSeQuedaOtroDistingueAnadidosQuitadosEIguales`. En la ronda 1 daba 28 de 28 en verde.

## Respuestas de la ronda 1, verificadas
- **F-01 (NARANJA), cerrado.** El caso `co` ahora pide `kyc-pe` antes que `kyc-co`. La prueba comprueba el orden de `resumen.plugins` en la respuesta y lo compara con `dep_plan.resumen.plugins`. También compara la columna `dep_plan.plugins` con el JSON exacto ordenado. M8 ahora cae, y la lectura de vuelta por Python lo confirma.
- **F-03 (AMARILLO), cerrado.** Ejecuté en una copia, tal como aparecen en `ronda2/ca5-4.txt` y `ca5-6.txt`, los comandos `sed -i -e … -e …` de M4 y M6. Las dos y las tres sustituciones se aplican, según el `diff`.
- **F-04 (AMARILLO), cerrado.** El ayudante `post` de `VerificacionVivoTest` ahora hace `verifyNoInteractions(adaptador)` y comprueba que `dep_plan` tiene 0 filas después de cada llamada. Así cubre todas las pruebas y no solo una.
- **F-02 (C-108) y F-05 (convención):** quedan como los arbitró el orquestador, sin cambios.

## Hallazgos
No hay hallazgos nuevos.

Dejo una observación sobre la evidencia, sin severidad. `ronda2/ca7.txt` se tomó en 001df01, antes del commit de la bitácora, y la bitácora lo declara. Mi CA-7 sobre 9d80cba incluye esos archivos y está dentro de las rutas permitidas.

## Tareas candidatas (defectos reales fuera de alcance)
- Nada nuevo. C-108 (afinar el grep «escrituras» de CA-6 y volver a `plan.estado()` en `PlanDto`) ya está registrada.

## Estado del worktree
`git status --short` sale vacío al terminar. Solo hay ignorados: `.gradle/`, `build/` y `backend/.jqwik-database`, que son cachés de compilación y pruebas. Las copias de mutación ya están borradas. No toqué `wt-m6r-U06-B` ni el puerto 18089, y el 18090 quedó libre.

## Rutas de transcripciones largas
Todo está en `/tmp/claude-1000/-home-casdav-Projects-Master-SynKra/9d987332-269f-4da9-9d66-0a5449231723/scratchpad/U07-B-r2/`:
- criterios: `ca123.txt` (CA-1 a CA-3), `ca4.txt` (con su script `ca4.sh`), `ca6.txt`, `ca7.txt`;
- control sin mutar: `control.txt`;
- mutaciones: `m1.txt` a `m10.txt`, con sus `m*.sed`;
- script de mutación: `corre.sh`.

```
VEREDICTO: VERDE
INFORME: revisiones/U07-B/ronda-2.md
```
