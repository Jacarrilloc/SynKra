# Requisitos no funcionales — U-agent

Respuestas que lo condicionan (`U-agent-nfr-requirements-plan.md`): Q1-C, Q2-B, Q3-B, Q4-A, Q5-B,
Q6-A, Q7-B, Q8-C, Q9-B, Q10-B. Sin contradicciones ni ambigüedades. Cada requisito lleva un
comando de verificación o declara explícitamente que no tiene umbral (AUTONOMIA-03). La
extensión de seguridad general está desactivada; los requisitos de seguridad de aquí son los
propios del producto (P1, P4, P5).

Los comandos usan `PL="mvn -q -am -Dsurefire.failIfNoSpecifiedTests=false"` y se ejecutan
en la raíz del repositorio, en Linux o WSL2.

## Rendimiento

| Id | Requisito | Origen | Verificación |
|---|---|---|---|
| NFR-AG-PF1 | Sin umbral de tiempo para `analyze` ni `propose` en este MVP. El tiempo medido se anota en el resumen de la unidad, sin criterio de aceptación | Q1-C | No aplica |
| NFR-AG-PF2 | La llamada al modelo espera como máximo 30 segundos; al vencer, la narrativa pasa a «narrativa no disponible» y no se reintenta | Q2-B | `$PL -pl agent test -Dtest=ClaudeAdapterTimeoutTest` termina en `0`: un servidor de prueba que tarda más del tiempo máximo produce «narrativa no disponible», con el tiempo máximo reducido a 2 segundos por propiedad de prueba |

## Fiabilidad

| Id | Requisito | Origen | Verificación |
|---|---|---|---|
| NFR-AG-RL1 | Un fallo del modelo no impide el informe: el veredicto no depende de la respuesta (BR-NR6) | Q8-A | `$PL -pl agent test -Dtest=NarratorFailureTest` termina en `0` |
| NFR-AG-RL2 | El informe se escribe de forma atómica (archivo temporal y renombrado), de modo que una interrupción no deja un informe a medias | Derivado | `$PL -pl agent test -Dtest=ReportWriterAtomicTest` termina en `0` |
| NFR-AG-RL3 | Un fallo de `propose` tras crear la rama devuelve el árbol a la rama original (BR-PR14) | BR-PR14 | `$PL -pl agent test -Dtest=ProposalFailureRecoveryTest` termina en `0`, en un repositorio temporal con remoto local |
| NFR-AG-RL4 | Mismo cambio e inventario dan el mismo veredicto en cada ejecución (BR-LC8) | RF-31 | `$PL -pl agent test -Dtest=LocatorDeterminismTest` termina en `0` |

## Seguridad y límite de autonomía

| Id | Requisito | Origen | Verificación |
|---|---|---|---|
| NFR-AG-SC1 | La lista blanca deniega por defecto. Cada uno de los comandos prohibidos (`merge`, `rebase`, `reset`, `push` a otra rama, `gh pr merge`, `gh pr review`, `gh pr ready`, `gh api`, `kubectl`, `helm`) es rechazado y registrado | P1, AUTONOMIA-01 | `$PL -pl agent test -Dtest=CommandWhitelistTest` termina en `0`, con una prueba por BR-WL1 a BR-WL16 |
| NFR-AG-SC2 | La clave de la API y el token de GitHub se leen de archivos; nunca aparecen en la salida estándar, en la salida de error, en los logs ni en el informe | Q6-A, BR-NR8 | `$PL -pl agent test -Dtest=SecretLeakTest` termina en `0`: con una clave de prueba conocida, ninguna salida capturada la contiene |
| NFR-AG-SC3 | No se comprueban los permisos de los archivos de secretos (riesgo aceptado). El README del agente recomienda modo `0600` | Q5-B | `grep -c "0600" agent/README.md` imprime al menos `1` |
| NFR-AG-SC4 | El texto del cambio no llega al modelo ni a los logs. El modelo recibe solo veredictos, país y tema | Q6-A, FD Q7-B | `$PL -pl agent test -Dtest=PromptContentTest` termina en `0`: el prompt capturado no contiene el texto del cambio |
| NFR-AG-SC5 | Tres cargas de inyección (ignorar instrucciones, pedir fusionar, comillas invertidas que intentan romper el bloque) no alteran los veredictos, no llegan al modelo y no rompen el bloque delimitado del archivo de solicitud | Q10-B, BR-RT1 | `$PL -pl agent test -Dtest=RedTeamInjectionTest` termina en `0` |
| NFR-AG-SC6 | El SDK de Anthropic solo aparece en el paquete del adaptador `claude` | AUTONOMIA-02, Q3-B | `grep -rln "com.anthropic" agent/src \| grep -v "/llm/claude/"` no imprime nada |
| NFR-AG-SC7 | La suite corre con `LLM_PROVIDER=fake` y con la red hacia `api.anthropic.com` bloqueada; el adaptador `claude` se prueba contra un servidor HTTP local | NFR-04, NFR-08 | `LLM_PROVIDER=fake mvn -q -pl agent -am test` termina en `0` y `grep -rn "api.anthropic.com" agent/src/test` no imprime nada |
| NFR-AG-SC8 | El agente no usa `kubectl` ni `helm` ni la API de Kubernetes | AUTONOMIA-01 | `grep -rniE -e kubectl -e helm -e kubernetes agent/src/main` solo encuentra la lista de comandos denegados de la lista blanca (un único archivo, `CommandWhitelist`) |

## Gasto de IA

| Id | Requisito | Origen | Verificación |
|---|---|---|---|
| NFR-AG-CS1 | Valores por defecto: presupuesto de 5.000 tokens al día, 300 tokens de salida por llamada, modelo `claude-haiku-5-5`, proveedor `fake` | Q9-C, FD supuesto 9 | `$PL -pl agent test -Dtest=ConfigDefaultsTest` termina en `0` |
| NFR-AG-CS2 | Con presupuesto 0 o agotado no se llama al modelo y la salida contiene «presupuesto agotado» | RF-43 | `$PL -pl agent test -Dtest=BudgetExhaustedTest` termina en `0`, con el contador de llamadas del adaptador en `0` |
| NFR-AG-CS3 | Cada ejecución añade una línea a `agent/logs/tokens.log` con ejecución, cambio, proveedor y tokens | RF-44 | `$PL -pl agent test -Dtest=TokenLogTest` termina en `0` |
| NFR-AG-CS4 | Una evaluación completa con `claude` puede agotar el presupuesto de un día; se sube `DAILY_TOKEN_BUDGET` de forma explícita para esa corrida | Q9-C, BR-BU7 | `grep -c "DAILY_TOKEN_BUDGET" eval/README.md` imprime al menos `1` |

## Mantenibilidad

| Id | Requisito | Origen | Verificación |
|---|---|---|---|
| NFR-AG-MT1 | Sin umbral de cobertura (Q8-C). En su lugar, cada regla de localización, lista blanca y presupuesto tiene al menos una prueba cuyo nombre contiene el identificador de la regla | Q8-C | `for r in $(seq -f 'BR_WL%g' 1 16) $(seq -f 'BR_LC%g' 1 8) $(seq -f 'BR_BU%g' 1 6); do grep -rq "$r" agent/src/test || echo FALTA $r; done` no imprime nada |
| NFR-AG-MT2 | Todas las dependencias Maven del agente con versión fija: sin rangos, `LATEST` ni `RELEASE` | Q7-B | `grep -n -e '\[' -e LATEST -e RELEASE agent/pom.xml` no imprime nada |
| NFR-AG-MT3 | El escaneo de vulnerabilidades de imágenes (Trivy) queda para `U-deploy` | Q7-B | No aplica en esta unidad |
| NFR-AG-MT4 | `agent/README.md` documenta comandos, variables de entorno, códigos de salida (`0`, `1`, `2`) y la recomendación de permisos de archivos secretos | Derivado | `grep -c -e analyze -e propose -e DAILY_TOKEN_BUDGET -e código agent/README.md` imprime al menos `4` |

## Portabilidad y operación

| Id | Requisito | Origen | Verificación |
|---|---|---|---|
| NFR-AG-PT1 | El comando `synkra` es un script `bin/synkra` que invoca un JAR ejecutable único; `make build` genera el JAR. Funciona en Linux y WSL2 con Java 21 | Q4-A, NFR-11 | `make build && bin/synkra --help` termina en `0` y su salida contiene `analyze` y `propose` |
| NFR-AG-PT2 | El classpath del agente no contiene Spring Boot web ni Tomcat, aunque dependa de `core` solo para el modelo del manifiesto | F2-B de Application Design | `mvn -pl agent dependency:tree \| grep -ciE "spring-boot-starter-web\|tomcat"` imprime `0` |
| NFR-AG-PT3 | Una sola ejecución a la vez. Sin bloqueo de archivo del presupuesto; con dos ejecuciones simultáneas el gasto puede subestimarse (riesgo aceptado). El Job de Kubernetes se define sin concurrencia | Q9-B | `grep -c "una sola ejecución" agent/README.md` imprime al menos `1` |

## Privacidad

| Id | Requisito | Origen | Verificación |
|---|---|---|---|
| NFR-AG-PV1 | En modo `claude`, los veredictos, el país y el tema salen hacia la API de Anthropic. No cumple la promesa de perímetro del PVB; se declara en el README | NFR-09 | `grep -n "perímetro" README.md` encuentra la declaración |
| NFR-AG-PV2 | Los logs contienen solo metadatos: id del cambio, huella, país, tema, veredictos, proveedor, conteos de tokens y errores | Q6-A | `$PL -pl agent test -Dtest=LogContentTest` termina en `0` |

## Usabilidad

| Id | Requisito | Origen | Verificación |
|---|---|---|---|
| NFR-AG-US1 | Mensajes y errores en español; todo error de entrada nombra el campo; códigos de salida `0`, `1`, `2` | FD, US-1.1 | `$PL -pl agent test -Dtest=CliExitCodesTest` termina en `0` |
| NFR-AG-US2 | `bin/synkra --help` y `bin/synkra analyze --help` describen los comandos | Derivado | `bin/synkra analyze --help` termina en `0` |

## Riesgos aceptados en esta etapa

| Riesgo | Origen | Efecto |
|---|---|---|
| Sin comprobación de permisos de archivos secretos | Q5-B | Un archivo de clave legible por otros usuarios no se detecta |
| Sin bloqueo de archivo del presupuesto | Q9-B | Con ejecuciones simultáneas, el tope diario puede superarse |
| Sin umbral de cobertura | Q8-C | La cobertura depende de la disciplina de nombrar pruebas por regla (NFR-AG-MT1) |
| Corpus de inyección de solo 3 cargas | Q10-B | Cobertura limitada de variantes de ataque |
| Sin umbral de tiempo | Q1-C | Una regresión de rendimiento no se detecta |
| SDK de Anthropic con sus propias dependencias | Q3-B | Posible conflicto de versiones con las de `core` (Jackson); se gestiona en NFR Design |

## Cobertura de NFR del proyecto

| NFR del proyecto | Requisitos de esta unidad |
|---|---|
| NFR-01 (límite de autonomía) | NFR-AG-SC1, NFR-AG-SC8 |
| NFR-04 (gasto de IA acotado) | NFR-AG-SC6, NFR-AG-CS1, NFR-AG-CS2 |
| NFR-05 (entrada no confiable) | NFR-AG-SC4, NFR-AG-SC5 |
| NFR-06 (recall y precisión) | Cubierto por la evaluación (BR-EV) |
| NFR-08 (costo cero en CI) | NFR-AG-SC7 |
| NFR-09 (trade-off declarado) | NFR-AG-PV1 |
| NFR-10 (traspaso) | Cubierto por el diseño funcional (BR-RP4) |
| NFR-11 (entorno) | NFR-AG-PT1 |
