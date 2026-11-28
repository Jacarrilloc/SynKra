# U05 Pod del país: resumen de la unidad

Unidad: U05 (`backend/`, paquete `io.synkra.kubernetes`, y `init-descarga/`). Historia dueña: H-15; contribuye a H-12, H-13, H-14, H-16, H-19 y H-28 (plan de la unidad y `tareas/U05-T10-resumen-unidad.md`).
Este resumen lo escribe U05-T10 sin cambiar código, pruebas, build ni scripts. Las salidas de abajo se produjeron en esta tarea, el 2026-11-27, con `--rerun`, sobre la rama `m6r/U05-T10` en el sha `03943ec` (punta de U05-T09, antes del commit de este resumen). Los archivos originales están en `evidencia/` (junto a este resumen). Nada se corrió contra un clúster: AUTONOMIA-01.

## Qué expone

- **API de módulo** (`io.synkra.kubernetes.api`, el único paquete público, atado por `FronterasModulosTest`, T09): la interfaz `AdaptadorKubernetes` con los cinco métodos del plan, `construir(PlanPais)`, `aplicar(ObjetosPais)`, `estado(namespace)`, `contextoFallo(namespace)` y `leerPais(pais)`, más dos auxiliares, `disponible()` y `existeNamespace(namespace)`.
- **Tipos de `kubernetes.api`:** `PlanPais`, `ObjetosPais` (interfaz con `yamlDeployment`, `yamlConfigMap` y `yamlService`, sin tipos de Fabric8, T09), `EstadoPais`, `FaseDespliegue`, `ContextoFalloK8s`, `LecturaPais` y las excepciones `NamespaceNoPermitidoException`, `ClienteNoDisponibleException`, `AplicacionFallidaException`, `LecturaFallidaException`, `LecturaPaisInconsistenteException`, `DespliegueAusenteException` y `ObjetosPaisIncompletosException`.
- **Implementación interna** (no pública): `AdaptadorFabric8`, `ConfiguracionKubernetes` (cliente con la cuenta del pod, ya no es bean público), `CuentaServicio`, `VariablesPod`, `GuardiaNamespace`, `ConstructorObjetosPais`, `LectorPais`, `LectorConfigMapPais` y `ObjetosPaisFabric8`.
- **Init container** (`init-descarga/`): `descargar.sh` (lee `/config/artefactos.txt`, descarga por hash de `$SYNKRA_URL`, comprueba `sha256sum`, reintenta con espera creciente, escribe solo en `/plugins`), `test.sh` y el `Dockerfile` de la imagen `synkra-init-descarga:1.0.0`.

## Cómo se prueba

- **Adaptador:** servidor simulado de Fabric8 (`KubernetesMockServer`, modo CRUD), con `HOME` y `KUBECONFIG` falsos; ninguna prueba toca un clúster ni `kubectl`. La guardia de namespace se prueba con cero peticiones registradas.
- **Script:** `init-descarga/test.sh` levanta un servidor HTTP local y un `sleep` falso para las esperas; 27 casos.
- **Imagen:** se construye con Docker local y se inspecciona; no se publica.
- **Fronteras:** `FronterasModulosTest` (U04) incluye `kubernetes`, y `io.fabric8` no aparece fuera de `io.synkra.kubernetes`.

## Salidas reales

### Pruebas del adaptador

```
$ ./gradlew :backend:test --rerun --tests '*kubernetes*' 2>&1 | tail -8
BUILD SUCCESSFUL in 15s
9 actionable tasks: 9 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.9.2/userguide/configuration_cache_enabling.html
```

(2026-11-27T19:00:49-05:00, sha `03943ec`; salida completa en `evidencia/gradle-kubernetes.txt`.)

Conteo por clase, de los XML de `backend/build/test-results/test/` (`evidencia/conteo-por-clase.txt`):

```
kubernetes.AplicarTest tests="10" skipped="0" failures="0" errors="0"
kubernetes.ClienteKubernetesTest tests="10" skipped="0" failures="0" errors="0"
kubernetes.ConfiguracionKubernetesTest tests="2" skipped="0" failures="0" errors="0"
kubernetes.ConstruirObjetosPaisTest tests="32" skipped="0" failures="0" errors="0"
kubernetes.EstadoYContextoTest tests="20" skipped="0" failures="0" errors="0"
kubernetes.GuardiaNamespaceTest tests="22" skipped="0" failures="0" errors="0"
kubernetes.ImagenInitCaracterizacionTest tests="2" skipped="0" failures="0" errors="0"
kubernetes.ImagenInitConfiguradaTest tests="3" skipped="0" failures="0" errors="0"
kubernetes.LeerPaisTest tests="24" skipped="0" failures="0" errors="0"
kubernetes.YamlObjetosPaisTest tests="2" skipped="0" failures="0" errors="0"
```

Suman 127 pruebas, sin saltos ni fallos. El filtro `*kubernetes*` toma los nombres de paquete `io.synkra.kubernetes.*`; `FronterasModulosTest` está en `io.synkra` y no entra en él.

### Script del init container

```
$ init-descarga/test.sh; echo "salida=$?"
ok hash-correcto
ok hash-alterado
ok catalogo-caido
ok http-500
ok http-404-sin-reintento
ok nombre-malo(../evil.jar)
ok nombre-malo(/abs.jar)
ok nombre-malo(a/b.jar)
ok nombre-malo(..)
ok hash-mal-formado
ok linea-mala-tras-buena
ok segundo-alterado
ok dos-correctos
ok lista-vacia
ok sin-url
ok sin-lista
ok espera-creciente
ok espera-base-invalida(0.5)
ok espera-base-invalida(-1)
ok espera-base-invalida(abc)
ok linea-vacia-en-medio
ok enlace-en-temporal-antiguo
ok enlace-en-destino
ok enlace-a-archivo-en-destino
ok plugins-es-enlace
ok archivo-previo-se-reemplaza
ok set-eu-presente
todos los casos ok
salida=0
```

(`evidencia/init-descarga-test.txt`, con fecha y sha.)

### Imagen del init

```
$ docker build -t synkra-init-descarga:1.0.0 init-descarga/
$ docker inspect -f '{{.Config.User}} {{json .Config.Entrypoint}}' synkra-init-descarga:1.0.0
10001:10001 ["/usr/local/bin/descargar.sh"]
```

Usuario numérico no root (10001:10001) y entrypoint en la ruta que invoca el Deployment (C-72). Salida completa en `evidencia/imagen-init.txt`.

## Trazabilidad

| Historia | Tareas que la cubren |
|---|---|
| H-15 (dueña) | U05-T03 (objetos del país, init container y ConfigMap), U05-T07 (script de descarga), U05-T08 (imagen), U05-T10 (este resumen) |
| H-12 (contribuye) | U05-T01 (cliente con la cuenta del pod), U05-T03 (`construir`), U05-T04 (`aplicar` con server-side apply, field manager `synkra`), U05-T09 (fronteras del módulo) |
| H-13 (contribuye, segunda capa D13) | U05-T02 (guardia de namespace); U05-T09 (ningún acceso a la API fuera del adaptador) |
| H-14 (contribuye) | U05-T05 (`estado`) |
| H-16 (contribuye) | U05-T05 (`contextoFallo`) |
| H-19 (contribuye) | U05-T06 (`leerPais`) |
| H-28 (contribuye) | U05-T08 (imagen no root) |

## Tareas

| Tarea | Qué construyó | PR | Rondas de revisión |
|---|---|---|---|
| U05-T01 | Cliente Fabric8 con la cuenta del pod | #70 | 2 |
| U05-T02 | Guardia de namespace (`synkra-<país>`), cero peticiones si se rechaza | #71 | 2 |
| U05-T03 | `construir`: Deployment, ConfigMap y Service | #76 | 3 |
| U05-T04 | `aplicar` con server-side apply | #78 | 2 |
| U05-T05 | `estado` y `contextoFallo` | #81 | 3 |
| U05-T06 | `leerPais` y formato de `plugins-activos.yaml` (C-81) | #83 | 3 |
| U05-T07 | `descargar.sh` y `test.sh` | #85 (abierto, apilado) | 2 |
| U05-T08 | Imagen del init container | #87 (abierto, apilado) | 1 |
| U05-T09 | Fronteras del módulo; cliente deja de ser bean; `ObjetosPais` sin Fabric8 | #90 (abierto, apilado) | 2 |
| U05-T10 | Este resumen | este PR (U05-T10) | (pendiente) |

Las rondas son los informes `revisiones/U05-T<NN>/ronda-N.md` (sin contar `ronda-N-respuesta.md`), contados con `ls`; todas terminaron en VERDE. Los PR salen de `gh pr list`; el PR de T03 es #76 y los de T01 y T02 son #70 y #71. Los PR #68, #73, #74 y #77 son de la rama del orquestador (`orq/U05-U02-tareas`), no de tareas.

## Candidatas abiertas que nombran U05, U07 o U08

Tomadas de `CANDIDATAS.md` (hasta C-102). Ninguna está marcada como resuelta allí; donde T09 tocó una, se anota.

- **Para U07 (despliegue):** C-35 (longitud de `Descriptor.NOMBRE` frente a los 63 de Kubernetes), C-40 (coerción numérica de `ConfiguracionJson`), C-46, C-49, C-51 y C-52 (extracción del init container: enlaces, `a/`, autorreferencias, Unicode; el script de T07 escribe archivos regulares y rechaza nombres con `/`), C-62 (N+1 de `Catalogo`), C-76 (reintentos implícitos del cliente Fabric8 frente a los de U07), C-82 y C-83 (`estado` da `LISTO` antes de mirar los pods nuevos; `contextoFallo` toma un solo pod y pierde eventos del ReplicaSet y el Deployment), C-87 (colisión de nombres de JAR en `construir`), C-90 (`leerPais` más laxo que el core).
- **Para U08:** C-17 (UID arbitrario y `fsGroup`), C-75 (server-side apply sin probar contra un servidor real: `kind-synkra`, nunca desde el loop), C-95 (`shellcheck` en la CI), C-97 (`emptyDir` de `/plugins` escribible para el UID 10001), C-98 y C-101 (digest y versiones fijas en las imágenes).
- **Pulido de U05 (sin tarea asignada hoy):** C-93 (pulido de `test.sh`), C-94 (`/plugins` de solo lectura en el core), C-102 (excepción concreta en `alCerrarElContextoElClienteQuedaCerrado`) y lo que dejó abierto T09 de C-73 (la prueba del defecto de la imagen no carga `application.properties`; unificar las reglas de imagen de `RegistroPermitido` y `exigirImagenInit`). C-68, C-70 y C-79 las recogió U05-T09 según su bitácora; C-64 lo cubre el caso `hash-alterado` de T07.
- **Compartidas con U02:** C-88 y C-77 (lector YAML seguro compartido en `verificador/`) y C-78 (versión de `snakeyaml-engine` repetida en tres `build.gradle.kts`).
