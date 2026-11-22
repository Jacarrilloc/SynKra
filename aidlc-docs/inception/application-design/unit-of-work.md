# Unidades de trabajo: SynKra

**Plan:** `../plans/unit-of-work-plan.md` (respuestas 1A a 7A). **Diseño:** `application-design.md`
(monolito modular con fronteras comprobadas, D1 y D14).

## Panorama

SynKra se descompone en **11 unidades**: 9 Must, que juntas dan la demo de punta a punta, y 2
Should, al final, que se pueden cortar enteras (R9). Cada módulo del backend es su propia unidad
y trae sus rutas REST (corte vertical, Q5): ninguna unidad es "el backend".

| # | Unidad | Componentes | Prio. | Se entrega como |
|---|---|---|---|---|
| U01 | Verificador y suite | C01 | Must | Librería Gradle `verificador` (+ `testFixtures`) |
| U02 | Core, contrato y plugins de país | C02, C03, C04 | Must | Librerías `plugin-api`, `kyc-co`, `kyc-pe`; imagen del core |
| U03 | Plataforma del backend | (base de C06 a C11) | Must | Aplicación Spring Boot vacía con SQLite, Flyway, errores y prueba de fronteras |
| U04 | Catálogo | C06 | Must | Módulo `catalogo` del backend |
| U05 | Pod del país | C09, C05 | Must | Módulo `kubernetes` del backend; imagen `init-descarga` |
| U06 | Registro y vista general | C08 | Must | Módulo `registro` del backend |
| U07 | Despliegue y reversión | C07 | Must | Módulo `despliegue` del backend |
| U08 | Instalación y límite | C13 (Must) | Must | `deploy/`, `make install`, `scripts/e2e-api.sh` |
| U09 | Front | C12 | Must | Imagen del front y su manifiesto |
| U10 | Seguridad y roles | C11 | Should | Módulo `seguridad` del backend |
| U11 | Endurecimiento y entrega | C13 (Should) | Should | NetworkPolicy, Pod Security, `compose.yaml`, Trivy, GitOps |

**Quién construye:** Julian con el loop de agentes (Q3). Cada unidad la puede llevar un
codificador solo, y cada tarea es un despacho: un cambio revisable con un comando.

**Comunicación entre módulos del backend (Q2):** llamadas directas a la interfaz pública de cada
módulo (su paquete `api`); lo demás es interno y la prueba de fronteras de U03 lo comprueba. Sin
eventos.

---

## Definición de cada unidad

### U01 · Verificador y suite
- **Ruta:** `verificador/`
- **Responsabilidad:** la regla de compatibilidad (rangos semver, veredicto por plugin y global,
  core equivocado, alternativas) y la lectura y validación de `synkra-plugin.yaml`. Publica en
  `testFixtures` la suite del PRD §11.1 para que U02 y U07 la reutilicen.
- **Qué NO hace:** no lee el catálogo (recibe una interfaz `Catalogo` de solo lectura); no conoce
  Spring, Kubernetes, la red ni los países.
- **Se prueba sola:** sí, pruebas unitarias y de propiedades (PBT parcial).
- **Terminada cuando:** `./gradlew :verificador:check` pasa con la suite completa, y
  `./gradlew :verificador:dependencies --configuration runtimeClasspath` no lista Spring ni Fabric8.

### U02 · Core, contrato y plugins de país
- **Rutas:** `plugin-api/`, `core/`, `plugins/kyc-co/`, `plugins/kyc-pe/`
- **Responsabilidad:** el contrato `SynkraPlugin` y `PuntoKyc`; el core que al arrancar verifica
  con U01 los plugins de `/plugins` contra su versión compilada, carga cada uno en su propio
  cargador y no queda listo si algo falla; `kyc-co 1.0.0`, `kyc-pe 1.3.0` (rango `2.x`) y
  `kyc-pe 2.0.0` (rango `3.x`); el *fixture* de un plugin que falla al iniciar.
- **Qué NO hace:** no descarga plugins (U05); no habla con el backend; no tiene lógica de país.
- **Se prueba sola:** sí, con pruebas de integración que arman un `/plugins` temporal.
- **Terminada cuando:** `./gradlew :core:check :plugins:kyc-co:check :plugins:kyc-pe:check` pasa
  (incluida la suite de U01 aplicada al arranque) y `grep -rniE "colombia|peru" core/src` no
  devuelve nada.

### U03 · Plataforma del backend
- **Ruta:** `backend/` (paquete `plataforma`)
- **Responsabilidad:** la aplicación Spring Boot vacía: configuración de SQLite en `/data` con
  Flyway, formato de error único, salud, la **prueba de fronteras de Spring Modulith** (D14) y el
  Dockerfile multi-stage no root del backend. OpenAPI como tarea Should final.
- **Qué NO hace:** ninguna funcionalidad de negocio; no tiene tablas propias.
- **Se prueba sola:** sí, arranca y responde salud sin ningún módulo.
- **Terminada cuando:** `./gradlew :backend:check` pasa (incluida `ApplicationModules.verify()`) y
  `curl localhost:8080/actuator/health` responde `UP` con la base creada en `/data/synkra.db`.

### U04 · Catálogo
- **Ruta:** `backend/` paquete `catalogo`
- **Responsabilidad:** registrar cores y versiones; subir plugins (lectura del descriptor con U01,
  SHA-256, path traversal, límite de tamaño); carpeta por core; filtrar por etiqueta; servir
  artefactos por hash; sus tablas y sus rutas REST. Should final: registro de imágenes permitido.
- **Qué NO hace:** no verifica combinaciones para desplegar (U07); no toca el clúster.
- **Se prueba sola:** sí, con `@SpringBootTest` y `curl` sobre el backend local.
- **Terminada cuando:** `./gradlew :backend:test --tests '*catalogo*'` pasa y los criterios de H-01
  a H-06 se cumplen con `curl` contra el backend local.

### U05 · Pod del país
- **Rutas:** `backend/` paquete `kubernetes`; `init-descarga/`
- **Responsabilidad:** construir los objetos del país (Deployment con el init container, ConfigMap
  de activos y hashes, Service) y aplicarlos con Fabric8 en `synkra-<país>`; rechazar por código
  todo namespace fuera de `synkra-*`; leer estado, log del core, eventos y lo que corre en un país.
  La imagen del init container y su script de descarga con comprobación de hash y 5 reintentos.
- **Qué NO hace:** no decide cuándo desplegar ni registra nada; no crea namespaces (S1); no
  verifica compatibilidad.
- **Se prueba sola:** sí. Las pruebas del adaptador usan el servidor simulado de Fabric8; el
  script se prueba contra un servidor HTTP local con un artefacto bueno y uno alterado.
- **Terminada cuando:** `./gradlew :backend:test --tests '*kubernetes*'` pasa y
  `init-descarga/test.sh` pasa los casos de hash correcto, hash alterado y catálogo caído.

### U06 · Registro y vista general
- **Ruta:** `backend/` paquete `registro`
- **Responsabilidad:** guardar cada despliegue confirmado con su resultado y contexto; historial
  por país; vista general leída del clúster con U05 y la marca de concordancia; sus tablas y rutas.
- **Qué NO hace:** no crea despliegues ni reversiones (U07).
- **Se prueba sola:** sí, con U05 sustituido por un doble de prueba.
- **Terminada cuando:** `./gradlew :backend:test --tests '*registro*'` pasa, incluida la prueba de
  que el registro sobrevive al reinicio de la aplicación con el mismo archivo SQLite.

### U07 · Despliegue y reversión
- **Ruta:** `backend/` paquete `despliegue`
- **Responsabilidad:** verificación en vivo; plan `pendiente` con resumen y cambios; validación de
  país (S4); confirmación que aplica con U05 y registra con U06; vigilancia asíncrona de 3 minutos
  sin reintentos; contexto del fallo; reversión como plan nuevo; sus rutas.
- **Qué NO hace:** no reintenta ni revierte por su cuenta (P2); no habla con Kubernetes salvo a
  través de U05.
- **Se prueba sola:** sí, con U05 sobre el servidor simulado de Fabric8.
- **Terminada cuando:** `./gradlew :backend:test --tests '*despliegue*'` pasa, incluida la prueba
  de que un plan sin confirmar no llama nunca a U05.

### U08 · Instalación y límite
- **Rutas:** `deploy/`, `Makefile`, `scripts/e2e-api.sh`
- **Responsabilidad:** registro local junto a `kind`; namespaces `synkra`, `synkra-co` y
  `synkra-pe`; `ServiceAccount` `synkra:deployer` con `Role` y `RoleBinding` solo en cada país;
  PVC, Deployment (`Recreate`) y Service del backend; `make install` y `make verificar-limite`;
  comprobación de imágenes no root; el script de punta a punta de CU1 a CU5 con `curl`.
- **Qué NO hace:** no despliega productos (eso lo hace SynKra); el front lo añade U09.
- **Se prueba sola:** el RBAC sí (se aplica y se pregunta con `kubectl auth can-i` sin el backend);
  el script de punta a punta necesita U02 a U07.
- **Terminada cuando:** en un `kind` limpio, `make install && make verificar-limite &&
  ./scripts/e2e-api.sh` termina con código 0.

### U09 · Front
- **Ruta:** `front/` y su manifiesto en `deploy/`
- **Responsabilidad:** Blazor Server con las 4 pantallas del PRD §9.4, que solo llama a la API;
  imagen multi-stage no root; Deployment y Service en `synkra`.
- **Qué NO hace:** ninguna regla de versión ni de compatibilidad (P5).
- **Se prueba sola:** sí, contra la API local; `dotnet test` con un cliente HTTP simulado.
- **Terminada cuando:** `dotnet test front.Tests` pasa, `grep -rniE "semver|compatib" front/` solo
  devuelve llamadas a la API, y la pantalla "Desplegar" completa CU2 en el clúster de la demo.

### U10 · Seguridad y roles *(Should)*
- **Ruta:** `backend/` paquete `seguridad`
- **Responsabilidad:** usuarios con rol en SQLite (BCrypt), HTTP Basic, reglas por ruta de los 4
  roles; el endpoint de artefactos por hash queda sin credenciales (D10).
- **Qué NO hace:** no cambia las reglas de negocio de los demás módulos.
- **Terminada cuando:** `./gradlew :backend:test --tests '*seguridad*'` pasa con la matriz de roles
  de H-30 (401, 403 y 2xx esperados por ruta).

### U11 · Endurecimiento y entrega *(Should)*
- **Rutas:** `deploy/` (políticas), `compose.yaml`, CI y repositorio de GitOps
- **Responsabilidad:** NetworkPolicy *deny-by-default* con las excepciones necesarias (init →
  catálogo, front → backend), Pod Security `restricted`, límites de recursos; `compose.yaml` con
  las 6 prácticas del módulo 3; escaneo con Trivy; instalación de SynKra con GitOps (módulo 8).
- **Qué NO hace:** no cambia el código de las unidades Must.
- **Terminada cuando:** `make install` con las políticas sigue pasando `./scripts/e2e-api.sh`, y
  `trivy image --severity CRITICAL --exit-code 1` pasa en todas las imágenes.

---

## Organización del código (greenfield, Q6)

```text
SynKra/
├── settings.gradle.kts         ← incluye los proyectos Java
├── build.gradle.kts            ← convenciones comunes (Java 21)
├── verificador/                ← U01 (testFixtures: la suite)
├── plugin-api/                 ← U02
├── core/                       ← U02
├── plugins/
│   ├── kyc-co/                 ← U02
│   └── kyc-pe/                 ← U02
├── backend/                    ← U03, U04, U05, U06, U07, U10
│   └── src/main/java/…/synkra/
│       ├── plataforma/
│       ├── catalogo/
│       ├── kubernetes/
│       ├── registro/
│       ├── despliegue/
│       └── seguridad/
├── init-descarga/              ← U05 (Dockerfile, descargar.sh, test.sh)
├── front/                      ← U09 (solución .NET)
├── deploy/                     ← U08, U09, U11 (manifiestos)
├── scripts/                    ← existente (setup, doctor, verificar-grafo) + e2e-api.sh (U08)
├── Makefile                    ← existente + install, verificar-limite (U08)
└── aidlc-docs/                 ← solo documentación
```

## Cumplimiento de extensiones

| Regla | Estado | Motivo |
|---|---|---|
| AUTONOMIA-01 | Cumple | Solo U07 aplica cambios al clúster, y solo tras la confirmación; U05 no decide; U08 crea el límite (RBAC) y lo comprueba; las tareas de U08 y U11 se ejecutan contra el `kind` local |
| AUTONOMIA-02 | Cumple | Cada unidad tiene un "terminada cuando" con comando |
