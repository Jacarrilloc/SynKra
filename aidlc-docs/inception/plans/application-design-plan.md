# Plan de Application Design — SynKra

**Insumos:** `requirements.md`, `stories.md`, `execution-plan.md` (todos aprobados).
**Alcance:** componentes, interfaces, servicios y dependencias de alto nivel. La lógica de
negocio detallada va en Functional Design, por unidad. No se escribe código.

Responde escribiendo la letra después de cada `[Answer]:`. Si ninguna opción encaja, elige la
última (Other) y descríbela. Las preguntas cubren solo lo que los requisitos y las historias
dejan abierto.

---

## Parte 1: Preguntas

## Question 1
Los plugins deben compilarse sin depender del kernel completo, y el agente necesita leer el modelo
de manifiesto de los plugins. ¿Cómo se reparte el contrato?

A) Un módulo `plugin-api` aparte (interfaces de los tres puntos de extensión, el modelo del manifiesto y los tipos compartidos). El kernel, los plugins y el agente dependen de él. Los plugins nunca dependen de `core`

B) Los contratos viven dentro de `core/` y los plugins dependen de `core` como librería

C) Dos módulos: `plugin-api` con las interfaces y `plugin-manifest` con el modelo y esquema del manifiesto, para que el agente dependa solo del manifiesto

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 2
¿Qué paquete base de Java se usa? Recuerda que la verificación del PRD busca palabras de país en
`core/src` sin distinguir mayúsculas; el nombre no puede contener `co` ni `pe` como palabra suelta.

A) `io.synkra`

B) `org.synkra`

C) `com.synkra`

X) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 3
¿Cómo carga el kernel los JAR de plugin que hay en el directorio?

A) Un cargador de clases aislado por JAR (`URLClassLoader`) y `ServiceLoader` sobre cada uno, para que un plugin no vea las clases de otro

B) Un único cargador de clases con todos los JAR del directorio y `ServiceLoader` sobre él

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 4
¿Cómo sabe el kernel qué plugin atiende una petición KYC? (RF-06 exige 404 si no hay plugin para el país pedido.)

A) El país va en la ruta de la petición (`/kyc/{country}/...`) y un registro resuelve el plugin por país

B) El país va en una cabecera HTTP y un registro resuelve el plugin por país

C) El kernel arranca con un país activo por configuración y rechaza peticiones de otro país con 404

X) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 5
¿Qué arquitectura interna se usa en el agente?

A) Hexagonal: un núcleo de dominio (localizador, generador de informe, control de presupuesto) con puertos (`LlmProvider`, `GitHost`, `PluginInventory`, `ChangeStore`) y adaptadores intercambiables

B) Capas simples (CLI, servicios, clientes), sin puertos formales salvo `LlmProvider`

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 6
¿Con qué marco se construye la CLI del agente?

A) picocli sobre Spring Boot sin servidor web (comparte el ecosistema del kernel y facilita la configuración por variables)

B) picocli sobre Java plano, sin Spring (arranque más rápido, menos dependencias)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 7
¿Cómo habla el agente con Git y GitHub? Esto determina la lista blanca de herramientas del agente (principio P5).

A) JGit para ramas y commits y un cliente de la API REST de GitHub para el PR; solo estas operaciones están expuestas en el puerto `GitHost`

B) Ejecuta los comandos `git` y `gh` como procesos, con una lista blanca explícita de subcomandos permitidos (`checkout -b`, `add`, `commit`, `push` a `synkra/*`, `pr create --draft`)

C) JGit para git y `gh pr create --draft` como proceso para el PR

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 8
Con la localización determinista (decisión Q4-C), el modelo solo redacta. Entonces, ¿qué contiene exactamente la "edición del plugin" que hace `propose` en la rama `synkra/<id>` (RF-34)?

A) Un archivo de solicitud de cambio dentro del plugin indicado (por ejemplo `plugins/pe/changes/<id>.md`) con el texto del cambio, el punto de extensión afectado y los archivos citados como evidencia. No modifica código fuente

B) El archivo de A más un comentario `TODO(synkra:<id>)` insertado en la clase del punto de extensión afectado, apuntando al archivo de solicitud

C) El modelo redacta un parche de código sobre la clase del punto de extensión afectado, limitado a ese archivo. Ese borrador sí modifica código fuente (el modelo decide el texto del parche, nunca el archivo ni el destino)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 9
Las historias asumen un "paso de aprobación" antes de `propose`. ¿Cómo se materializa?

A) Un comando `synkra approve <id>` que marca el informe como aprobado (sello con fecha y usuario dentro del informe). `propose` falla si no está aprobado

B) Ejecutar `synkra propose <id>` ya es la aprobación (el PRD lo describe así): exige que el informe exista y pide confirmación interactiva `[y/N]`; `--yes` la omite

C) Igual que B, sin confirmación interactiva: basta con que el informe exista

X) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 10
La evaluación necesita leer los informes con un script Python y el humano necesita leerlos. ¿Qué formato tienen?

A) Markdown con una tabla de formato fijo (`| plugins/xx | veredicto | punto de extensión | motivo |`), que el script Python analiza

B) Markdown para el humano más un archivo JSON gemelo (`informes/<id>.json`) con el mismo contenido, que lee el script

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 11
¿Dónde se guarda el presupuesto diario de tokens y el log de tokens del agente?

A) En archivos locales del agente (un contador por día y `tokens.log`), en un directorio configurable; en Kubernetes, un volumen del Job

B) En un ConfigMap o Secret de Kubernetes que el agente actualiza (requiere permisos de escritura en el clúster)

C) Solo en memoria por ejecución; el log de tokens va a la salida estándar y no hay acumulado diario entre ejecuciones

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## Parte 2: Pasos de generación (tras responder y aprobar)

- [x] 1. Analizar las respuestas y resolver ambigüedades o contradicciones
- [x] 2. Crear `aidlc-docs/inception/application-design/components.md`: componentes, responsabilidades e interfaces (kernel, plugin-api, registro de plugins, API KYC, plugins `co` y `pe`, agente y sus puertos, adaptadores `claude` y `fake`, evaluación, despliegue)
- [x] 3. Crear `component-methods.md`: firmas de métodos, propósito y tipos de entrada y salida; las reglas detalladas quedan para Functional Design
- [x] 4. Crear `services.md`: servicios de orquestación (analizar un cambio, proponer un PR, control de presupuesto) y su coordinación
- [x] 5. Crear `component-dependency.md`: matriz de dependencias, patrones de comunicación y flujo de datos, con diagrama Mermaid validado y alternativa en texto
- [x] 6. Crear `application-design.md` que consolide los cuatro documentos
- [x] 7. Verificar que ningún componente da al agente permisos de `merge`, `push` a `main` ni `kubectl apply` (AUTONOMIA-01), que todo uso del modelo pasa por `LlmProvider` (AUTONOMIA-02) y que cada componente tiene una historia asociada
- [x] 8. Actualizar `aidlc-state.md` y registrar la aprobación en `audit.md`

---

## Preguntas de seguimiento (aclaraciones de las respuestas)

Revisé tus respuestas: Q1 B, Q2 C, Q3 B, Q4 C, Q5 B, Q6 B, Q7 B, Q8 A, Q9 C, Q10 A, Q11 A.
Encontré tres puntos que se contradicen entre sí o con lo ya aprobado. Resuélvelos antes de
generar el diseño.

### Seguimiento 1 — Q4-C contra US-2.5 y RF-06 (cómo llega el país en una petición)
Elegiste (Q4-C) que el kernel arranca con un país activo por configuración y rechaza con 404
las peticiones de otro país. Pero la historia US-2.5 prueba "llamar al mismo endpoint para
Colombia" sin decir cómo la petición nombra el país, y RF-06 pide 404 cuando "no hay plugin
para el país pedido". Con un país activo fijo, hay que decidir si la petición lleva el país.

## Question F1
¿Cómo se entiende el país en una petición KYC?

A) La ruta lleva el país (`/kyc/{country}/...`). El kernel compara con su país activo y responde 404 si no coincide. Las historias y RF-06 quedan como están

B) La petición no lleva país: el kernel responde siempre por su país activo, y el 404 por país distinto no existe. RF-06 y el segundo criterio de US-2.5 se reescriben a "si el plugin del país activo no está cargado, `/kyc/...` responde 503"

C) La petición lleva el país en un campo del cuerpo (JSON), y el kernel compara con su país activo y responde 404 si no coincide

X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Seguimiento 2 — Q1-B contra Q6-B (el agente y el modelo del manifiesto)
Con Q1-B, los contratos y el modelo del manifiesto viven en `core/`, que es una aplicación
Spring Boot. Con Q6-B, el agente es Java plano sin Spring. Si el agente depende de `core` para
leer el manifiesto, arrastra Spring Boot a una CLI que debía ser ligera.

## Question F2
¿Cómo lee el agente los manifiestos de los plugins?

A) El agente los lee como datos (YAML) con su propio modelo mínimo de lectura, sin depender de `core`. Un esquema de manifiesto único en el repositorio (`schemas/plugin-manifest.schema.json`) lo valida en ambos lados, y un test comprueba que el modelo del kernel y el del agente aceptan los mismos ejemplos

B) El agente depende del módulo `core` solo para el modelo del manifiesto (acepta la dependencia transitiva de Spring Boot)

C) Se extrae el modelo del manifiesto a un módulo pequeño `plugin-manifest` sin Spring (cambia Q1: los puntos de extensión siguen en `core/`, solo el manifiesto sale)

X) Other (please describe after [Answer]: tag below)

[Answer]: B

### Seguimiento 3 — Q9-C contra US-1.6 y el principio "aprobación humana en cada salida"
Elegiste (Q9-C) que `propose` solo exige que el informe exista, sin confirmación ni comando de
aprobación. Entonces ejecutar `propose` es la aprobación. La historia US-1.6 (ya aprobada) tiene
un criterio que dice "si el informe existe pero no corrí el paso de aprobación, `propose`
falla", y ese paso ya no existe. Además, sin confirmación, el riesgo R7 (fatiga de aprobación)
queda solo en manos de la revisión del PR.

## Question F3
¿Cómo se concilia?

A) Se mantiene Q9-C. Se elimina el segundo criterio de US-1.6 y se reescribe RF-35 como "`propose` exige que exista el informe". Se acepta que la aprobación humana de la salida es el PR (nadie fusiona sin revisión) y se documenta el riesgo R7

B) Se mantiene Q9-C y se añade una salvaguarda sin confirmación interactiva: `propose` falla si el informe tiene más de 24 horas o si el YAML del cambio se modificó después del análisis (comparando un hash guardado en el informe)

C) Se vuelve a Q9-A o Q9-B (comando `approve` o confirmación `[y/N]`), y las historias quedan como están

X) Other (please describe after [Answer]: tag below)

[Answer]: A
