# Plan de NFR Requirements — U-agent

**Insumos:** `U-agent/functional-design/` (aprobado), `requirements.md` (NFR-01 a NFR-12, RF-40 a
RF-44, RF-52, RF-60 a RF-63) y `application-design/` (Java 21 plano con picocli, `LlmProvider`,
lista blanca).
**Alcance:** requisitos no funcionales del agente y decisiones de tecnología. La extensión de
seguridad general está desactivada por el usuario; los requisitos de seguridad de aquí son los
propios del producto (P1, P4, P5).

**Ya decidido (no se vuelve a preguntar):**
- Java 21 sin Spring, CLI con picocli, Maven; el agente depende del JAR plano de `core` (Application Design).
- Procesos `git` y `gh` con lista blanca (Application Design Q7-B). Linux o WSL2 (R10 del PRD).
- Presupuesto de 5.000 tokens al día y 300 por llamada; modelo `claude-haiku-5-5` por defecto; proveedor por defecto `fake` (Functional Design).
- Sin reintento ante fallo del modelo (Functional Design Q8-A).
- La clave de la API y el token de GitHub llegan por archivo, nunca por variable de entorno visible en el manifiesto (RF-63).

Responde escribiendo la letra después de cada `[Answer]:`. Si ninguna opción encaja, elige la
última (Other) y descríbela.

---

## Parte 1: Preguntas

## Question 1
¿Qué tiempos máximos se exigen? (AUTONOMIA-03 pide umbrales medibles.)

A) `analyze` sin llamada al modelo: menos de 5 segundos con el repositorio de SynKra. Con llamada al modelo: menos de 30 segundos. `propose`: menos de 60 segundos

B) Mismos umbrales que A, pero medidos solo sobre el cálculo local (localización e informe), sin contar red ni Git

C) Sin umbral de tiempo en este MVP

X) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 2
¿Cuánto espera el agente una respuesta de la API del modelo antes de rendirse? (Un fallo da «narrativa no disponible», sin reintento.)

A) 20 segundos

B) 30 segundos

C) 60 segundos

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 3
¿Con qué se implementa el adaptador `claude`?

A) El cliente HTTP del JDK (`java.net.http`) y Jackson, que ya llega con `core`: ninguna dependencia nueva para hablar con la API

B) El SDK oficial de Anthropic para Java, confinado al paquete del adaptador

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 4
Las historias usan el comando `synkra analyze ...`. ¿Cómo se expone ese comando?

A) Un JAR ejecutable único (con las dependencias empaquetadas) y un script `bin/synkra` que lo invoca; `make build` genera el JAR

B) Igual que A, más un objetivo `make install-cli` que copia el script y el JAR a `~/.local/bin` para tener `synkra` en el PATH

C) Imagen nativa con GraalVM (arranque más rápido, build mucho más pesado)

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5
El archivo con la clave de la API (`ANTHROPIC_API_KEY_FILE`) y el del token de GitHub (`AGENT_TOKEN_FILE`) ¿qué comprobación de permisos se exige?

A) El agente se niega a leer un archivo cuyos permisos permitan acceso a otros usuarios (modo más permisivo que `0600`) y explica cómo corregirlo. En Kubernetes, el montaje del Secret se exime con un indicador explícito

B) Sin comprobación de permisos; se confía en el sistema donde corre

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 6
¿Qué se escribe en los logs del agente?

A) Solo metadatos: id del cambio, huella, país, tema, veredictos, proveedor, conteos de tokens y errores. Nunca el texto del cambio, la respuesta del modelo, la clave ni el token

B) Lo anterior, y a nivel de depuración (opt-in con una variable) también las peticiones y respuestas del modelo, enmascarando la clave y el token

X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 7
Las dependencias Maven del agente son una superficie de cadena de suministro. ¿Qué control se pide en esta unidad?

A) Versiones fijadas (sin rangos), una tarea que lista el árbol de dependencias y una que ejecuta `trivy fs` sobre el repositorio, con el umbral de cero hallazgos CRITICAL

B) Solo versiones fijadas; el escaneo de imágenes con Trivy queda para `U-deploy`

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 8
¿Qué cobertura de pruebas se exige a las clases críticas para el límite de autonomía y el gasto (localizador, lista blanca de comandos, presupuesto)?

A) Al menos 90 % de líneas y 80 % de ramas en esas tres áreas, medido con JaCoCo; el resto del agente sin umbral

B) 100 % de ramas en la lista blanca y 85 % de líneas en el localizador y el presupuesto

C) Sin umbral de cobertura; basta con las pruebas por regla (BR-WL, BR-LC, BR-BU)

X) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 9
Dos ejecuciones del agente al mismo tiempo (por ejemplo, la evaluación en paralelo) actualizan el mismo archivo de presupuesto. ¿Qué se exige?

A) Un bloqueo de archivo a nivel de sistema operativo en la actualización del presupuesto, para que el total gastado no se subestime

B) Nada: se asume una sola ejecución a la vez

X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 10
Las pruebas de red-teaming de inyección (BR-RT1) ¿con cuántas cargas distintas se ejecutan?

A) Un archivo de datos con al menos 8 cargas: ignorar instrucciones, pedir fusionar, cambiar de rol, texto con encabezados Markdown, HTML y comillas invertidas que intentan romper el bloque, órdenes disfrazadas de cita, y texto muy largo

B) 3 cargas: ignorar instrucciones, pedir fusionar y comillas invertidas

X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Parte 2: Pasos de generación (tras responder)

- [x] 1. Analizar las respuestas y resolver ambigüedades o contradicciones
- [x] 2. Crear `aidlc-docs/construction/U-agent/nfr-requirements/nfr-requirements.md`: rendimiento, fiabilidad, seguridad, mantenibilidad, portabilidad, gasto de IA y privacidad, cada uno con umbral y comando de verificación (AUTONOMIA-03)
- [x] 3. Crear `tech-stack-decisions.md`: tecnología elegida por componente, con motivo y alternativas descartadas
- [x] 4. Verificar que cada NFR de los requisitos que toca al agente (NFR-01, NFR-04, NFR-05, NFR-06, NFR-08, NFR-09, NFR-10) queda cubierto
- [x] 5. Actualizar `aidlc-state.md` y registrar la aprobación en `audit.md`
