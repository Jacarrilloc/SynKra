# Ronda 1 — U07-A (U07-T01 y U07-T02)

VEREDICTO: VERDE

Revisé 97f9f80..23f0ab3 en /home/julian/Escritorio/Proyectos/wt-m6r-U07-A, rama m6r/U07-A. Corrí los seis criterios yo mismo y todos pasan. Las cuatro mutaciones de CA-4 y ocho más que añadí hacen caer la prueba de su regla. Solo quedan tres AMARILLO, ninguno bloquea.

El código de producción y las pruebas no cambian entre 6fde709 y 23f0ab3. Los commits 000f0ed y 23f0ab3 solo tocan `bitacoras/`, así que la evidencia sacada en 6fde709 corresponde al código actual.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1a | PlanRepositorioTest | `./gradlew :backend:test --rerun --tests '*PlanRepositorioTest'` y grep del XML | pasa: `BUILD SUCCESSFUL`, `tests="12" skipped="0" failures="0" errors="0"`, la suite tarda 2.326 s (no se saltó nada) |
| 1b | Lectura de vuelta de `build/sqlite-pruebas/PlanRepositorioTest/synkra.db` | el python3 -I literal del CA | pasa: las 5 filas `aa`, `ab`(41), `ad`, `ae`(confirmado), `af`(vencido), todas con `integer`, `1800000` y `3`; `(0,)`; `[('dep_plan',)]` |
| 1c | La tabla existe en la aplicación arrancada (puerto 18087, libre) | `bootJar` + `java -jar ... --server.port=18087` con HOME, KUBECONFIG y user.home falsos; espera con `timeout 90` hasta health; python3 -I | pasa: `['dep_plan']`, `[('1',), ('2',), ('4',)]`, `(0,)`, `[('dep_plan_pais_estado',)]`. Detuve el proceso y el puerto quedó libre |
| 2 | PaisPermitidoTest y `ajenos` | `--rerun --tests '*PaisPermitidoTest'`, grep del XML, grep de imports | pasa: `tests="6" skipped="0" failures="0" errors="0"`, `ajenos=1` |
| 3 | Un `SYNKRA_PAISES='co,PE'` impide arrancar (puerto 18088, libre) | el literal del CA, con HOME y KUBECONFIG falsos | pasa: `salida=1`, conteo `1`. La causa es `IllegalStateException: synkra.paises (SYNKRA_PAISES) debe ser una lista...` |
| 4 | Cada regla tiene una prueba que muerde | copias con `git archive HEAD` en mktemp, una por mutación (12 en total), borradas al terminar | pasa, detalle abajo |
| 5 | Todo el backend | `./gradlew :backend:test --rerun :backend:check` | pasa: `BUILD SUCCESSFUL`, 35 XML con 457 pruebas, todos con skipped, failures y errors en 0. `FronterasModulosTest` 9/9 |
| 6 | Diff acotado | `git diff --name-only 97f9f80...HEAD` y diff de properties y del plan | pasa (sha `23f0ab3`): solo `despliegue/**` en main y test, V4, properties (línea en blanco + comentario + propiedad) y la línea 3 del plan (C-30), más `bitacoras/`. Sin `Clock`, sin `api`, sin cambios fuera de alcance |

CA-4, mutaciones del criterio. Cada una dio `BUILD FAILED` con `<failure` en el XML de la clase dueña de la regla:
- **lista** (`if (false)`): cae `PaisPermitidoTest.rechazaUnPaisConFormaValidaFueraDeLaLista`.
- **forma** (`CASE_INSENSITIVE`): caen `rechazaTodoLoQueNoTieneLaForma` y `unaListaInvalidaFallaAlArrancarNombrandoLaPropiedad`.
- **sin U05** (inyectar `AdaptadorKubernetes` y llamar a `existeNamespace`; mi script comprueba que el parche se aplicó): caen `aceptaLosPaisesConfiguradosYDerivaElNamespace` y `nadaDeLoAnteriorLlamaAlAdaptadorDeKubernetes`, es decir, `verifyNoInteractions`.
- **esquema** (sin el CHECK de estado): cae `PlanRepositorioTest.rechazaEstadoDesconocido`.

Mutaciones extra mías, todas mordidas:
- `\p{Ll}{2}` en vez de `[a-z]{2}` (no ASCII): cae `rechazaTodoLoQueNoTieneLaForma`, por la `с` cirílica, que comprobé en bytes (321 201).
- `namespace` sin `exigir`: caen 2 pruebas.
- Sin `AUTOINCREMENT`: cae `losIdsNoSeReutilizan`.
- Sin `CHECK (vence_en > creado_en)`: cae `rechazaVencimientoNoPosterior`.
- Sin `strip()`: cae `laListaAceptaEspaciosAlrededorDeCadaEntrada`.
- Sin el CHECK tipo/origen: caen `rechazaReversionSinOrigen` y `rechazaDespliegueConOrigen`.
- Sin `CHECK json_valid(plugins)`: cae `rechazaJsonInvalido`.
- Sin el CHECK de país: cae `rechazaPaisConMayusculas`.

El rojo previo de CA-1 sin V4 está en `bitacoras/U07-A-evidencia/rojo-ca1.txt`: sha 97f9f80, 11 de 13 fallan. No lo repetí, porque es un estado histórico.

## Hallazgos

### F-01 · AMARILLO · backend/src/test/java/io/synkra/despliegue/PlanRepositorioTest.java (`rechazaJsonInvalido`) · Los CHECK `json_valid(resumen)` y `json_valid(cambios)` no tienen prueba
`sql(...)` solo deja variar `plugins`. Si se quita el CHECK de `resumen` o de `cambios`, ninguna prueba cae. Las dos instancias son de la misma clase. El criterio solo pide un rechazo por JSON inválido, así que no bloquea.

### F-02 · AMARILLO · PaisPermitidoTest.java:61 · La prueba solo afirma explícitamente que el motivo no contiene lo recibido cuando la entrada pasa de 3 caracteres
El `doesNotContain` tiene una guarda `length() > 3`. Para `mx`, `PE`, `" co"`, `c`, `""` y los cirílicos, la garantía sale indirectamente del `isEqualTo(motivo)` exacto. Lo comprobé: ninguna de esas entradas de dos letras aparece en los dos textos constantes. `c` y `""` son subcadenas triviales, y eso explica la guarda. Es correcto, pero no queda escrito como pide la letra del CA-2.

### F-03 · AMARILLO · bitacoras/U07-A-evidencia/ · Evidencia fuera de la lista literal de CA-6 y `ca6.txt` desfasado
- CA-6 enumera `bitacoras/U07-A.md` y `revisiones/U07-A/`, pero no la carpeta `bitacoras/U07-A-evidencia/`. La convención la diste tú en el encargo, así que no lo trato como desborde.
- `ca6.txt` se capturó en `000f0ed` y lista `ca1.txt`, que 23f0ab3 ya borró. Mi propia ejecución de CA-6 sobre 23f0ab3 lo sustituye.

## Tareas candidatas (defectos reales fuera de alcance)
- Ninguna.

## Notas del entorno
- En CA-5, Gradle corrió con el HOME real (por el GRADLE_USER_HOME real) y KUBECONFIG falso. Los procesos `java -jar` de CA-1 y CA-3 corrieron con HOME, KUBECONFIG y user.home falsos.
- Nada de kubectl ni clúster. Los puertos 18086 a 18088 estaban libres antes y lo quedaron después. Borré todos los mktemp.
- `git status --short` en el worktree sale vacío al terminar: no lo dejé sucio.
- No tengo herramientas de escritura, así que no creé `revisiones/U07-A/ronda-1.md`. El texto de este informe es el que va en ese archivo.

## Rutas de transcripciones largas (scratchpad)
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/mut/salida.txt (las 12 mutaciones)
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca5.txt y /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca5-xml.txt
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/ca3.log
- /tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/app18087.log

```
VEREDICTO: VERDE
AMARILLO|backend/src/test/java/io/synkra/despliegue/PlanRepositorioTest.java (rechazaJsonInvalido)|Los CHECK json_valid(resumen) y json_valid(cambios) no tienen prueba
AMARILLO|backend/src/test/java/io/synkra/despliegue/PaisPermitidoTest.java:61|«El motivo no contiene lo recibido» solo se afirma explícitamente para entradas de más de 3 caracteres
AMARILLO|bitacoras/U07-A-evidencia/|Evidencia fuera de la lista literal de CA-6 y ca6.txt capturado en 000f0ed
INFORME: revisiones/U07-A/ronda-1.md
```
