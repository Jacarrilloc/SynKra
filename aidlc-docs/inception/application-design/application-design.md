# Application Design: SynKra (consolidado)

Consolida `components.md`, `component-methods.md`, `services.md` y `component-dependency.md`.
Respuestas de diseño en `../plans/application-design-plan.md`.

## 1. Forma del sistema

- **Backend:** **monolito modular** (decisiones D1 y D14): un solo desplegable Spring Boot (Java 21) en `synkra`, con seis módulos internos de fronteras comprobadas:
  catálogo (C06), despliegue (C07), registro (C08), adaptador de Kubernetes (C09), API (C10) y
  seguridad (C11, Should). Una réplica, estrategia `Recreate`.
- **Librerías:** verificador (C01), compartido por el backend y el core, y contrato de plugins (C02).
- **Producto desplegado:** core (C03) con los plugins de país (C04) en `synkra-<país>`, con el
  init container de descarga (C05).
- **Front:** Blazor Server (C12) en `synkra`, que solo llama a la API.
- **Instalación:** `deploy/` y `make install` (C13), que crean también los namespaces de los países
  y la cuenta `synkra:deployer`.

## 2. Frontera dentro y fuera del clúster

| Dentro del clúster | Fuera del clúster |
|---|---|
| `synkra`: backend (C06 a C11), front (C12), PVC con SQLite y artefactos | Registro de imágenes (local junto a `kind`) |
| `synkra-<país>`: core (C03) con plugins (C04) e init container (C05) | Navegador, CLI futura |
| | Comandos de instalación (C13) desde la máquina del operador |

## 3. Persistencia

| Dato | Dónde | Dueño |
|---|---|---|
| Cores, versiones, plugins, etiquetas, hash | SQLite `/data/synkra.db` | C06 |
| JAR de plugins | PVC `/data/artefactos/<core>/<plugin>/<versión>.jar` | C06 |
| Planes | SQLite | C07 |
| Despliegues, resultado y contexto | SQLite | C08 |
| Usuarios y roles (Should) | SQLite | C11 |
| Plugins en el pod | `emptyDir` `/plugins`, se rehace en cada arranque | C05 |
| Plugins activos y hashes del país | ConfigMap en `synkra-<país>` | C09 lo escribe; C03 y C05 lo leen |

Esquema con Flyway; el detalle de tablas va en Functional Design.

## 4. Decisiones estructurales y alternativas descartadas

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| D1 | Monolito modular | Microservicios | SQLite admite un solo escritor; más servicios = más despliegues sin beneficio en el demo |
| D2 | Monorepo con **Gradle** multi-proyecto | Maven (recomendado al principio), un repo por pieza | Pedido de Julian; un solo `./gradlew check` construye C01 y todo lo que lo usa (P1) |
| D3 | `ServiceLoader` + `URLClassLoader` por plugin | PF4J, contexto de Spring por plugin | Cumple RF-26 con el JDK, sin dependencias |
| D4 | Versión del core compilada; activos en archivo montado | Variables de entorno | La versión no se puede falsear desde fuera; el archivo es lo que lee la vista general |
| D5 | Init container con `curl` y `sha256sum` | Programa Java, imagen del backend | Solo descarga y comprueba el hash; la compatibilidad la verifica el core |
| D6 | JAR en el PVC, metadatos en SQLite | BLOB en SQLite | Lo que dice el PRD; base pequeña |
| D7 | Spring Data JDBC + Flyway | JPA/Hibernate, `JdbcTemplate` | Pocas tablas; evita el dialecto comunitario de Hibernate |
| D8 | Vigilancia asíncrona (202 + consulta) | Petición síncrona de 3 min | Se corta en navegador y proxy |
| D9 | Blazor Server | Blazor WebAssembly | Sin CORS ni API expuesta al navegador |
| D10 | Descarga por hash sin credenciales, solo desde `synkra-*` | Token en un Secret por país | No reparte credenciales; el hash es llave y comprobación |
| D11 | Adaptador de Kubernetes propio (C09) | Que C07 y C08 usen Fabric8 cada uno | Rompe el ciclo C07 ↔ C08 y deja un solo punto que habla con el clúster |
| D12 | Reversión como servicio de C07 | Reversión en C08 | Rompe el ciclo C07 ↔ C08; la reversión es un despliegue más |
| D14 | **Monolito modular con fronteras comprobadas**: cada módulo del backend (catálogo, despliegue, registro, adaptador de Kubernetes, seguridad) expone solo su interfaz pública, tiene sus propios controladores y tablas, y una prueba con Spring Modulith (`ApplicationModules.of(SynkraApp.class).verify()`) falla si un módulo usa las clases internas de otro o si aparece un ciclo | Fronteras solo por convención de paquetes | Pedido de Julian ("realiza el proyecto con el patrón de monolito modular"): sin una comprobación, un monolito modular se degrada a un monolito sin más. La prueba es el criterio por comando (AUTONOMIA-02) |
| D13 | Tres capas para el namespace: C07 valida el país, C09 rechaza lo que no sea `synkra-*`, RBAC | Solo RBAC | Defensa en profundidad (escenario "escalar el despliegue") |

## 5. Dependencias

Grafo y matriz en `component-dependency.md`: 13 componentes, 19 dependencias, **sin ciclos**
(comprobado con `scripts/verificar-grafo.py`). C01, C02, C09 y C11 no dependen de nada.

## 6. Cobertura de historias

| Historias | Componentes |
|---|---|
| H-01 a H-06, H-31 | C06 (+ C01) |
| H-07, H-08, H-10 | C01 |
| H-09, H-11 a H-17, H-20 | C07 (+ C09, C08) |
| H-16, H-18, H-19 | C08 (+ C09) |
| H-21, H-22 | C03 (+ C01, C02) |
| H-23 | C04 |
| H-15 | C05 |
| H-24, H-29 | C10 (+ C13 para el script) |
| H-30 | C11 |
| H-25 | C12 |
| H-26 a H-28, H-32 a H-34 | C13 |

Las 34 historias tienen al menos un componente.

## 7. Cumplimiento de extensiones

| Regla | Estado | Motivo |
|---|---|---|
| AUTONOMIA-01 | Cumple | Solo S4 aplica cambios, y solo tras la confirmación; S5 no reintenta ni revierte; ningún componente crea namespaces; tres capas de namespace (D13) |
| AUTONOMIA-02 | Cumple | Cada historia conserva su comando; los métodos de C03, C09 y C10 son los que esos comandos consultan |
| PBT parcial | Preparado | C01 expone ida y vuelta del descriptor (PBT-02) y la propiedad de inclusión de rangos (PBT-03); se detallan en Functional Design |
