# Paso 0 del PRD: análisis de conflictos

> Corresponde al Paso 0 del Prompt 1 (`prompts-para-especificacion.md` del módulo 3), aplicado
> el 21-nov-2026. Se cruzaron `../../Modulo 2/pvb.md` (versión del 21-nov), los cuatro archivos
> de `../docs/` y el PRD anterior (`prd.md`, versión 1.0 del 17-oct-2026, alcance con agente de
> IA).
>
> **Las decisiones son de Julian.** Cada conflicto trae una recomendación, marcada
> **(recomendada)**. El PRD nuevo no se empieza hasta que todas las líneas `[Answer]: A. Se reescribe (Julian, 21-nov-2026).` tengan
> respuesta.

## Resumen

| # | Tipo | Doc A dice... | Doc B dice... | Decisión requerida |
|---|---|---|---|---|
| 1 | Supuesto incompatible | El PRD viejo es la especificación de un agente de IA regulatorio | El PVB nuevo: plataforma de despliegue sin IA | ¿Se reescribe el PRD o se adapta? |
| 2 | Validación frente a crítica | PVB §3: la ventaja es Trust ("nada incompatible llega al clúster") | `critica.md` §1 y §8: el motor ya es gratis; no es defendible como producto comercial | ¿Cómo se posiciona el PRD? |
| 3 | Supuesto incompatible | PVB §3: el compilado trae los plugins y el core los detecta | PRD viejo §7-8: un init container monta el plugin en el pod | ¿Cómo llegan los plugins al pod? |
| 4 | Supuesto incompatible | `overview.md` §4: un plugin puede ser un Kustomize component | PVB §3: el plugin no es un manifiesto, es un artefacto dentro del compilado | ¿Qué escribe el lienzo en el overlay? |
| 5 | Vacío crítico | PVB §3: el contrato tiene versión | Ningún documento dice si un plugin puede depender de otro | ¿El contrato soporta dependencias entre plugins? |
| 6 | Vacío crítico | PVB: front de arrastrar y soltar | Ningún documento elige su tecnología | ¿Con qué se construye el front? |
| 7 | Vacío crítico | PVB §5: SynKra abre PRs | Ningún documento dice con qué identidad ni en qué servidor Git | ¿Con qué identidad escribe SynKra en Git? |
| 8 | Vacío crítico | Memoria del proyecto: multi-tenant es una pregunta abierta | El PVB dice "sin multi-tenant" en el alcance | ¿Se confirma que no hay multi-tenant? |
| 9 | Contradicción con el curso | Prompt 1, segmentos 10 y 11: métricas y plan de evaluación de la IA | PVB §6: sin IA (desviación declarada) | ¿Qué va en el segmento 11? |
| 10 | Contradicción de datos | `CLAUDE.md` y `entradas/`: North Star de cambios regulatorios y alcance con agente | PVB §8: North Star de despliegues sin plugins incompatibles | ¿Cuándo se alinean esos archivos? |

---

## Conflicto 1: ¿se reescribe el PRD o se adapta?

El PRD viejo tiene 13 segmentos completos, pero su centro es el agente (casos de uso, journeys,
MoSCoW, segmento 11). Algunas partes siguen siendo válidas: los plugins KYC de CO y PE, el
principio "el kernel no conoce países", "un país = un overlay" y la protección de `main`.

A) Reescribir el PRD desde cero, segmento por segmento, y **reutilizar** solo lo que siga siendo válido, citándolo **(recomendada)**
B) Editar el PRD viejo: quitar el agente y conservar la estructura
C) Other (please describe after [Answer]: A. Acepto (Julian). tag below)

[Answer]: D. Se carga un core y se utiliza una carpeta de plugins disponibles para ese core. Lo que se quiere es que sea mucho más visual que cualquier otra cosa: que el usuario entienda visualmente qué se está desplegando y con qué plugins (Julian). Lectura: la imagen del core lee al arrancar una carpeta de plugins disponibles, verifica cada uno y activa los que pide el despliegue. El front muestra de forma visual el core y los plugins que lleva cada despliegue.

## Conflicto 2: ¿cómo se posiciona el PRD?

El PVB elige Trust como ventaja. La crítica concluye que la idea es defendible como proyecto
académico y como capa delgada sobre Kustomize y Argo CD, pero no como producto comercial
frente a KubeVela, Backstage y Port. El Prompt 1 dice que esta tensión "NO debe resolverse a
favor del optimismo por defecto".

A) Capa delgada y opinada sobre Kustomize y Argo CD, cuyo aporte es el contrato y la verificación. Se presenta como proyecto académico, con el veredicto de la crítica citado tal cual **(recomendada)**
B) Producto comercial que compite con KubeVela y Backstage
C) Other (please describe after [Answer]: A. Acepto (Julian). tag below)

[Answer]: A. Acepto (Julian).

## Conflicto 3: ¿cómo llegan los plugins al pod?

El PVB dice que el plugin "viaja con el core" y que el core lo detecta al arrancar. El PRD
viejo usaba un init container por plugin. Las dos opciones son compatibles con "el core
detecta y verifica", pero tienen costos distintos.

A) **Una sola imagen con todos los plugins compilados.** El overlay solo dice cuáles se activan. Es lo más simple, pero cada imagen lleva el código de todos los países (riesgo 2 del PVB §9) **(recomendada para el MVP)**
B) **Imagen del core más un init container por plugin** que copia el artefacto a un volumen compartido. Cada país lleva solo su código, pero hay una imagen por plugin que construir y publicar
C) Una imagen por país, construida desde el mismo core. Contradice "cambiar los plugins no obliga a reconstruir"
D) Other (please describe after [Answer]: D. Blazor, porque el equipo lo considera más robusto y le resulta más fácil por experiencia (Julian). Consecuencia: el front es un servicio .NET aparte que consume la API del core (Java). tag below)

[Answer]: D. De momento es un demo, así que no hace falta un pipeline tan definido (Julian). Lectura: sin GitHub App. SynKra usa un token de grano fino limitado al repo de despliegue y sigue proponiendo cambios en ramas `synkra/*`.

## Conflicto 4: ¿qué escribe el lienzo en el overlay?

Si el plugin no es un manifiesto aparte, el overlay solo tiene que transmitirle al core la
lista de plugins activos.

A) **Un Kustomize component por plugin**, que añade el plugin a la lista del ConfigMap del core. El lienzo activa o desactiva components, y Argo CD los soporta desde la v2.10 **(recomendada)**
B) Un ConfigMap por país con la lista completa, que el lienzo reescribe
C) Other (please describe after [Answer]: A. Acepto (Julian). tag below)

[Answer]: A. Acepto (Julian).

## Conflicto 5: ¿el contrato soporta dependencias entre plugins?

Ejemplo: un plugin de "firma electrónica PE" que requiere el plugin "KYC PE".

A) **MVP solo con compatibilidad core-plugin** (versión del contrato). Las dependencias entre plugins quedan en Should **(recomendada)**
B) Incluir dependencias entre plugins en el MVP (`requires: [kyc-pe>=1.0]`)
C) Other (please describe after [Answer]: A. Acepto (Julian). tag below)

