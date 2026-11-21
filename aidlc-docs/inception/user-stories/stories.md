# Historias de usuario: SynKra

**Fuentes:** `aidlc-docs/inception/requirements/requirements.md` (aprobado) y PRD v2 §5 y §7.
**Plan:** `aidlc-docs/inception/plans/story-generation-plan.md` (respuestas 1D, 2B, 3A, 4A, 5A,
6X).

**Convenciones**
- Épicas por capacidad (E1 a E7); cada historia se escribe desde una persona (`personas.md`) y
  lleva su caso de uso o journey.
- Criterios **Dado / Cuando / Entonces**; cada uno termina con la comprobación (→ comando).
  `$API` es la URL de la API (en el demo, `https://synkra.local/api`). Los comandos exactos y
  los datos de prueba se fijan en las tareas.
- Las historias **Should** van aparte, en la épica E8, para poder cortarlas sin tocar las demás.
- Los *fixtures* de la suite (plugin incompatible, descriptor incompleto, hash alterado, plugin
  que falla al iniciar) son artefactos de prueba, no plugins del producto (supuesto S3).

---

## E1 · Catálogo de compilados (M1) · CU1

### H-01 · Registrar un core
**Como** P-OPE **quiero** registrar un core con su versión y su imagen **para** que su carpeta de
plugins exista y se pueda desplegar. · **Must** · CU1, journey 7.2 · RF-01

- **Dado** que no existe el core `synkra-core`, **cuando** registro `synkra-core 2.1.0` con su
  imagen y digest, **entonces** responde 201 y la carpeta del core existe vacía
  → `curl -X POST $API/cores …` = 201; `curl $API/cores/synkra-core/plugins` = `[]`
- **Dado** que ya existe `synkra-core 2.1.0`, **cuando** registro `synkra-core 3.0.0`, **entonces**
  la carpeta es la misma y el core tiene dos versiones → `curl $API/cores/synkra-core` lista `2.1.0` y `3.0.0`
- **Dado** una imagen sin digest o una versión que no es semver, **cuando** la registro,
  **entonces** responde 422 con el campo inválido → `curl` = 422

### H-02 · Subir un plugin a la carpeta de un core
**Como** P-PUB **quiero** subir el JAR de mi plugin a la carpeta de un core **para** que quede
disponible para los despliegues. · **Must** · CU1 · RF-02, RF-04

- **Dado** el core `synkra-core` registrado, **cuando** subo `kyc-pe-1.3.0.jar` con un
  `synkra-plugin.yaml` completo, **entonces** responde 201 con el descriptor leído (nombre,
  versión, core, rango compatible, etiquetas) → `curl -F archivo=@kyc-pe-1.3.0.jar $API/cores/synkra-core/plugins` = 201
- **Dado** el plugin subido, **cuando** consulto su detalle, **entonces** el SHA-256 coincide con
  el del archivo local → `curl $API/cores/synkra-core/plugins/kyc-pe/1.3.0 | jq -r .sha256` = `sha256sum kyc-pe-1.3.0.jar`

### H-03 · Rechazar un descriptor inválido al subirlo
**Como** P-PUB **quiero** que un descriptor incompleto se rechace al subirlo, con el motivo,
**para** corregirlo antes de que nadie intente desplegarlo. · **Must** · CU1, PRD §11.1 · RF-03

- **Dado** un JAR sin `synkra-plugin.yaml`, o sin nombre, versión del plugin o core compatible,
  **cuando** lo subo, **entonces** responde 422 y nombra el campo que falta → casos "descriptor incompleto" de la suite (`mvn verify`)
- **Dado** un descriptor con el rango `>=2.x<`, **cuando** lo subo, **entonces** responde 422
  "rango mal formado" → `curl` = 422
- **Dado** un descriptor que dice `core: otro-core`, **cuando** lo subo a la carpeta de
  `synkra-core`, **entonces** responde 422 "el plugin no pertenece a este core" → `curl` = 422

### H-04 · Rechazar artefactos peligrosos
**Como** P-OPE **quiero** que SynKra rechace artefactos con rutas fuera de su carpeta o
demasiado grandes **para** que nadie escriba fuera de `/plugins` ni llene el volumen. · **Must**
· PRD §11.4 · RF-05

- **Dado** un JAR con una entrada `../../etc/x`, **cuando** lo subo, **entonces** responde 422
  "ruta no permitida" y no queda nada en el volumen → `curl` = 422; `kubectl exec … ls /data/artefactos` sin el archivo
- **Dado** un artefacto de 25 MB con el límite en 20 MB, **cuando** lo subo, **entonces** responde
  413 con el límite configurado → `curl` = 413

### H-05 · Filtrar la carpeta por etiqueta
**Como** P-RES **quiero** filtrar la carpeta de un core por país **para** ver solo los plugins
de mi país. · **Must** · CU2 · RF-06

- **Dado** `kyc-co` (`pais=co`) y `kyc-pe` (`pais=pe`) en la carpeta, **cuando** filtro por
  `pais=pe`, **entonces** solo aparece `kyc-pe` → `curl "$API/cores/synkra-core/plugins?pais=pe" | jq length` = 1

### H-06 · Carpetas de plugins independientes
**Como** P-OPE **quiero** que un plugin de la carpeta de un core no exista para otro core **para**
que nunca se mezclen productos distintos. · **Must** · P4 · RF-07

- **Dado** `kyc-pe` en la carpeta de `synkra-core` y otro core `otro-core`, **cuando** listo la
  carpeta de `otro-core`, **entonces** `kyc-pe` no aparece → `curl $API/cores/otro-core/plugins` = `[]`
- **Dado** lo anterior, **cuando** pido un plan con `otro-core` y `kyc-pe`, **entonces** responde
  422 "plugin de otra carpeta" → `curl -X POST $API/despliegues …` = 422

---

## E2 · Motor de verificación (M2) · CU4

### H-07 · Una regla de compatibilidad, en una librería
**Como** P-OPE **quiero** que la compatibilidad se decida con una sola librería, sin
dependencias de Spring ni de Kubernetes, **para** que el backend y el core no puedan opinar
distinto. · **Must** · P1, R1 · RF-08, RF-09

- **Dado** el core `2.1.0` y un plugin con rango `2.x`, **cuando** verifico, **entonces** el
  veredicto es "compatible" → prueba unitaria del módulo del verificador
- **Dado** cualquier versión `2.*`, **cuando** la comparo con `2.x`, **entonces** siempre
  satisface, y ninguna `3.*` lo hace → prueba de propiedades (PBT-03) con generadores de versiones semver
- **Dado** el módulo del verificador, **cuando** reviso sus dependencias, **entonces** no
  incluye Spring ni Fabric8 → `mvn dependency:tree` del módulo sin `spring` ni `fabric8`

### H-08 · Explicar por qué una combinación no sirve
**Como** P-RES **quiero** que un rechazo diga el motivo y qué versión sí sirve **para** saber qué
corregir sin preguntarle a nadie. · **Must** · CU4 · RF-10

- **Dado** el core `2.1.0` y `kyc-pe 2.0.0` (rango `3.x`) con `kyc-pe 1.3.0` (rango `2.x`) en la
  carpeta, **cuando** verifico, **entonces** el rechazo dice "requiere core 3.x" y sugiere
  `kyc-pe 1.3.0` → `curl -X POST $API/verificaciones … | jq .plugins[0].alternativas`
- **Dado** que no existe ninguna versión compatible, **cuando** verifico, **entonces** el rechazo
  lo dice explícitamente → caso de la suite
- **Dado** toda la suite, **cuando** corre, **entonces** el 100% de los rechazos por versión trae
  alternativa o "no hay alternativa" → `mvn verify` (métrica de utilidad)

### H-09 · Verificar sin desplegar
**Como** P-RES **quiero** ver la verificación mientras elijo los plugins **para** no llegar al
resumen con una combinación que no sirve. · **Must** · CU2, CU4, journey 7.1 · RF-11

- **Dado** una combinación inválida, **cuando** llamo a la verificación, **entonces** responde 200
  con veredicto "rechazada" y no crea ningún plan → `curl -X POST $API/verificaciones` = 200; `curl $API/despliegues?estado=pendiente` sin cambios

### H-10 · La suite de combinaciones bloquea el build
**Como** P-OPE **quiero** que la suite del PRD §11.1 corra en cada build del verificador y del
core **para** que una regresión de compatibilidad no se pueda construir. · **Must** · PRD §11 ·
RF-12

- **Dado** la suite con sus 7 grupos (válidas, versión incompatible, plugin ausente, carpeta
  equivocada, descriptor incompleto, integridad, plugin no activado), **cuando** corro el build,
  **entonces** todos los casos pasan → `mvn verify` = 0
- **Dado** un caso cuyo resultado esperado se altera, **cuando** corro el build, **entonces**
  falla → `mvn verify` ≠ 0

---

## E3 · Motor de despliegue (M3) · CU2, CU3

### H-11 · Preparar un despliegue y ver su resumen
**Como** P-RES **quiero** que SynKra me muestre lo que va a desplegar, y qué cambia frente a lo
que corre hoy, antes de confirmar **para** confirmar sabiendo qué pasa. · **Must** · CU2, CU3,
P2 · RF-13

- **Dado** una combinación válida para `pe`, **cuando** creo el plan, **entonces** responde 201 en
  estado `pendiente` con namespace, imagen, plugins y versiones, y el clúster no cambia
  → `curl -X POST $API/despliegues` = 201; `kubectl get deploy -n synkra-pe` igual que antes
- **Dado** que `pe` ya corre con `kyc-pe 1.3.0`, **cuando** creo un plan que añade otro plugin,
  **entonces** el resumen lista lo añadido, lo quitado y lo que se mantiene
  → `curl … | jq .cambios`
- **Dado** una combinación inválida, **cuando** creo el plan, **entonces** responde 422 con los
  motivos y no se crea nada → `curl` = 422

### H-12 · Confirmar y desplegar
**Como** P-RES **quiero** confirmar el plan **para** que mi país corra con esa combinación.
· **Must** · CU2, journey 7.1, P2 · RF-14

- **Dado** un plan `pendiente`, **cuando** lo confirmo, **entonces** SynKra aplica con la cuenta
  `synkra:deployer` el Deployment del core con su init container, el ConfigMap de plugins
  activos y el Service en `synkra-pe` → `curl -X POST $API/despliegues/{id}/confirmacion` = 202; `kubectl get deploy,cm,svc -n synkra-pe`
- **Dado** el despliegue aplicado, **cuando** leo el ConfigMap, **entonces** lista exactamente los
  plugins confirmados → `kubectl get cm -n synkra-pe -o jsonpath=…`
- **Dado** un plan ya confirmado, **cuando** lo confirmo otra vez, **entonces** responde 409
  → `curl` = 409

### H-13 · Nada se despliega fuera de `synkra-*`
**Como** P-OPE **quiero** que el backend rechace cualquier país o namespace que no sea
`synkra-<país>` **para** tener una defensa antes del RBAC. · **Must** · P2, PRD §11.4 · RF-15

- **Dado** la lista de países `co, pe`, **cuando** pido un plan para `default`, `kube-system` o
  `mx`, **entonces** responde 422 y no llega ninguna petición a Kubernetes → `curl` = 422; log del backend sin llamadas a la API de Kubernetes

### H-14 · Saber si el despliegue quedó listo o falló, sin reintentos
**Como** P-RES **quiero** ver el estado hasta que los pods estén listos **para** saber si mi país
quedó corriendo. · **Must** · CU2, journey 7.4, P2 · RF-16

- **Dado** un despliegue confirmado y sano, **cuando** los pods quedan listos antes de 3 minutos,
  **entonces** su estado es `listo` → `curl $API/despliegues/{id} | jq .estado` = `listo`
- **Dado** un despliegue cuyo pod no queda listo en 3 minutos, **cuando** vence el plazo,
  **entonces** su estado es `fallido` y SynKra no crea otro despliegue ni revierte
  → `jq .estado` = `fallido`; `curl $API/paises/pe/despliegues | jq length` sin cambios

### H-15 · El pod de un país solo recibe los plugins elegidos
**Como** P-OPE **quiero** que el init container descargue solo los plugins elegidos y verifique su
hash **para** que un pod no lleve código de otro país ni un artefacto alterado. · **Must** · PRD
§9.3, R6, R7 · RF-17

- **Dado** un despliegue de `pe` con `kyc-pe`, **cuando** el pod arranca, **entonces** `/plugins`
  solo contiene `kyc-pe` → `kubectl exec -n synkra-pe deploy/core -- ls /plugins`
- **Dado** un artefacto con el hash alterado, **cuando** el init container lo descarga,
  **entonces** el pod queda en `Init:Error` y el core no arranca → `kubectl get pod -n synkra-pe`
- **Dado** que el catálogo no responde, **cuando** el init container intenta 5 veces, **entonces**
  termina con error y un mensaje claro → `kubectl logs -n synkra-pe <pod> -c descarga-plugins`

### H-16 · Recibir el contexto de un despliegue fallido
**Como** P-SRE **quiero** que un despliegue fallido traiga todo el contexto **para** decidir
volver atrás y avisar al dueño del plugin sin investigar a ciegas. · **Must** · journey 7.4 ·
RF-18

- **Dado** un despliegue `fallido`, **cuando** abro su detalle, **entonces** trae la combinación,
  el resultado de la verificación, el log de arranque del core, los eventos del pod y quién
  confirmó y cuándo → `curl $API/despliegues/{id} | jq '.contexto | keys'` con los cinco campos

### H-17 · Abandonar a mitad del flujo no despliega nada
**Como** P-RES **quiero** que si cierro el navegador antes de confirmar no pase nada **para** no
desplegar por accidente. · **Must** · journey 7.3, P2 · RF-13, RF-19

- **Dado** un plan `pendiente` que nadie confirma, **cuando** pasa el tiempo, **entonces** el
  clúster no cambia y el plan no aparece en el registro de despliegues
  → `kubectl get deploy -n synkra-pe` sin cambios; `curl $API/paises/pe/despliegues` sin ese plan

---

## E4 · Registro y reversión (M4) · CU5

### H-18 · Todo despliegue queda registrado, y sobrevive a un reinicio
**Como** P-SRE **quiero** ver quién desplegó qué y cuándo en cada país **para** tener la traza que
Git no da. · **Must** · CU5, P7 · RF-19, RF-21, RF-23

- **Dado** un despliegue confirmado, **cuando** consulto el historial de `pe`, **entonces** aparece
  con quién, cuándo, core, plugins, versiones, tipo y resultado → `curl $API/paises/pe/despliegues`; `sqlite3 /data/synkra.db 'select … from despliegue'`
- **Dado** lo anterior, **cuando** borro el pod del backend, **entonces** el catálogo y el
  historial siguen iguales → `kubectl delete pod -n synkra -l app=synkra-backend`; mismo `curl`

### H-19 · Ver qué corre en cada país, leído del clúster
**Como** P-SRE **quiero** una vista general leída del clúster, con la marca de si coincide con el
registro, **para** saber qué corre de verdad. · **Must** · CU5, PRD §10 concordancia · RF-20

- **Dado** `co` y `pe` desplegados, **cuando** consulto la vista general, **entonces** cada país
  muestra core, plugins, versiones y estado iguales a los del clúster → `curl $API/paises` frente a `kubectl get deploy,cm -n synkra-co -n synkra-pe`
- **Dado** que alguien cambia el ConfigMap de `pe` a mano, **cuando** consulto, **entonces** `pe`
  aparece marcado "no coincide con el registro" → `curl $API/paises | jq '.[] | select(.pais=="pe") .concuerda'` = `false`

### H-20 · Volver al despliegue anterior
**Como** P-RES (o P-SRE con rol Desplegador) **quiero** volver a un despliegue anterior **para**
que el país regrese a una combinación conocida. · **Must** · CU5, journey 7.4, P7 · RF-22

- **Dado** un despliegue `fallido` y uno `listo` anterior, **cuando** pido la reversión al
  anterior, **entonces** se crea un plan `pendiente` de tipo `reversion` que pasa por la
  verificación → `curl -X POST $API/despliegues/{anterior}/reversion` = 201, `jq .tipo` = `reversion`
- **Dado** ese plan, **cuando** lo confirmo, **entonces** el clúster vuelve a la combinación
  anterior y el registro apunta al original → `kubectl get cm -n synkra-pe …`; `curl … | jq .original`
- **Dado** que el despliegue anterior ya no es compatible (por ejemplo, se borró un plugin),
  **cuando** pido la reversión, **entonces** responde 422 con el motivo → `curl` = 422

---

## E5 · Core (M7) y plugins de país (M8)

### H-21 · El core verifica sus plugins al arrancar
**Como** P-OPE **quiero** que el core verifique con la misma librería los plugins de `/plugins`
**para** que, aunque el motor fallara, nada incompatible arranque. · **Must** · P1, PRD §9.3 ·
RF-24, RF-25, RF-27

- **Dado** `/plugins` con `kyc-pe 1.3.0` (rango `2.x`) y el ConfigMap pidiéndolo, **cuando** el core
  `2.1.0` arranca, **entonces** lo activa y lo expone → `curl localhost:8080/plugins` lista `kyc-pe 1.3.0`; readiness `200`
- **Dado** `/plugins` con un plugin de rango `3.x`, **cuando** arranca, **entonces** termina con
  código ≠ 0 y el motivo en el log → prueba de integración del core
- **Dado** el ConfigMap pidiendo un plugin que no está en `/plugins`, **cuando** arranca,
  **entonces** no arranca → prueba de integración
- **Dado** un plugin presente y no pedido, **cuando** arranca, **entonces** no se cargan sus
  clases ni sus rutas → `curl localhost:8080/plugins` sin él
- **Dado** el código del core, **cuando** busco lógica de país, **entonces** no hay
  → `grep -rniE "colombia|peru" core/src` sin resultados

### H-22 · Un plugin que falla al iniciar no deja el core a medias
**Como** P-SRE **quiero** que si un plugin falla al iniciarse el core no quede listo **para** que
el fallo se vea y el despliegue se marque fallido. · **Must** · R2, journey 7.4 · RF-26

- **Dado** el *fixture* de un plugin que lanza una excepción al iniciar, **cuando** el core
  arranca, **entonces** la readiness nunca pasa y el log nombra el plugin → prueba de integración; en el clúster, `kubectl get pod` en `CrashLoopBackOff`
- **Dado** dos plugins, **cuando** cargo ambos, **entonces** cada uno tiene su propio cargador de
  clases → prueba unitaria del cargador

### H-23 · Plugins de Colombia y Perú
**Como** P-PUB **quiero** los plugins `kyc-co` y `kyc-pe` con reglas simuladas, y un
`kyc-pe 2.0.0` compatible con `3.x`, **para** demostrar la variación por país y el rechazo de
CU4. · **Must** · PRD §8, CU4 · RF-28

- **Dado** el core con `kyc-co`, **cuando** llamo a su punto de KYC con un documento válido e
  inválido, **entonces** responde según la regla simulada de Colombia → `curl` a cada endpoint
- **Dado** el core con `kyc-pe`, ídem con la regla de Perú → `curl`
- **Dado** los JAR construidos, **cuando** leo sus descriptores, **entonces** llevan `pais=co`,
  `pais=pe` y los rangos `2.x` y `3.x` → `unzip -p kyc-pe-2.0.0.jar synkra-plugin.yaml`

---

## E6 · API (M5) y front (M6)

### H-24 · Los cinco casos de uso, sin el front
**Como** P-OPE **quiero** hacer todo por la API **para** que el front no sea indispensable y la
CLI futura tenga en qué apoyarse. · **Must** · P5 · RF-29

- **Dado** SynKra instalado, **cuando** corro el script de punta a punta (CU1 a CU5 con `curl`),
  **entonces** termina con código 0 → `./scripts/e2e-api.sh`
- **Dado** cualquier error de la API, **cuando** lo leo, **entonces** tiene el mismo formato
  (código, mensaje, detalles) → pruebas de la API

### H-25 · Usar SynKra desde el front
**Como** P-RES **quiero** las cuatro pantallas (vista general, catálogo, desplegar con
verificación en vivo, detalle con contexto del fallo y vuelta atrás) **para** hacer la demo sin
la terminal. · **Must** · PRD §9.4, P5 · RF-31

- **Dado** el front desplegado, **cuando** recorro CU2, CU4 y CU5 en el navegador, **entonces**
  cada paso coincide con la respuesta de la API → recorrido de la demo con el log de la API abierto
- **Dado** el código del front, **cuando** busco reglas de versión, **entonces** no hay: toda
  decisión viene de la API → `grep -rniE "semver|compatib" front/` solo en llamadas a la API

---

## E7 · Instalación y límite en el clúster

### H-26 · Instalar SynKra con un comando
**Como** P-OPE **quiero** instalar SynKra en `kind` con un comando, que también cree los
namespaces de los países, **para** llegar rápido al primer despliegue. · **Must** · journey 7.2,
PRD §10 · RF-32

- **Dado** un clúster `kind` vacío, **cuando** corro la instalación, **entonces** el backend, el
  front y el PVC quedan listos y existen `synkra-co` y `synkra-pe` → `make install`; `kubectl get pods -n synkra`; `kubectl get ns`

### H-27 · Comprobar el límite de la cuenta de despliegue
**Como** P-OPE **quiero** comprobar con un comando qué puede hacer SynKra en el clúster **para**
confiar en que no escribe fuera de sus namespaces. · **Must** · P2, PRD §3 · RF-33

- **Dado** SynKra instalado, **cuando** pregunto si `synkra:deployer` puede crear Deployments en
  `default`, **entonces** la respuesta es `no` → `kubectl auth can-i create deployments -n default --as=system:serviceaccount:synkra:deployer`
- **Dado** lo mismo en `synkra-pe`, **entonces** `yes`; y en `kube-system` y a nivel de clúster
  (`create namespaces`), `no` → `kubectl auth can-i …`

### H-28 · Imágenes mínimas y sin root
**Como** P-OPE **quiero** que todas las imágenes sean multi-stage y corran sin root **para**
reducir lo que puede hacer un contenedor comprometido. · **Must** · PRD §8, módulo 3 · RF-34

- **Dado** las imágenes del backend, el front, el core y el init container, **cuando** las inspecciono,
  **entonces** el usuario no es root → `docker inspect -f '{{.Config.User}}' <imagen>` no vacío ni `root`/`0`

---

## E8 · Should (se cortan primero si falta tiempo)

### H-29 · API documentada
**Como** P-OPE **quiero** la API en OpenAPI **para** escribir la CLI futura sin leer el código.
· **Should** · RF-30
- **Dado** el backend en marcha, **cuando** pido el documento, **entonces** responde 200 y
  contiene `/despliegues` → `curl $API/v3/api-docs | jq '.paths | keys'`

### H-30 · Iniciar sesión y respetar los roles
**Como** P-OPE **quiero** usuarios con rol y HTTP Basic **para** que solo quien tiene el rol
correcto suba, despliegue o revierta. · **Should** · PRD §9.2, R3 · RF-36
- **Dado** un usuario Lector, **cuando** crea un plan, **entonces** 403 → `curl -u lector:… -X POST $API/despliegues` = 403
- **Dado** un Publicador, **cuando** confirma un plan, **entonces** 403; un Desplegador, **cuando**
  sube un JAR, **entonces** 403 → `curl`
- **Dado** una petición sin credenciales, **entonces** 401 → `curl` = 401

### H-31 · Solo imágenes del registro permitido
**Como** P-OPE **quiero** que solo se registren cores de mi registro **para** que nadie despliegue
una imagen de origen desconocido. · **Should** · PRD §11.4, R3 · RF-37
- **Dado** el registro permitido `localhost:5001`, **cuando** registro un core de `docker.io`,
  **entonces** 422 → `curl` = 422

### H-32 · Endurecer los namespaces
**Como** P-OPE **quiero** NetworkPolicy *deny-by-default*, Pod Security `restricted` y límites de
recursos en `synkra` y `synkra-*` **para** contener un pod comprometido. · **Should** · módulos 1
y 7 · RF-38
- **Dado** los namespaces creados, **cuando** reviso sus etiquetas y políticas, **entonces**
  tienen `enforce=restricted` y una NetworkPolicy que niega por defecto
  → `kubectl get ns -L pod-security.kubernetes.io/enforce`; `kubectl get netpol -A`
- **Dado** un pod de prueba sin permiso explícito, **cuando** llama al backend, **entonces** no
  conecta → `kubectl run … curl` con *timeout*

### H-33 · Entorno de desarrollo y escaneo
**Como** P-PUB **quiero** un `compose.yaml` con las 6 prácticas del módulo 3 y las imágenes
escaneadas con Trivy **para** probar en local y no publicar vulnerabilidades críticas. ·
**Should** · RF-39
- **Dado** el repositorio, **cuando** levanto `docker compose up`, **entonces** el backend responde
  salud → `curl localhost:8080/actuator/health`
- **Dado** las imágenes, **cuando** las escaneo, **entonces** no hay hallazgos `CRITICAL`
  → `trivy image --severity CRITICAL --exit-code 1 <imagen>` = 0

### H-34 · SynKra mismo se instala con GitOps
**Como** P-OPE **quiero** que la instalación de SynKra se reconcilie desde Git **para** cubrir el
módulo 8 sin meter Git en el despliegue de productos. · **Should** · PRD §13 · RF-35
- **Dado** el repositorio de instalación, **cuando** el controlador de GitOps reconcilia,
  **entonces** el backend y el front quedan con la versión del repositorio → estado `Ready` del recurso de GitOps (`flux get kustomizations` o equivalente)

---

## Cobertura de requisitos

| RF | Historias | RF | Historias | RF | Historias |
|---|---|---|---|---|---|
| RF-01 | H-01 | RF-14 | H-12 | RF-27 | H-21 |
| RF-02 | H-02 | RF-15 | H-13 | RF-28 | H-23 |
| RF-03 | H-03 | RF-16 | H-14 | RF-29 | H-24 |
| RF-04 | H-02 | RF-17 | H-15 | RF-30 | H-29 |
| RF-05 | H-04 | RF-18 | H-16 | RF-31 | H-25 |
| RF-06 | H-05 | RF-19 | H-17, H-18 | RF-32 | H-26 |
| RF-07 | H-06 | RF-20 | H-19 | RF-33 | H-27 |
| RF-08 | H-07 | RF-21 | H-18 | RF-34 | H-28 |
| RF-09 | H-07 | RF-22 | H-20 | RF-35 | H-34 |
| RF-10 | H-08 | RF-23 | H-18 | RF-36 | H-30 |
| RF-11 | H-09 | RF-24 | H-21 | RF-37 | H-31 |
| RF-12 | H-10 | RF-25 | H-21 | RF-38 | H-32 |
| RF-13 | H-11, H-17 | RF-26 | H-22 | RF-39 | H-33 |

Los 39 RF quedan cubiertos; ninguna historia carece de RF.

## Personas por historia

| Persona | Historias |
|---|---|
| P-OPE Operador | H-01, H-04, H-06, H-07, H-10, H-13, H-15, H-21, H-24, H-26 a H-32, H-34 |
| P-PUB Desarrollador de plugins | H-02, H-03, H-23, H-33 |
| P-RES Responsable de país | H-05, H-08, H-09, H-11, H-12, H-14, H-17, H-20, H-25 |
| P-SRE SRE o revisor | H-16, H-18, H-19, H-20, H-22 |

## Resumen

- **34 historias**: 28 Must (E1 a E7) y 6 Should (E8). Algo por encima de la estimación de 25 a
  30, porque los *edge cases* (H-16, H-17) y el límite (H-13, H-27) van como historias propias.
- **INVEST:** cada historia se prueba sola con sus comandos; las dependencias entre ellas (por
  ejemplo, H-12 necesita H-11) se ordenan en Units Generation.
- **AUTONOMIA-01:** cumple. Ninguna historia despliega sin confirmación (H-11, H-12, H-17), ninguna
  reintenta ni revierte sola (H-14, H-20), y H-27 comprueba el RBAC con `kubectl auth can-i`.
- **AUTONOMIA-02:** cumple. Todo criterio termina en un comando o una prueba nombrada.
- **PBT (parcial):** H-07 pide propiedades sobre los rangos semver; el diseño de M2 las detalla.
