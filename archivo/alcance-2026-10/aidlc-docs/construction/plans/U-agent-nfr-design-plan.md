# Plan de NFR Design — U-agent

> **OMITIDO por el usuario el 2026-10-17.** Las preguntas de este archivo quedaron sin responder.
> Sus primeras opciones se usan como decisiones por defecto N1 a N13 en
> `U-agent-code-generation-plan.md`, donde el usuario puede corregirlas.

**Insumos:** `U-agent/nfr-requirements/` (aprobado: 29 requisitos y pila), `U-agent/functional-design/`.
**Alcance:** patrones y componentes lógicos que hacen cumplir los NFR: fiabilidad, seguridad
del proceso, gasto, aislamiento del SDK y del classpath. No incluye infraestructura (eso es
Infrastructure Design).

**Decisiones que tomo yo (confirma o corrige al aprobar el diseño):**
1. **Registro de eventos.** Se usa el `System.Logger` del JDK, a la salida de error, con mensajes de formato fijo que solo llevan metadatos (NFR-AG-PV2). Sin biblioteca de logging.
2. **Mapeo de códigos de salida.** Tres tipos de excepción propios: error de entrada (`1`), fallo operativo (`1`) y abstención (`2`). Un único manejador de picocli las traduce a códigos y mensajes.
3. **Reloj inyectable.** El día del presupuesto y la marca del informe salen de un `Clock` inyectado (UTC), para poder probar el cambio de día.
4. **Escritura atómica.** El informe, el archivo de presupuesto y los archivos de solicitud se escriben a un archivo temporal y se renombran. `tokens.log` se abre en modo añadir.
5. **Presupuesto en dos tiempos.** Reservar antes de la llamada y registrar el gasto real después (BR-BU2, BR-BU5). Sin bloqueo de archivo (Q9-B de NFR).
6. **Valores sensibles.** El token de GitHub llega al proceso hijo solo por su entorno y nunca al entorno del proceso del agente.

Responde escribiendo la letra después de cada `[Answer]:`. Si ninguna opción encaja, elige la
última (Other) y descríbela.

---

## Parte 1: Preguntas

## Question 1
NFR-AG-PT2 exige que el classpath del agente no tenga Spring Boot web ni Tomcat, aunque dependa de `core` por el modelo del manifiesto. ¿Cómo se logra?

A) Exclusiones en el `pom.xml` del agente sobre su dependencia de `core` (se excluyen los artefactos de Spring). No se toca `core`

B) Enmendar el plan de `U-core`: declarar las dependencias de Spring como `optional` en `core`, de modo que ningún consumidor del JAR plano (agente ni plugins) las herede, mientras el JAR ejecutable del kernel las sigue incluyendo

C) Extraer el modelo del manifiesto a un módulo pequeño sin Spring (revierte la decisión F2-B de Application Design y cambia los planes de `U-core` y `U-plugins`)

X) Other (please describe after [Answer]: tag below)

[Answer]: 

## Question 2
¿Cómo se estructura la lista blanca de comandos?

A) Una tabla declarativa de patrones permitidos (cada entrada con nombre, forma de argv y validadores de argumentos) y un único `ProcessRunner` como lugar donde se usa `ProcessBuilder`; una prueba comprueba que `ProcessBuilder` no aparece en ninguna otra clase

B) Métodos tipados (`GitClient.createBranch`, `GitClient.push`, etc.) que arman su propio argv, y la lista blanca como verificación final antes de lanzar el proceso

X) Other (please describe after [Answer]: tag below)

[Answer]: 

## Question 3
¿Cuánto espera el agente a los procesos `git` y `gh`?

A) Con tiempo máximo: 60 segundos para operaciones locales y 120 segundos para las que usan red (`fetch`, `push`, `gh pr create`). Al vencer, se mata el proceso, se aplica la recuperación de BR-PR14 y el error es `1`

B) Sin tiempo máximo

X) Other (please describe after [Answer]: tag below)

[Answer]: 

## Question 4
`git push` por HTTPS necesita credenciales. La lista blanca no permite `gh auth` y el token no debe quedar en la URL del remoto ni en los argumentos. ¿Cómo se autentica?

A) Un script auxiliar `bin/synkra-askpass` indicado con `GIT_ASKPASS` solo en el entorno del proceso hijo de `git push`; imprime el token leído de `AGENT_TOKEN_FILE`

B) Un paso de preparación hecho por una persona fuera del agente (`gh auth setup-git`), de modo que `git` use `gh` como ayudante de credenciales con `GH_TOKEN`. El agente no ejecuta nada de autenticación

C) Una llave SSH de despliegue en un archivo, con `GIT_SSH_COMMAND` en el entorno del proceso hijo, y el remoto configurado por SSH

X) Other (please describe after [Answer]: tag below)

[Answer]: 

## Question 5
Los valores sensibles (clave de la API, token) no deben aparecer en ninguna salida. Además de no escribirlos nunca, ¿se añade una red de seguridad?

A) Sí: un registro central de secretos cargados, y todo texto que sale del agente (salida estándar, salida de error, logs, mensajes de excepción, cuerpo del PR) pasa por un filtro que reemplaza esos valores por `***`

B) No: se confía en que el diseño nunca los pasa a ninguna salida, y la prueba `SecretLeakTest` lo verifica

X) Other (please describe after [Answer]: tag below)

[Answer]: 

## Question 6
Hay reglas de arquitectura que deben mantenerse con el tiempo: el SDK de Anthropic solo en su paquete, `ProcessBuilder` solo en `ProcessRunner`, y ningún Spring en el classpath. ¿Cómo se hacen cumplir?

A) Pruebas JUnit que recorren el código fuente y las dependencias con texto (sin dependencia nueva)

B) ArchUnit como dependencia de pruebas, con reglas de arquitectura declarativas

X) Other (please describe after [Answer]: tag below)

[Answer]: 

## Question 7
`score.py` y `propose` leen informes que escribe `analyze`. Si el formato cambia en el futuro, ¿qué pasa?

A) El informe lleva `formato: 1` en el encabezado; `ReportReader` y `score.py` rechazan versiones desconocidas con un mensaje claro

B) Sin versión de formato: el lector simplemente falla si no encuentra lo que espera

X) Other (please describe after [Answer]: tag below)

[Answer]: 

---

## Parte 2: Pasos de generación (tras responder)

- [ ] 1. Analizar las respuestas y resolver ambigüedades o contradicciones
- [ ] 2. Crear `aidlc-docs/construction/U-agent/nfr-design/nfr-design-patterns.md`: patrones de resiliencia, seguridad, gasto, aislamiento del classpath y del SDK, cada uno ligado a los NFR que cumple
- [ ] 3. Crear `logical-components.md`: componentes lógicos (`ProcessRunner`, `CommandWhitelist`, `SecretRegistry`, `Clock`, `AtomicFiles`, `ExitCodeMapper`, `BudgetStore` y otros), sus responsabilidades y dependencias, con diagrama Mermaid validado y alternativa en texto
- [ ] 4. Verificar que los 29 NFR de la unidad quedan cubiertos por un patrón o un componente, y registrar enmiendas a artefactos aprobados si hacen falta
- [ ] 5. Actualizar `aidlc-state.md` y registrar la aprobación en `audit.md`
