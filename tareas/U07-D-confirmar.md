# U07-D: confirmar un plan, y el plugin de otra carpeta (P4)

**Unidad:** U07: `Despliegue y reversión`
**Tareas del plan que agrupa:** U07-T07 y U07-T11
**Historias que implementa:** H-12, H-06 (contribuye: P4 por HTTP), H-17 (cierra la segunda mitad: el plan pendiente no está en el registro)
**Depende de:** U07-C y U06-C (terminadas), y por ellas U07-A, U07-B, U06-A, U06-B, U04 y U05. Sale de `<unión de las puntas VERDES de U07-C y U06-C>`: el codificador hace el merge en su rama como **primer paso** (decisión 1, CA-0). Es la primera tarea que usa `registro.api` desde `despliegue`. Asume como dado todo lo que fijaron esas tareas: `Plan` y sus estados, `PlanRepositorio`, `PaisPermitido`, `VerificacionVivo`, `PlanServicio`, `RutasPlanes`, `VeredictoDto`, `Resumen` y `PluginResumen`, `CodigosDespliegue` y `MotivosDespliegue`, `VencimientoPlanes`, `GuardaConfirmacion` (R-D5), `RutasConfirmacion`, el bean `Clock` de `plataforma/Reloj.java`, `RelojMovible`, `SimuladorKubernetes`, `RegistroApi` (`registrar`, `actualizarResultado`, `obtener`, `historial`, `ultimoListo`), `GET /api/despliegues/{id}` y `GET /api/paises/{pais}/despliegues` de `registro`, y las carpetas `build/sqlite-pruebas/<Clase>`.

---

## Alcance

**Dentro** (una línea, concreta):

> `POST /api/despliegues/{id}/confirmacion` de verdad (reemplaza el 501 de U07-C): R-D5 con la guarda de U07-C, vuelve a verificar (R-D6), un solo `aplicando` por país (R-D7), guarda quién creó y quién confirmó (R-D8), registra en `aplicando` con `RegistroApi.registrar` y aplica con U05 `construir` + `aplicar`, y responde 202 (R-D9); un fallo al aplicar queda `fallido` en el registro, sin reintentos; y P4 (`otro-core` + `kyc-pe`): ni al crear ni al confirmar un plugin de otra carpeta llega al clúster.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- U07-T08 en adelante: la vigilancia (R-D10, `estado` y `contextoFallo` de U05), retomar tras reinicio (R-D12), la reversión (R-D11), las fronteras de `despliegue` (`allowedDependencies`, U07-T12) y el resumen. Tras el 202 la fila queda `aplicando` y nadie la vigila hasta U07-E.
- C-76, C-82, C-83 y C-109 (decisión 12): van en U07-E. C-35 y C-87 no se tocan (decisión 12). C-90 y C-110 siguen igual.
- Un barrido programado (`@Scheduled`, `@EnableScheduling`), `REQUIRES_NEW` en cualquier sitio (C-107) y un `GET` nuevo sobre `/api/despliegues/**` (es de `registro`).
- Cualquier cambio en el código de producción de `registro`, `catalogo`, `kubernetes` y `plataforma`, en `verificador/`, `core/`, `plugin-api/`, `plugins/`, `init-descarga/`, en las migraciones, `application.properties`, `build.gradle.kts`, `FronterasModulosTest.java` y los `package-info.java`. `RegistroServicio` sigue con su reloj privado (decisión 2). Usar desde `despliegue` algo de `registro` que no esté en `registro.api`, o leer o escribir las tablas `reg_*` con SQL desde `despliegue`.
- Cambiar pruebas existentes, salvo los dos casos de `PlanPendienteTest` que fija la decisión 10 y las líneas que **se añaden** a `SimuladorKubernetes.java` (decisión 9).
- Nada de `kubectl` ni de clúster real: el único Kubernetes es el servidor simulado de Fabric8 (AUTONOMIA-01).

---

## Archivos de contexto

- `tareas/U07-C-plan-vence.md` y `tareas/U06-C-vista-general-fronteras.md` (las dos puntas que se unen), `tareas/U07-B-crear-plan.md`, `tareas/U07-A-planes-pais.md`, `tareas/U06-A-registro-base.md` y `tareas/U06-B-historial-reinicio.md` (contratos previos)
- `aidlc-docs/construction/plans/U07-despliegue-code-generation-plan.md` (U07-T07 y U07-T11)
- `aidlc-docs/construction/U07-despliegue/functional-design/business-rules.md` (R-D5 a R-D9, R-D13), `business-logic-model.md` (estados) y `domain-entities.md`
- `aidlc-docs/inception/requirements/requirements.md` (RF-14) y `aidlc-docs/inception/user-stories/stories.md` (H-06, H-12 y H-17)
- `unidades-y-tareas.md` (sección U07)
- En la unión: `backend/src/main/java/io/synkra/despliegue/` (todo), `backend/src/main/java/io/synkra/registro/api/` (`RegistroApi`, `NuevoDespliegue`, `PluginDesplegado`, `DespliegueRegistrado`, `EstadoDespliegue`, `TipoDespliegue`, `ContextoFallo`), `backend/src/main/java/io/synkra/kubernetes/api/` (`AdaptadorKubernetes.disponible`, `construir`, `aplicar`, `PlanPais`, `AplicacionFallidaException`) y `backend/src/main/java/io/synkra/catalogo/api/` (`obtenerCore`, `obtenerPlugin`, `PluginSubido.etiquetas`)
- `backend/src/test/java/io/synkra/despliegue/CrearPlanTest.java`, `PlanPendienteTest.java` y `RelojMovible.java`, y `backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java`
- `backend/src/main/java/io/synkra/plataforma/BaseDeDatos.java` (pool de **una** conexión) y `Reloj.java`
- `verificador/src/testFixtures/java/io/synkra/verificador/SuiteCombinaciones.java` (S2, S4 y S7)
- `CANDIDATAS.md` (C-34, C-35, C-76, C-82, C-83, C-87, C-107, C-108, C-109)

---

## Decisiones del orquestador

1. **La unión es el primer paso, y se comprueba antes de escribir nada.**
   - `git switch -c m6r/U07-D <punta VERDE de U07-C>` y `git merge --no-ff --no-edit <punta VERDE de U06-C>`. Se espera un merge **sin conflictos**: las dos ramas añadieron las mismas tres líneas de `synkra.paises` en `application.properties` (decisión 3 de U06-C) y solo U07-C tiene un bean `Clock` (decisión 2 de U07-C).
   - Si git pide resolver algo, el codificador **no** lo resuelve: `git merge --abort`, lo anota en la bitácora y para (lo arbitra el orquestador).
   - El commit de merge es la base del despacho: su sha (`<sha de la unión>`) va en la bitácora y en `ca0.txt`, y es la base de CA-7.
   - CA-0 se corre sobre ese commit y su evidencia es el primer commit tras el merge. Ninguna línea de código antes.
2. **El reloj.** Todo lo nuevo usa el bean `Clock` de `plataforma/Reloj.java` (inyectado como `java.time.Clock`). `RegistroServicio` no se toca: sigue con su reloj privado, así que `terminado_en` no sale del reloj de la prueba (las pruebas no lo afirman con valor exacto). Pasarlo al bean se propone como candidata.
3. **Nombres nuevos.** Todo en `io.synkra.despliegue`, no público.
   - `ConfirmacionServicio` (`@Service`): `Confirmado confirmar(long id, String cabeceraUsuario)`, con `Confirmado(Plan plan, String confirmadoPor, Instant confirmadoEn)` (record). **No** es `@Transactional` (decisión 6).
   - `ReservaConfirmacion` (`@Component`): `Instant reservar(Plan plan, String confirmadoPor, String veredicto, List<PluginDesplegado> plugins)`, `@Transactional` con la propagación por defecto (decisión 6). Devuelve el `confirmadoEn` que registró.
   - `ResumenCatalogo` (`@Component`): `Resumen armar(String pais, Seleccion seleccion)`, el paso 3 de la decisión 6 de U07-B sacado de `PlanServicio`, que pasa a usarlo sin cambiar lo que produce (así crear y confirmar arman el resumen con el mismo código).
   - `Usuario` (clase final con un método estático puro): `String de(String cabecera)`, decisión 5.
   - El rechazo `RECHAZADA` → 422 de `PlanServicio` (`rechazaSiNoCompatible` y su `detalle`) pasa a un método estático de paquete que usan los dos servicios, sin cambiar su salida.
   - `RutasConfirmacion` llama a `ConfirmacionServicio` en vez de dar el 501, con su record anidado `ConfirmacionDto(long id, String pais, String estado, String creadoPor, String confirmadoPor, Instant confirmadoEn)`.
   - En `CodigosDespliegue` se añaden `DESPLIEGUE_EN_CURSO`, `PLAN_DESACTUALIZADO`, `APLICACION_FALLIDA` y `USUARIO_INVALIDO`, y se **quita** `CONFIRMACION_NO_DISPONIBLE` (con su mensaje). Textos nuevos en `MotivosDespliegue`:
     - `DESPLIEGUE_EN_CURSO_MENSAJE = "Ya hay un despliegue en curso en el país"`;
     - `PLAN_DESACTUALIZADO_MENSAJE = "El catálogo cambió desde que se creó el plan"`;
     - `APLICACION_FALLIDA_MENSAJE = "No se pudo aplicar el plan en el clúster"`;
     - `KUBERNETES_APLICAR_MENSAJE = "No hay clúster al que aplicar el plan"` (con el código `KUBERNETES_NO_DISPONIBLE` que ya existe);
     - `USUARIO_MENSAJE = "El usuario no es válido"` y `USUARIO_FORMA = "El usuario debe tener de 1 a 64 caracteres: minúsculas, dígitos, punto, guion o guion bajo"`.
4. **`despliegue` usa `registro` solo por `registro.api`.** Inyecta `RegistroApi`; nunca una clase de `io.synkra.registro` fuera de `api`, ni SQL sobre `reg_*`. Modulith (`elBackendRealRespetaSusFronteras`) lo vigila desde ya: `registro.api` es `@NamedInterface("api")`. No hay ciclo (`registro` no depende de `despliegue`). Las `allowedDependencies` de `despliegue` son de U07-G.
5. **R-D8: los dos usuarios, sin autenticación.** U10 (HTTP Basic) se cortó, así que el usuario llega en la cabecera opcional `X-Synkra-Usuario`, tanto al crear el plan como al confirmarlo. Es un marcador de identidad, no un control de acceso: se anota en la bitácora y se propone como candidata.
   - `Usuario.de(cabecera)`: sin cabecera (nula), `"anonimo"` (como hoy). Con cabecera, debe cumplir `[a-z0-9][a-z0-9._-]{0,63}`; si no (vacía incluida): 422 `ErrorApi(USUARIO_INVALIDO, USUARIO_MENSAJE, [DetalleError("X-Synkra-Usuario", USUARIO_FORMA)])`. El motivo no repite lo recibido (C-34).
   - Es el **paso 0** de las dos rutas, antes de R-D1 al crear y antes de la guarda al confirmar. `RutasPlanes` lee la cabecera con `@RequestHeader(name = "X-Synkra-Usuario", required = false)` y `PlanServicio.prepararPlan` recibe `creadoPor` (en vez de la constante). `CrearPlanTest` no cambia: sin cabecera sigue dando `anonimo`.
   - `creado_por` sale del plan y `confirmado_por` de la cabecera de la confirmación; pueden ser iguales o distintos (Q9: cualquier Desplegador confirma).
6. **R-D5 a R-D9 (`ConfirmacionServicio.confirmar`), el orden es contrato.**
   0. `Usuario.de(cabecera)` (decisión 5).
   1. `guarda.exigirConfirmable(id)` de U07-C: 404, 410 `PLAN_VENCIDO` o 409 `PLAN_NO_PENDIENTE`.
   2. R-D6: `verificacion.verificar(plan.core(), plan.versionCore(), <plugins de dep_plan.plugins>)`. Si `RECHAZADA`: 422 `COMBINACION_RECHAZADA`, exactamente con el formato de la creación (mensaje y un `DetalleError` por motivo). El plan sigue `pendiente`, sin fila en el registro y sin peticiones al clúster.
   3. El resumen: `resumenCatalogo.armar(plan.pais(), verificado.seleccion())` debe ser **igual** (`equals` del record) al `Resumen` leído de `dep_plan.resumen`. Si no: 422 `ErrorApi(PLAN_DESACTUALIZADO, PLAN_DESACTUALIZADO_MENSAJE, [])`, plan `pendiente`. Motivo: R-D6 juzga por nombre y versión, pero lo que se aplica lleva imagen y sha256; así lo que llega al clúster es lo que se mostró **y** lo que el catálogo dice hoy.
   4. Si `!kubernetes.disponible()`: 503 `ErrorApi(KUBERNETES_NO_DISPONIBLE, KUBERNETES_APLICAR_MENSAJE, [])`, plan `pendiente`, sin fila en el registro. `disponible()` no habla con la red.
   5. `objetos = kubernetes.construir(new PlanPais(plan.pais(), resumen.imagen(), <PluginPlan de resumen.plugins()>))`. Es local (sin peticiones); un error aquí deja el plan `pendiente`.
   6. `reserva.reservar(...)`, en **una** transacción (`@Transactional`, propagación por defecto, nunca `REQUIRES_NEW`):
      1. `UPDATE dep_plan SET estado = 'confirmado' WHERE id = ? AND estado = 'pendiente' AND vence_en > ?` (con `reloj`). Si no cambia ninguna fila: se relee el plan; si sigue `pendiente` (venció entre medias), 410 `PLAN_VENCIDO`; si no, 409 `PLAN_NO_PENDIENTE`. La guarda vive en el `WHERE`, como en U07-C.
      2. R-D7: si algún elemento de `registro.historial(plan.pais())` está en `APLICANDO`: 409 `ErrorApi(DESPLIEGUE_EN_CURSO, DESPLIEGUE_EN_CURSO_MENSAJE, [])`. Un `listo` o un `fallido` no bloquean.
      3. `registro.registrar(new NuevoDespliegue(plan.id(), plan.pais(), <tipo>, plan.origenId(), plan.core(), plan.versionCore(), resumen.imagen(), plan.creadoPor(), confirmadoPor, reloj.instant(), <veredicto>, <plugins>))`. `veredicto` es el JSON de `VeredictoDto.de(veredicto)` del paso 2; `plugins` son los de `resumen.plugins()` con las `etiquetas` de `catalogo.obtenerPlugin(core, nombre, version)`. `registrar` se une a esta transacción (R-R1).
      - Cualquier excepción revierte los tres pasos: un 409 de R-D7 deja el plan `pendiente`. Con el pool de una conexión, dos confirmaciones simultáneas del mismo país se serializan en esta transacción: la segunda ve el `aplicando` de la primera.
   7. `kubernetes.aplicar(objetos)`, **fuera** de la transacción (no se retiene la única conexión mientras se habla con el clúster). Se llama una vez; no se reintenta (P2).
      - Si lanza `AplicacionFallidaException`: `registro.actualizarResultado(plan.id(), FALLIDO, new ContextoFallo("", "", e.getMessage()))` y 502 `ErrorApi(APLICACION_FALLIDA, APLICACION_FALLIDA_MENSAJE, [])`. El plan queda `confirmado` y el registro `fallido`: lo que se intentó aplicar queda registrado aunque haya quedado a medias.
      - Cualquier otra `RuntimeException`: igual a `fallido` (con `eventos` = el mensaje, o el nombre simple de la clase si es nulo) y se relanza (500 de `ManejadorErrores`).
   8. 202 con `ConfirmacionDto` (`estado` `"aplicando"`, `confirmadoEn` en ISO-8601) y cabecera `Location: /api/despliegues/{id}` (el `GET` de `registro`, que ahora sí existe).
   - **Desviación de R-D9** (se anota en la bitácora): R-D9 dice «`construir` + `aplicar` → `registrar`»; aquí se registra **antes** de aplicar. Motivo: R-D7 necesita reservar el país antes de tocar el clúster, la transacción no puede incluir la llamada al clúster (una sola conexión), y así ningún intento de aplicar queda sin rastro en el registro (North Star). Si el proceso muere entre los pasos 6 y 7, queda un `aplicando` sin nada aplicado: lo cierra la vigilancia de U07-E por plazo (se avisa en la candidata).
   - «Lanzar la vigilancia» (final de R-D9) es de U07-E.
7. **P4 (`CarpetaEquivocadaPlanTest`), al crear y al confirmar.**
   - Crear (en `pe`): `POST /api/despliegues` con `otro-core` 1.0.0 + `kyc-pe` 1.3.0 (que es de la carpeta de `synkra-core`) da 422 `COMBINACION_RECHAZADA` con exactamente un detalle de campo `plugins.kyc-pe`, sin fila en `dep_plan` y sin peticiones al simulador; `POST /api/verificaciones` con la misma selección da 200 `RECHAZADA` con `CORE_EQUIVOCADO` para `kyc-pe`, y su `mensaje` es el `motivo` de aquel detalle. Lo mismo al revés: `synkra-core` 2.1.0 + `kyc-xx` 1.0.0 (S7). Esta parte ya la da U07-B: el codificador lo anota (no hay rojo posible) y la prueba la fija.
   - Confirmar: un plan `pendiente` y vigente de `co` insertado con SQL (un `dep_plan` manipulado, con la columna `plugins` exactamente `[{"nombre":"kyc-pe","version":"1.3.0"}]`) con `otro-core` 1.0.0 + `kyc-pe` 1.3.0 y un resumen coherente (imagen `c`, sha256 del catálogo) da 422 `COMBINACION_RECHAZADA` (paso 2 de la decisión 6), sin peticiones, sin fila en el registro y el plan sigue `pendiente`. Este es el rojo de U07-T11.
   - Usa el servidor simulado (como `CrearPlanTest`), su carpeta fija `build/sqlite-pruebas/CarpetaEquivocadaPlanTest` y `synkra.paises=pe,co`.
8. **`ConfirmarTest`: datos y casos.**
   - `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@TestBean AdaptadorKubernetes adaptador` (fábrica que devuelve `SIM.adaptador()`) y `@TestBean Clock reloj` (fábrica que devuelve un `RelojMovible`). El catálogo se carga como en `CrearPlanTest` (`SuiteCombinaciones.contenido()`, imágenes `a`, `b` y `c`); si hace falta compartir código, va en un ayudante de prueba **nuevo**. Además se suben dos plugins propios de esta clase, de la carpeta de `synkra-core` y con `coreCompatible` `2.x`: `kyc-zz` 1.0.0 y `kyc-yy` 1.0.0.
   - Carpeta fija `build/sqlite-pruebas/ConfirmarTest`, que el `@DynamicPropertySource` borra y crea de nuevo; no se borran filas entre pruebas. `synkra.paises=pe,co,cl,ar,uy,bo,ec,py`.
   - `T1 = 2026-11-20T15:00:00.456Z` (`1795186800456` ms). Cada prueba usa su propio país y su propia hora base, fijada con `reloj.fija(...)` antes de crear sus planes. Los planes se crean por `POST /api/despliegues` con S2 (`synkra-core` 2.1.0 + `kyc-pe` 1.3.0) salvo que se diga otra cosa, con `X-Synkra-Usuario: ana` al crear y `X-Synkra-Usuario: luis` al confirmar, salvo en `cl` y `py`. Se drena `SIM.nuevasPeticiones()` justo antes de cada confirmación que se mide.
     - `pe`, base `T1` (H-12 y H-17): antes de confirmar, `GET /api/despliegues/{id}` da 404 `NO_ENCONTRADO` y `GET /api/paises/pe/despliegues` da `[]`. Con `fija(T1 + 10 min)`, confirmar con `X-Synkra-Usuario: luis` da 202, `Location` `/api/despliegues/{id}` y el cuerpo `{id, pais: pe, estado: aplicando, creadoPor: ana, confirmadoPor: luis, confirmadoEn: T1 + 10 min}`. Las peticiones son exactamente tres `PATCH`, en este orden: el ConfigMap `synkra-core-config`, el Service `synkra-core` y el Deployment `synkra-core` de `synkra-pe` (se compara método y ruta sin la consulta). `SIM.adaptador().leerPais("pe")` da la imagen `a` y `kyc-pe` 1.3.0 con el sha256 del catálogo. `GET /api/despliegues/{id}` da 200 con `estado` `aplicando`, `creadoPor` `ana`, `confirmadoPor` `luis` y `veredicto.global` `APROBADA`. Una segunda confirmación da 409 `PLAN_NO_PENDIENTE` sin peticiones. Y `reserva.reservar(...)` llamado directamente con el `Plan` leído **antes** de confirmar da 409 `PLAN_NO_PENDIENTE` (no `DESPLIEGUE_EN_CURSO`: el `UPDATE` va primero) y el registro de `pe` sigue con una fila.
     - `co`, base `T1 + 1 h` (R-D7): el primer plan se confirma (202). Un segundo plan, creado a `T1 + 1 h + 1 min`, da 409 `DESPLIEGUE_EN_CURSO` con el mensaje exacto, sin peticiones, y sigue `pendiente`; el registro de `co` tiene una fila.
     - `cl`, base `T1 + 2 h` (R-D7 solo mira `aplicando`, y el usuario por defecto): antes, por `RegistroApi`, se registran los ids 900 (`cl`, termina `LISTO`) y 901 (`cl`, termina `FALLIDO` con contexto). El plan se crea y se confirma **sin** cabecera: 202, `creadoPor` y `confirmadoPor` `anonimo`.
     - `ar`, base `T1 + 3 h` (R-D6, catálogo cambiado entre medias): plan con `kyc-zz` 1.0.0 (201); después `update cat_plugin set core_compatible = '3.x' where nombre = 'kyc-zz'`. Confirmar da 422 `COMBINACION_RECHAZADA` con exactamente un detalle `plugins.kyc-zz` cuyo `motivo` es el `mensaje` que da `POST /api/verificaciones` en ese momento para la misma selección; sin peticiones, `pendiente`, sin fila en el registro.
     - `uy`, base `T1 + 4 h` (resumen desactualizado): plan con `kyc-yy` 1.0.0; después `update cat_plugin set sha256 = '<64 veces 7>' where nombre = 'kyc-yy'`. Confirmar da 422 `PLAN_DESACTUALIZADO` con el mensaje exacto y `detalles` vacío; sin peticiones, `pendiente`, sin fila en el registro.
     - `bo`, base `T1 + 5 h` (falla al aplicar, sin reintentos): `SIM.fallaAplicar("bo")` (decisión 9). Confirmar da 502 `APLICACION_FALLIDA` con el mensaje exacto; las peticiones son exactamente los tres `PATCH` (uno por objeto, el del Deployment falla, ninguno repetido); el plan queda `confirmado` y `registro.obtener(id)` da `FALLIDO` con contexto `("", "", "No se pudo aplicar Deployment/synkra-core")`.
     - `ec`, base `T1 + 6 h` (R-D7 con dos confirmaciones a la vez): dos planes (a `T1 + 6 h` y `+ 1 s`); dos hilos los confirman a la vez (un `CountDownLatch` los suelta juntos; `Future.get` con plazo de 20 s). Uno da 202 y el otro 409 `DESPLIEGUE_EN_CURSO`; en total hay exactamente tres `PATCH`; el registro de `ec` tiene una fila.
     - `py`, base `T1 + 7 h` (usuario inválido): con `X-Synkra-Usuario: Luis Pérez`, crear da 422 `USUARIO_INVALIDO` sin fila y sin peticiones; el plan creado con `ana` y confirmado con `X-Synkra-Usuario: LUIS` da 422 `USUARIO_INVALIDO` (campo `X-Synkra-Usuario`, motivo exacto), sin peticiones, y sigue `pendiente`.
   - Todos los rechazos comparan `codigo`, `mensaje` exacto y `detalles` (campo y motivo) exactos.
9. **Una sola línea nueva de ayuda en `SimuladorKubernetes`.** Se **añade** (sin cambiar ni quitar ninguna línea) el método público `void fallaAplicar(String pais)`: el `PATCH` del Deployment `synkra-core` de `synkra-<pais>` responde 500 siempre (con la ruta que use el cliente, consulta incluida si el simulador la exige). Su API pública sigue sin tipos de Fabric8.
10. **`PlanPendienteTest` tras quitar el 501.** Los casos de U07-C que esperaban 501 (`py`, y `uy` a `venceEn − 1 ms`) dejan de llamar a la ruta (ahora aplicaría): llaman a `guarda.exigirConfirmable(id)` directamente (la prueba está en el mismo paquete), afirman que devuelve el plan `pendiente` y que `nuevasPeticiones()` sigue vacía. Nada más de `PlanPendienteTest` cambia, y su lectura de vuelta es la misma de U07-C (CA-4).
11. **`ConfirmarSinClusterTest`** (R-D9 sin clúster): `@MockitoBean AdaptadorKubernetes adaptador` (`disponible()` da `false`, `leerPais` da vacío), carpeta `build/sqlite-pruebas/ConfirmarSinClusterTest`, `synkra.paises=pe`. Un plan S2 creado por HTTP, al confirmarse, da 503 `KUBERNETES_NO_DISPONIBLE` con `KUBERNETES_APLICAR_MENSAJE`, sigue `pendiente` y `registro.obtener(id)` es vacío; `verify(adaptador, never()).construir(any())` y `never().aplicar(any())`.
12. **Candidatas que se deciden aquí.**
    - **C-35** (longitud de nombres): no muerde en el despliegue. Los objetos de Kubernetes tienen nombres fijos (`synkra-core`, `synkra-core-config`) y el nombre del plugin solo va dentro del ConfigMap. Sigue con el pulido de U04 (con C-55).
    - **C-87** (colisión de JAR): cerrada por construcción en este camino. Lo que se aplica sale del catálogo: nombres con `Descriptor.NOMBRE` (sin puntos), versiones `X.Y.Z` (sin guiones) y nombres únicos en la selección (S10, `PLUGIN_REPETIDO`), así que `nombre-X.Y.Z.jar` no se repite. Se escribe el argumento en la bitácora y no se añade código. La laxitud de `PlanPais.SEGURO` sigue para el pulido de U05.
    - **C-76** (reintentos implícitos del cliente de Fabric8), **C-82** y **C-83**: van en U07-E. C-76 cubre también el `aplicar` de esta tarea, pero se arregla en `kubernetes` junto con el sondeo de 5 s. Aquí el simulador corre con `requestRetryBackoffLimit(0)`, y la prueba `bo` afirma un solo `PATCH` por objeto.
    - **C-107**: ningún `REQUIRES_NEW`. Una sola transacción en `reservar`, con `registrar` unido a ella.
    - **C-108**: los `grep` de escrituras de esta tarea miran la variable del adaptador (`kubernetes\.`), no cualquier `.estado(`; y `PlanDto.de` vuelve a usar `plan.estado()` (hay otros estados).
    - **C-109**: el barrido programado de vencidos va en U07-E.
13. **Bitácora en `bitacoras/U07-D.md`, evidencia en `bitacoras/U07-D-evidencia/`** (un `caN.txt` por criterio, salida completa con fecha, sha y comando), y el revisor en `revisiones/U07-D/`.

---

## Criterios de aceptación

- [ ] **CA-0**: la unión quedó limpia y en verde, antes de cualquier cambio
  ```bash
  git log -1 --format='%H %P'
  git ls-files -u | wc -l
  git show --cc --format= HEAD | wc -l
  git diff HEAD^1 HEAD -- backend/src/main/resources/application.properties | wc -l
  git diff HEAD^2 HEAD -- backend/src/main/resources/application.properties | wc -l
  grep -cx 'synkra.paises=${SYNKRA_PAISES:co,pe}' backend/src/main/resources/application.properties
  grep -rlE "Clock\.system" backend/src/main/java | sort
  grep -rnE "^\s*((public|static|protected|private)\s+)*Clock\s+[a-zA-Z]+\s*\(" backend/src/main/java
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado:
  - el commit es un merge cuyos padres son, en orden, `<punta VERDE de U07-C>` y `<punta VERDE de U06-C>`; ese sha es `<sha de la unión>`;
  - `0`, `0`, `0` y `0` (sin archivos sin fusionar, sin resolución a mano, y `application.properties` idéntico al de las dos puntas);
  - `1` (una sola línea `synkra.paises`);
  - `backend/src/main/java/io/synkra/plataforma/Reloj.java` y `backend/src/main/java/io/synkra/registro/RegistroServicio.java` (su reloj privado, que no es bean);
  - una sola línea, la de `Clock reloj()` en `Reloj.java`;
  - `BUILD SUCCESSFUL` y ningún XML con `skipped`, `failures` o `errors` distinto de 0; entre ellos están `RelojTest` (un solo bean `Clock`), `PlanPendienteTest`, `CrearPlanTest`, `VistaGeneralTest`, `PaisesConfiguradosTest`, `HistorialTest` y `FronterasModulosTest`.

- [ ] **CA-1**: U07-T07. Confirmar (R-D5 a R-D9) contra el servidor simulado, con lectura de vuelta
  ```bash
  ./gradlew :backend:test --rerun --tests '*ConfirmarTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*ConfirmarTest.xml
  python3 -I - backend/build/sqlite-pruebas/ConfirmarTest/synkra.db <<'EOF'
  import sqlite3, sys
  c = sqlite3.connect(sys.argv[1])
  q = lambda s: print(c.execute(s).fetchall())
  q("select p.pais, p.estado, r.estado, r.creado_por, r.confirmado_por, json_extract(r.veredicto, '$.global') from dep_plan p"
    " left join reg_despliegue r on r.id = p.id where p.pais != 'ec' order by p.pais, p.creado_en")
  q("select p.estado, r.estado from dep_plan p left join reg_despliegue r on r.id = p.id where p.pais = 'ec' order by p.estado")
  q("select id, pais, estado from reg_despliegue where id not in (select id from dep_plan) order by id")
  q("select x.log_core, x.log_init, x.eventos from reg_contexto x join dep_plan p on p.id = x.despliegue_id where p.pais = 'bo'")
  q("select (select count(*) from reg_despliegue r join dep_plan p on p.id = r.id where p.estado != 'confirmado'),"
    " (select count(*) from dep_plan where estado = 'confirmado' and id not in (select id from reg_despliegue)),"
    " (select count(*) from reg_despliegue r join dep_plan p on p.id = r.id where r.confirmado_en < p.creado_en or r.confirmado_en >= p.vence_en)")
  q("select r.confirmado_en - p.creado_en, r.imagen = json_extract(p.resumen, '$.imagen') from dep_plan p join reg_despliegue r on r.id = p.id where p.pais = 'pe'")
  EOF
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, `tests` ≥ 10 y `skipped="0" failures="0" errors="0"`, con los casos y afirmaciones de la decisión 8.
  - Python, en orden:
    - `[('ar', 'pendiente', None, None, None, None), ('bo', 'confirmado', 'fallido', 'ana', 'luis', 'APROBADA'), ('cl', 'confirmado', 'aplicando', 'anonimo', 'anonimo', 'APROBADA'), ('co', 'confirmado', 'aplicando', 'ana', 'luis', 'APROBADA'), ('co', 'pendiente', None, None, None, None), ('pe', 'confirmado', 'aplicando', 'ana', 'luis', 'APROBADA'), ('py', 'pendiente', None, None, None, None), ('uy', 'pendiente', None, None, None, None)]` (en `bo`, `co` y `pe` el plan se confirma con `luis`);
    - `[('confirmado', 'aplicando'), ('pendiente', None)]`;
    - `[(900, 'cl', 'listo'), (901, 'cl', 'fallido')]`;
    - `[('', '', 'No se pudo aplicar Deployment/synkra-core')]`;
    - `[(0, 0, 0)]`;
    - `[(600000, 1)]`.

- [ ] **CA-2**: U07-T11. Un plugin de otra carpeta no llega al clúster, ni al crear ni al confirmar (P4)
  ```bash
  ./gradlew :backend:test --rerun --tests '*CarpetaEquivocadaPlanTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*CarpetaEquivocadaPlanTest.xml
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select pais, core, estado, plugins from dep_plan order by id').fetchall()); print(c.execute('select count(*) from reg_despliegue').fetchone())" backend/build/sqlite-pruebas/CarpetaEquivocadaPlanTest/synkra.db
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, `tests` ≥ 3 y sin saltos ni fallos, con lo de la decisión 7: en cada intento, `nuevasPeticiones()` vacía; los 422 comparan código, mensaje y detalles exactos; `CORE_EQUIVOCADO` en `/api/verificaciones`.
  - Python: `[('co', 'otro-core', 'pendiente', '[{"nombre":"kyc-pe","version":"1.3.0"}]')]` (solo el plan manipulado; ninguna creación dejó fila) y `(0,)`.

- [ ] **CA-3**: sin clúster, la confirmación no toca nada: en la prueba con doble y en el JAR real
  ```bash
  ./gradlew :backend:test --rerun --tests '*ConfirmarSinClusterTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*ConfirmarSinClusterTest.xml
  ./gradlew :backend:bootJar
  export SYNKRA_DATA=$(mktemp -d); J=$(mktemp -d); H=$(mktemp -d)
  arranca() { HOME="$H" KUBECONFIG="$H/kubeconfig-falso" SYNKRA_PAISES=pe java -Duser.home="$H" -jar backend/build/libs/backend-*.jar --server.port=18093 > "$J/log-$1.txt" 2>&1 & PID=$!; timeout 90 bash -c 'until curl -sf localhost:18093/actuator/health >/dev/null; do sleep 1; done'; }
  python3 -I -c "import sys, zipfile
  with zipfile.ZipFile(sys.argv[1], 'w') as z: z.writestr('synkra-plugin.yaml', 'nombre: kyc-pe\nversion: 1.3.0\ncore: synkra-core\ncoreCompatible: 2.x\n')" "$J/a.jar"
  # 1. Primer arranque: catálogo por la API (dos cores y kyc-pe 1.3.0).
  arranca 0
  for c in synkra-core:2.1.0:a otro-core:1.0.0:c; do IFS=: read n v x <<< "$c"; curl -s -o /dev/null -w '%{http_code}\n' -H 'Content-Type: application/json' \
    -d "{\"nombre\":\"$n\",\"version\":\"$v\",\"imagen\":\"localhost:5001/$n@sha256:$(printf "$x%.0s" $(seq 64))\"}" localhost:18093/api/cores; done
  curl -s -o /dev/null -w '%{http_code}\n' -F "archivo=@$J/a.jar" localhost:18093/api/cores/synkra-core/plugins
  kill $PID; wait $PID 2>/dev/null
  # 2. Planes sembrados con SQL (el JAR no puede crearlos sin clúster).
  python3 -I - "$SYNKRA_DATA/synkra.db" "$J/a.jar" <<'EOF'
  import hashlib, json, sqlite3, sys, time
  c = sqlite3.connect(sys.argv[1]); sha = hashlib.sha256(open(sys.argv[2], "rb").read()).hexdigest()
  ahora = int(time.time() * 1000); m = 60000
  def img(n, x): return "localhost:5001/%s@sha256:%s" % (n, x * 64)
  def resumen(core, v, imagen, s): return json.dumps({"namespace": "synkra-pe", "core": core, "versionCore": v, "imagen": imagen,
      "plugins": [{"nombre": "kyc-pe", "version": "1.3.0", "sha256": s}]})
  filas = ((81, "synkra-core", "2.1.0", img("synkra-core", "a"), sha, "pendiente", ahora),
           (82, "synkra-core", "2.1.0", img("synkra-core", "a"), sha, "pendiente", ahora - 31 * m),
           (83, "synkra-core", "2.1.0", img("synkra-core", "a"), sha, "confirmado", ahora),
           (84, "otro-core", "1.0.0", img("otro-core", "c"), sha, "pendiente", ahora),
           (85, "synkra-core", "2.1.0", img("synkra-core", "a"), "7" * 64, "pendiente", ahora))
  for i, core, v, imagen, s, estado, en in filas:
      c.execute("insert into dep_plan (id, pais, tipo, origen_id, core, version_core, plugins, resumen, cambios, estado, creado_por,"
                " creado_en, vence_en) values (?, 'pe', 'despliegue', null, ?, ?, ?, ?, '{}', ?, 'ana', ?, ?)",
                (i, core, v, '[{"nombre":"kyc-pe","version":"1.3.0"}]', resumen(core, v, imagen, s), estado, en, en + 30 * m))
  c.commit()
  EOF
  # 3. Segundo arranque: intentos de confirmación y el registro.
  arranca 1
  confirma() { curl -s -o "$J/r.json" -w '%{http_code} ' -X POST "${@:2}" localhost:18093/api/despliegues/$1/confirmacion
    python3 -I -c "import json,sys; r=json.load(open(sys.argv[1])); print(r['codigo'], r['mensaje'], [d['campo'] for d in r['detalles']])" "$J/r.json"; }
  confirma 81 -H 'X-Synkra-Usuario: Luis Pérez'; for id in 81 82 83 84 85 999; do confirma $id -H 'X-Synkra-Usuario: luis'; done
  curl -s -w ' %{http_code}\n' localhost:18093/api/paises/pe/despliegues
  curl -s -o /dev/null -w '%{http_code}\n' localhost:18093/api/despliegues/81
  kill $PID; wait $PID 2>/dev/null
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select id, estado from dep_plan order by id').fetchall()); print(c.execute('select count(*) from reg_despliegue').fetchone())" "$SYNKRA_DATA/synkra.db"
  ```
  Esperado, en orden:
  - `BUILD SUCCESSFUL` sin saltos ni fallos, con lo de la decisión 11;
  - `201`, `201`, `201`;
  - `422 USUARIO_INVALIDO El usuario no es válido ['X-Synkra-Usuario']`;
  - `503 KUBERNETES_NO_DISPONIBLE No hay clúster al que aplicar el plan []`;
  - `410 PLAN_VENCIDO El plan venció sin confirmarse []`;
  - `409 PLAN_NO_PENDIENTE El plan ya no está pendiente []`;
  - `422 COMBINACION_RECHAZADA La combinación no es compatible ['plugins.kyc-pe']` (P4 en el JAR real);
  - `422 PLAN_DESACTUALIZADO El catálogo cambió desde que se creó el plan []`;
  - `404 NO_ENCONTRADO Recurso no encontrado []`;
  - `[] 200` y `404` (ningún plan pendiente está en el registro, H-17);
  - `[(81, 'pendiente'), (82, 'vencido'), (83, 'confirmado'), (84, 'pendiente'), (85, 'pendiente')]` y `(0,)`.

  Si el puerto 18093 está ocupado, se anota en la bitácora y se usa otro en todo el criterio. La bitácora pega la salida tal cual.

- [ ] **CA-4**: un plan sin confirmar sigue sin aplicar nada (R-D13), y no queda rastro del 501
  ```bash
  ./gradlew :backend:test --rerun --tests '*PlanPendienteTest' --tests '*RelojTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*PlanPendienteTest.xml backend/build/test-results/test/TEST-*RelojTest.xml
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select pais, estado, creado_en from dep_plan order by pais, creado_en').fetchall())" backend/build/sqlite-pruebas/PlanPendienteTest/synkra.db
  grep -rn "CONFIRMACION_NO_DISPONIBLE\|NOT_IMPLEMENTED" backend/src; echo "501=$?"
  git diff <sha de la unión> HEAD -- backend/src/test/java/io/synkra/despliegue/PlanPendienteTest.java | grep -E '^[-+][^-+]'
  ```
  Esperado:
  - `BUILD SUCCESSFUL` y sin saltos ni fallos en las dos clases;
  - `[('ar', 'vencido', 1795010400123), ('ar', 'pendiente', 1795010430123), ('bo', 'confirmado', 1795017600123), ('cl', 'pendiente', 1795003200123), ('co', 'pendiente', 1795003200123), ('ec', 'vencido', 1794999600123), ('ec', 'pendiente', 1795000200123), ('pe', 'pendiente', 1795003200123), ('py', 'pendiente', 1795006800123), ('uy', 'vencido', 1795014000123)]` (la misma de U07-C);
  - `501=1`;
  - las líneas cambiadas de `PlanPendienteTest` son solo las de los casos `py` y `uy` de la decisión 10; la bitácora las pega.

- [ ] **CA-5**: rojo primero. Cada regla nueva tiene una prueba que muerde, demostrado en una copia
  ```bash
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
  # Mutación anotada literalmente en la bitácora; una por regla, por ejemplo:
  #   R-D6:               ConfirmacionServicio no vuelve a verificar (ar da 202; la confirmación P4 da 202)
  #   R-D6 pendiente:     la verificación va después de reservar (ar queda confirmado)
  #   resumen:            no se compara el resumen con el catálogo (uy da 202)
  #   R-D7:               se quita la comprobación de aplicando (co, segundo plan, da 202)
  #   R-D7 solo aplicando: se bloquea si el historial no está vacío (cl da 409)
  #   transacción:        se quita @Transactional de reservar (el 409 de co deja el plan confirmado)
  #   UPDATE primero:     se quita «estado = 'pendiente' AND» del UPDATE (reservar con el plan viejo de pe da DESPLIEGUE_EN_CURSO)
  #   R-D8:               confirmadoPor = plan.creadoPor() (pe registra ana y ana)
  #   usuario:            Usuario.de acepta cualquier texto (py da 202)
  #   orden R-D9:         aplicar antes de reservar (co trae PATCH en el 409; ec trae seis PATCH)
  #   fallo registrado:   no se llama a actualizarResultado al fallar aplicar (bo queda aplicando)
  #   fuera de la tx:     @Transactional en ConfirmacionServicio.confirmar (bo queda pendiente y sin fila)
  #   sin clúster:        se quita la comprobación de disponible() (ConfirmarSinClusterTest)
  #   P4 al crear:        VerificacionVivo deja de buscar en las demás carpetas (CarpetaEquivocadaPlanTest: PLUGIN_AUSENTE)
  sed -i '<mutación>' "$D/backend/src/main/java/io/synkra/despliegue/<Clase>.java"
  (cd "$D" && ./gradlew :backend:test --tests '*ConfirmarTest' --tests '*CarpetaEquivocadaPlanTest' --tests '*ConfirmarSinClusterTest')
  grep -l '<failure' "$D"/backend/build/test-results/test/TEST-*.xml
  rm -rf "$D"
  ```
  Esperado: con cada una de las catorce mutaciones (una copia por mutación), `BUILD FAILED`, y el XML de la clase dueña de la regla muestra al menos un `<failure` en la prueba de esa regla. La bitácora pega el `sed` (o el parche) y el nombre de la prueba que cayó. Sin `git stash` ni cambios en el worktree. Además, la bitácora registra el rojo de `ConfirmarTest`, de `ConfirmarSinClusterTest` y del caso de confirmación de `CarpetaEquivocadaPlanTest` antes de `ConfirmacionServicio` y `ReservaConfirmacion`.

- [ ] **CA-6**: todo el backend sigue pasando, y `despliegue` usa `registro` y el clúster solo por donde debe
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  grep -rnE "io\.synkra\.registro\." backend/src/main/java/io/synkra/despliegue | grep -vE "io\.synkra\.registro\.api\."; echo "registro-interno=$?"
  grep -rn "reg_" backend/src/main/java/io/synkra/despliegue; echo "tablas-reg=$?"
  grep -rlE "kubernetes\.(aplicar|construir)\(" backend/src/main/java/io/synkra/despliegue
  grep -rcE "kubernetes\.(aplicar|construir)\(" backend/src/main/java/io/synkra/despliegue/ConfirmacionServicio.java
  grep -rnE "kubernetes\.(estado|contextoFallo|existeNamespace)\(" backend/src/main/java/io/synkra/despliegue; echo "vigilancia=$?"
  grep -rn "REQUIRES_NEW" backend/src/main/java; echo "requiresNew=$?"
  grep -rn "@Scheduled\|@EnableScheduling" backend/src/main/java; echo "programado=$?"
  grep -n "Transactional" backend/src/main/java/io/synkra/despliegue/ConfirmacionServicio.java; echo "confirmar-tx=$?"
  ```
  Esperado:
  - `BUILD SUCCESSFUL` y ningún XML con `skipped`, `failures` o `errors` distinto de 0 (`FronterasModulosTest` incluida: Modulith acepta que `despliegue` use `registro.api`; `CrearPlanTest`, `VerificacionVivoTest`, `VistaGeneralTest` y `HistorialTest` pasan sin cambios).
  - `registro-interno=1` y `tablas-reg=1`.
  - La lista de archivos con `aplicar` o `construir` es solo `backend/src/main/java/io/synkra/despliegue/ConfirmacionServicio.java`, con `2` líneas.
  - `vigilancia=1`, `requiresNew=1`, `programado=1` y `confirmar-tx=1`.

- [ ] **CA-7**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <sha de la unión>...HEAD
  git diff --name-status <sha de la unión>...HEAD -- backend/src/test backend/src/main/resources backend/src/main/java/io/synkra/registro backend/src/main/java/io/synkra/plataforma backend/src/main/java/io/synkra/kubernetes backend/src/main/java/io/synkra/catalogo
  git diff <sha de la unión>...HEAD -- backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java | grep -c '^-[^-]'
  ```
  Esperado: solo estas rutas. Se anota el sha.
  - `backend/src/main/java/io/synkra/despliegue/**` (nuevos: `ConfirmacionServicio.java`, `ReservaConfirmacion.java`, `ResumenCatalogo.java` y `Usuario.java`; cambian `RutasConfirmacion.java`, `PlanServicio.java`, `RutasPlanes.java`, `CodigosDespliegue.java` y `MotivosDespliegue.java`)
  - `backend/src/test/java/io/synkra/despliegue/**`: la segunda orden da `A` para `ConfirmarTest.java`, `CarpetaEquivocadaPlanTest.java`, `ConfirmarSinClusterTest.java` (y algún ayudante de prueba nuevo si hace falta) y `M` solo para `PlanPendienteTest.java` (decisión 10)
  - `backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java`: `M`, y la tercera orden da `0` (solo se añade, decisión 9)
  - ninguna línea de `backend/src/main/resources`, `registro`, `plataforma`, `kubernetes` ni `catalogo`
  - `bitacoras/U07-D.md`, `bitacoras/U07-D-evidencia/` y `revisiones/U07-D/`

  `ca7.txt` se genera justo antes de su propio commit, con el árbol limpio y sin commits de código después: ese commit solo trae `ca7.txt` (y la línea de la bitácora que lo cita).

---

**Rojo primero:** `ConfirmarTest`, `ConfirmarSinClusterTest` y el caso de confirmación de `CarpetaEquivocadaPlanTest` antes de `ConfirmacionServicio`, `ReservaConfirmacion`, `Usuario` y el cambio de `RutasConfirmacion`. Un commit «rojo» trae solo las pruebas (y la línea añadida a `SimuladorKubernetes`) y, si hace falta para compilar, firmas vacías que lanzan `UnsupportedOperationException`, nunca la lógica: la bitácora pega su sha y su `git show --stat`. Los casos de creación de P4 ya pasan con U07-B y se anotan así. Cada rojo queda en la bitácora, y CA-5 demuestra que cada regla muerde con el código terminado.
