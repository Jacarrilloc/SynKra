# U06-C: vista general con concordancia y fronteras del registro

**Unidad:** U06: `Registro y vista general`
**Tareas del plan que agrupa:** U06-T05 y U06-T07
**Historias que implementa:** H-19, H-18 (contribuye: fronteras del módulo)
**Depende de:** U06-B (terminada) y U05 (terminada, en `main`). Sale de `<punta VERDE de U06-B>`. Asume como dado todo lo que fijaron U06-A y U06-B: `V3__registro.sql`, los ids que da quien registra, `RegistroApi` (`registrar`, `actualizarResultado`, `obtener`, `historial`), `DespliegueRegistrado`, `RutasRegistro`, `CodigosRegistro`, `MotivosRegistro`, el orden de R-R5 (`confirmado_en DESC, id DESC`), el `Clock` privado de `RegistroServicio` (sin bean `Clock`) y las carpetas `build/sqlite-pruebas/<Clase>`. Corre **en paralelo con U07-C**: no puede usar nada de `despliegue` (ni `PaisPermitido` ni `dep_plan`), así que lee `synkra.paises` por su cuenta.

---

## Alcance

**Dentro** (una línea, concreta):

> `RegistroApi.ultimoListo(pais)`, la vista general `VistaGeneralApi.vistaGeneral()` y su ruta `GET /api/paises`: por cada país de `synkra.paises`, en su orden (R-R7), lee el clúster con U05 `leerPais` y lo compara con su último despliegue `listo` (R-R6, `concuerda`); y las fronteras del módulo `registro` en `FronterasModulosTest` (solo depende de `kubernetes.api` y de `plataforma`, su API no expone tipos ajenos y no tiene rutas de escritura).

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- U06-T08: el resumen de la unidad (va en U06-D).
- Todo lo de U07: `dep_plan`, `PaisPermitido`, la regla de un solo `aplicando` por país (R-D7), cualquier ruta de escritura. Importar `io.synkra.despliegue` desde `registro`.
- Un bean `java.time.Clock`: lo crea U07-C, que corre en paralelo; dos beans del mismo tipo chocarían al unir las ramas. `RegistroServicio` sigue con su reloj privado.
- El estado de los pods (`AdaptadorKubernetes.estado`, réplicas listas, `CrashLoopBackOff`): la vista general solo dice cómo salió la **lectura** del clúster (decisión 5). Tampoco `existeNamespace`, `construir`, `aplicar` ni `contextoFallo`.
- C-90 (`leerPais` es más laxo que el core): se deja igual. La vista muestra el ConfigMap tal como lo lee U05.
- `backend/src/test/java/io/synkra/kubernetes/SimuladorKubernetes.java`: es de U07-B (otra rama). Esta tarea no lo añade ni escribe otro con ese nombre (decisión 8).
- Cambios en `V3__registro.sql`, en `registrar`, `actualizarResultado`, `obtener`, `historial`, `TruncadoContexto`, en los constructores de `RegistroServicio` y de `RutasRegistro`, y en las pruebas de U06-A y U06-B.
- Cambios en `verificador/`, `core/`, `plugin-api/`, `plugins/`, `init-descarga/`, en los módulos `catalogo`, `kubernetes` y `plataforma` del backend y en `build.gradle.kts`. En `application.properties`, solo lo que fija la decisión 3.
- Nada de `kubectl` ni de clúster real: el clúster de las pruebas es un doble del `AdaptadorKubernetes` (decisión 8).

---

## Archivos de contexto

- `tareas/U06-A-registro-base.md` y `tareas/U06-B-historial-reinicio.md` (contratos previos)
- `tareas/U07-A-planes-pais.md` (decisión 6: cómo valida `PaisPermitido` la misma propiedad; se copia la regla, no la clase)
- `aidlc-docs/construction/plans/U06-registro-code-generation-plan.md` (U06-T05 y T07)
- `aidlc-docs/construction/U06-registro/functional-design/business-rules.md` (R-R6 y R-R7), `domain-entities.md` (`EstadoPais`) y `business-logic-model.md` (vista general S7, interfaz pública)
- `aidlc-docs/inception/user-stories/stories.md` (H-19)
- `unidades-y-tareas.md` (sección U06)
- En la punta de U06-B: `backend/src/main/java/io/synkra/registro/` y `registro/api/`, y `backend/src/test/java/io/synkra/registro/HistorialTest.java` (estilo `RANDOM_PORT`, carpeta fija)
- `backend/src/main/java/io/synkra/kubernetes/api/` (`AdaptadorKubernetes.leerPais`, `LecturaPais`, `ClienteNoDisponibleException`, `LecturaFallidaException`, `LecturaPaisInconsistenteException`)
- `backend/src/test/java/io/synkra/FronterasModulosTest.java` (reglas de U05-T09: estilo de las reglas de ArchUnit sobre `produccion()`)
- `backend/src/main/java/io/synkra/plataforma/BaseDeDatos.java` (pool de **una** conexión)
- `CANDIDATAS.md` (C-34, C-90 y C-107)

---

## Decisiones del orquestador

1. **Nombres nuevos.**
   - En `io.synkra.registro.api` (públicos):
     - un método nuevo en `RegistroApi`: `Optional<DespliegueRegistrado> ultimoListo(String pais)`;
     - la interfaz `VistaGeneralApi`, con `List<VistaPais> vistaGeneral()`;
     - los records `VistaPais` y `PluginCorriendo(String nombre, String version, String sha256)`;
     - el enum `LecturaCluster` (`VACIO`, `LEIDO`, `INCONSISTENTE`, `NO_DISPONIBLE`).
     - No se llama `EstadoPais`, como en `domain-entities.md`: ese nombre ya lo usa `kubernetes.api.EstadoPais` (réplicas y fase), y los dos tipos se verían juntos.
   - En `io.synkra.registro` (no públicos):
     - `PaisesConfigurados` (`@Component`), decisión 3;
     - `Concordancia` (clase final con un método estático puro), decisión 6;
     - `VistaGeneralServicio implements VistaGeneralApi` (`@Service`), decisión 7;
     - `RutasVistaGeneral` (`@RestController`, `GET /api/paises`), con su record anidado `VistaPaisDto`.
   - `RegistroServicio` implementa `ultimoListo`. La vista general va en un servicio aparte para no cambiar los constructores de `RegistroServicio` (la prueba de U06-A usa el de paquete) y para que cada lectura del registro sea una llamada por el proxy de Spring, con su transacción propia (decisión 7). Se anota en la bitácora como desviación de `business-logic-model.md`, que pone `vistaGeneral` en `RegistroApi`: sigue en `registro.api`, en otra interfaz.
2. **`ultimoListo` (R-R6, «último despliegue `listo`»).**
   - `@Transactional(readOnly = true)`.
   - El último `listo` del país por el orden de R-R5: `WHERE pais = ? AND estado = 'listo' ORDER BY confirmado_en DESC, id DESC LIMIT 1`. Un `aplicando` o un `fallido` más reciente no cuenta.
   - Devuelve el mismo `DespliegueRegistrado` que `obtener` (plugins ordenados por nombre), con `contexto` nulo.
   - País que no son dos letras minúsculas (nulo incluido): `IllegalArgumentException`, como `historial`. País bien formado sin `listo`: `Optional.empty()`. No mira `synkra.paises`.
3. **`synkra.paises` en `registro`, sin depender de `despliegue`.**
   - `application.properties` recibe **exactamente** las tres líneas que añadió U07-A (`8182b9d`), en el mismo sitio (al final del archivo, tras el comentario de U05-T03): una línea en blanco, el comentario `# R-D1 y R-R7: países permitidos …` y `synkra.paises=${SYNKRA_PAISES:co,pe}`. Se copian de `git show 8182b9d:backend/src/main/resources/application.properties`, byte a byte, para que al unir con U07 git vea el mismo cambio en los dos lados y no haya conflicto. CA-2 lo comprueba con `diff`.
   - `PaisesConfigurados` la recibe con `@Value("${synkra.paises}")` y la valida en el constructor con las mismas reglas que `PaisPermitido` (decisión 6 de U07-A): separa por `,`, recorta los espacios de cada entrada, cada entrada es `[a-z]{2}`, sin vacías ni repetidas, al menos una. Si no cumple: `IllegalStateException` al arrancar, con un mensaje que nombra `synkra.paises (SYNKRA_PAISES)`.
   - `List<String> lista()` devuelve los países **en el orden de la propiedad** (R-R7), sin ordenarlos.
   - La regla queda repetida en dos módulos a propósito: se propone como candidata unificarla en `plataforma` cuando se unan U06 y U07. Se anota en la bitácora.
4. **`VistaPais` y su JSON.**
   - `VistaPais(String pais, LecturaCluster lectura, String imagen, List<PluginCorriendo> plugins, Boolean concuerda, Long ultimoDespliegueId, String core, String versionCore)`.
   - `imagen` y `plugins` son **lo leído del clúster** (H-19: «leída del clúster»): con `LEIDO`, la imagen del Deployment y los plugins del ConfigMap ordenados por nombre y versión; en los demás casos, `imagen` nula y `plugins` vacía (nunca nula).
   - `ultimoDespliegueId`, `core` y `versionCore` son del último `listo` del registro, o nulos si no hay. `LecturaPais` no trae la versión del core, así que se muestra la del registro.
   - `VistaPaisDto` (JSON): `pais`, `lectura`, `imagen`, `plugins` (`[{nombre, version, sha256}]`), `concuerda`, `ultimoDespliegueId`, `core`, `versionCore`. Claves en camelCase (convención de U06-B); `lectura` en minúsculas (`vacio`, `leido`, `inconsistente`, `no_disponible`); `concuerda` es `true`, `false` o `null`.
   - `GET /api/paises` → 200 con la lista en el orden de `synkra.paises`, siempre (también sin clúster). No recibe parámetros.
5. **La lectura del clúster, país por país.** Un país que no se puede leer no tumba la vista entera:
   - `leerPais` vacío: `VACIO`.
   - `leerPais` con valor: `LEIDO`.
   - `LecturaPaisInconsistenteException`: `INCONSISTENTE` y `concuerda = false` (lo que hay en el clúster no es un despliegue válido de SynKra, sea cual sea el registro).
   - `ClienteNoDisponibleException` o `LecturaFallidaException`: `NO_DISPONIBLE` y `concuerda = null`. No se sabe qué corre: nunca se afirma `true` ni `false` sin haber leído.
   - Cualquier otra excepción se propaga (500 de `ManejadorErrores`).
   - La vista **solo** llama a `leerPais`, una vez por país configurado y nunca para otro país (ni recorre namespaces).
6. **R-R6 (`Concordancia.concuerda(Optional<DespliegueRegistrado> ultimo, Optional<LecturaPais> leido)`).**
   - Los dos vacíos: `true`. Uno sin el otro: `false`.
   - Los dos presentes: `true` si y solo si `ultimo.imagen()` es **igual, como texto**, a `leido.imagenCore()` (incluye el digest) y el **conjunto** de ternas `(nombre, version, sha256)` de los plugins es el mismo en los dos (el orden no importa; un plugin de más o de menos, otra versión u otro sha256 dan `false`).
   - La versión del core no se compara aparte: `LecturaPais` no la trae y la imagen con digest ya identifica el build. Se anota en la bitácora como desviación de R-R6 («misma versión del core»).
7. **`VistaGeneralServicio` no retiene la conexión mientras lee el clúster.**
   - `vistaGeneral()` **no** es `@Transactional`. Para cada país, primero `registro.ultimoListo(pais)` (por la interfaz `RegistroApi` inyectada, con su transacción de solo lectura, que termina al volver) y después `leerPais(pais)`.
   - Motivo: el pool tiene una sola conexión (`BaseDeDatos`). Si la vista la retuviera durante la lectura del clúster, cualquier otra petición a la base esperaría hasta 30 s. Lo ata una prueba (CA-1) y una mutación (CA-5). Nada de `REQUIRES_NEW` (C-107).
8. **El clúster de las pruebas es un doble, no el servidor simulado.** `VistaGeneralTest` reemplaza el bean con `@MockitoBean AdaptadorKubernetes adaptador` y programa `leerPais` país por país.
   - Motivo: el ayudante del servidor simulado (`SimuladorKubernetes`) llega con U07-B, que va en otra rama; copiarlo aquí ataría esta tarea a U07-B y escribir otro con el mismo nombre chocaría al unir. La lectura real de U05 ya la prueban `LeerPaisTest` y sus vecinas: aquí se prueba qué hace el registro con lo que `leerPais` devuelve o lanza.
   - Tras cada caso, `InOrder` comprueba las llamadas a `leerPais` en el orden de `synkra.paises`, `never()` para `mx`, y `verifyNoMoreInteractions(adaptador)` (solo `leerPais`, decisión 5).
9. **SQLite y datos de `VistaGeneralTest`.**
   - Carpeta fija `build/sqlite-pruebas/VistaGeneralTest`, que el `@DynamicPropertySource` borra y crea de nuevo; no se borran filas entre pruebas. Fija `synkra.paises=pe,co,cl,ar,uy,bo,ec,ve,py,sv` (el orden no es alfabético, a propósito).
   - Filas, registradas por `RegistroApi` (`registrar` y `actualizarResultado`). `T0` es un instante fijo con milisegundos distintos de cero; `A` y `B` son `localhost:5001/synkra-core@sha256:` seguido de 64 `a` o 64 `b`; los sha256 de los plugins son 64 veces una misma cifra (`b`, `c`, `d`, `e`, `f`, `9`):

     | id | país | `confirmadoEn` | final | imagen | plugins |
     |---|---|---|---|---|---|
     | 41 | `pe` | `T0 + 4 s` | `LISTO` | A | `kyc-aml` 1.0.0 (`d`), `kyc-pe` 1.3.0 (`b`) |
     | 42 | `pe` | `T0 + 1 s` | `LISTO` | A | `kyc-pe` 1.2.0 (`e`) |
     | 43 | `pe` | `T0 + 6 s` | `FALLIDO` con contexto | A | `kyc-pe` 1.3.0 (`b`) |
     | 44 | `co` | `T0 + 2 s` | `LISTO` | A | `kyc-co` 1.0.0 (`c`) |
     | 45 | `cl` | `T0 + 3 s` | `LISTO` | A | `kyc-pe` 1.3.0 (`b`) |
     | 46 | `uy` | `T0 + 5 s` | `LISTO` | A | `kyc-pe` 1.3.0 (`b`) |
     | 47 | `pe` | `T0 + 7 s` | queda `aplicando` | A | `kyc-pe` 1.3.0 (`b`) |
     | 48 | `cl` | `T0 + 3 s` (empata con el 45) | `LISTO` | B | `kyc-pe` 1.3.0 (`b`) |
     | 49 | `mx` | `T0 + 5 s` | `LISTO` | A | `kyc-pe` 1.3.0 (`b`) |
     | 50 | `sv` | `T0 + 5 s` | `LISTO` | A | `kyc-pe` 1.3.0 (`b`) |

   - Lo que devuelve el doble en cada país, y lo esperado:

     | país | `leerPais` | último `listo` | `lectura` | `concuerda` |
     |---|---|---|---|---|
     | `pe` | A, `kyc-pe` 1.3.0 (`b`) y `kyc-aml` 1.0.0 (`d`), **en ese orden** | 41 | `leido` | `true` |
     | `co` | A, `kyc-co` 1.0.0 con sha `f` (ConfigMap con otro hash) | 44 | `leido` | `false` |
     | `cl` | A, `kyc-pe` 1.3.0 (`b`) (igual al 45, no al 48) | 48 (desempate por id) | `leido` | `false` |
     | `ar` | vacío | ninguno | `vacio` | `true` |
     | `uy` | vacío | 46 | `vacio` | `false` |
     | `bo` | A, `kyc-pe` 1.3.0 (`b`) | ninguno | `leido` | `false` |
     | `ec` | `LecturaPaisInconsistenteException` | ninguno | `inconsistente` | `false` |
     | `ve` | `LecturaFallidaException` | ninguno | `no_disponible` | `null` |
     | `py` | `ClienteNoDisponibleException` | ninguno | `no_disponible` | `null` |
     | `sv` | A, `kyc-pe` 1.3.0 (`b`) y además `kyc-extra` 1.0.0 (`9`) | 50 | `leido` | `false` |

   - `cl` cubre a la vez «imagen con otro digest» (el 48 tiene B) y el desempate de R-R5: si el desempate fuera por id ascendente, el último sería el 45 y saldría `true`.
10. **Fronteras (U06-T07).**
    - `registro/package-info.java` pasa a `@ApplicationModule(allowedDependencies = {"kubernetes :: api", "plataforma"})` (con su import de `org.springframework.modulith.ApplicationModule`), sin cambiar su Javadoc. Así `elBackendRealRespetaSusFronteras` (el `verify()` que ya existe) falla si `registro` usa `catalogo`, `despliegue` o un paquete interno de `kubernetes`, aunque sea por su `api`.
    - En `FronterasModulosTest` se **añaden** pruebas, sin cambiar ni quitar ninguna línea de las que hay:
      - `laApiDeRegistroSoloMencionaTiposPropios`: las firmas públicas de `io.synkra.registro.api..` (métodos, constructores, campos, supertipos, componentes de record, argumentos de tipo) no mencionan ningún tipo de `io.synkra..` fuera de `io.synkra.registro.api..` (por ejemplo, `LecturaPais` no se filtra en `VistaPais`), ni de `io.fabric8..`. Puede seguir el patrón de `laApiNoExponeFabric8` con una regla nueva; no se cambia la existente.
      - `registroNoTieneRutasDeEscritura`: ningún método de `io.synkra.registro..` lleva `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping` ni un `@RequestMapping` con `POST`, `PUT`, `PATCH` o `DELETE`.
      - `registroSoloDependeDeKubernetesApiYPlataforma`: con `ApplicationModules`, las dependencias del módulo `registro` (sus módulos destino) son exactamente `kubernetes` y `plataforma`. Si la API de Modulith 1.4.14 no lo permite sin rodeos, se escribe con ArchUnit sobre `produccion()` (las clases de `io.synkra.registro..` solo dependen de `io.synkra.registro..`, `io.synkra.kubernetes.api..` y `io.synkra.plataforma..` dentro de `io.synkra..`) y se anota en la bitácora.
    - Que cada regla muerde se demuestra con mutaciones en copias (CA-5), no con clases de juguete nuevas.
11. **Bitácora en `bitacoras/U06-C.md`**, y el revisor en `revisiones/U06-C/`.

---

## Criterios de aceptación

- [ ] **CA-1**: U06-T05. Vista general con concordancia (R-R6) sobre los países configurados (R-R7), con lectura de vuelta del archivo de la prueba
  ```bash
  ./gradlew :backend:test --rerun --tests '*VistaGeneralTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*VistaGeneralTest.xml
  python3 -I -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute('select id, pais, estado from reg_despliegue order by id').fetchall()); print(c.execute(\"select d.pais, d.id from reg_despliegue d where d.id = (select e.id from reg_despliegue e where e.pais = d.pais and e.estado = 'listo' order by e.confirmado_en desc, e.id desc limit 1) order by d.pais\").fetchall()); print(c.execute('select despliegue_id, group_concat(nombre) from (select * from reg_plugin order by despliegue_id, nombre) group by despliegue_id having count(*) > 1').fetchall())" backend/build/sqlite-pruebas/VistaGeneralTest/synkra.db
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, `tests` ≥ 8 y `skipped="0" failures="0" errors="0"`. Las pruebas afirman, con los datos de la decisión 9:
    - `vistaGeneral()` da los diez países en el orden `pe, co, cl, ar, uy, bo, ec, ve, py, sv`, sin `mx`, con la `lectura`, el `concuerda` y el `ultimoDespliegueId` de la tabla (41, 44, 48, nulo, 46, nulo, nulo, nulo, nulo, 50);
    - en `pe`, `plugins` sale ordenado (`kyc-aml`, `kyc-pe`) aunque el doble los dio al revés, `imagen` es A, y `core` y `versionCore` son los del 41;
    - en `ar`, `ec`, `ve` y `py`, `imagen` es nula y `plugins` vacía;
    - el doble recibió `leerPais` una vez por país, en ese orden (`InOrder`), nunca `leerPais("mx")`, y nada más (`verifyNoMoreInteractions`);
    - `GET /api/paises` (con `RANDOM_PORT` y `TestRestTemplate`) da 200 y el mismo contenido; cada elemento tiene exactamente las claves `concuerda`, `core`, `imagen`, `lectura`, `pais`, `plugins`, `ultimoDespliegueId` y `versionCore`; `lectura` va en minúsculas y `concuerda` es JSON `null` en `ve` y `py`;
    - `ultimoListo("pe")` es el 41 y es igual (`equals`) a `obtener(41)`; `ultimoListo("cl")` es el 48; `ultimoListo("ar")` es vacío; `ultimoListo("PE")` y `ultimoListo(null)` lanzan `IllegalArgumentException`;
    - mientras el doble atiende `leerPais("pe")`, otro hilo hace `jdbc.queryForObject("select count(*) from reg_despliegue", Long.class)` y termina en menos de 5 s (`Future.get(5, TimeUnit.SECONDS)`), y el resultado es 10 (la vista no retiene la única conexión, decisión 7);
    - `Concordancia.concuerda` directamente: los cuatro casos de vacíos (vacío y vacío `true`; los dos mixtos `false`) y los de la tabla.
  - Python:
    - `[(41, 'pe', 'listo'), (42, 'pe', 'listo'), (43, 'pe', 'fallido'), (44, 'co', 'listo'), (45, 'cl', 'listo'), (46, 'uy', 'listo'), (47, 'pe', 'aplicando'), (48, 'cl', 'listo'), (49, 'mx', 'listo'), (50, 'sv', 'listo')]`;
    - `[('cl', 48), ('co', 44), ('mx', 49), ('pe', 41), ('sv', 50), ('uy', 46)]`;
    - `[(41, 'kyc-aml,kyc-pe')]`.

- [ ] **CA-2**: `registro` lee `synkra.paises` por su cuenta, con la misma propiedad que U07-A, y un valor inválido impide arrancar
  ```bash
  ./gradlew :backend:test --rerun --tests '*PaisesConfiguradosTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*PaisesConfiguradosTest.xml
  git show 8182b9d:backend/src/main/resources/application.properties | diff - backend/src/main/resources/application.properties; echo "igual=$?"
  ./gradlew :backend:bootJar
  L=$(mktemp); H=$(mktemp -d)
  HOME="$H" KUBECONFIG="$H/kubeconfig-falso" SYNKRA_DATA=$(mktemp -d) SYNKRA_PAISES='co,PE' timeout 60 java -Duser.home="$H" -jar backend/build/libs/backend-*.jar --server.port=18091 > "$L" 2>&1; echo "salida=$?"
  grep -c "SYNKRA_PAISES" "$L"; grep -ci "paisesConfigurados" "$L"
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, sin saltos ni fallos. `PaisesConfiguradosTest` (JUnit sin Spring) afirma: `"co, pe"` da `[co, pe]`; `"pe,co,ar"` da `[pe, co, ar]` (el orden se conserva); `""`, `"co,,pe"`, `"co,co"`, `"CO"`, `"col"` y `"c"` lanzan `IllegalStateException` con `synkra.paises (SYNKRA_PAISES)` en el mensaje, y el mensaje no repite lo recibido.
  - `igual=0` (sin salida del `diff`).
  - `salida=` distinto de 0 y de 124 (falla al arrancar, no por el `timeout`), y los dos conteos ≥ 1 (en esta rama no hay `despliegue`: el rechazo viene de `registro`).

- [ ] **CA-3**: el JAR real, sin clúster, responde la vista general en el orden configurado y sin inventar concordancia
  ```bash
  ./gradlew :backend:bootJar
  export SYNKRA_DATA=$(mktemp -d); R=$(mktemp -d); H=$(mktemp -d)
  arranca() { HOME="$H" KUBECONFIG="$H/kubeconfig-falso" SYNKRA_PAISES=pe,co,ar java -Duser.home="$H" -jar backend/build/libs/backend-*.jar --server.port=18091 > "$R/log-$1.txt" 2>&1 & PID=$!; timeout 90 bash -c 'until curl -sf localhost:18091/actuator/health >/dev/null; do sleep 1; done'; }
  # 1. Primer arranque: solo migra. Se para y se siembran filas (el registro no tiene ruta de escritura).
  arranca 0; kill $PID; wait $PID 2>/dev/null
  python3 -I - "$SYNKRA_DATA/synkra.db" <<'EOF'
  import sqlite3, sys, json
  c = sqlite3.connect(sys.argv[1])
  ver = json.dumps({"global": "APROBADA", "porPlugin": [], "motivosGlobales": []})
  img = "localhost:5001/synkra-core@sha256:" + "a" * 64
  for i, pais in ((61, "pe"), (62, "mx")):
      c.execute("insert into reg_despliegue (id, pais, tipo, origen_id, core, version_core, imagen, creado_por, confirmado_por,"
                " confirmado_en, estado, terminado_en, veredicto) values (?, ?, 'despliegue', null, 'synkra-core', '2.1.0', ?,"
                " 'ana', 'luis', 1795000001123, 'listo', 1795000001999, ?)", (i, pais, img, ver))
      c.execute("insert into reg_plugin (despliegue_id, nombre, version, sha256, etiquetas) values (?, 'kyc-pe', '1.3.0', ?, '{}')",
                (i, "b" * 64))
  c.commit()
  EOF
  # 2. Segundo arranque: la vista general.
  arranca 1
  curl -s -o "$R/v.json" -w '%{http_code}\n' localhost:18091/api/paises
  python3 -I -c "import json,sys; l=json.load(open(sys.argv[1])); print([(d['pais'], d['lectura'], d['concuerda'], d['ultimoDespliegueId'], d['versionCore'], d['imagen'], d['plugins']) for d in l]); print(sorted(l[0]))" "$R/v.json"
  kill $PID; wait $PID 2>/dev/null
  ```
  Esperado, en orden:
  - `200`;
  - `[('pe', 'no_disponible', None, 61, '2.1.0', None, []), ('co', 'no_disponible', None, None, None, None, []), ('ar', 'no_disponible', None, None, None, None, [])]` (el JAR no tiene cuenta de servicio: no se sabe qué corre, y `mx` no sale porque no está configurado);
  - `['concuerda', 'core', 'imagen', 'lectura', 'pais', 'plugins', 'ultimoDespliegueId', 'versionCore']`.

  Si el puerto 18091 está ocupado, se anota en la bitácora y se usa otro en todo el criterio (también en CA-2). La bitácora pega la salida tal cual.

- [ ] **CA-4**: U06-T07. Fronteras del módulo `registro`
  ```bash
  ./gradlew :backend:test --rerun --tests '*FronterasModulosTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*FronterasModulosTest.xml
  grep -o 'testcase name="[A-Za-z]*"' backend/build/test-results/test/TEST-*FronterasModulosTest.xml
  grep -n "allowedDependencies" backend/src/main/java/io/synkra/registro/package-info.java
  grep -rn "io.synkra.despliegue\|io.synkra.catalogo" backend/src/main/java/io/synkra/registro; echo "ajenos=$?"
  grep -rnE "io\.synkra\.kubernetes\." backend/src/main/java/io/synkra/registro | grep -vE "io\.synkra\.kubernetes\.api\."; echo "internos=$?"
  grep -rnE "\.(aplicar|construir|contextoFallo|existeNamespace)\(" backend/src/main/java/io/synkra/registro; echo "k8s-escrituras=$?"
  grep -rn "@Bean" backend/src/main/java/io/synkra/registro; echo "beans=$?"
  ```
  Esperado:
  - `BUILD SUCCESSFUL`, sin saltos ni fallos; la lista de `testcase` trae las pruebas que ya había y además `laApiDeRegistroSoloMencionaTiposPropios`, `registroNoTieneRutasDeEscritura` y `registroSoloDependeDeKubernetesApiYPlataforma`.
  - Una línea con `allowedDependencies = {"kubernetes :: api", "plataforma"}`.
  - `ajenos=1`, `internos=1`, `k8s-escrituras=1` y `beans=1` (ningún bean nuevo en `registro`, tampoco un `Clock`).

- [ ] **CA-5**: rojo primero. Cada regla nueva tiene una prueba que muerde, demostrado en una copia
  ```bash
  D=$(mktemp -d); git archive HEAD | tar -x -C "$D"
  # Mutación anotada literalmente en la bitácora; una por regla, por ejemplo:
  #   R-R6 digest:     Concordancia no compara la imagen (cl da true)
  #   R-R6 sha256:     Concordancia compara los plugins solo por (nombre, version) (co da true)
  #   R-R6 conjunto:   Concordancia compara las listas en orden (pe da false)
  #   R-R6 de más:     Concordancia acepta que lo leído contenga lo registrado (sv da true)
  #   R-R6 vacíos:     «uno sin el otro» da true (bo y uy dan true)
  #   último listo:    quitar «estado = 'listo'» de ultimoListo (pe toma el 47)
  #   desempate:       «id DESC» pasa a «id ASC» en ultimoListo (cl toma el 45 y da true)
  #   R-R7:            vistaGeneral recorre List.of("pe", "co", "mx") en vez de paises.lista()
  #   no disponible:   ClienteNoDisponibleException da concuerda = false (py da false)
  #   conexión:        @Transactional(readOnly = true) en VistaGeneralServicio.vistaGeneral
  #   frontera:        RegistroServicio recibe un CatalogoApi en un campo nuevo con @Autowired
  #   API:             VistaPais gana un componente LecturaPais lectura2
  #   escritura:       RutasVistaGeneral gana un @PostMapping vacío
  sed -i '<mutación>' "$D/backend/src/main/java/io/synkra/registro/<ruta>.java"
  (cd "$D" && ./gradlew :backend:test --tests '*VistaGeneralTest' --tests '*FronterasModulosTest')
  grep -l '<failure' "$D"/backend/build/test-results/test/TEST-*.xml
  rm -rf "$D"
  ```
  Esperado: con cada una de las trece mutaciones (una copia por mutación), `BUILD FAILED`, y el XML de la clase dueña de la regla muestra al menos un `<failure` en la prueba de esa regla (`VistaGeneralTest` para las diez primeras y `FronterasModulosTest` para las tres últimas, cada una en su prueba: `elBackendRealRespetaSusFronteras` o `registroSoloDependeDeKubernetesApiYPlataforma` para «frontera»). Con «conexión», la prueba de la decisión 7 cae por el plazo de 5 s, no por los 30 s del pool. La bitácora pega el `sed` (o el parche) y el nombre de la prueba que cayó. Sin `git stash` ni cambios en el worktree. Además, la bitácora registra el rojo de `VistaGeneralTest` y de `PaisesConfiguradosTest` antes de su código.

- [ ] **CA-6**: todo el backend sigue pasando
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL` y ningún XML con `skipped`, `failures` o `errors` distinto de 0 (las pruebas de U06-A y U06-B siguen verdes sin cambios).

- [ ] **CA-7**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <punta VERDE de U06-B>...HEAD
  git diff --name-status <punta VERDE de U06-B>...HEAD -- backend/src/test
  git diff <punta VERDE de U06-B>...HEAD -- backend/src/test/java/io/synkra/FronterasModulosTest.java | grep -c '^-[^-]'
  ```
  Esperado: solo estas rutas. Se anota el sha.
  - `backend/src/main/java/io/synkra/registro/**` (incluye `registro/api/` y `package-info.java`)
  - `backend/src/main/resources/application.properties`: solo las tres líneas de la decisión 3 (CA-2 da `igual=0`)
  - `backend/src/test/java/io/synkra/registro/**`: la segunda orden solo da `A` (archivos nuevos: `VistaGeneralTest.java` y `PaisesConfiguradosTest.java`, más algún ayudante de prueba nuevo si hace falta), salvo la línea `M` de `FronterasModulosTest.java`
  - `backend/src/test/java/io/synkra/FronterasModulosTest.java`: la tercera orden da `0` (solo se añade)
  - `bitacoras/U06-C.md` y `revisiones/U06-C/`

---

**Rojo primero:** `VistaGeneralTest` antes de `ultimoListo`, `Concordancia`, `VistaGeneralServicio` y `RutasVistaGeneral`; `PaisesConfiguradosTest` antes de `PaisesConfigurados`; cada prueba nueva de `FronterasModulosTest` antes de `allowedDependencies`. Un commit «rojo» trae solo la prueba y, si hace falta para compilar, firmas vacías que lanzan `UnsupportedOperationException`, nunca la lógica: la bitácora pega su sha y su `git show --stat`. Cada rojo queda en la bitácora, y CA-5 demuestra que cada regla muerde con el código terminado.
