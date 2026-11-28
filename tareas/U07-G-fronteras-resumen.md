# U07-G: fronteras de `despliegue` y resumen de la unidad U07

**Unidad:** U07: `Despliegue y reversión`
**Tareas del plan que agrupa:** U07-T12 y U07-T13
**Historias que implementa:** H-12 (fronteras del módulo y resumen, como en el plan); el resumen recoge además la trazabilidad de H-06, H-08, H-09, H-11, H-13, H-14, H-16, H-17 y H-20
**Depende de:** U07-F (terminada), y por ella U07-E, U07-D, U07-C, U07-B, U07-A, U06 (entera) y U05. Sale de `<punta VERDE de U07-F>`. Asume como dado todo lo que fijaron esas tareas, y en particular: `despliegue` sin paquete `api` y sin clases públicas; `registro/package-info.java` con `@ApplicationModule(allowedDependencies = {"kubernetes :: api", "plataforma"})` (U06-C); `@NamedInterface("api")` en `catalogo.api`, `kubernetes.api` y `registro.api`; `verificador` como **módulo compartido** (`@Modulithic(sharedModules = "verificador")` en `SynkraApp`, C-36), que es una biblioteca de otro proyecto Gradle (`project(":verificador")`), no un módulo del backend; y las pruebas de `FronterasModulosTest` de U05-T09 y U06-T07 (`produccion()`, `backendReal()`, `laApiNoExponeFabric8`, `registroSoloDependeDeKubernetesApiYPlataforma`).

---

## Alcance

**Dentro** (una línea, concreta):

> Las fronteras del módulo `despliegue` (U07-T12): `despliegue/package-info.java` pasa a `@ApplicationModule(allowedDependencies = {"catalogo :: api", "kubernetes :: api", "registro :: api", "plataforma"})` y `FronterasModulosTest` gana tres pruebas (solo se **añaden** líneas) que fijan que `despliegue` solo usa las API de `catalogo`, `kubernetes` y `registro`, más `plataforma` y la biblioteca `verificador`, nada de `io.fabric8`, y que ningún otro código de producción depende de `despliegue`; y el resumen de la unidad (U07-T13) en `aidlc-docs/construction/U07-despliegue/code/summary.md`, con la salida real de `./gradlew :backend:test --rerun --tests '*despliegue*'` y de `./gradlew :backend:test --rerun :backend:check`, las desviaciones con su fuente, la trazabilidad y las candidatas de U07.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cualquier cambio de código de producción salvo `backend/src/main/java/io/synkra/despliegue/package-info.java` (decisión 2). Ni en `despliegue` (aunque una regla nueva encontrara algo: se para y se reporta, decisión 4), ni en `registro`, `catalogo`, `kubernetes`, `plataforma`, `SynkraApp.java`, `verificador/`, `core/`, `plugin-api/`, `plugins/`, `init-descarga/`, las migraciones, `application.properties` o `build.gradle.kts`.
- Cambiar o quitar líneas de `FronterasModulosTest.java` (solo se añaden), y cambiar cualquier otra prueba o ayudante de prueba (`SimuladorKubernetes.java` incluido). No se añaden clases de juguete nuevas: que cada regla muerde se demuestra con mutaciones en copias (CA-2).
- Dar a `despliegue` un paquete `api` o clases públicas: nadie de fuera lo usa (decisión 3).
- Cambiar `registro/package-info.java` o la regla `registroSoloDependeDeKubernetesApiYPlataforma`.
- Tocar `CANDIDATAS.md`, el plan de U07, `unidades-y-tareas.md`, `aidlc-docs/aidlc-state.md` o `aidlc-docs/audit.md`: lo hace el orquestador al cerrar la unidad. No se cierran ni se reabren candidatas.
- Repetir los criterios de U07-A a U07-F o volver a correr sus mutaciones: el resumen cita las cifras de sus revisiones y bitácoras, con su fuente (CA-5). No se inventa nada que no esté en un archivo.
- Si algo falla al correr los comandos del resumen, se para y se reporta; no se arregla.
- Nada de `kubectl` ni de clúster real (AUTONOMIA-01). Esta tarea no necesita arrancar el JAR.

---

## Archivos de contexto

- Plantillas: `tareas/U06-C-vista-general-fronteras.md` (decisión 10 y CA-4: fronteras de `registro`) y `tareas/U06-D-resumen.md` (resumen, tabla «Cifras y su fuente» y comprobación literal); el resultado de esta última, `aidlc-docs/construction/U06-registro/code/summary.md` y su `evidencia/`, y `bitacoras/U06-D-evidencia/ca3.sh`.
- `aidlc-docs/construction/plans/U07-despliegue-code-generation-plan.md` (U07-T12 y U07-T13, «Trazabilidad») y `unidades-y-tareas.md` (sección U07)
- `aidlc-docs/construction/U07-despliegue/functional-design/` (`business-rules.md` R-D1 a R-D13, `domain-entities.md`, `business-logic-model.md`)
- `tareas/U07-A-planes-pais.md` a `tareas/U07-F-reversion.md` (en la rama del orquestador; las tareas que se fusionaron están también en las ramas `m6r/U07-*`)
- `bitacoras/U07-A.md` a `bitacoras/U07-F.md`, sus carpetas `bitacoras/U07-*-evidencia/`, y `revisiones/U07-A/` a `revisiones/U07-F/` (`ronda-N.md` y `ronda-N-respuesta.md`)
- En la punta de U07-F: `backend/src/main/java/io/synkra/despliegue/` (todo), `backend/src/main/java/io/synkra/SynkraApp.java`, los `package-info.java` de `catalogo.api`, `kubernetes.api`, `registro.api` y `registro`, y `backend/src/test/java/io/synkra/FronterasModulosTest.java`
- Modulith 1.4.14: `ApplicationModule.getAllowedDependencies` añade los módulos compartidos a las dependencias permitidas, y `NamedInterfaces.discoverNamedInterfaces` siempre incluye el paquete base (interfaz sin nombre) de cada módulo (fuentes en `~/.gradle/caches/modules-2/files-2.1/org.springframework.modulith/spring-modulith-core/1.4.14/*/spring-modulith-core-1.4.14-sources.jar`)
- **Candidatas:** `CANDIDATAS.md` de la rama. Las que todavía no estén en ella (al escribir esta tarea, C-121 a C-123 solo estaban en la rama del orquestador) se leen de `/home/casdav/Projects/Master/wt-orq-U04/CANDIDATAS.md`, **solo lectura**. No se copian al repo.

---

## Decisiones del orquestador

1. **Qué usa `despliegue` de verdad** (comprobado en `main` `39a242c`, con U07-A a U07-D; U07-E y U07-F no lo cambian, decisión 6 de U07-E y «Fuera» de U07-F):
   - `catalogo.api` (`CatalogoApi`), `kubernetes.api` (`AdaptadorKubernetes`, `PlanPais`, `LecturaPais` y las excepciones), `registro.api` (`RegistroApi`, `NuevoDespliegue`, `PluginDesplegado`, `EstadoDespliegue`, `TipoDespliegue`, `ContextoFallo`) y `plataforma` (`ErrorApi`, `ExcepcionApi`, `DetalleError`, `CodigosError`; el bean `Clock` de `Reloj.java` se inyecta como `java.time.Clock`).
   - `io.synkra.verificador` (`Verificador`, `Seleccion`, `Veredicto`, `Catalogo`…): es la biblioteca del proyecto Gradle `verificador`, declarada **módulo compartido** en `SynkraApp` (C-36, prueba `verificadorEsModuloCompartido`). Modulith añade los módulos compartidos a las dependencias permitidas de todos, así que **no** va en `allowedDependencies`, pero sí sale entre los módulos destino de `despliegue`.
   - Nada de `io.fabric8`, ningún paquete interno de otro módulo, y ninguna clase de producción fuera de `io.synkra.despliegue` importa `io.synkra.despliegue`.
   - Desviación del plan, que se anota en la bitácora y en el resumen: U07-T12 dice «solo usa `catalogo.api`, `kubernetes.api` y `registro.api`»; también usa `plataforma` (como `registro`) y `verificador`.
2. **`despliegue/package-info.java`.** Queda así, sin cambiar su Javadoc ni la línea `package`: el Javadoc actual, la línea `@ApplicationModule(allowedDependencies = {"catalogo :: api", "kubernetes :: api", "registro :: api", "plataforma"})`, `package io.synkra.despliegue;` y el import de `org.springframework.modulith.ApplicationModule`. Es el mismo patrón que `registro/package-info.java` (U06-C). Su efecto: el `verify()` de `elBackendRealRespetaSusFronteras` falla si `despliegue` usa una clase del **paquete base** de `catalogo`, `kubernetes` o `registro`. Sin esta línea, Modulith lo permite: el paquete base de cada módulo es siempre una interfaz (la sin nombre), aunque el módulo declare `api`. Ese es el rojo de la decisión 5.
3. **`despliegue` no expone nada.** No tiene paquete `api` ni clases públicas, y nadie fuera de él lo usa: las rutas HTTP son su única cara. No se crea un paquete `api` vacío.
4. **Pruebas nuevas en `FronterasModulosTest`** (se **añaden**, debajo de las de U06-T07, con un comentario de sección `// ---- U07-T12: las fronteras del módulo despliegue ----`, sin cambiar ni quitar ninguna línea):
   - `despliegueSoloDependeDeSusModulosPermitidos`: con `backendReal()`, `getModuleByName("despliegue")` y `getDependencies(modulos, DependencyType.values()).uniqueModules()`, los identificadores, ordenados, son **exactamente** `catalogo`, `kubernetes`, `plataforma`, `registro` y `verificador` (como `registroSoloDependeDeKubernetesApiYPlataforma`).
   - `despliegueSoloUsaLasApisDeOtrosModulos`: ArchUnit sobre `produccion()`. Ninguna clase de `io.synkra.despliegue..` depende de una clase de `io.synkra..` que no esté en `io.synkra.despliegue..`, `io.synkra.catalogo.api..`, `io.synkra.kubernetes.api..`, `io.synkra.registro.api..`, `io.synkra.plataforma..` o `io.synkra.verificador..`, ni de nada de `io.fabric8..`. El mensaje de la regla nombra la clase y la dependencia.
   - `ningunModuloDependeDeDespliegue`: ArchUnit sobre `produccion()`. Ninguna clase fuera de `io.synkra.despliegue..` (también las del paquete raíz `io.synkra`, como `SynkraApp`) depende de una clase de `io.synkra.despliegue..`.
   - Las tres pasan con el código de la punta de U07-F. Si alguna no pasa, el codificador **no** cambia el código de `despliegue` ni la regla para que pase: para y lo reporta con la violación literal. Si `uniqueModules()` de Modulith 1.4.14 no devuelve `verificador` (o devuelve otra cosa) con el código real, también para y lo reporta antes de ajustar la lista.
5. **Rojo primero, sin clases de juguete.** El código real ya cumple las reglas, así que no hay rojo sobre él. El rojo se muestra así:
   - **El hueco, antes del cambio.** En una copia de `<punta VERDE de U07-F>` (sin esta tarea), con la mutación «interno de registro» de CA-2, `./gradlew :backend:test --tests '*FronterasModulosTest'` da `BUILD SUCCESSFUL`: Modulith no lo veía. Se pega en la bitácora.
   - **Las pruebas nuevas, antes de `allowedDependencies`.** Un commit trae solo las tres pruebas; la misma mutación, en una copia de ese commit, ya hace caer `despliegueSoloUsaLasApisDeOtrosModulos` pero no `elBackendRealRespetaSusFronteras`. Se pega en la bitácora.
   - **Con todo.** CA-2: cada mutación cae en la prueba de su regla.
6. **El resumen: archivo y secciones.** Se llama `summary.md` y lleva, con estos encabezados exactos de nivel 2, en este orden:
   `## Qué expone`, `## Fronteras`, `## Cómo se prueba`, `## Salidas reales`, `## Desviaciones del diseño`, `## Trazabilidad`, `## Tareas`, `## Candidatas abiertas`, `## Cifras y su fuente`.
   La cabecera (antes de la primera sección) dice la unidad, las historias (dueñas y contribuciones, como en el plan), la fecha, el sha de las corridas, que el único código que tocó U07-G es `despliegue/package-info.java` y pruebas añadidas a `FronterasModulosTest`, y que nada se corrió contra un clúster.
7. **Qué expone.** Saca los nombres de los archivos de la punta, no de las bitácoras:
   - el paquete `io.synkra.despliegue` (sin `api`), la migración `V4__despliegue.sql` (`dep_plan`, sus CHECK, el índice, `origen_id` sin clave foránea) y la lista de clases de `backend/src/main/java/io/synkra/despliegue/` (`ls`);
   - las rutas, sacadas con `grep -rn 'Mapping(' backend/src/main/java/io/synkra/despliegue`: `POST /api/verificaciones`, `POST /api/despliegues`, `POST /api/despliegues/{id}/confirmacion`, `POST /api/despliegues/{id}/reversion` (y la que hubiera de más, tal cual sale del `grep`);
   - lo que U07 añadió fuera de `despliegue`, cada cosa con la tarea que la trajo: `plataforma/Reloj.java` (bean `Clock`, U07-C), `RegistroApi.aplicando()` (U07-E, C-116), los cambios en `kubernetes` de U07-E (C-76, C-82, C-83), las propiedades nuevas de `application.properties` y el ayudante de prueba `SimuladorKubernetes` (U07-B, ampliado en U07-D y U07-E). Lo que diga cada línea sale del `git diff` de esa tarea o de su bitácora, con la ruta;
   - una tabla R-D1 a R-D13 contra la clase de prueba que la ata (`PaisPermitidoTest`, `VerificacionVivoTest`, `CrearPlanTest`, `CambiosPropiedades`, `PlanPendienteTest`, `ConfirmarTest`, `ConfirmarSinClusterTest`, `CarpetaEquivocadaPlanTest`, `VigilanciaTest`, `RetomarVigilanciaTest`, `ReversionTest`, y las que haya de verdad): cada nombre de clase se comprueba con `ls backend/src/test/java/io/synkra/*/`.
8. **Fronteras.** Qué fija U07-G (decisiones 1 a 4), con el `grep -n allowedDependencies` de los dos `package-info.java` y la lista de `testcase` de `FronterasModulosTest` de la corrida fresca. Dice que `verificador` es biblioteca compartida (C-36) y no un módulo con fronteras propias.
9. **Cómo se prueba.** Sin clúster ni `kubectl` (AUTONOMIA-01): el servidor simulado de Fabric8 en modo CRUD (`SimuladorKubernetes`, con `aceptaAplicar`, C-121) o un doble de Mockito (`ConfirmarSinClusterTest`, `PaisPermitidoTest`); el reloj de prueba `RelojMovible` sobre el bean `Clock`; la programación apagada en las pruebas (`synkra.programacion.activa`, U07-E, C-117) y la vigilancia llamada a mano (`revisar()`); SQLite en `build/sqlite-pruebas/<Clase>`; lecturas de vuelta con `python3 -I`, sin `sqlite3`. Anota qué toma el filtro `*despliegue*` (las clases de `io.synkra.despliegue.*`) y qué **no** toma: `FronterasModulosTest` (en `io.synkra`) y las pruebas que U07 añadió en otros paquetes (`plataforma.RelojTest`, `registro.AplicandoTest` y las de `kubernetes` de U07-E). Esas salen en la tercera corrida (decisión 10) y en `:backend:check`.
10. **Salidas reales.** Se corren en esta tarea, en este orden, con `$JAVA_HOME` en Java 21 (se anota la ruta), sobre el último commit que toca código (el de las pruebas y el `package-info.java`):
    1. `./gradlew :backend:test --rerun --tests '*despliegue*'; echo "salida=$?"`, y justo después, del mismo árbol, el conteo por clase con el mismo comando que `aidlc-docs/construction/U06-registro/code/evidencia/conteo-por-clase.txt`, con `*despliegue*` en vez de `*registro*`.
    2. `./gradlew :backend:test --rerun --tests '*FronterasModulosTest' --tests '*RelojTest' --tests '*AplicandoTest' <y las clases de prueba que U07-E añadió en kubernetes>; echo "salida=$?"`, con su conteo por clase. Los nombres de las clases de `kubernetes` se sacan de `git diff --name-status --diff-filter=A <base de U07-E>...<punta VERDE de U07-E> -- backend/src/test/java/io/synkra/kubernetes` (se pega la orden y su salida); cada nombre se comprueba con `ls`. Si una clase nombrada aquí no existe, se anota y se quita de la orden, sin inventar otra.
    3. `./gradlew :backend:test --rerun :backend:check; echo "salida=$?"`, y el recuento de XML (cuántos, y cuántos tienen `skipped`, `failures` o `errors` distinto de 0), con los dos comandos de `aidlc-docs/construction/U06-registro/code/evidencia/conteo-xml.txt`.
    Cada corrida se guarda completa, con fecha, sha, `JAVA_HOME`, comando y salida, en `aidlc-docs/construction/U07-despliegue/code/evidencia/<nombre>.txt`. En el resumen se pega solo lo que sale de `tail -6` (o de `grep -n` aparte para líneas citadas, C-29), copiado tal cual y **sin elipsis `[...]`** dentro de los bloques. Ninguna cifra de pruebas o XML de una revisión anterior (852 pruebas de `main` `39a242c`, etc.) se presenta como la de hoy: el resumen dice la de la corrida fresca.
    Si el total de `*despliegue*` no coincide con la suma de las cifras por clase de las revisiones de U07-A a U07-F, se anota la diferencia con su causa (por ejemplo, pruebas añadidas en una ronda 2); no se ajusta nada para que cuadre.
11. **Desviaciones del diseño.** Lista numerada; cada una dice qué decía el diseño, el plan o la tarea, qué se hizo, quién lo anotó (ruta de la bitácora o revisión, con el apartado) y si el revisor la aceptó (frase literal del informe, o «el informe no la comenta»). Como mínimo, todas con fuente en un archivo real:
    - U07-B: «lo que corre» sale de `AdaptadorKubernetes.leerPais` y no del último `listo` del registro, como dice R-D4 (`bitacoras/U07-B.md`, «Desviación del diseño (decisión 2)»). Se dice si U07-D a U07-F lo mantuvieron (`grep -n leerPais` en `PlanServicio.java` de la punta).
    - U07-B: `PlanDto` respondía `estado` con `Plan.PENDIENTE` para esquivar el `grep` de escrituras (C-108, `bitacoras/U07-B.md`); U07-D volvió a `plan.estado()` (`bitacoras/U07-D.md`, «Cambio»).
    - U07-C: confirmar respondía 501 temporal hasta U07-D (`bitacoras/U07-C.md`; `revisiones/U07-C/ronda-2.md`); U07-D lo quitó (`tareas/U07-D-confirmar.md`, decisión 10, y su CA-4).
    - U07-C: el bean `Clock` en `plataforma/Reloj.java`, con nombre explícito `configuracionReloj` (`bitacoras/U07-C.md`, «Cambio»); `RegistroServicio` sigue con su reloj privado (C-113).
    - U07-D: R-D9 registra `aplicando` **antes** de aplicar (`bitacoras/U07-D.md`, desviación 4; `revisiones/U07-D/ronda-1.md`). En `aidlc-docs` se escribe «aprobada por el responsable del proyecto», sin nombre de persona (regla de C-30), y se cita la bitácora como fuente. Se dice cómo cierra U07-E el `aplicando` huérfano (C-115).
    - U07-D: `X-Synkra-Usuario` es una identidad sin autenticar mientras U10 esté cortada (C-114; `bitacoras/U07-D.md`, desviación 5).
    - U07-D: `SimuladorKubernetes.aceptaAplicar(pais)`, porque el modo CRUD de Fabric8 7.9.0 no entiende server-side apply (C-121; `bitacoras/U07-D.md`, desviación 1).
    - U07-D: los `grep` de `REQUIRES_NEW` y `reg_` de los criterios casaban con comentarios (C-122; `bitacoras/U07-D.md`, desviación 3 y ronda 2, F-02 y F-03).
    - U07-D: C-35 no muerde en el despliegue y C-87 queda cerrada por construcción en ese camino (`bitacoras/U07-D.md`, desviación 6).
    - U07-E y U07-F: las que anoten sus bitácoras y revisiones (por ejemplo: cambios en `kubernetes` por C-76, C-82 y C-83; `RegistroApi.aplicando()`; la programación apagada en pruebas; el log de un solo pod, C-119; si `ConfirmacionServicio` cambió para la reversión; `origenId` frente al `.original` de H-20). Solo con su fuente: si un archivo no la trae, no va.
    - U07-G: la desviación del plan de la decisión 1 (`plataforma` y `verificador`).
    Si en las bitácoras o revisiones aparece otra desviación no listada, se añade con su fuente; no se omite ninguna de arriba.
12. **Cifras y su fuente.** La última sección es una tabla de tres columnas, `| cifra | ruta | cadena |`, con una fila por cada número de prueba, de mutación, de ronda o de XML que el resumen cite fuera de las salidas pegadas. `ruta` va entre acentos graves, relativa a la raíz del repositorio, y es el archivo de `revisiones/`, `bitacoras/` o `evidencia/` de donde sale. `cadena` va entre acentos graves y es un texto literal que aparece en ese archivo y contiene la cifra (sin el carácter `|`). Si la revisión y la bitácora dan cifras distintas para lo mismo, se citan las dos con su fuente y no se reconcilian. Las filas de U07-E y U07-F salen de sus `revisiones/U07-E/` y `revisiones/U07-F/` y de sus bitácoras, como las de las demás: **cada cifra sale de un archivo real**, y la que no esté en ningún archivo no se escribe.
13. **Tareas.** Tabla de U07-A a U07-G con qué construyó cada una, tareas del plan que cubre (A: T01 y T02; B: T03 a T05; C: T06; D: T07 y T11; E: T08 y T09; F: T10; G: T12 y T13; se comprueba en la cabecera de cada tarea), PR (de `git log --merges` y `gh pr list --state merged </dev/null`; en `main` `39a242c` estaban #96 U07-A, #98 U07-B, #100 U07-C y #104 U07-D; los de U07-E y U07-F, si ya existen, de las mismas órdenes; no se inventan) y rondas (los `ronda-N.md` de `revisiones/U07-*/`, contados con `ls`, sin los `-respuesta`), con el veredicto de cada ronda copiado de su línea `VEREDICTO:`. U07-G: «sin PR todavía», «por revisar».
14. **Candidatas abiertas.** Se sacan con `grep`, no a mano (C-21):
    - **De U07:** C-30, C-35, C-76, C-82, C-83, C-87, C-90, C-106, C-108, C-109, C-112 a C-119 y C-121 a C-123, más las que hayan salido de U07-E, U07-F o U07-G (filas cuyo «Salió de» nombre `U07`). Para cada una: qué es (en pocas palabras), su columna «Decisión» tal como está, y, si una bitácora o revisión de U07 dice que se resolvió o se decidió (por ejemplo C-30 en `bitacoras/U07-A.md`; C-35, C-87 y C-108 en `bitacoras/U07-D.md`; C-76, C-82, C-83, C-109, C-115 y C-116 en U07-E), esa frase con su ruta. El resumen separa «abiertas» de «decididas en U07 según su bitácora», sin cerrar ninguna en `CANDIDATAS.md`.
    - **Relacionadas:** C-107 y C-110 (nombran a U07-D y U07-E en su decisión) y las filas cuya «Decisión» nombre `U07` (`grep -n 'U07' CANDIDATAS.md`: C-40, C-46, C-49, C-51, C-52, C-62, C-73 al escribir esta tarea), con su estado y sin decir que U07 las resolvió si ningún archivo lo dice.
    - Cada fila lleva la ruta de `CANDIDATAS.md` de donde se leyó (la de la rama, o la absoluta del orquestador).
15. **Trazabilidad.** Tabla `Historia | Tareas que la cubren | Entregada por`, con la del plan: H-09 (T03), H-11 (T01, T04, T05), H-12 (T07, T12, T13), H-13 (T02), H-14 (T08, T09), H-16 (T08), H-17 (T06), H-20 (T10), H-06 (contribuye: T11), H-08 (contribuye: T03); la columna «Entregada por» nombra la tarea U07-A a U07-G. Una línea aparte dice que el plan nombra H-24 como contribución y que ninguna tarea de U07 la ata por sí sola, y otra dice qué tareas contribuyen además a H-17 según su cabecera (U07-D, U07-E, U07-F).
16. **Formato.** Español, sin emojis. Los diagramas de Mermaid, si pones alguno, se validan (`common/content-validation.md`); no hace falta ninguno.
17. **Bitácora en `bitacoras/U07-G.md`, evidencia en `bitacoras/U07-G-evidencia/`** (un `caN.txt` por criterio, salida completa con fecha, sha y comando; los guiones `caN.sh` se versionan), y el revisor en `revisiones/U07-G/`. Toda la evidencia de código sale del último commit que toca código; la del resumen, de ese mismo sha (o de un commit posterior que solo traiga documentación, con la nota).

---

## Criterios de aceptación

- [ ] **CA-1**: U07-T12. Fronteras del módulo `despliegue`
  ```bash
  ./gradlew :backend:test --rerun --tests '*FronterasModulosTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*FronterasModulosTest.xml
  grep -o 'testcase name="[A-Za-z0-9]*()"' backend/build/test-results/test/TEST-*FronterasModulosTest.xml
  grep -Fcx '@ApplicationModule(allowedDependencies = {"catalogo :: api", "kubernetes :: api", "registro :: api", "plataforma"})' backend/src/main/java/io/synkra/despliegue/package-info.java
  grep -Fcx '@ApplicationModule(allowedDependencies = {"kubernetes :: api", "plataforma"})' backend/src/main/java/io/synkra/registro/package-info.java
  grep -rnE '^import (static )?io\.synkra\.' backend/src/main/java/io/synkra/despliegue | grep -vE 'io\.synkra\.(despliegue|catalogo\.api|kubernetes\.api|registro\.api|plataforma|verificador)\.'; echo "imports-ajenos=$?"
  grep -rlE '^import (static )?io\.fabric8\.' backend/src/main/java/io/synkra/despliegue; echo "fabric8=$?"
  grep -rlE '^import (static )?io\.synkra\.despliegue\.' backend/src/main/java | grep -v '/io/synkra/despliegue/'; echo "inverso=$?"
  grep -rlE '^public ' backend/src/main/java/io/synkra/despliegue; echo "publicas=$?"
  ls -d backend/src/main/java/io/synkra/despliegue/api; echo "api=$?"
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, sin saltos ni fallos; la lista de `testcase` trae las pruebas que ya había y además `despliegueSoloDependeDeSusModulosPermitidos()`, `despliegueSoloUsaLasApisDeOtrosModulos()` y `ningunModuloDependeDeDespliegue()`.
  - `1` y `1` (los dos `package-info.java`; el de `registro` no cambió).
  - `imports-ajenos=1`, `fabric8=1`, `inverso=1`, `publicas=1` y `api=` distinto de 0 (`ls` no lo encuentra).

- [ ] **CA-2**: cada regla de frontera muerde, demostrado en una copia
  ```bash
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"; (cd "$D" && git init -q)
  # Mutación anotada literalmente en la bitácora (sed o parche); una por copia:
  #   interno de registro:    MotivosRegistro pasa a public y ConfirmacionServicio gana un campo de ese tipo
  #   interno de kubernetes:  GuardiaNamespace pasa a public y PlanServicio gana un campo de ese tipo
  #   interno de catalogo:    CodigosCatalogo pasa a public y VerificacionVivo gana un campo de ese tipo
  #   fabric8:                PlanServicio gana un campo io.fabric8.kubernetes.client.KubernetesClient
  #   módulo nuevo:           una clase pública nueva io.synkra.otro.Extra y un campo de ese tipo en PlanServicio
  #   inverso:                CodigosDespliegue pasa a public y plataforma/ManejadorErrores gana un campo de ese tipo
  #   allowedDependencies:    se quita "registro :: api" de despliegue/package-info.java
  sed -i '<mutación>' "$D/backend/src/main/java/io/synkra/<ruta>.java"
  (cd "$D" && ./gradlew :backend:test --tests '*FronterasModulosTest')
  grep -B1 '<failure' "$D"/backend/build/test-results/test/TEST-*FronterasModulosTest.xml | grep -o 'testcase name="[A-Za-z0-9]*()"'
  rm -rf "$D"
  ```
  Esperado: con cada una de las siete mutaciones (una copia por mutación, con `git init -q`), `BUILD FAILED`, y el XML muestra un `<failure` en la prueba de esa regla:
  - las tres de «interno»: `despliegueSoloUsaLasApisDeOtrosModulos` y `elBackendRealRespetaSusFronteras`;
  - «fabric8»: `despliegueSoloUsaLasApisDeOtrosModulos` (y `soloElModuloKubernetesImportaFabric8`, que ya existía);
  - «módulo nuevo»: `despliegueSoloDependeDeSusModulosPermitidos` y `elBackendRealRespetaSusFronteras`;
  - «inverso»: `ningunModuloDependeDeDespliegue`;
  - «allowedDependencies»: `elBackendRealRespetaSusFronteras`.

  Si la última orden no casa por el formato del XML (el `<failure` no va en la línea siguiente al `testcase`), la bitácora lo anota y pega en su lugar el `testcase` con su `<failure` copiado del XML. Además, la bitácora pega los dos rojos de la decisión 5 (la mutación «interno de registro» con `BUILD SUCCESSFUL` en la copia de `<punta VERDE de U07-F>`, y en la copia del commit que solo trae las pruebas, cayendo solo `despliegueSoloUsaLasApisDeOtrosModulos`), con el sha y el `git show --stat` de ese commit. Sin `git stash` ni cambios en el worktree.

- [ ] **CA-3**: U07-T13. El resumen existe con las secciones, en orden
  ```bash
  f=aidlc-docs/construction/U07-despliegue/code/summary.md
  test -f "$f" && echo existe
  grep -n '^## ' "$f"
  for s in 'Qué expone' 'Fronteras' 'Cómo se prueba' 'Salidas reales' 'Desviaciones del diseño' 'Trazabilidad' 'Tareas' 'Candidatas abiertas' 'Cifras y su fuente'; do printf '%s ' "$s"; grep -c "^## $s\$" "$f"; done
  for h in H-06 H-08 H-09 H-11 H-12 H-13 H-14 H-16 H-17 H-20 H-24; do printf '%s ' $h; grep -c "$h" "$f"; done
  for c in C-30 C-35 C-76 C-82 C-83 C-87 C-90 C-106 C-107 C-108 C-109 C-110 C-112 C-113 C-114 C-115 C-116 C-117 C-118 C-119 C-121 C-122 C-123; do printf '%s ' $c; grep -c "$c" "$f"; done
  for r in R-D1 R-D2 R-D3 R-D4 R-D5 R-D6 R-D7 R-D8 R-D9 R-D10 R-D11 R-D12 R-D13; do printf '%s ' $r; grep -cw -- "$r" "$f"; done
  grep -n -i 'leerPais\|Plan\.PENDIENTE\|501\|antes de aplicar\|X-Synkra-Usuario\|aceptaAplicar' "$f"
  grep -c 'David\|Julian' "$f"
  ```
  Esperado: `existe`; el `grep -n` lista los nueve encabezados en ese orden; cada sección da `1`; cada historia, cada candidata y cada regla da al menos `1`; el `grep -n -i` trae líneas de la sección de desviaciones con cada uno de los seis términos; el último `grep -c` da `0` (sin nombres de personas, regla de C-30).

- [ ] **CA-4**: las salidas reales son frescas y de la punta
  ```bash
  ./gradlew :backend:test --rerun --tests '*despliegue*'; echo "salida=$?"
  ./gradlew :backend:test --rerun :backend:check; echo "salida=$?"
  f=aidlc-docs/construction/U07-despliegue/code/summary.md
  grep -c 'BUILD SUCCESSFUL' "$f"; grep -c 'salida=0' "$f"
  ls aidlc-docs/construction/U07-despliegue/code/evidencia/
  head -3 aidlc-docs/construction/U07-despliegue/code/evidencia/*.txt
  git log -1 --format=%h -- backend
  ```
  Esperado: las dos corridas dan `BUILD SUCCESSFUL` y `salida=0` (con `$JAVA_HOME` en Java 21, que se anota); el resumen contiene al menos tres `BUILD SUCCESSFUL` y tres `salida=0` (las tres corridas de la decisión 10); cada archivo de `evidencia/` trae fecha, sha, `JAVA_HOME` y comando en sus primeras líneas, y el sha es el que da la última orden (el último commit que toca `backend`). El revisor reproduce los mismos conteos de pruebas por clase y de XML.

- [ ] **CA-5**: toda cifra citada sale de un archivo real
  Guardar este guion como `bitacoras/U07-G-evidencia/ca5.sh` (se versiona) y correrlo desde la raíz de la rama:
  ```bash
  f=aidlc-docs/construction/U07-despliegue/code/summary.md
  sed -n '/^## Cifras y su fuente/,/^## [^C]/p' "$f" | grep '^| ' | tail -n +3 | while IFS='|' read -r _ c r s _; do
    r=$(echo "$r" | tr -d ' `'); s=$(echo "$s" | sed 's/^ *`//; s/` *$//')
    if grep -qF -- "$s" "$r"; then echo "ok $r"; else echo "FALTA $r :: $s"; fi
  done | sort | uniq -c
  ```
  Esperado: ninguna línea `FALTA`, y al menos una fila `ok` por cada una de `revisiones/U07-A/` a `revisiones/U07-F/`. Para cada número de prueba, mutación, ronda o XML del texto del resumen que no está en la tabla: el revisor lo busca con `grep -rnF '<cifra>' revisiones/U07-* bitacoras/U07-* aidlc-docs/construction/U07-despliegue/code/evidencia` y debe aparecer; si no aparece, es hallazgo. Las cifras de la corrida fresca salen de `evidencia/`, no de esta tabla.

- [ ] **CA-6**: toda salida pegada es copia literal de su archivo
  Guardar como `bitacoras/U07-G-evidencia/ca6.sh` y correrlo desde la raíz:
  ```bash
  d=aidlc-docs/construction/U07-despliegue/code
  awk '/^```/{b=!b;next} b' "$d/summary.md" | grep -v '^\$ ' | while IFS= read -r l; do
    [ -z "$l" ] && continue
    grep -rqxF -- "$l" "$d/evidencia" bitacoras/U07-G-evidencia bitacoras/U07-A-evidencia bitacoras/U07-B-evidencia bitacoras/U07-C-evidencia bitacoras/U07-D-evidencia bitacoras/U07-E-evidencia bitacoras/U07-F-evidencia revisiones/U07-A revisiones/U07-B revisiones/U07-C revisiones/U07-D revisiones/U07-E revisiones/U07-F || echo "NO LITERAL: $l"
  done
  grep -c '\[\.\.\.\]' "$d/summary.md"
  ```
  Esperado: el bucle no imprime nada y el último `grep -c` da `0` (ningún bloque con elipsis). Las líneas `$ comando` quedan fuera del bucle; su comando se compara a mano con el de las primeras líneas del archivo de `evidencia/` correspondiente.

- [ ] **CA-7**: todo el backend sigue pasando
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  grep -rnE 'Propagation\.REQUIRES_NEW|PROPAGATION_REQUIRES_NEW|setPropagationBehavior' backend/src/main/java; echo "requiresNew=$?"
  ```
  Esperado: `BUILD SUCCESSFUL`, ningún XML con `skipped`, `failures` o `errors` distinto de 0 (las pruebas de U07-A a U07-F y de U06 pasan sin cambios), y `requiresNew=1`.

- [ ] **CA-8**: el diff queda acotado
  Guardar como `bitacoras/U07-G-evidencia/ca8.sh`; `ca8.txt` se genera justo antes de su propio commit, con el árbol limpio y sin commits de código después: ese commit solo trae `ca8.sh`, `ca8.txt` y la línea de la bitácora que los cita.
  ```bash
  git rev-parse --short HEAD; git status --porcelain | wc -l
  git diff --stat <punta VERDE de U07-F>...HEAD
  git diff --name-only <punta VERDE de U07-F>...HEAD | grep -v -E '^(backend/src/main/java/io/synkra/despliegue/package-info\.java$|backend/src/test/java/io/synkra/FronterasModulosTest\.java$|aidlc-docs/construction/U07-despliegue/code/|bitacoras/U07-G\.md$|bitacoras/U07-G-evidencia/|revisiones/U07-G/)'; echo "fuera=$?"
  git diff <punta VERDE de U07-F>...HEAD -- backend/src/test/java/io/synkra/FronterasModulosTest.java | grep -c '^-[^-]'
  git diff <punta VERDE de U07-F>...HEAD -- backend/src/main/java/io/synkra/despliegue/package-info.java | grep -c '^-[^-]'
  ```
  Esperado: `fuera=1` (solo estas rutas), y `0` y `0` (en `FronterasModulosTest.java` y en `package-info.java` solo se añaden líneas). El sha impreso es el del padre del commit de `ca8.txt`, y el `wc -l` de `git status` cuenta solo `ca8.sh` y `ca8.txt` (se anota). Si hace falta evidencia que no cae en esas rutas, se para y se pregunta.

---

**Rojo primero:** el código real ya cumple las fronteras, así que el rojo se muestra con la mutación «interno de registro» (decisión 5): pasa en la copia de `<punta VERDE de U07-F>` (el hueco), cae en `despliegueSoloUsaLasApisDeOtrosModulos` con solo las pruebas nuevas, y cae además en `elBackendRealRespetaSusFronteras` con el `package-info.java`. El commit de las pruebas no trae el `package-info.java`: la bitácora pega su sha y su `git show --stat`. El resumen se escribe después del último commit de código, y sus corridas salen de ese sha.
