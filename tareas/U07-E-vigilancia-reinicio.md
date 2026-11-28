# U07-E: vigilancia sin reintentos y retomar tras un reinicio

**Unidad:** U07: `Despliegue y reversión`
**Tareas del plan que agrupa:** U07-T08 y U07-T09
**Historias que implementa:** H-14, H-16 (contexto de un fallo de verdad: logs y eventos del clúster), H-17 (contribuye: un plan que nadie toca vence solo, C-109)
**Depende de:** U07-D (terminada), y por ella U07-C, U06-C, U07-B, U07-A, U06-A, U06-B, U04 y U05. Sale de `<punta VERDE de U07-D>`. Asume como dado todo lo que fijaron esas tareas: `Plan` y sus estados, `PlanRepositorio`, `PlanServicio`, `VencimientoPlanes` (`vencerPendientes`, `vencerSiToca`), `GuardaConfirmacion`, `ConfirmacionServicio`, `ReservaConfirmacion`, `ResumenCatalogo`, `Usuario` (`X-Synkra-Usuario`), `RutasConfirmacion` (202), `CodigosDespliegue` y `MotivosDespliegue`, el bean `Clock` de `plataforma/Reloj.java`, `RelojMovible`, `SimuladorKubernetes` (con `fallaAplicar` y `fallaLecturas`), `RegistroApi` (`registrar`, `actualizarResultado`, `obtener`, `historial`, `ultimoListo`), `GET /api/despliegues/{id}` y `GET /api/paises/{pais}/despliegues`, el registro en `aplicando` **antes** de aplicar (desviación de R-D9 aprobada por David en U07-D) y las carpetas `build/sqlite-pruebas/<Clase>`.

---

## Alcance

**Dentro** (una línea, concreta):

> La vigilancia de R-D10 como una pasada periódica (`VigilanciaDespliegues.revisar()`) sobre los despliegues `aplicando` del registro (`RegistroApi.aplicando()`, C-116): `listo` si lo que corre es lo confirmado y está listo, `fallido` con el contexto de U05 si el pod falla o vence el plazo (C-115 incluido), sin escribir nunca en el clúster; la programación con `@EnableScheduling` y propiedades (`synkra.vigilancia.intervalo`, `synkra.vigilancia.plazo`, `synkra.planes.barrido`), que retoma sola tras un reinicio (R-D12) y vence los planes que nadie toca (C-109); y en `kubernetes`: el cliente sin reintentos implícitos (C-76), `estado` que juzga la versión nueva (C-82) y los eventos del Deployment y sus ReplicaSet en el contexto (C-83).

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- U07-T10 en adelante: la reversión (R-D11, U07-F), las fronteras de `despliegue` (`allowedDependencies`, U07-T12) y el resumen (U07-G).
- Cambiar la confirmación: `ConfirmacionServicio`, `ReservaConfirmacion`, `RutasConfirmacion` y `PlanServicio` no cambian. «Lanzar la vigilancia» (final de R-D9) es la siguiente pasada programada (decisión 3): no hay hilo por despliegue, ni `@Async`, ni llamada a la vigilancia desde la confirmación.
- Cualquier reintento de una escritura: la vigilancia nunca llama a `construir` ni a `aplicar`, ni revierte (P2). Volver a **leer** en la pasada siguiente es el sondeo, no un reintento.
- `REQUIRES_NEW` en cualquier sitio (C-107), y `@Transactional` en la vigilancia o en las tareas programadas (no se retiene la única conexión mientras se habla con el clúster).
- C-90, C-110, C-112, C-113 (el reloj privado de `RegistroServicio` sigue igual: las pruebas no afirman `terminado_en` con valor exacto) y C-114: siguen como están.
- Los logs de todos los pods en el contexto (C-83, decisión 9): sigue el log de un solo pod.
- Cualquier cambio en `catalogo`, `plataforma`, `verificador/`, `core/`, `plugin-api/`, `plugins/`, `init-descarga/`, las migraciones, `build.gradle.kts`, `FronterasModulosTest.java` y los `package-info.java`. En `registro` solo se **añade** `aplicando()` (decisión 5); en `kubernetes` solo cambian `ConfiguracionKubernetes.java` y `LectorPais.java` (decisiones 7 a 9); en `application.properties` solo se **añaden** las líneas de la decisión 4.
- Cambiar pruebas existentes. Solo se **añaden** líneas a `SimuladorKubernetes.java` (decisión 10) y un archivo nuevo de configuración de pruebas (decisión 4). Si una prueba existente cae por las decisiones 8 o 9, el codificador no la toca: lo anota en la bitácora y para (lo arbitra el orquestador).
- Nada de `kubectl` ni de clúster real: el único Kubernetes es el servidor simulado de Fabric8 (AUTONOMIA-01).

---

## Archivos de contexto

- `tareas/U07-D-confirmar.md` (la base: decisiones 6 y 12), `tareas/U07-C-plan-vence.md` (decisión 6: `vencerPendientes()` sin programar), `tareas/U06-A-registro-base.md` y `tareas/U06-B-historial-reinicio.md` (R-R1 a R-R4, reinicio con `.run(...)`)
- `aidlc-docs/construction/plans/U07-despliegue-code-generation-plan.md` (U07-T08 y U07-T09)
- `aidlc-docs/construction/U07-despliegue/functional-design/business-rules.md` (R-D9, R-D10, R-D12) y `business-logic-model.md` (estados)
- `aidlc-docs/construction/U06-registro/functional-design/business-rules.md` (R-R2 a R-R4)
- `aidlc-docs/inception/user-stories/stories.md` (H-14 y H-16)
- `unidades-y-tareas.md` (sección U07)
- En la punta de U07-D: `backend/src/main/java/io/synkra/despliegue/` (todo), `backend/src/main/java/io/synkra/registro/` (`RegistroServicio`, `api/RegistroApi`, `api/DespliegueRegistrado`, `api/ContextoFallo`), `backend/src/main/java/io/synkra/kubernetes/` (`api/AdaptadorKubernetes`: `disponible`, `estado`, `contextoFallo`, `leerPais`; `api/EstadoPais`, `api/FaseDespliegue`, `api/ContextoFalloK8s`, las excepciones de lectura; `ConfiguracionKubernetes`, `LectorPais`)
- `backend/src/test/java/io/synkra/despliegue/ConfirmarTest.java`, `CatalogoDePrueba.java` y `RelojMovible.java`; `backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java`, `EstadoYContextoTest.java` (cómo se siembran pods, eventos y logs) y `ClienteKubernetesTest.java` (cliente con cuenta y TLS); `backend/src/test/java/io/synkra/registro/RegistroReinicioTest.java` (reinicio)
- `backend/src/main/java/io/synkra/plataforma/BaseDeDatos.java` (pool de **una** conexión) y `Reloj.java`
- `CANDIDATAS.md` (C-76, C-82, C-83, C-107, C-108, C-109, C-111, C-113, C-115, C-116)

---

## Decisiones del orquestador

1. **Nombres nuevos.** Todo en `io.synkra.despliegue`, no público.
   - `VigilanciaDespliegues` (`@Component`): `int revisar()`, una pasada sobre `registro.aplicando()`; devuelve cuántos despliegues cerró (a `listo` o `fallido`). Inyecta `RegistroApi`, `AdaptadorKubernetes` (en un campo llamado `kubernetes`, C-108), el bean `Clock` y `@Value("${synkra.vigilancia.plazo}") Duration plazo`. **No** es `@Transactional`.
   - `TareasProgramadas` (`@Configuration(proxyBeanMethods = false)`, `@EnableScheduling`, `@ConditionalOnProperty(name = "synkra.programacion.activa", havingValue = "true")`) con dos métodos: `@Scheduled(fixedDelayString = "${synkra.vigilancia.intervalo}") void vigilar()`, que llama a `vigilancia.revisar()`, y `@Scheduled(fixedDelayString = "${synkra.planes.barrido}") void vencer()`, que llama a `vencimiento.vencerPendientes()` (C-109). Es el **único** sitio con `@Scheduled` o `@EnableScheduling` del backend.
   - En `MotivosDespliegue` se añaden:
     - `VIGILANCIA_PLAZO = "Venció el plazo de vigilancia sin que el despliegue quedara listo"`;
     - `VIGILANCIA_NO_APLICADO = "Lo que corre en el clúster no es lo que se confirmó"`;
     - `VIGILANCIA_SIN_LECTURA = "No se pudo leer el estado del despliegue en el clúster"`.
2. **R-D10: una pasada (`revisar()`), el orden es contrato.** Para cada `d` de `registro.aplicando()`, en ese orden, y aislando cada fila (una `RuntimeException` de una fila se registra en el log y la pasada sigue con la siguiente):
   1. `vencido = reloj.instant()` ≥ `d.confirmadoEn() + plazo` (a `plazo − 1 ms` todavía no; a `plazo` exacto, sí).
   2. La lectura, solo con lecturas del adaptador:
      - Si `!kubernetes.disponible()`: **sin lectura** (sin llamar a nada más).
      - Si no, `kubernetes.leerPais(d.pais())`. **Coincide** si está presente, su imagen es `d.imagen()` y su conjunto de `(nombre, versión, sha256)` es el de `d.plugins()`. Vacío, o distinto: **no aplicado**. `ClienteNoDisponibleException`, `LecturaFallidaException` o `LecturaPaisInconsistenteException`: **sin lectura**.
      - Solo si coincide, `kubernetes.estado("synkra-" + d.pais())`. `DespliegueAusenteException`: no aplicado; `ClienteNoDisponibleException` o `LecturaFallidaException`: sin lectura.
   3. La decisión:
      - Coincide y `LISTO`: `registro.actualizarResultado(d.id(), LISTO, null)`. No se lee el contexto.
      - Coincide y `FALLIDO` (`CrashLoopBackOff`, `Init:Error`…): `fallido` **en esta misma pasada**, sin esperar al plazo, con `ContextoFallo(k.logCore(), k.logInit(), k.eventos())` de `k = kubernetes.contextoFallo("synkra-" + d.pais())`.
      - Cualquier otro caso (`EN_CURSO`, no aplicado o sin lectura): si `!vencido`, nada (sigue `aplicando`); si `vencido`, `fallido` con `ContextoFallo(logCore, logInit, eventos)`, donde `eventos` es `VIGILANCIA_PLAZO`, más `"\n" + VIGILANCIA_NO_APLICADO` si no estaba aplicado o `"\n" + VIGILANCIA_SIN_LECTURA` si no hubo lectura, más `"\n" + k.eventos()` si no es vacío; y `logCore`, `logInit` y `k` salen de `contextoFallo` si hay cliente y la llamada no falla (si no, `""`).
   - Un `TransicionNoPermitidaException` de `actualizarResultado` (otra pasada ya la cerró) no es un error: la fila se salta.
   - Una fila cerrada no se vuelve a mirar (ya no está en `aplicando()`): después de un `fallido` no hay ninguna petición más para ese país. Una pasada sin filas `aplicando` no hace **ninguna** petición al clúster.
   - **C-115**: un `aplicando` sin nada aplicado (el proceso murió entre `reservar` y `aplicar`, desviación de R-D9) es «no aplicado»: se cierra por plazo con `VIGILANCIA_PLAZO` + `VIGILANCIA_NO_APLICADO`. Lo mismo si en el país corre otra cosa (un despliegue anterior listo): **nunca** se da por `listo` lo que no es lo confirmado (C-82, del lado de `despliegue`).
3. **La programación (`TareasProgramadas`).**
   - `fixedDelay` (no `fixedRate`): una pasada empieza cuando terminó la anterior, así que dos pasadas nunca se solapan, aunque una lectura tarde más que el intervalo. El planificador de Spring Boot tiene un solo hilo (sin tocar `spring.task.scheduling.*`): `vigilar` y `vencer` no corren a la vez.
   - Sin `initialDelay`: la primera pasada corre al arrancar. Por eso **R-D12 sale por construcción**: la vigilancia no guarda nada en memoria; al arrancar, la primera pasada encuentra las filas `aplicando` en SQLite y las vigila. Nunca las reaplica (no hay `aplicar` en la vigilancia).
   - El plazo cuenta desde `confirmado_en` del registro, que U07-D toma del bean `Clock`; la vigilancia usa el mismo bean. Así las pruebas mueven el tiempo con `RelojMovible` y **nunca** esperan los 3 minutos de verdad.
4. **Las propiedades.**
   - Se **añaden** a `application.properties`, con un comentario de una línea cada una:
     ```properties
     synkra.programacion.activa=true
     synkra.vigilancia.intervalo=${SYNKRA_VIGILANCIA_INTERVALO:PT5S}
     synkra.vigilancia.plazo=${SYNKRA_VIGILANCIA_PLAZO:PT3M}
     synkra.planes.barrido=${SYNKRA_PLANES_BARRIDO:PT1M}
     ```
   - Los valores van en ISO-8601 (`PT5S`): es el formato que acepta `fixedDelayString` (Spring 6.2). `VigilanciaDespliegues` rechaza al arrancar un `plazo` cero o negativo con `IllegalStateException("synkra.vigilancia.plazo debe ser positivo")`.
   - Archivo **nuevo** `backend/src/test/resources/config/application.properties` con una sola línea, `synkra.programacion.activa=false`. Spring Boot lo lee además del principal y le gana (`classpath:/config/` va antes que `classpath:/`), así que en **todas** las pruebas existentes (`ConfirmarTest`, `PlanPendienteTest`, `CrearPlanTest`…) no hay tareas programadas: sus listas de peticiones y el `vencerPendientes()` de `ec` de `PlanPendienteTest` no cambian. No entra en el JAR. La prueba que necesita la programación la enciende con `--synkra.programacion.activa=true` (decisión 12).
   - **Sin `sleep` fijo en ninguna prueba**: `VigilanciaTest` llama a `revisar()` a mano con la programación apagada; `RetomarVigilanciaTest` usa la programación real con intervalos de 100 ms y espera con Awaitility (`org.awaitility`, ya viene con `spring-boot-starter-test`): `await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(50)).until(...)`. Ningún `Thread.sleep` en las pruebas nuevas.
5. **C-116: `RegistroApi.aplicando()`.** Se **añade** a la interfaz `List<DespliegueRegistrado> aplicando()`: los despliegues en `aplicando` de **todos** los países, ordenados por `confirmado_en ASC, id ASC` (el más antiguo primero: el que antes vence), sin contexto y con sus plugins (ordenados por nombre, como en `obtener`). Lo implementa `RegistroServicio` con `@Transactional(readOnly = true)`, sin mirar `synkra.paises` (como `ultimoListo`: una fila `aplicando` de un país que ya no está configurado también se vigila y se cierra). Prueba nueva `AplicandoTest` en `registro` (decisión 13). Nada más de `registro` cambia.
6. **`despliegue` sigue usando `registro` solo por `registro.api`**, y `kubernetes` solo por `kubernetes.api` (`EstadoPais`, `FaseDespliegue`, `ContextoFalloK8s`, `LecturaPais` y las excepciones). El mapeo de `ContextoFalloK8s` a `ContextoFallo` es campo a campo, en la vigilancia (contrato de U06-A).
7. **C-76: el cliente de producción sin reintentos implícitos.** En `ConfiguracionKubernetes.cliente(...)` se añade `.withRequestRetryBackoffLimit(0)`. Cubre también el `aplicar` de U07-D (una petición por objeto, también fuera de las pruebas) y quita los hasta 11 `GET` y unos 19 s por lectura ante un 5xx o 429, que chocaban con el sondeo de 5 s. Los tiempos de espera de conexión y de petición quedan en los valores por defecto de Fabric8 (10 s): con `fixedDelay`, una lectura lenta solo retrasa la pasada siguiente. Prueba nueva `ReintentosClienteTest` (decisión 13).
8. **C-82: `estado` juzga la versión nueva** (`LectorPais.estado`). `LISTO` exige, además de `listas == deseadas > 0`:
   - `status.replicas == deseadas` (sin pods viejos sobrando), `status.updatedReplicas == deseadas` y `status.observedGeneration ≥ metadata.generation`; un campo nulo cuenta como cumplido (un clúster real siempre los da; así las pruebas de U05, que no los siembran, siguen igual);
   - y **ningún** pod del país con razón de fallo: una razón de fallo en cualquier pod gana a las réplicas listas (`FALLIDO` con esa razón). Así un pod viejo listo ya no tapa a uno nuevo que falla.
   - Si no es `LISTO` ni hay razón de fallo, `EN_CURSO`, como hoy. Ninguna petición nueva: se leen los mismos objetos. Prueba nueva `EstadoRolloutTest` (decisión 13); `EstadoYContextoTest` no cambia y debe seguir pasando.
9. **C-83: el contexto trae también los eventos del Deployment y de sus ReplicaSet** (`LectorPais.contexto`). Además de los eventos de los pods del país, entran los eventos cuyo `involvedObject` es el `Deployment` `synkra-core` o un `ReplicaSet` cuyo nombre empieza por `synkra-core-` (así un `FailedCreate` por cuota, sin ningún pod, llega al registro). Mismo formato (`tipo razón: mensaje`), mismo orden y la misma única lista de eventos (sin peticiones nuevas). El log sigue siendo el del pod con más reinicios: R-R4 pide **el** log del core (200 líneas) y el del init; los fallos de los demás pods quedan en los eventos. Se anota en la bitácora. Prueba nueva `ContextoEventosTest` (decisión 13).
10. **Ayudas nuevas en `SimuladorKubernetes`** (solo se **añaden** líneas; su API pública sigue sin tipos de Fabric8):
    - constantes públicas `LOG_CORE = "core: no arranca\n"`, `LOG_INIT = "descarga: plugins listos\n"` y `EVENTO = "Warning BackOff: Back-off restarting failed container"`;
    - `void marcaListo(String pais)`: pone en el `status` del Deployment `synkra-core` de `synkra-<pais>` `replicas`, `readyReplicas` y `updatedReplicas` iguales a `spec.replicas` (o 1) y `observedGeneration` igual a `metadata.generation`, borra antes los pods que haya en `synkra-<pais>` (un rollout terminado reemplaza los pods; así un pod que falló antes no tapa el listo, decisión 8) y crea un pod `synkra-core-listo` corriendo y listo (con la etiqueta que usa `LectorPais`);
    - `void marcaFallo(String pais, String razon)`, con `razon` `CrashLoopBackOff` o `Init:Error`: crea el pod `synkra-core-fallo` (3 reinicios) con el contenedor `core` en espera por `CrashLoopBackOff`, o con el init terminado con código 1; un evento `Warning`/`BackOff`/`Back-off restarting failed container` de ese pod; y sirve su log del init (`LOG_INIT`) y, solo con `CrashLoopBackOff`, el del core (`LOG_CORE`), con las rutas que pide `LectorPais` (como `EstadoYContextoTest`);
    - `void marcaRolloutConFallo(String pais)`: el Deployment con `spec.replicas` 1 y `status` `replicas` 2, `readyReplicas` 1 y `updatedReplicas` 1; un pod viejo `synkra-core-viejo` listo (0 reinicios) y uno nuevo `synkra-core-nuevo` en `CrashLoopBackOff` (3 reinicios) con el evento y los logs de `marcaFallo`;
    - todas drenan lo que siembran (no cuenta en `nuevasPeticiones()`). En modo CRUD el `status` puede sobrevivir a un `PATCH` posterior: las pruebas no afirman `EN_CURSO` después de un `marcaListo` del mismo país.
11. **`VigilanciaTest` (U07-T08): datos y casos.**
    - `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@TestBean AdaptadorKubernetes adaptador` (fábrica que devuelve `SIM.adaptador()`) y `@TestBean Clock reloj` (fábrica que devuelve un `RelojMovible`), como `ConfirmarTest`; catálogo con `CatalogoDePrueba` (S2: `synkra-core` 2.1.0 + `kyc-pe` 1.3.0, imagen `a`). Carpeta fija `build/sqlite-pruebas/VigilanciaTest` (el `@DynamicPropertySource` la borra y crea), `synkra.paises=pe,co,cl,ar,uy,bo,ec,py` y `synkra.vigilancia.plazo=PT2M` (distinto del valor por defecto, para que la propiedad muerda). La programación queda apagada (decisión 4) y la prueba llama a `vigilancia.revisar()`.
    - `T2 = 2026-11-22T15:00:00.789Z` (`1795359600789` ms). Cada prueba usa su propio país y su propia hora base, y **termina con todas sus filas cerradas** (`revisar()` recorre todos los países: así ninguna prueba ve filas de otra, sea cual sea el orden). Los planes se crean y confirman por HTTP (`X-Synkra-Usuario: ana` al crear, `luis` al confirmar), a la hora base. Se drena `SIM.nuevasPeticiones()` justo después de cada confirmación (sus tres `PATCH`): desde ahí, toda petición de la prueba es `GET` (**cero escrituras**, R-D10), y se afirma en cada caso.
      - `pe`, base `T2` (listo): `revisar()` a `T2 + 10 s` da 0 y la fila sigue `aplicando`; `SIM.marcaListo("pe")`; `revisar()` a `T2 + 20 s` da 1, `registro.obtener(id)` es `LISTO` sin contexto y `GET /api/despliegues/{id}` da `estado` `listo`; ninguna petición fue a un `/log`. Otra `revisar()` da 0 y `nuevasPeticiones()` queda vacía.
      - `co`, base `T2 + 1 h` (vence el plazo, H-14): `revisar()` a `T2 + 1 h + 2 min − 1 ms` da 0 (`aplicando`); a `T2 + 1 h + 2 min` da 1 y la fila es `FALLIDO` con contexto `("", "", VIGILANCIA_PLAZO)`. `GET /api/paises/co/despliegues` tiene una fila, también tras otra `revisar()` (no se crea otro despliegue ni se revierte).
      - `cl`, base `T2 + 2 h` (`CrashLoopBackOff`, antes del plazo): `SIM.marcaFallo("cl", "CrashLoopBackOff")`; `revisar()` a `T2 + 2 h + 30 s` da 1 y la fila es `FALLIDO` con contexto `(LOG_CORE, LOG_INIT, EVENTO)`; `GET /api/despliegues/{id}` trae `contexto` con esos tres textos.
      - `ar`, base `T2 + 3 h` (`Init:Error`): `SIM.marcaFallo("ar", "Init:Error")`; a `T2 + 3 h + 30 s`, `FALLIDO` con `("", LOG_INIT, EVENTO)`.
      - `uy`, base `T2 + 4 h` (C-82, rollout): `SIM.marcaRolloutConFallo("uy")`; a `T2 + 4 h + 30 s`, `FALLIDO` (no `LISTO`) con `(LOG_CORE, LOG_INIT, EVENTO)`.
      - `bo`, base `T2 + 5 h` (C-115 y lo que corre no es lo confirmado): se siembra `bo` con `SIM.siembra(...)` (imagen `b` y `kyc-co` 1.0.0) y `SIM.marcaListo("bo")`; se registra con `RegistroApi.registrar` el id 950 (`bo`, S2 con imagen `a`, `confirmadoEn` `T2 + 5 h`), sin confirmar ni aplicar (lo que deja un proceso que murió entre `reservar` y `aplicar`). `revisar()` a `T2 + 5 h + 1 min` da 0 (aunque `estado` diría `LISTO`); a `T2 + 5 h + 2 min`, `FALLIDO` con `("", "", VIGILANCIA_PLAZO + "\n" + VIGILANCIA_NO_APLICADO)`.
      - `py`, base `T2 + 7 h` (C-115 con el país vacío): id 951 registrado igual en `py`, sin nada en el clúster. A `+ 2 min`, `FALLIDO` con el mismo contexto que `bo`.
      - `ec`, base `T2 + 6 h` (sin lectura): tras confirmar, `SIM.fallaLecturas("ec")`. A `T2 + 6 h + 1 min`, 0 (`aplicando`: una lectura fallida no cierra nada); a `+ 2 min`, `FALLIDO` con `("", "", VIGILANCIA_PLAZO + "\n" + VIGILANCIA_SIN_LECTURA)`.
    - Todo contexto se compara exacto (los tres campos). Ningún caso afirma `terminado_en` con valor exacto (C-113).
12. **`RetomarVigilanciaTest` (U07-T09, R-D12, y C-109): dos arranques sobre la misma carpeta.**
    - Dos contextos con `new SpringApplicationBuilder(SynkraApp.class).initializers(...).run("--clave=valor", ...)` (C-111: propiedades por `.run`, nunca `.properties(...)`). El inicializador registra en el `GenericApplicationContext` dos beans **primarios** (`registerBean(..., d -> d.setPrimary(true))`): `AdaptadorKubernetes` (`SIM.adaptador()`) y `Clock` (un `RelojMovible` compartido por los dos arranques). Sin `allow-bean-definition-overriding`. Argumentos comunes: `--synkra.data=<carpeta fija build/sqlite-pruebas/RetomarVigilanciaTest, borrada al empezar>`, `--server.port=0`, `--synkra.paises=pe,co`, `--synkra.vigilancia.plazo=PT3M`.
    - `T4 = 2026-11-22T18:00:00.789Z` (`1795370400789` ms).
    - Primer arranque, con `--synkra.programacion.activa=false`: catálogo con `CatalogoDePrueba`; a `T4` se crean por HTTP un plan S2 en `pe` (id 1) y otro en `co` (id 2); a `T4 + 1 min` se confirman los dos (dos 202, seis `PATCH`) y se crea otro plan S2 en `pe` que nadie confirma (id 3). Se cierra el contexto con las dos filas `aplicando`.
    - Entre arranques: `SIM.marcaListo("pe")` (en `co` nada) y `reloj.fija(T4 + 2 min)`; se drenan las peticiones.
    - Segundo arranque, con `--synkra.programacion.activa=true`, `--synkra.vigilancia.intervalo=PT0.1S` y `--synkra.planes.barrido=PT0.1S`: con Awaitility, `pe` (id 1) llega a `LISTO`; `co` (id 2) sigue `aplicando`. Con `reloj.fija(T4 + 4 min)`, `co` llega a `FALLIDO` con `("", "", VIGILANCIA_PLAZO)`. Con `reloj.fija(T4 + 31 min)`, el plan 3 llega a `vencido` sin que nadie lo toque (C-109). En todo el segundo arranque, `nuevasPeticiones()` solo trae `GET` (no se reaplica: cero escrituras).
    - Otro caso, `unPlazoNoPositivoNoArranca`: un arranque con `--synkra.vigilancia.plazo=PT0S` (en otra carpeta temporal) falla y su causa raíz es la `IllegalStateException` con el mensaje exacto de la decisión 4.
    - Cada contexto se cierra en un `finally`.
13. **Pruebas nuevas fuera de `despliegue`.**
    - `AplicandoTest` (`registro`, `@SpringBootTest` con la carpeta fija `build/sqlite-pruebas/AplicandoTest`): por `RegistroApi` se registran 41 (`pe`, `t + 2 s`), 42 (`co`, `t + 1 s`), 43 (`pe`, `t`, termina `LISTO`), 44 (`co`, `t`, termina `FALLIDO`) y 45 (`cl`, `t + 2 s`). `aplicando()` da exactamente los ids `[42, 41, 45]`, todos `APLICANDO`, con `contexto` nulo y los plugins de cada uno.
    - `ReintentosClienteTest` (`kubernetes`): el adaptador que arma `ConfiguracionKubernetes` con una cuenta de prueba (servidor simulado en TLS, como `ClienteKubernetesTest`; si hace falta compartir código, va en un ayudante de prueba **nuevo** y `ClienteKubernetesTest` no cambia). Su cliente tiene `getRequestRetryBackoffLimit()` 0. Con el `GET` del Deployment de `synkra-pe` respondiendo 500 siempre, `estado("synkra-pe")` lanza `LecturaFallidaException` y el servidor recibió **un** `GET` a esa ruta; con el `PATCH` del ConfigMap respondiendo 500 siempre, `aplicar(construir(plan))` lanza `AplicacionFallidaException` y hubo **un** `PATCH`.
    - `EstadoRolloutTest` (`kubernetes`, servidor simulado como `EstadoYContextoTest`): pod viejo listo + pod nuevo en `CrashLoopBackOff` con réplicas listas = deseadas → `FALLIDO` con razón `CrashLoopBackOff`; pod viejo listo + pod nuevo `Pending` (`replicas` 2, `updatedReplicas` 1, `readyReplicas` 1, deseadas 1) → `EN_CURSO`; `observedGeneration` 1 con `generation` 2 y todo lo demás listo → `EN_CURSO`; todos los campos presentes y coherentes → `LISTO`.
    - `ContextoEventosTest` (`kubernetes`): sin pods, un evento `Warning FailedCreate: <mensaje>` del `ReplicaSet` `synkra-core-7d9f` y otro `Normal ScalingReplicaSet: <mensaje>` del `Deployment` `synkra-core` → `eventos` es exactamente esas dos líneas en orden de hora; un evento del `ReplicaSet` `otro-5c4b` y uno del `Deployment` `otro` no aparecen; con un pod del país, sus eventos y los del ReplicaSet salen mezclados por hora.
14. **Bitácora en `bitacoras/U07-E.md`, evidencia en `bitacoras/U07-E-evidencia/`** (un `caN.txt` por criterio, salida completa con fecha, sha y comando), y el revisor en `revisiones/U07-E/`. La bitácora anota las decisiones 8 y 9 (qué cambió en `kubernetes` y por qué) y el argumento de R-D12 por construcción (decisión 3).

---

## Criterios de aceptación

- [ ] **CA-1**: U07-T08. La vigilancia contra el servidor simulado, sin escrituras, con lectura de vuelta
  ```bash
  ./gradlew :backend:test --rerun --tests '*VigilanciaTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*VigilanciaTest.xml
  grep -o 'testcase name="[A-Za-z0-9]*()"' backend/build/test-results/test/TEST-*VigilanciaTest.xml
  python3 -I - backend/build/sqlite-pruebas/VigilanciaTest/synkra.db <<'EOF'
  import sqlite3, sys
  c = sqlite3.connect(sys.argv[1])
  q = lambda s: print(c.execute(s).fetchall())
  q("select r.pais, r.estado, x.log_core, x.log_init, x.eventos from reg_despliegue r"
    " left join reg_contexto x on x.despliegue_id = r.id order by r.pais, r.id")
  q("select (select count(*) from reg_despliegue where estado = 'aplicando'),"
    " (select count(*) from reg_despliegue where terminado_en is null),"
    " (select count(*) from reg_despliegue r where r.id not in (select id from dep_plan))")
  EOF
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, `tests` ≥ 8 y `skipped="0" failures="0" errors="0"`, con los casos y afirmaciones de la decisión 11 (un `testcase` por país, nombres en la evidencia).
  - Python, en orden:
    - `[('ar', 'fallido', '', 'descarga: plugins listos\n', 'Warning BackOff: Back-off restarting failed container'), ('bo', 'fallido', '', '', 'Venció el plazo de vigilancia sin que el despliegue quedara listo\nLo que corre en el clúster no es lo que se confirmó'), ('cl', 'fallido', 'core: no arranca\n', 'descarga: plugins listos\n', 'Warning BackOff: Back-off restarting failed container'), ('co', 'fallido', '', '', 'Venció el plazo de vigilancia sin que el despliegue quedara listo'), ('ec', 'fallido', '', '', 'Venció el plazo de vigilancia sin que el despliegue quedara listo\nNo se pudo leer el estado del despliegue en el clúster'), ('pe', 'listo', None, None, None), ('py', 'fallido', '', '', 'Venció el plazo de vigilancia sin que el despliegue quedara listo\nLo que corre en el clúster no es lo que se confirmó'), ('uy', 'fallido', 'core: no arranca\n', 'descarga: plugins listos\n', 'Warning BackOff: Back-off restarting failed container')]`;
    - `[(0, 0, 2)]` (ninguna fila abierta; solo 950 y 951 no vienen de un plan).

- [ ] **CA-2**: U07-T09. Tras un reinicio se retoma la vigilancia sin reaplicar, y los planes vencen solos (C-109)
  ```bash
  ./gradlew :backend:test --rerun --tests '*RetomarVigilanciaTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*RetomarVigilanciaTest.xml
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select id, pais, estado from dep_plan order by id').fetchall()); print(c.execute('select id, pais, estado, creado_por, confirmado_por from reg_despliegue order by id').fetchall()); print(c.execute('select despliegue_id, log_core, log_init, eventos from reg_contexto').fetchall())" backend/build/sqlite-pruebas/RetomarVigilanciaTest/synkra.db
  grep -rn "Thread\.sleep" backend/src/test/java/io/synkra/despliegue/VigilanciaTest.java backend/src/test/java/io/synkra/despliegue/RetomarVigilanciaTest.java; echo "sleep=$?"
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, `tests` ≥ 2 y sin saltos ni fallos, con lo de la decisión 12 (en el segundo arranque, solo `GET`).
  - `[(1, 'pe', 'confirmado'), (2, 'co', 'confirmado'), (3, 'pe', 'vencido')]`;
  - `[(1, 'pe', 'listo', 'ana', 'luis'), (2, 'co', 'fallido', 'ana', 'luis')]`;
  - `[(2, '', '', 'Venció el plazo de vigilancia sin que el despliegue quedara listo')]`;
  - `sleep=1`.

- [ ] **CA-3**: C-76, C-82, C-83 y C-116 en sus módulos, y las pruebas de U05 y U06 siguen igual
  ```bash
  ./gradlew :backend:test --rerun --tests '*ReintentosClienteTest' --tests '*EstadoRolloutTest' --tests '*ContextoEventosTest' --tests '*AplicandoTest' --tests '*EstadoYContextoTest' --tests '*ClienteKubernetesTest' --tests '*AplicarTest' --tests '*HistorialTest'
  for c in ReintentosClienteTest EstadoRolloutTest ContextoEventosTest AplicandoTest EstadoYContextoTest ClienteKubernetesTest AplicarTest HistorialTest; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*.$c.xml; done
  grep -n "withRequestRetryBackoffLimit" backend/src/main/java/io/synkra/kubernetes/ConfiguracionKubernetes.java
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select id, pais, estado from reg_despliegue order by id').fetchall())" backend/build/sqlite-pruebas/AplicandoTest/synkra.db
  ```
  Esperado:
  - `BUILD SUCCESSFUL` y las ocho clases sin saltos ni fallos (`tests` ≥ 2 en `ReintentosClienteTest`, ≥ 4 en `EstadoRolloutTest`, ≥ 3 en `ContextoEventosTest`, ≥ 1 en `AplicandoTest`), con lo de la decisión 13.
  - Una sola línea: `.withRequestRetryBackoffLimit(0)`.
  - `[(41, 'pe', 'aplicando'), (42, 'co', 'aplicando'), (43, 'pe', 'listo'), (44, 'co', 'fallido'), (45, 'cl', 'aplicando')]`.

- [ ] **CA-4**: el JAR real, sin clúster: retoma al arrancar, cierra por plazo lo que no puede leer y vence los planes con los valores por defecto
  ```bash
  ./gradlew :backend:bootJar
  export SYNKRA_DATA=$(mktemp -d); J=$(mktemp -d); H=$(mktemp -d)
  arranca() { HOME="$H" KUBECONFIG="$H/kubeconfig-falso" SYNKRA_PAISES=co,pe java -Duser.home="$H" -jar backend/build/libs/backend-*.jar --server.port=18094 > "$J/log-$1.txt" 2>&1 & PID=$!; timeout 90 bash -c 'until curl -sf localhost:18094/actuator/health >/dev/null; do sleep 1; done'; }
  # 1. Primer arranque: solo migra.
  arranca 0; kill $PID; wait $PID 2>/dev/null
  # 2. Filas sembradas con SQL: un aplicando viejo (pe), uno reciente (co) y dos planes pendientes (pe).
  python3 -I - "$SYNKRA_DATA/synkra.db" <<'EOF'
  import sqlite3, sys, time
  c = sqlite3.connect(sys.argv[1]); ahora = int(time.time() * 1000); m = 60000
  img = "localhost:5001/synkra-core@sha256:" + "a" * 64
  for i, pais, estado, en in ((91, "pe", "confirmado", ahora - 5 * m), (92, "co", "confirmado", ahora),
                              (93, "pe", "pendiente", ahora - 31 * m), (94, "pe", "pendiente", ahora)):
      c.execute("insert into dep_plan (id, pais, tipo, origen_id, core, version_core, plugins, resumen, cambios, estado, creado_por,"
                " creado_en, vence_en) values (?, ?, 'despliegue', null, 'synkra-core', '2.1.0', '[]', '{}', '{}', ?, 'ana', ?, ?)",
                (i, pais, estado, en, en + 30 * m))
  for i, pais, en in ((91, "pe", ahora - 4 * m), (92, "co", ahora)):
      c.execute("insert into reg_despliegue (id, pais, tipo, origen_id, core, version_core, imagen, creado_por, confirmado_por,"
                " confirmado_en, estado, terminado_en, veredicto) values (?, ?, 'despliegue', null, 'synkra-core', '2.1.0', ?,"
                " 'ana', 'luis', ?, 'aplicando', null, '{\"global\":\"APROBADA\",\"porPlugin\":[],\"motivosGlobales\":[]}')",
                (i, pais, img, en))
  c.commit()
  EOF
  # 3. Segundo arranque con los valores por defecto (PT5S, PT3M, PT1M): nadie llama a ninguna ruta.
  arranca 1
  python3 -I - "$SYNKRA_DATA/synkra.db" <<'EOF'
  import sqlite3, sys, time
  fin, r = time.time() + 60, None
  while time.time() < fin:
      c = sqlite3.connect("file:" + sys.argv[1] + "?mode=ro", uri=True)
      r = c.execute("select (select estado from reg_despliegue where id = 91), (select estado from dep_plan where id = 93)").fetchone()
      c.close()
      if r == ("fallido", "vencido"):
          break
      time.sleep(1)
  print("espera=" + ("0" if r == ("fallido", "vencido") else "1 %r" % (r,)))
  EOF
  for id in 91 92; do curl -s localhost:18094/api/despliegues/$id | python3 -I -c "import json,sys; r=json.load(sys.stdin); print(r['id'], r['estado'], r['contexto'])"; done
  kill $PID; wait $PID 2>/dev/null
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select id, estado from dep_plan order by id').fetchall()); print(c.execute('select id, estado from reg_despliegue order by id').fetchall())" "$SYNKRA_DATA/synkra.db"
  ```
  Esperado, en orden:
  - `espera=0` (en menos de 60 s, sin ninguna petición HTTP: lo hizo la programación);
  - `91 fallido {'logCore': '', 'logInit': '', 'eventos': 'Venció el plazo de vigilancia sin que el despliegue quedara listo\nNo se pudo leer el estado del despliegue en el clúster'}`;
  - `92 aplicando None` (su plazo no ha vencido);
  - `[(91, 'confirmado'), (92, 'confirmado'), (93, 'vencido'), (94, 'pendiente')]` (compárese con el plan 74 de U07-C, que seguía `pendiente`);
  - `[(91, 'fallido'), (92, 'aplicando')]`.

  Si el puerto 18094 está ocupado, se anota en la bitácora y se usa otro en todo el criterio. La bitácora pega la salida tal cual.

- [ ] **CA-5**: rojo primero. Cada regla nueva tiene una prueba que muerde, demostrado en una copia
  ```bash
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q)
  # Mutación anotada literalmente en la bitácora; una por regla, por ejemplo:
  #   listo:              revisar() nunca marca LISTO (pe sigue aplicando)
  #   coincide:           no se compara leerPais con el registro (bo da listo)
  #   fallo inmediato:    un FALLIDO espera al plazo (cl sigue aplicando a los 30 s)
  #   frontera del plazo: «>=» pasa a «>» (co a plazo exacto sigue aplicando)
  #   propiedad:          el plazo fijo en 3 min, sin leer synkra.vigilancia.plazo (co a 2 min sigue aplicando)
  #   contexto:           el fallido se registra con ContextoFallo("", "", "") (cl sin logs)
  #   sin lectura:        una LecturaFallidaException cierra en fallido en el acto (ec a 1 min da fallido)
  #   cero escrituras:    con EN_CURSO, revisar() llama a kubernetes.aplicar(kubernetes.construir(...)) (co trae PATCH)
  #   R-D12:              se quita @EnableScheduling (RetomarVigilanciaTest: pe nunca llega a listo)
  #   C-109:              vencer() no llama a vencerPendientes() (RetomarVigilanciaTest: el plan 3 sigue pendiente)
  #   C-116:              aplicando() sin «estado = 'aplicando'» (AplicandoTest: aparecen 43 y 44)
  #   C-116 orden:        «confirmado_en ASC» pasa a «DESC» (AplicandoTest)
  #   C-76:               se quita .withRequestRetryBackoffLimit(0) (ReintentosClienteTest: más de un GET)
  #   C-82:               LectorPais.estado vuelve a dar LISTO con listas == deseadas (uy y EstadoRolloutTest)
  #   C-83:               los eventos vuelven a filtrarse solo por pods (ContextoEventosTest)
  sed -i '<mutación>' "$D/backend/src/main/java/io/synkra/<módulo>/<Clase>.java"
  (cd "$D" && ./gradlew :backend:test --tests '*VigilanciaTest' --tests '*RetomarVigilanciaTest' --tests '*AplicandoTest' --tests '*ReintentosClienteTest' --tests '*EstadoRolloutTest' --tests '*ContextoEventosTest')
  grep -l '<failure' "$D"/backend/build/test-results/test/TEST-*.xml
  rm -rf "$D"
  ```
  Esperado: con cada una de las quince mutaciones (una copia por mutación, con `git init -q`), `BUILD FAILED`, y el XML de la clase dueña de la regla muestra al menos un `<failure` en la prueba de esa regla. La bitácora pega el `sed` (o el parche) y el nombre de la prueba que cayó. Sin `git stash` ni cambios en el worktree. Además, la bitácora registra el rojo de `VigilanciaTest`, `RetomarVigilanciaTest`, `AplicandoTest`, `ReintentosClienteTest`, `EstadoRolloutTest` y `ContextoEventosTest` antes de su implementación.

- [ ] **CA-6**: todo el backend sigue pasando, y la vigilancia solo lee
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  grep -rlE "kubernetes\.(aplicar|construir)\(" backend/src/main/java/io/synkra/despliegue
  grep -oE "kubernetes\.[a-zA-Z]+\(" backend/src/main/java/io/synkra/despliegue/VigilanciaDespliegues.java | sort -u
  grep -rnE "io\.synkra\.registro\." backend/src/main/java/io/synkra/despliegue | grep -vE "io\.synkra\.registro\.api\."; echo "registro-interno=$?"
  grep -rn "reg_" backend/src/main/java/io/synkra/despliegue; echo "tablas-reg=$?"
  grep -rln "@Scheduled\|@EnableScheduling" backend/src/main/java
  grep -c "@Scheduled(" backend/src/main/java/io/synkra/despliegue/TareasProgramadas.java
  grep -n "Transactional" backend/src/main/java/io/synkra/despliegue/VigilanciaDespliegues.java backend/src/main/java/io/synkra/despliegue/TareasProgramadas.java; echo "vigilancia-tx=$?"
  grep -rn "REQUIRES_NEW" backend/src/main/java; echo "requiresNew=$?"
  grep -rlE "Clock\.system" backend/src/main/java | sort
  cat backend/src/test/resources/config/application.properties
  ```
  Esperado:
  - `BUILD SUCCESSFUL` y ningún XML con `skipped`, `failures` o `errors` distinto de 0 (`ConfirmarTest`, `PlanPendienteTest`, `CrearPlanTest`, `CarpetaEquivocadaPlanTest`, `ConfirmarSinClusterTest`, `RelojTest`, `VistaGeneralTest`, `EstadoYContextoTest` y `FronterasModulosTest` pasan sin cambios).
  - Solo `backend/src/main/java/io/synkra/despliegue/ConfirmacionServicio.java` (la vigilancia no aplica nada).
  - Solo `kubernetes.contextoFallo(`, `kubernetes.disponible(`, `kubernetes.estado(` y `kubernetes.leerPais(`.
  - `registro-interno=1` y `tablas-reg=1`.
  - Solo `backend/src/main/java/io/synkra/despliegue/TareasProgramadas.java`, con `2`.
  - `vigilancia-tx=1` y `requiresNew=1`.
  - `backend/src/main/java/io/synkra/plataforma/Reloj.java` y `backend/src/main/java/io/synkra/registro/RegistroServicio.java` (sin relojes nuevos).
  - `synkra.programacion.activa=false` (una línea).

- [ ] **CA-7**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <punta VERDE de U07-D>...HEAD
  git diff --name-status <punta VERDE de U07-D>...HEAD -- backend/src/test backend/src/main/resources backend/src/main/java/io/synkra/registro backend/src/main/java/io/synkra/kubernetes backend/src/main/java/io/synkra/plataforma backend/src/main/java/io/synkra/catalogo
  git diff <punta VERDE de U07-D>...HEAD -- backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java backend/src/main/resources/application.properties backend/src/main/java/io/synkra/registro/api/RegistroApi.java | grep -c '^-[^-]'
  ```
  Esperado: solo estas rutas. Se anota el sha.
  - `backend/src/main/java/io/synkra/despliegue/**` (nuevos: `VigilanciaDespliegues.java` y `TareasProgramadas.java`; cambia solo `MotivosDespliegue.java`)
  - `backend/src/main/java/io/synkra/registro/api/RegistroApi.java` y `backend/src/main/java/io/synkra/registro/RegistroServicio.java` (`M`)
  - `backend/src/main/java/io/synkra/kubernetes/ConfiguracionKubernetes.java` y `LectorPais.java` (`M`)
  - `backend/src/main/resources/application.properties` (`M`)
  - `backend/src/test/java/io/synkra/despliegue/VigilanciaTest.java` y `RetomarVigilanciaTest.java`, `backend/src/test/java/io/synkra/registro/AplicandoTest.java`, `backend/src/test/java/io/synkra/kubernetes/ReintentosClienteTest.java`, `EstadoRolloutTest.java`, `ContextoEventosTest.java` (y algún ayudante de prueba nuevo si hace falta) y `backend/src/test/resources/config/application.properties`: `A`
  - `backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java`: `M`
  - la tercera orden da `0` (en el simulador, `application.properties` y `RegistroApi` solo se añade)
  - ninguna línea de `plataforma` ni de `catalogo`
  - `bitacoras/U07-E.md`, `bitacoras/U07-E-evidencia/` y `revisiones/U07-E/`

  `ca7.txt` se genera justo antes de su propio commit, con el árbol limpio y sin commits de código después: ese commit solo trae `ca7.txt` (y la línea de la bitácora que lo cita). Toda la demás evidencia sale del último commit que toca código.

---

**Rojo primero:** `VigilanciaTest`, `RetomarVigilanciaTest`, `AplicandoTest`, `ReintentosClienteTest`, `EstadoRolloutTest` y `ContextoEventosTest` antes de `VigilanciaDespliegues`, `TareasProgramadas`, `aplicando()` y los cambios de `ConfiguracionKubernetes` y `LectorPais`. Un commit «rojo» trae solo las pruebas, las líneas añadidas a `SimuladorKubernetes`, `config/application.properties` de pruebas y, si hace falta para compilar, firmas vacías que lanzan `UnsupportedOperationException` (`RegistroApi.aplicando()` en `RegistroServicio`, `VigilanciaDespliegues.revisar()`), nunca la lógica: la bitácora pega su sha y su `git show --stat`. Cada rojo queda en la bitácora, y CA-5 demuestra que cada regla muerde con el código terminado.
