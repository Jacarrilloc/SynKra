# Plan de Application Design: SynKra

**Entradas:** `requirements.md`, `stories.md`, `personas.md` y `execution-plan.md` (aprobados).
**Salidas:** en `aidlc-docs/inception/application-design/`: `components.md`,
`component-methods.md`, `services.md`, `component-dependency.md` y `application-design.md`.
Las reglas de negocio en detalle (rangos, estados, esquema de datos) quedan para Functional
Design, por unidad.

## Pasos

- [x] 1. Identificar los componentes a partir de los módulos M1 a M8 del PRD y de las épicas E1 a E8, con la frontera dentro/fuera del clúster de cada uno
- [x] 2. Generar `components.md`: propósito, responsabilidades, interfaz y dónde corre cada componente
- [x] 3. Generar `component-methods.md`: firmas y entradas/salidas, trazadas a historias
- [x] 4. Generar `services.md`: servicios de orquestación (preparar plan, confirmar, vigilar, revertir) y sus interacciones
- [x] 5. Generar `component-dependency.md`: matriz de dependencias, comunicación y flujo de datos; verificar que no hay ciclos
- [x] 6. Registrar cada decisión estructural con su alternativa descartada
- [x] 7. Generar `application-design.md` consolidado
- [x] 8. Validar completitud (toda historia tocada por algún componente) y cumplimiento de extensiones

---

## Preguntas

Igual que antes: letra después de `[Answer]:`, o "aprobado" en el chat para aceptar las
recomendaciones.

## Question 1
¿Qué forma tiene el backend de SynKra?

A) Un monolito modular en Spring Boot: un solo desplegable con módulos internos (catálogo, verificación, despliegue, registro, API, seguridad) y fronteras por paquete

B) Microservicios: catálogo, despliegue y registro como servicios separados

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** SQLite exige una sola réplica con un solo escritor (RNF-07), y separar en
servicios multiplicaría despliegues sin necesidad. Las unidades se recortan por módulo, no por
proceso.

[Answer]: A

## Question 2
¿Cómo se organiza el repositorio?

A) Monorepo: Maven multi-módulo para lo Java (`verificador/`, `plugin-api/`, `core/`, `plugins/kyc-co/`, `plugins/kyc-pe/`, `backend/`), el front en `front/` con su propio `dotnet`, el init container en `init-descarga/` y los manifiestos en `deploy/`

B) Igual, pero con Gradle

C) Un repositorio por pieza

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Un solo `mvn verify` construye la librería compartida y todo lo que la
usa, que es lo que sostiene P1. Maven es lo más conocido en Spring.

[Answer]: B. Gradle multi-proyecto (Julian: "cambia maven por gradle").

## Question 3
¿Cómo carga el core sus plugins?

A) Un módulo `plugin-api` con las interfaces (el contrato y los puntos de extensión); cada plugin la implementa y se descubre con `ServiceLoader` dentro de su propio `URLClassLoader`. Sin librerías extra

B) Con PF4J (framework de plugins para Java)

C) Un contexto de Spring por plugin

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Cumple RF-26 (un cargador por plugin) con lo que trae el JDK y es fácil de
explicar en la sustentación. PF4J sirve, pero es una dependencia más.

[Answer]: A

## Question 4
¿Cómo sabe el core su versión y qué plugins debe activar?

A) La versión va compilada en el core (propiedad de build); la lista de plugins activos llega en un archivo montado desde el ConfigMap (`/config/plugins-activos.yaml`)

B) Las dos por variables de entorno del Deployment

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** La versión del core no debe poder cambiarse desde fuera (sería mentirle al
verificador), y un archivo montado es lo que la vista general lee del clúster (RF-20).

[Answer]: A

## Question 5
¿Cómo se implementa el init container que descarga los plugins?

A) Una imagen mínima con `curl` y `sha256sum` y un script: lee la lista de plugins y hashes del ConfigMap, descarga cada uno, verifica el hash y escribe solo en `/plugins`

B) Un programa Java pequeño que reutiliza la librería de verificación

C) La imagen del backend con otro punto de entrada

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Su único trabajo es descargar y comprobar el hash; la compatibilidad la
verifica el core al arrancar. Imagen pequeña, no root y fácil de leer.

[Answer]: A

## Question 6
¿Dónde se guardan los artefactos y sus metadatos?

A) Los JAR como archivos en el PVC (`/data/artefactos/<core>/<plugin>/<versión>.jar`) y los metadatos en SQLite (`/data/synkra.db`), en el mismo volumen

B) Los JAR como BLOB dentro de SQLite

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Es lo que dice el PRD ("en el mismo volumen") y mantiene la base pequeña.

[Answer]: A

## Question 7
¿Cómo accede el backend a SQLite?

A) Spring Data JDBC con el driver `sqlite-jdbc` y migraciones con Flyway

B) JPA/Hibernate con el dialecto comunitario de SQLite

C) `JdbcTemplate` a mano

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Pocas tablas y consultas simples; Spring Data JDBC evita los problemas de
Hibernate con SQLite, y Flyway deja el esquema versionado.

[Answer]: A

## Question 8
¿Cómo vigila el backend un despliegue confirmado hasta que queda `listo` o `fallido`?

A) De forma asíncrona: la confirmación responde 202 y una tarea del backend consulta el estado del Deployment con Fabric8 cada pocos segundos hasta el plazo de 3 minutos; si falla, recoge el log del core y los eventos del pod para el contexto

B) De forma síncrona: la petición de confirmación espera hasta el final

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Una petición de 3 minutos se corta en el navegador y en un proxy, y el
front ya necesita consultar el estado (H-14).

[Answer]: A

## Question 9
¿Qué modelo de Blazor usa el front?

A) Blazor Server: el servidor .NET llama a la API con las credenciales del usuario; sin CORS y sin código en el navegador que hable con la API

B) Blazor WebAssembly: el navegador llama a la API directamente

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Es la forma más simple de "solo consume la API" (P5), y no obliga a abrir la
API al navegador.

[Answer]: A

## Question 10
¿Cómo descarga el init container los artefactos si la API tiene autenticación (Should)?

A) Un endpoint de solo lectura por hash (`GET /api/artefactos/{sha256}`) sin credenciales, alcanzable solo desde los namespaces `synkra-*` (NetworkPolicy, Should). Quien no conoce el hash no puede pedir nada

B) Un token de solo lectura en un Secret de cada namespace de país

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** No reparte credenciales por los namespaces de los países, y el hash es a
la vez la llave de la descarga y su comprobación de integridad.

[Answer]: A
