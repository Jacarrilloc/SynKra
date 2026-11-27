# Ronda 1 — U02-T10

VEREDICTO: VERDE

Worktree `/home/julian/Escritorio/Proyectos/wt-m6r-U02-T10` en `bae98f3`, revisado sobre `git diff 288e5c0..bae98f3` (13 archivos, +335). Volví a correr yo los cuatro criterios. Al terminar, `git status --short` sale vacío: el worktree sigue limpio. Todas las mutaciones se hicieron en copias temporales con `git archive` en `mktemp -d`, ya borradas.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Pruebas y los dos descriptores | `./gradlew :plugins:kyc-pe:test --rerun :plugins:kyc-pe:check :plugins:kyc-pe:assemble`, luego `grep` en el XML y `unzip -p` de cada JAR | **pasa**. `BUILD SUCCESSFUL`, `tests="18" skipped="0" failures="0" errors="0"`. Ninguna prueba tarda 0,000 s en todas sus filas: la de los JAR tarda 0,029 s, así que corrieron de verdad. 8 dígitos aprobado. Rechazados: 7, 9, `""` y `"1"` (motivo de longitud); letras, espacio, signo `-` y dígitos arábigo-índicos (motivo de no dígitos); solicitud nula (su motivo). `LectorDescriptor` lee los dos JAR construidos y comprueba `version`, `coreCompatible`, `core`, `etiquetas` y nombre. `unzip -p`: 1.3.0 con 2.x y 2.0.0 con 3.x, los dos con `pais: pe`. Los JAR no traen `io/synkra/plugin/api/` dentro. |
| 2 | El core real acepta uno y rechaza el otro | `:core:bootJar :plugins:kyc-pe:assemble`, luego `java -jar core-2.1.0.jar --server.port=18197 --synkra.core.config=$D/config --synkra.core.plugins=$D/plugins`, con espera por `timeout 90` sobre la readiness | **pasa**. Con 1.3.0: readiness `UP`. `/plugins` da `[{"nombre":"kyc-pe","version":"1.3.0"}]`. `12345678` da `{"aprobado":true,"motivo":"DNI válido"}`. 7 y 9 dígitos y `""` dan el motivo de longitud. `1234567a` y `١٢٣٤٥٦٧٨` dan el motivo de no dígitos. El log dice `Plugins cargados: [kyc-pe]`. Con 2.0.0: `salida=1` y 2 líneas `VERSION_INCOMPATIBLE plugin=kyc-pe`, que piden 3.x frente a un core 2.1.0. Maté el proceso, el puerto quedó libre y borré el temporal. |
| 3 | El resto del build sigue pasando | Los cuatro `test --rerun` y `check` del criterio | **pasa**. `BUILD SUCCESSFUL`, sin saltos ni fallos: verificador 178, plugin-api 4, core 54, kyc-co 17. |
| 4 | El diff queda acotado | `git rev-parse --short HEAD; git diff --stat 288e5c0...HEAD` | **pasa**. Solo toca `plugins/kyc-pe/**`, `bitacoras/U02-T10*` y una línea `+include("plugins:kyc-pe")` en `settings.gradle.kts`. No toca `core/`, `verificador/`, `plugin-api/`, `backend/` ni `kyc-co`. |

## No se repite el fallo de U02-T09 (motivos sin comprobar)
Hice 8 mutaciones propias, cada una con `--rerun`, y las 8 rompen la suite:
- M1: no dígitos devuelve el motivo de longitud. Fallan 5.
- M2: `Character.isDigit` deja pasar dígitos no ASCII. Falla 1, la arábigo-índica.
- M3: solicitud nula devuelve el motivo de longitud. Falla 1.
- M4: `!=` cambiado por `>` deja pasar los cortos. Fallan 3.
- M5: el rango de 2.0.0 pasa a 2.x. Falla 1, la prueba del JAR 2.0.0.
- M6: la versión de 2.0.0 pasa a 1.3.0. Falla 1.
- M7: 8 dígitos pasa a 9. Fallan 9.
- M8: la etiqueta pasa a `pais: co`. Fallan 2.

Las pruebas de rechazo comprueban el motivo exacto con `assertEquals(MOTIVO_*)`. También comprobé que el resultado de la prueba no queda viejo: cambiar el rango en `build.gradle.kts` sin `--rerun` vuelve a ejecutar la prueba y falla.

## Hallazgos
No quedan ROJO ni NARANJA.

### F-01 · AMARILLO · plugins/kyc-pe/src/main/java/io/synkra/kycpe/PluginKycPe.java (MOTIVO_DOCUMENTO_NULO y la rama `d == null`) · Rama defensiva sin prueba ni chequeo de unicidad
`SolicitudKyc` ya impide documento nulo con `requireNonNull`, así que la rama no se puede alcanzar desde una prueba. Además, `losMotivosSonDistintos` deja fuera `MOTIVO_DOCUMENTO_NULO`. Para quedar igual que `kyc-co` habría que quitar la rama, o al menos meter la constante en el `Set`. No bloquea.

### F-02 · AMARILLO · bitacoras/U02-T10-evidencia/ca3.txt · La evidencia de CA-3 es pobre
Solo trae `BUILD SUCCESSFUL` y cinco líneas de conteo con la etiqueta `core` (10, 12 y 1). No da conteos para verificador, plugin-api ni kyc-co. Mi corrida da 178, 4, 54 y 17, sin saltos. No bloquea porque mi corrida es la evidencia.

### F-03 · AMARILLO · bitacoras/U02-T10-evidencia/ca4.txt y ca2.txt · Sha anterior y ruido en la transcripción
`ca4.txt` anota `bb8b641` y no la punta `bae98f3`; es inevitable si se commitea después, pero conviene decirlo. `ca2.txt` empieza con cuatro `SOLICITUD_INVALIDA`, que la bitácora ya explica como un error del curl, y trae una línea `kill: (9` cortada. La corrida buena está en el mismo archivo y coincide con la mía.

### F-04 · AMARILLO · bitacoras/U02-T10-evidencia/rojo.txt · El rojo es de compilación, no de comportamiento
El commit `ed7042a` mete las pruebas sin `PluginKycPe` y el build falla con `11 errors` al compilar. Eso cumple «pruebas antes que la regla», pero no demuestra que cada prueba falle por la razón correcta. Mis mutaciones cubren ese hueco.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna nueva.

## Rutas de transcripciones largas
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca1.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca3.txt

No tengo herramientas de escritura, así que no guardé el informe en `revisiones/U02-T10/ronda-1.md`. El texto de arriba es el informe; hay que guardarlo en esa ruta.

```
VEREDICTO: VERDE
AMARILLO|plugins/kyc-pe/src/main/java/io/synkra/kycpe/PluginKycPe.java (MOTIVO_DOCUMENTO_NULO)|Rama defensiva sin prueba ni chequeo de unicidad
AMARILLO|bitacoras/U02-T10-evidencia/ca3.txt|La evidencia de CA-3 es pobre
AMARILLO|bitacoras/U02-T10-evidencia/ca4.txt y ca2.txt|Sha anterior y ruido en la transcripción
AMARILLO|bitacoras/U02-T10-evidencia/rojo.txt|El rojo es de compilación, no de comportamiento
INFORME: revisiones/U02-T10/ronda-1.md
```
