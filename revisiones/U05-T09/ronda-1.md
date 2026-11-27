# Ronda 1: U05-T09

VEREDICTO: NO-VERDE

Los cuatro criterios pasan cuando los corro yo, y el diff respeta el alcance. El veredicto es NO-VERDE por dos hallazgos NARANJA: la nueva API pública `yaml*()` no tiene ninguna prueba, y nada comprueba que el cliente se cierre ahora que su cierre depende del adaptador.

Entorno: JAVA_HOME de Adoptium 21 aarch64 y GRADLE_USER_HOME real. HOME, user.home, KUBECONFIG y kubeconfig apuntaban a un `mktemp -d` falso. Sin kubectl ni clúster. Base 26b6124, HEAD 2060878.

## Criterios de aceptación, verificados por mí
| # | Criterio | Comando que corrí | Resultado |
|---|---|---|---|
| 1 | Las fronteras se cumplen y la prueba muerde | `./gradlew :backend:test --rerun --tests '*FronterasModulosTest'`, más el grep del XML y los dos grep de `io.fabric8` | pasa: BUILD SUCCESSFUL, `tests="9" skipped="0" failures="0" errors="0"`, `fuera=1`, `api=1`. Hay dos pruebas nuevas que muerden con juguetes: `laReglaDeFabric8FueraDeKubernetesMuerdeConUnaClaseDeJuguete` y `laReglaDeApiMuerdeConUnaFirmaPublicaConFabric8` |
| 2 | El cliente no se puede inyectar (lectura de vuelta del contexto) | `./gradlew :backend:test --rerun --tests '*ConfiguracionKubernetesTest*'` | pasa: 2/0/0/0. Con cuenta, `getBeanNamesForType(KubernetesClient)` está vacío, el adaptador sale una vez y `disponible()` da true |
| 3 | Todo el backend sigue pasando | `./gradlew :backend:test --rerun :backend:check` y un bucle sobre los XML | pasa: BUILD SUCCESSFUL, 32 suites, 437 pruebas, 0 saltos, 0 fallos, 0 errores. Las suites de ~0,001 s son pruebas unitarias puras (las de caracterización y `CoreRegistradoTest`), sin estado externo |
| 4 | El diff queda acotado | `git diff --stat 26b6124...HEAD` | pasa: solo toca `backend/src/main/java/io/synkra/kubernetes/**`, `backend/src/test/**` y `bitacoras/U05-T09*`. sha 2060878 |

Rojo primero, reproducido por mí: saqué 9f419f7 con `git archive` a una copia en el scratchpad y corrí las tres clases. Fallan las dos pruebas esperadas: `laApiPublicaDeKubernetesNoMencionaFabric8`, con 6 violaciones en el constructor y los accesores de `ObjetosPais`, y `ConfiguracionKubernetesTest.conCuentaDeServicio...`. También falló `laDocumentacionGeneradaNoSeVersiona`, pero solo porque mi copia no es un repo git (`git check-ignore` devuelve 128); no es un defecto del diff.

Alcance, revisado:
- **`construir`**: solo cambia su tipo de retorno, que ahora es el más concreto `ObjetosPaisFabric8` en `AdaptadorFabric8`.
- **`aplicar`**: mismo orden y mismas excepciones. Hay dos cambios nuevos, los dos dentro de lo pedido:
  - `ObjetosPaisIncompletosException`, que lo exige la decisión 5.
  - El rechazo de un `ObjetosPais` ajeno, que es consecuencia directa de convertir `ObjetosPais` en interfaz.
- **`estado`, `contextoFallo` y `leerPais`**: no se tocan.
- **Pruebas existentes**: solo se adaptaron por el tipo, como pide la tarea.
- **Caracterización en `catalogo`**: es de solo prueba y cumple la decisión 4.
- **Unificación de la imagen del init y C-76**: no entran en el diff.

## Hallazgos

### F-01 · NARANJA · `kubernetes/api/ObjetosPais.java` y `ObjetosPaisFabric8.java:42-54` · la nueva API pública `yamlDeployment/yamlConfigMap/yamlService` no tiene ninguna prueba
Es la cara que se le entrega a U07, la decisión 3 dice «se expone como texto», y la salida no se lee en ningún sitio. `grep -rn "yamlDeployment\|yamlConfigMap\|yamlService" backend/src/test` no da resultados. Hace falta una prueba que llame a `construir` y lea de vuelta cada YAML desde la interfaz `ObjetosPais`: kind, nombre, namespace `synkra-<país>` y, por ejemplo, la imagen del init en el Deployment. Lo ideal es que la compare con el árbol de `ConstruirObjetosPaisTest`.

### F-02 · NARANJA · `ConfiguracionKubernetes.java:28-33` y `AdaptadorFabric8.java:40-46` · nada comprueba que el cliente se cierre con el contexto
Antes el cierre era explícito: `@Bean(destroyMethod = "close")` sobre el cliente. Ahora depende de que Spring deduzca el método de destrucción porque `AdaptadorFabric8` implementa `AutoCloseable`, mientras que el tipo declarado del bean es `AdaptadorKubernetes`, que no lo implementa. Es justo el punto de la decisión 1 («ciclo de vida del adaptador»). La deducción debería funcionar, pero ninguna prueba lo lee de vuelta: `grep close backend/src/test` no da resultados. Hace falta una prueba que cierre el contexto del runner y compruebe que `AdaptadorFabric8.close()` cerró el cliente. Ejemplo: después de `ctx.close()`, el cliente obtenido con `cliente(adaptador)` rechaza peticiones, o se usa un espía del cierre.

### F-03 · AMARILLO · `SynkraAppTest.java:51-56` · `elClienteDeKubernetesNoEsUnBeanDeLaAplicacion` es vacía
La aplicación de prueba arranca sin cuenta de servicio, y con el código viejo tampoco había bean en ese caso (`@Conditional`). En 9f419f7 la prueba pasa: corrí `SynkraAppTest` en la copia en rojo y salió 4/0/0/0. CA-2 queda cubierto por `ConfiguracionKubernetesTest`, pero esta prueba no aporta evidencia. Habría que darle una cuenta completa en ese contexto o quitarla.

### F-04 · AMARILLO · `FronterasModulosTest.java` (`laApiNoExponeFabric8`) · la regla de la API tiene huecos
Hoy no los pisa nada, pero un tipo de Fabric8 podría entrar a la API pública por cualquiera de ellos:
- Solo mira `getRawSuperclass` y `getRawInterfaces`, así que un argumento de tipo como `extends Algo<ConfigMap>` se le escapa.
- No revisa las cláusulas `throws`.
- No revisa los límites de los parámetros de tipo de la clase.
- No revisa las anotaciones públicas.

### F-05 · AMARILLO · `bitacoras/U05-T09-evidencia/ca1.txt` · la evidencia de CA-1 no trae los greps
La salida de `fuera=1` y `api=1` solo aparece afirmada en la bitácora, no capturada en el archivo. Mi ejecución la confirma, así que no bloquea.

### F-06 · AMARILLO · `AdaptadorFabric8.java:36-38` · el accesor `cliente()` existe solo para las pruebas
Es de paquete y no rompe la frontera, porque la regla prohíbe Fabric8 fuera del módulo. Aun así, conviene documentarlo como costura de prueba.

## Tareas candidatas (defectos reales fuera de alcance)
- C-73, tercer punto: la prueba del defecto de la imagen sigue sin cargar `application.properties`. No entraba en las decisiones de esta tarea y la candidata sigue abierta.
- Unificar `RegistroPermitido` con `exigirImagenInit`: ahora está documentado con pruebas de caracterización en los dos paquetes y queda pendiente.

## Estado del worktree
Termina limpio: `git status --short` sale vacío y HEAD sigue en 2060878. Borré mis temporales: el HOME y el kubeconfig falsos, y la copia en rojo.

## Transcripciones largas
Están en `/tmp/claude-1000/-home-julian-Escritorio-Proyectos-SynKra/76af36c7-fa86-447c-9ae5-76b2a7831e37/scratchpad/`:
- `ca1.log`: CA-1.
- `ca2.log`: CA-2.
- `ca3.log`: CA-3.
- `ca3-res.txt`: conteos por suite de CA-3.
- `rojo.log`: corrida en 9f419f7.

VEREDICTO: NO-VERDE
NARANJA|kubernetes/api/ObjetosPais.java + ObjetosPaisFabric8.java:42-54|la API yaml* para U07 no tiene ninguna prueba
NARANJA|ConfiguracionKubernetes.java:28-33 + AdaptadorFabric8.java:40-46|el cierre del cliente con el ciclo de vida del adaptador no se lee de vuelta
INFORME: revisiones/U05-T09/ronda-1.md
