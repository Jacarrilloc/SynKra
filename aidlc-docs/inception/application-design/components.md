# Componentes: SynKra

**Decisiones de base** (`application-design-plan.md`): backend como monolito modular en Spring
Boot (Q1); monorepo con **Gradle multi-proyecto** para lo Java, `front/` en .NET, `init-descarga/`
y `deploy/` (Q2); plugins con `plugin-api` + `ServiceLoader` y un `URLClassLoader` por plugin
(Q3); versión del core compilada y plugins activos en un archivo montado del ConfigMap (Q4);
init container con `curl` y `sha256sum` (Q5); JAR en el PVC y metadatos en SQLite (Q6); Spring
Data JDBC con Flyway (Q7); vigilancia asíncrona (Q8); Blazor Server (Q9); descarga por hash sin
credenciales (Q10).

## Mapa

| ID | Componente | Módulo PRD | Proyecto en el repo | Dónde corre | Estado |
|---|---|---|---|---|---|
| C01 | Verificador (librería) | M2 | `verificador/` (Gradle, con `testFixtures`) | Dentro del backend y dentro del core | Sin estado |
| C02 | Contrato de plugins | M7 | `plugin-api/` (Gradle) | Dentro del core y de cada plugin | Sin estado |
| C03 | Core (runtime) | M7 | `core/` (Gradle, Spring Boot) | Clúster, `synkra-<país>` | Sin estado |
| C04 | Plugins de país | M8 | `plugins/kyc-co/`, `plugins/kyc-pe/` (Gradle) | Dentro del core | Sin estado |
| C05 | Init de descarga | M3 (§9.3) | `init-descarga/` (Dockerfile + script) | Clúster, `synkra-<país>`, init container | Escribe en `/plugins` (emptyDir) |
| C06 | Catálogo | M1 | `backend/` paquete `catalogo` | Clúster, `synkra` | SQLite + JAR en el PVC |
| C07 | Despliegue | M3 | `backend/` paquete `despliegue` | Clúster, `synkra` | Planes en SQLite |
| C08 | Registro | M4 | `backend/` paquete `registro` | Clúster, `synkra` | Registro en SQLite |
| C09 | Adaptador de Kubernetes | M3, M4 | `backend/` paquete `kubernetes` | Clúster, `synkra` | Sin estado (lee y escribe en el clúster) |
| C10 | API REST | M5 | `backend/` paquete `api` | Clúster, `synkra` | Sin estado |
| C11 | Seguridad (Should) | §9.2 | `backend/` paquete `seguridad` | Clúster, `synkra` | Usuarios en SQLite |
| C12 | Front | M6 | `front/` (.NET, Blazor Server) | Clúster, `synkra` | Sin estado |
| C13 | Instalación y límite | §7.2, P2 | `deploy/`, `Makefile`, `scripts/` | Fuera del clúster (comandos) | Crea recursos en el clúster |

**Externos al clúster:** el registro de imágenes (en el demo, un registro local junto a `kind`),
el navegador y la CLI futura (PRD §9.6).

---

## C01 · Verificador
- **Propósito:** la única regla de compatibilidad del sistema (P1, RNF-08).
- **Responsabilidades:** leer y validar `synkra-plugin.yaml`; interpretar rangos semver (`2.x`,
  `>=2.1.0 <3.0.0`); dar un veredicto por plugin y global; proponer alternativas a partir de una
  lista de versiones disponibles. Expone en `testFixtures` la **suite de combinaciones** del PRD
  §11.1, que reutilizan el backend y el core.
- **Interfaz:** API Java pura (`Verificador`, `LectorDescriptor`, `RangoVersion`).
- **No hace:** no accede a la red, a la base de datos, a Spring ni a Kubernetes; no sabe de países.
- **Historias:** H-03, H-07, H-08, H-10, H-21.

## C02 · Contrato de plugins
- **Propósito:** lo que un plugin debe implementar para que el core lo cargue.
- **Responsabilidades:** interfaz `SynkraPlugin` (nombre, iniciar, detener) y puntos de extensión
  (`PuntoKyc`); sin lógica.
- **No hace:** no conoce versiones ni descriptores (eso es de C01).
- **Historias:** H-21, H-23.

## C03 · Core
- **Propósito:** el micro-kernel que se despliega en cada país.
- **Responsabilidades:** al arrancar, leer `/plugins` y `/config/plugins-activos.yaml`, verificar
  cada plugin activo con C01 contra su versión compilada, cargar cada uno en su propio
  `URLClassLoader`, iniciar y exponer los puntos de extensión; no quedar listo si algo falla;
  exponer `GET /plugins` y la salud.
- **No hace:** no tiene lógica de país (P3); no descarga plugins (C05); no habla con el backend.
- **Historias:** H-21, H-22.

## C04 · Plugins de país
- **Propósito:** la variación por país.
- **Responsabilidades:** `kyc-co 1.0.0` y `kyc-pe 1.3.0` (rango `2.x`) y `kyc-pe 2.0.0` (rango
  `3.x`, para CU4), con reglas simuladas y su `synkra-plugin.yaml`.
- **No hace:** no conoce al core más allá de C02.
- **Historias:** H-23.

## C05 · Init de descarga
- **Propósito:** dejar en `/plugins` exactamente los plugins elegidos (§9.3).
- **Responsabilidades:** leer la lista de artefactos y hashes del ConfigMap; descargar cada uno
  por hash desde C06 (`GET /api/artefactos/{sha256}`); comprobar el hash; reintentar 5 veces con
  espera creciente; escribir solo en `/plugins`.
- **No hace:** no verifica compatibilidad (lo hace C03 al arrancar).
- **Historias:** H-15.

## C06 · Catálogo
- **Propósito:** los compilados disponibles, agrupados por core (P4, S2).
- **Responsabilidades:** registrar cores y sus versiones; recibir JAR, rechazar los peligrosos
  (path traversal, tamaño), leer el descriptor con C01, calcular el SHA-256, guardar el JAR en el
  PVC y los metadatos en SQLite; listar y filtrar por etiqueta; servir artefactos por hash.
- **No hace:** no decide compatibilidad de una combinación (C01 vía C07).
- **Historias:** H-01 a H-06, H-31.

## C07 · Despliegue
- **Propósito:** de una combinación elegida a un país corriendo, con confirmación (P2).
- **Responsabilidades:** preparar planes (`pendiente`) con resumen y cambios; validar país y
  namespace (S1, S4); confirmar y aplicar con C09; vigilar hasta `listo` o `fallido` sin
  reintentos; recoger el contexto del fallo; preparar reversiones.
- **No hace:** no guarda el registro (lo escribe en C08); no revierte solo.
- **Historias:** H-06, H-09, H-11 a H-17, H-20.

## C08 · Registro
- **Propósito:** la traza de cada despliegue confirmado (P7) y la vista general.
- **Responsabilidades:** guardar y consultar despliegues confirmados con su resultado y contexto;
  historial por país; vista general leída del clúster con C09 y la marca de concordancia.
- **No hace:** no crea despliegues ni reversiones (C07).
- **Historias:** H-16, H-18, H-19.

## C09 · Adaptador de Kubernetes
- **Propósito:** el único punto del backend que habla con el clúster, con la cuenta
  `synkra:deployer`.
- **Responsabilidades:** construir los objetos del país (Deployment con el init container,
  ConfigMap, Service) y aplicarlos con *server-side apply* en `synkra-<país>`; leer estado,
  logs y eventos; leer lo que corre en cada `synkra-*`. Rechaza por código cualquier namespace
  fuera de `synkra-*` (segunda línea tras C07; la tercera es el RBAC).
- **No hace:** no decide nada; no crea namespaces (S1).
- **Historias:** H-12, H-13, H-14, H-16, H-19.

## C10 · API REST
- **Propósito:** toda la funcionalidad de C06 a C08 por HTTP (P5).
- **Responsabilidades:** controladores, formato de error uniforme, OpenAPI (Should).
- **No hace:** no tiene reglas de negocio.
- **Historias:** H-24, H-29.

## C11 · Seguridad (Should)
- **Propósito:** los 4 roles del PRD §9.2.
- **Responsabilidades:** usuarios con rol en SQLite; HTTP Basic; reglas por ruta; el endpoint
  de artefactos por hash queda sin credenciales (Q10).
- **Historias:** H-30.

## C12 · Front
- **Propósito:** la demo visual (P5: solo consume la API).
- **Responsabilidades:** las 4 pantallas del PRD §9.4 en Blazor Server, llamando a C10 con las
  credenciales del usuario.
- **No hace:** ninguna regla de versión ni de compatibilidad.
- **Historias:** H-25.

## C13 · Instalación y límite
- **Propósito:** instalar SynKra en `kind` con un comando y hacer verificable P2.
- **Responsabilidades:** manifiestos de `synkra` (backend, front, PVC, Service, Ingress o
  port-forward), namespaces de los países, `ServiceAccount` `synkra:deployer` con `Role` y
  `RoleBinding` por país, imágenes no root, registro local; `make install`; script de punta a
  punta `scripts/e2e-api.sh`. Should: NetworkPolicy, Pod Security, `compose.yaml`, Trivy, GitOps.
- **Historias:** H-24 (script), H-26, H-27, H-28, H-32, H-33, H-34.
