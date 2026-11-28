# Ronda 2: U05-T09

VEREDICTO: VERDE

Corrí yo mismo los cuatro criterios y pasan todos. Los dos NARANJA de la ronda 1 quedan cerrados: reproduje su par falla-pasa con mutaciones propias. No queda ningún ROJO ni NARANJA en pie, solo un AMARILLO nuevo que no bloquea.

Entorno: JAVA_HOME de Adoptium 21 aarch64 y GRADLE_USER_HOME real. HOME, `-Duser.home`, KUBECONFIG y `-Dkubeconfig` apuntaban a un `mktemp -d` falso. Sin kubectl ni clúster. Base 26b6124, HEAD 3b9f31c.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Las fronteras se cumplen y la prueba muerde | `./gradlew :backend:test --rerun --tests '*FronterasModulosTest'`, el grep del XML y los dos grep de `io.fabric8` | pasa: BUILD SUCCESSFUL, `tests="9" skipped="0" failures="0" errors="0"`, `fuera=1`, `api=1`. Las dos pruebas que muerden con juguete siguen presentes |
| 2 | El cliente no se puede inyectar (lectura de vuelta del contexto) | `./gradlew :backend:test --rerun --tests '*ConfiguracionKubernetesTest*'` | pasa: 2/0/0/0 en 0,52 s |
| 3 | Todo el backend sigue pasando | `./gradlew :backend:test --rerun :backend:check` y el bucle sobre los XML | pasa: BUILD SUCCESSFUL, 33 suites, 439 pruebas, 0 saltos, 0 fallos, 0 errores. Las cuentas cuadran: 437 − 1 (prueba quitada de SynkraAppTest) + 2 (YamlObjetosPaisTest) + 1 (prueba del cierre) = 439 |
| 4 | El diff queda acotado | `git diff --name-only 26b6124...HEAD`, filtrado por las rutas permitidas | pasa: ningún archivo cae fuera de `backend/src/main/java/io/synkra/kubernetes/**`, `backend/src/test/**`, `bitacoras/U05-T09*` y `revisiones/U05-T09/`. `SynkraAppTest` queda con diff neto vacío. sha 3b9f31c |

Rojo de la ronda 2, reproducido por mí en una copia de 3b9f31c sacada con `git archive` a un `mktemp -d`:
- **Mutación del YAML y del cierre.** Puse `yamlService` a devolver el Deployment y quité `cliente.close()` de `AdaptadorFabric8.close()`. Fallan las tres pruebas esperadas:
  - `alCerrarElContextoElClienteQuedaCerrado`, en ClienteKubernetesTest.java:137.
  - `cadaYamlTraeSuKindNombreYNamespace`, en :48.
  - `losTresYamlCoincidenConElArbolEsperado`, en :65.

  Coincide con `rojo-r2.txt`.
- **Mutación de la regla.** Desactivé solo la comprobación de `throws` en la regla de la API y falla `laReglaDeApiMuerdeConUnaFirmaPublicaConFabric8`. En esa corrida también falló `laDocumentacionGeneradaNoSeVersiona`, pero solo porque la copia no es un repo git, igual que en la ronda 1. No es un defecto del diff.

Alcance de la ronda 2: el código de producción solo cambia un Javadoc (`AdaptadorFabric8.cliente()`). El resto son pruebas y juguetes en `src/test`. `construir`, `aplicar`, `estado`, `contextoFallo` y `leerPais` no se tocan.

## Cierre de los hallazgos de la ronda 1
- **F-01 (NARANJA), cerrado.** `YamlObjetosPaisTest` lee los tres YAML desde la interfaz `ObjetosPais`: kind, nombre, `synkra-pe`, imagen del core e imagen del init. Además los compara con `pais-pe-esperado.yaml`. Muerde, lo comprobé con la mutación.
- **F-02 (NARANJA), cerrado.** `alCerrarElContextoElClienteQuedaCerrado` cierra el contexto del runner y lee el estado de vuelta: el cliente falla y el servidor sigue con `getRequestCount()==1`. Con `close()` vacío falla, también comprobado. Mantener la deducción de Spring del método de cierre queda justificado porque la prueba lo demuestra.
- **F-03 (AMARILLO), cerrado.** La prueba vacía se quitó, como permitió el arbitraje. CA-2 sigue cubierto por `ConfiguracionKubernetesTest`.
- **F-04 (AMARILLO), cerrado, y va más allá del mínimo arbitrado.** La regla cubre argumentos de tipo de superclase e interfaces, `throws`, límites de parámetros de tipo de clase y de método, y anotaciones de clase, método y campo. Hay un juguete por caso y la prueba exige cada mensaje.
- **F-05 (AMARILLO), cerrado.** `ca1.txt` trae ahora `fuera=1` y `api=1` capturados.
- **F-06 (AMARILLO), cerrado.** El Javadoc marca `cliente()` como costura de prueba.

## Hallazgos
### F-07 · AMARILLO · `ClienteKubernetesTest.java:137` · la aserción del cliente cerrado acepta cualquier excepción
`assertThatThrownBy(() -> cliente.get().getKubernetesVersion()).isNotNull()` pasaría con cualquier Throwable. La compensa la aserción siguiente, que pide que no llegue ninguna petición nueva al servidor (`getRequestCount()==1`), y la mutación demuestra que la prueba muerde. Aun así, convendría exigir el tipo o el mensaje concreto del cliente cerrado. No bloquea.

## Tareas candidatas (defectos reales fuera de alcance)
- Siguen abiertas las dos de la ronda 1:
  - C-73, tercer punto: la prueba del defecto de la imagen no carga `application.properties`.
  - Unificar `RegistroPermitido` con `exigirImagenInit`.
- Ninguna nueva.

## Estado del worktree
Termina limpio: `git status --short` sale vacío y HEAD sigue en 3b9f31c. Borré mis temporales, el HOME y el kubeconfig falsos y la copia mutada, y comprobé que ya no existen.

## Rutas de transcripciones largas
En `/tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/r2/`:
- `ca1.log`
- `ca2.log`
- `ca3.log`
- `ca3-res.txt`
- `rojo.log`: mutación del YAML y del cierre.
- `rojo-f04.log`: mutación de la regla de `throws`.

No tengo herramientas de escritura, así que este informe no está escrito en `revisiones/U05-T09/ronda-2.md`. Tiene que guardarlo el orquestador.

```
VEREDICTO: VERDE
AMARILLO|ClienteKubernetesTest.java:137|la aserción del cliente cerrado acepta cualquier excepción
INFORME: revisiones/U05-T09/ronda-2.md
```
