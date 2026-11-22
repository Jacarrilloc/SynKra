# Métodos de los componentes: SynKra

Firmas de alto nivel en notación Java (C# para C12). Las reglas de negocio en detalle (cómo se
ordenan las alternativas, la máquina de estados, el esquema de tablas) se fijan en Functional
Design de cada unidad. `Resultado<T>` es un valor o una lista de `Motivo` (código + mensaje +
campo), nunca una excepción para errores de negocio.

## C01 · Verificador

| Método | Entrada → Salida | Propósito | Historias |
|---|---|---|---|
| `LectorDescriptor.leer(InputStream jar)` | JAR → `Resultado<Descriptor>` | Abre el JAR, lee `synkra-plugin.yaml`, valida campos obligatorios | H-03 |
| `Descriptor.aYaml()` / `LectorDescriptor.desdeYaml(String)` | ida y vuelta | Serializar y leer el descriptor (PBT-02) | H-03 |
| `RangoVersion.parsear(String)` | `"2.x"` → `Resultado<RangoVersion>` | Rango semver, con `N.x` como abreviatura | H-07 |
| `RangoVersion.incluye(Version)` | `Version` → `boolean` | ¿La versión del core está en el rango? (PBT-03) | H-07 |
| `Verificador.verificar(Version core, List<Descriptor> elegidos, String nombreCore)` | → `Veredicto` | Veredicto por plugin y global; detecta core equivocado (P4) | H-07, H-21 |
| `Verificador.alternativas(Veredicto, Catalogo disponible)` | → `Veredicto` con alternativas | Qué versión del plugin o del core sí sirve, o "no hay" | H-08 |
| `SuiteCombinaciones.casos()` (testFixtures) | → `List<Caso>` | Los 7 grupos del PRD §11.1 con su resultado esperado | H-10 |

`Catalogo disponible` es una interfaz de solo lectura (versiones del core y de cada plugin) que
el backend implementa con C06 y las pruebas con datos fijos; así C01 no depende de nada.

## C02 · Contrato de plugins

| Método | Propósito |
|---|---|
| `SynkraPlugin.nombre(): String` | Identidad del plugin cargado |
| `SynkraPlugin.iniciar(ContextoPlugin)` / `detener()` | Ciclo de vida; una excepción en `iniciar` impide que el core quede listo |
| `PuntoKyc.validar(SolicitudKyc): ResultadoKyc` | Punto de extensión de ejemplo, implementado por `kyc-co` y `kyc-pe` |

## C03 · Core

| Método | Entrada → Salida | Historias |
|---|---|---|
| `ArranquePlugins.ejecutar(Path plugins, Path activos)` | → `Resultado<List<PluginCargado>>`; si falla, el proceso termina con código ≠ 0 | H-21 |
| `CargadorPlugins.cargar(Path jar)` | → `PluginCargado` en su propio `URLClassLoader` | H-22 |
| `GET /plugins` | → `[{nombre, version}]` | H-21 |
| `GET /actuator/health/readiness` | → `UP` solo tras cargar los plugins | H-21, H-22 |
| `POST /kyc/{plugin}` | → respuesta del `PuntoKyc` | H-23 |

## C05 · Init de descarga

| Paso del script | Entrada → Salida | Historias |
|---|---|---|
| `descargar.sh` | `/config/artefactos.txt` (líneas `nombre sha256`) + `SYNKRA_URL` → archivos en `/plugins`; código ≠ 0 si un hash no coincide o se agotan 5 intentos | H-15 |

## C06 · Catálogo

| Método | Entrada → Salida | Historias |
|---|---|---|
| `registrarCore(NuevoCore)` | nombre, versión, imagen con digest → `Resultado<Core>` | H-01, H-31 |
| `obtenerCore(String nombre)` | → `Core` con sus versiones | H-01 |
| `subirPlugin(String core, InputStream jar, Map<String,String> etiquetas)` | → `Resultado<Plugin>` (descriptor, sha256) | H-02, H-03, H-04 |
| `listarPlugins(String core, Map<String,String> filtro)` | → `List<Plugin>` | H-05, H-06 |
| `obtenerPlugin(String core, String nombre, Version v)` | → `Optional<Plugin>` | H-02 |
| `abrirArtefacto(String sha256)` | → `Optional<InputStream>` | H-15 |

## C07 · Despliegue

| Método | Entrada → Salida | Historias |
|---|---|---|
| `verificar(Seleccion)` | país, core, versión, plugins → `Veredicto` (sin crear nada) | H-09 |
| `prepararPlan(Seleccion, Usuario)` | → `Resultado<Plan>` en `pendiente`, con resumen y cambios | H-11, H-13, H-17 |
| `confirmar(PlanId, Usuario)` | → `Resultado<Despliegue>` en `aplicando`; 409 si ya estaba confirmado | H-12 |
| `vigilar(DespliegueId)` (asíncrono) | → estado final `listo` o `fallido`, con contexto | H-14, H-16 |
| `prepararReversion(DespliegueId origen, Usuario)` | → `Resultado<Plan>` de tipo `reversion` | H-20 |
| `obtenerPlan(PlanId)` | → `Plan` | H-11 |

## C08 · Registro

| Método | Entrada → Salida | Historias |
|---|---|---|
| `registrar(Despliegue)` / `actualizarResultado(DespliegueId, Estado, Contexto)` | → `void` | H-18, H-16 |
| `obtener(DespliegueId)` | → `Despliegue` con contexto | H-16 |
| `historial(String pais)` | → `List<Despliegue>` | H-18 |
| `vistaGeneral()` | → `List<EstadoPais>` (lo que corre, leído con C09, + `concuerda`) | H-19 |

## C09 · Adaptador de Kubernetes

| Método | Entrada → Salida | Historias |
|---|---|---|
| `aplicar(ObjetosPais)` | Deployment + ConfigMap + Service → `void`; lanza si el namespace no es `synkra-*` | H-12, H-13 |
| `construir(Plan, String urlCatalogo)` | → `ObjetosPais` (con el init container y los hashes) | H-12, H-15 |
| `estado(String namespace)` | → `EstadoDespliegueK8s` (réplicas listas, reinicios) | H-14 |
| `contextoFallo(String namespace)` | → log de arranque del core + eventos del pod | H-16 |
| `leerPais(String namespace)` | → core, plugins activos y versiones leídos del clúster | H-19 |

## C10 · API REST

| Ruta | Llama a | Historias |
|---|---|---|
| `POST/GET /api/cores`, `GET /api/cores/{nombre}` | C06 | H-01 |
| `POST/GET /api/cores/{nombre}/plugins`, `GET …/{plugin}/{version}` | C06 | H-02 a H-06 |
| `GET /api/artefactos/{sha256}` | C06 | H-15 |
| `POST /api/verificaciones` | C07 | H-08, H-09 |
| `POST /api/despliegues`, `GET /api/despliegues/{id}` | C07, C08 | H-11, H-16 |
| `POST /api/despliegues/{id}/confirmacion` | C07 | H-12 |
| `POST /api/despliegues/{id}/reversion` | C07 | H-20 |
| `GET /api/paises`, `GET /api/paises/{pais}/despliegues` | C08 | H-18, H-19 |
| `GET /api/v3/api-docs` (Should) | springdoc | H-29 |

Formato de error único: `{codigo, mensaje, detalles: [{campo, motivo}]}` con 401, 403, 404, 409,
413 o 422.

## C11 · Seguridad (Should)

| Método | Propósito | Historias |
|---|---|---|
| `UsuariosSqlite.loadUserByUsername(String)` | Usuarios y roles desde SQLite (claves con BCrypt) | H-30 |
| Reglas por ruta | Operador: cores y usuarios; Publicador: subir plugins; Desplegador: planes, confirmación y reversión; Lector: solo GET; `/api/artefactos/**` sin credenciales | H-30 |

## C12 · Front (C#)

| Página | Llama a | Historias |
|---|---|---|
| `VistaGeneral.razor` | `GET /api/paises` | H-25 |
| `Catalogo.razor` | `GET/POST /api/cores…` | H-25 |
| `Desplegar.razor` | `POST /api/verificaciones` en vivo, `POST /api/despliegues`, `…/confirmacion` | H-25 |
| `Detalle.razor` | `GET /api/despliegues/{id}`, `…/reversion` | H-25 |

## C13 · Instalación y límite

| Comando | Propósito | Historias |
|---|---|---|
| `make install` | Registro local, imágenes, namespaces, RBAC, PVC, backend y front | H-26 |
| `make verificar-limite` | Las comprobaciones `kubectl auth can-i` de H-27 | H-27 |
| `scripts/e2e-api.sh` | CU1 a CU5 con `curl` | H-24 |
