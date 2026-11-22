# Product Vision Board: SynKra

**Módulo 2: AI & Agentic Engineering · Tópicos Especiales en Informática**

**Versión:** 21 de noviembre de 2026.

> **Convención de evidencia:**
>
> - **[INTERNO]**: proyección, supuesto o estimación propia. Es defendible, pero no está
>   verificada externamente.
> - **[VERIFICAR]**: afirmación sobre el mundo exterior que no se pudo confirmar con una fuente
>   primaria.
> - Toda cifra sin etiqueta lleva fuente. Los enlaces completos están en
>   `../Modulo 3/docs/mercado.md` (validación), `../Modulo 3/docs/critica.md` (crítica
>   adversarial), `../Modulo 3/docs/overview.md` e `../Modulo 3/docs/icp.md`.

> **Desviación declarada frente al curso.** El curso pide que el producto "use IA de forma
> defendible". **SynKra no usa IA**: así se decidió el 21-nov-2026. Las secciones 4, 5 y 6 de la
> plantilla, pensadas para productos con IA, se responden explicando **por qué** la IA no aporta
> aquí, en lugar de añadir una IA decorativa. El riesgo que esto implica para la evaluación está
> en la sección 9.

---

## PRODUCTO

**Nombre del producto:** SynKra

**Descripción en una línea (qué hace y para quién):**
Herramienta de **despliegue** para aplicaciones **micro-kernel** ya compiladas, sobre
Kubernetes. Desde un front de acceso, el equipo elige el core y los plugins que lleva cada país.
SynKra verifica que la combinación sea compatible y la despliega directamente en el clúster. A
futuro, la idea es que automatice esos despliegues. Está pensada para equipos de plataforma que
mantienen un mismo producto en varios países.

**Tagline interno:** _"El core es uno; cada país elige sus plugins, y nada incompatible llega al
clúster."_

**Alcance de la primera iteración (adición a la plantilla, para sustentar viabilidad):**
un core en Java 21 / Spring Boot con las funciones básicas, plugins con dos etiquetas de versión
(la del plugin y la del core compatible),
dos plugins de país (Colombia y Perú), un motor que valida la selección y la despliega, y un
front de acceso en Blazor. Corre en un clúster local de `kind`. **SynKra no compila ni usa Git:
parte de artefactos ya compilados.** Sin IA, sin multi-tenant y sin marketplace de plugins.

---

## 1. PROBLEMA

Una empresa que despliega el mismo producto en varios países no mantiene un solo sistema:
mantiene **un núcleo común más las diferencias de cada país**. El patrón es real. Oracle
FLEXCUBE separa en su instalación los objetos del **kernel** de los del **cluster** de cada país,
y Temenos vende "Country Model Banks" sobre un núcleo común (`docs/overview.md`, §2).

Cuando las variantes se mantienen como ramas o repos por país (*clone-and-own*), aparecen tres
costos:

- **Un cambio del core hay que replicarlo a mano en cada país.** Es la práctica que Thoughtworks
  desaconseja por los riesgos de las ramas de larga vida (Radar, Gitflow en "Hold").
- **Nadie tiene una vista única de qué tiene cada país.** La respuesta vive en la cabeza de dos
  o tres personas, o en una hoja de cálculo.
- **Armar una variante nueva es trabajo de YAML experto.** Quien decide qué funciones tiene un
  país (producto, negocio) no puede armarla ni revisarla sin un ingeniero.

El patrón micro-kernel resuelve el primer costo: lo común vive en un solo core y cada país aporta
plugins (Richards, *Software Architecture Patterns*, 2022). Pero **no resuelve los otros dos**.
Hay que saber qué plugins hay, cuáles son compatibles con qué versión del core y cuáles pide cada
país, y hay que garantizar que una combinación inválida no llegue al clúster.

El dolor de fondo está medido: "Too complex to understand or run" es el reto n.º 1 al usar
proyectos CNCF en producción (46%, y subió 13% interanual) (CNCF Annual Survey 2024, fig. 15).

**Lo que NO es el problema.** No es desplegar en Kubernetes: Argo CD, Kustomize y Helm lo
resuelven gratis, y Argo CD soporta Kustomize components desde la v2.10. No es construir un
portal de desarrolladores: eso es Backstage. El hueco es más estrecho: **un contrato de plugins
para una aplicación micro-kernel, con verificación de compatibilidad antes de desplegar y una
vista por país que alguien que no sea experto pueda usar**.

**¿Este problema sobrevive a las próximas 2-3 generaciones de modelos foundation?**

**[X] Sí, porque es un problema de WORKFLOW/INTEGRACIÓN, no de OUTPUT**

Ningún modelo sabe qué plugins compiló esta empresa, con qué versión del core es compatible cada
uno ni qué pidió cada país. Esa información vive en el compilado y en el clúster, y la verificación de
compatibilidad es una regla determinista, no una generación.

**Durability Score: 3/5**

No es 4 porque el motor de abajo ya está comoditizado (Kustomize components, Helm con
condiciones) y KubeVela ofrece una interfaz web para usuarios que no son expertos en Kubernetes.
Lo durable es el contrato y la verificación. El front es fácil de copiar.

---

## 2. SEGMENTO TARGET

**¿Para quién es este producto?**

**Beachhead:** equipos de plataforma o de entrega de **3 a 15 ingenieros**, en empresas de
software que despliegan **un mismo producto en 2 o más países**, que ya operan Kubernetes y que
quieren dejar de mantener una rama o un repo por país. Vertical inicial: vendors de software
financiero y fintechs con presencia en Colombia y Perú (`docs/icp.md`).

Señales de que una empresa está en el beachhead:

1. Tiene Kubernetes en producción y algo de GitOps, aunque sea a medias.
2. Hoy cada país es una rama, un repo o un fork.
3. "Qué tiene activado el país X" se responde preguntándole a una persona.
4. Hay gente de producto que necesita armar o revisar una variante sin escribir YAML.

**NO es para:** productos de un solo país; equipos sin Kubernetes; equipos que ya resolvieron la
variación con Helm o Kustomize y no quieren otra capa; ni organizaciones que ya invirtieron en
Backstage o KubeVela y están contentas.

**¿Quién controla el veto de confianza?**

1. **El Head of Platform o SRE Lead.** Su pregunta: _"¿esto añade una pieza que tengo que operar,
   con permisos de escritura sobre mi clúster?"_ La respuesta tiene que ser verificable: la
   cuenta de servicio de SynKra solo puede crear y cambiar recursos en sus namespaces
   (`synkra-*`), y eso se comprueba con `kubectl auth can-i`.
2. **El arquitecto de software dueño del core.** Su pregunta: _"¿un plugin puede romper el
   core?"_ La respuesta es la etiqueta de versión del core compatible de cada plugin y el
   rechazo al arrancar si un plugin no cumple.

El Product Owner de país es el **usuario**, no el veto.

---

## 3. VENTAJA COMPETITIVA PRIMARIA

**[X] Trust**: ofrecemos una confiabilidad que otros no pueden igualar

Concretamente: **ninguna combinación de plugins incompatible llega al clúster**, y eso se puede
comprobar.

1. **Doble verificación de compatibilidad (desde el día 1).** Cada plugin es un **contenedor
   lógico**: un artefacto compilado con dos etiquetas de versión, la suya y la del core compatible.
   El motor verifica la selección **antes** de desplegar, y el core la vuelve a verificar
   **al arrancar**: detecta qué plugins trae el compilado, los compara con la lista que pide el
   despliegue y no arranca si falta uno o si es incompatible.
2. **Despliegue acotado (desde el día 1).** Una combinación que no pasa la verificación nunca
   se aplica. SynKra solo tiene permisos en sus namespaces, y cada despliegue queda registrado
   (quién, cuándo, qué core y qué plugins), con la opción de volver al anterior.
3. **Una sola vista por país (se construye operando).** El front muestra, leyendo el propio
   clúster, qué core y qué plugins corren en cada país. La hoja de cálculo deja de existir.

**Por qué no elegí Data:** SynKra no genera datos únicos.
**Por qué no elegí Distribution:** no controlo ningún canal. Me monto en Kubernetes, que es de
otros.

---

## 4. ARENA COMPETITIVA

**[X] Enhancer**, con una salvedad: la plantilla define las tres arenas en función de la IA, y
SynKra no usa IA. La lectura que sí aplica es la de fondo: SynKra **mejora un workflow que ya
existe** (armar y desplegar variantes por país) sin crear un mercado nuevo.

**Cómo sobrevivo o complemento a los gigantes:**

- **Contra Kustomize y Helm: no compito, me monto encima.** SynKra puede usar Kustomize para
  generar los manifiestos de cada despliegue.
- **Contra Argo CD y el flujo GitOps:** es la alternativa que muchos equipos ya usan; corre en
  cerca del 60% de los clústeres encuestados (CNCF, 24-jul-2025). SynKra **no pasa por Git**:
  despliega artefactos ya compilados. Es más simple para un demo, pero renuncia a la revisión y
  al historial que da Git (sección 9).
- **Contra KubeVela (CNCF Incubating):** es el competidor más cercano. Modela servicios con OAM
  y tiene una interfaz web, VelaUX. SynKra modela **un core y sus plugins con un contrato de
  versiones**. Si esa diferencia no le importa a un comprador, KubeVela gana.
- **Contra Backstage y Port:** cubren el "elijo y despliego" con plantillas y formularios. Port
  es gratis hasta 5 usuarios. Ninguno conoce el contrato de un micro-kernel.

**Riesgo honesto de esta arena:** el piso de precio es USD 0 y el competidor más cercano es un
proyecto de la CNCF. SynKra es defendible como proyecto académico y como capa delgada y opinada
encima de lo que ya existe, no como producto que desplace a esos actores (`docs/critica.md`, §8).

---

## 5. UX PARADIGM

**[X] Assistant**, entendido sin IA: **el usuario está en control y la plataforma valida y
propone**.

**Por qué este paradigma:** quien arma la variante decide qué plugins lleva su país. SynKra no
decide nada: verifica que la combinación sea válida, explica por qué una no lo es ("el plugin
`kyc-pe 2.0` es compatible con el core `3.x`, y el core es el `2.1`") y, cuando el usuario confirma,
despliega.

**Qué hace sin preguntar:** descubrir los plugins disponibles para cada core y sus versiones,
verificar la compatibilidad mientras el usuario elige, y mostrar qué corre en cada país.

**Qué no hace nunca:** desplegar sin la confirmación del usuario, desplegar una combinación que
no pasó la verificación, ni tocar recursos fuera de sus namespaces.

---

## 6. AI DECISION TRIANGLE

**No aplica: SynKra no usa IA (desviación declarada).**

**Por qué la IA no aporta aquí**, que es lo que esta sección tiene que defender:

- **La decisión central es determinista.** "¿Este plugin es compatible con este core?" se
  responde comparando versiones de un contrato. Un modelo añadiría la posibilidad de equivocarse
  en el único paso que tiene que ser verificable.
- **La ventaja es la confianza (sección 3).** Prometer que "nada incompatible llega al clúster"
  solo es creíble si el verificador es una regla que se puede probar, no una inferencia.
- **Añadir una IA para cumplir el requisito del curso** (por ejemplo, "sugerir plugins") sería
  exactamente la IA decorativa que el curso dice que no cuenta: si se quitara, el producto
  funcionaría igual.

**Prioridad equivalente sin IA:** optimizo para **corrección verificable**, por encima de la
velocidad de armado y de la riqueza visual del front.

**Trade-offs que acepto:** un front más austero; menos flexibilidad (solo se despliega lo que
cumple el contrato); y perder puntos en el criterio de "coherencia técnica" del curso.

---

## 7. MODELO ECONÓMICO

**Contexto:** SynKra es un proyecto académico y no tiene un modelo de cobro validado. Lo que
sigue es una hipótesis **[INTERNO]**.

**Modelo de pricing:** **[X] Freemium / Reverse Trial**, en forma de *open core*: el core, el
contrato y el motor serían abiertos, y se cobraría por soporte y por funciones de empresa (SSO,
auditoría, varios clústeres).

**Por qué no otro modelo:** el piso del mercado es USD 0 (Kustomize, Argo CD, KubeVela,
Backstage, el plan gratis de Port). Un producto cerrado y de pago desde el inicio no tiene con
qué competir.

**¿El pricing escala si tienes 10x usuarios?** **[X] Necesita ajuste**

SynKra corre en el clúster del cliente, así que su costo marginal no es infraestructura: es
soporte. Escala solo si la instalación es reproducible (un comando sobre `kind` o sobre un
clúster existente).

**Costo estimado por usuario/mes, revenue y margen:** **no se rellenan.** No hay datos para
estimarlos sin inventar, y la plantilla del curso pide no rellenar con humo.

---

## 8. MÉTRICAS DE ÉXITO

**North Star:** **% de despliegues por país hechos desde SynKra en los que ningún plugin
incompatible llegó al clúster.** _Target: 100%._ **[INTERNO]**

**Métricas de usuario:**

1. **Tiempo para desplegar la variante de un país nuevo**, desde que se abre el front hasta que
   los pods están listos, frente a hacerlo a mano con YAML y `kubectl`. _Target: -50% en el piloto._ **[INTERNO]**
   No existe un dato público de partida: se mide en el piloto (`docs/mercado.md`, §5).
2. **Adopción:** % de despliegues por país que se hacen desde SynKra frente a hacerlos a
   mano. Esta métrica existe por el riesgo de adopción de los portales (sección 9).
   _Target: >70% al final del piloto._ **[INTERNO]**

**Métricas de verificación** (sustituyen a las "métricas específicas de IA"):

1. **Combinaciones inválidas detectadas antes de desplegar**, sobre una suite de casos conocidos
   (plugin ausente, versión del core incompatible, plugin en la carpeta de otro core).
   _Target: 100%._
2. **Concordancia entre el motor y el core:** toda combinación que el motor aprueba, el core la
   acepta al arrancar, y viceversa. _Target: 0 discrepancias._

---

## 9. RIESGOS CRÍTICOS

**1. ¿Qué pasa si el problema desaparece en 12 meses por comoditización?**

**Riesgo alto, y en parte ya ocurrió.** Seleccionar componentes por overlay ya es gratis (Argo
CD ≥ v2.10 con Kustomize components, Helm con condiciones), y KubeVela ofrece composición con
interfaz web. Si KubeVela o Backstage añaden un modelo de "core + plugins con contrato", la
diferenciación de SynKra desaparece.

_Mitigación:_ no competir en desplegar, sino en el **contrato y la verificación**, y montarse
encima de Kubernetes y Kustomize en lugar de reemplazarlos.

**2. ¿Puede un competidor replicar tu producto con la misma API en menos de 6 semanas?**

**Sí, casi todo.** Un front que despliega un `kustomization.yaml` se hace en semanas. Lo que cuesta
más replicar es una verificación de compatibilidad entre core y plugins con la doble
verificación probada, y
ni siquiera eso es un foso fuerte. Aceptarlo es parte de la honestidad que pide esta sección.

**3. Si tienes éxito a escala, ¿cuál es la primera forma en que se rompe la confianza?**

En orden de probabilidad:

1. **El motor y el core no coinciden.** El motor aprueba una combinación que el core rechaza
   al arrancar, o al revés. El despliegue sale y el pod no arranca. _Mitigación:_
   una sola librería de verificación, compartida por el motor y el core, y la métrica de
   concordancia de la sección 8.
2. **Un plugin no activado se ejecuta.** El compilado de cada país trae el código de todos
   los países. Si el aislamiento falla, se ejecuta código de otro país. _Mitigación:_ un
   cargador separado por plugin, y una prueba de que un plugin no activado no carga clases ni
   expone rutas.
3. **SynKra se vuelve el punto único de falla con privilegios.** Despliega directo en el
   clúster, sin un PR que alguien revise. Si el front se compromete, alguien puede desplegar
   lo que quiera. _Mitigación:_ acceso al front con autenticación; RBAC limitado a los
   namespaces `synkra-*`; solo se despliegan imágenes de un registro permitido; y un registro de
   cada despliegue. Cada punto se comprueba con un comando.
4. **Sin Git, se pierde el historial.** Nadie revisa el cambio antes de que salga, y "qué
   corría ayer en Perú" solo se sabe si SynKra lo registró. _Mitigación:_ el registro de
   despliegues y la opción de volver al anterior son Must, no extras.

**Riesgos adicionales del proyecto (adición a la plantilla):**

5. **Adopción.** Los portales de plataforma tienen una adopción interna baja: según TechTarget,
   Spotify estimó públicamente que la adopción media de Backstage es del 10% (TechTarget,
   7-nov-2023). Una capa más puede empeorar la complejidad que dice reducir, y el reto n.º 1
   con contenedores es cultural (46%, CNCF Annual Survey 2024). Por eso la adopción es una
   métrica (sección 8).
6. **Sin IA, se incumple una condición del curso.** Se declaró como desviación y se justifica en
   la sección 6, pero puede costar puntos en el criterio de "coherencia técnica".
7. **Alcance frente a tiempo.** La sustentación se movió a la semana siguiente al 21-nov-2026
   **[VERIFICAR fecha exacta]**. Cuatro fases de construcción no caben en ese plazo. El MVP se
   recorta a un core, dos plugins y un front mínimo.

---

## Checklist de entrega

- [x] Producto seleccionado: corre sobre Kubernetes. **No usa IA: desviación declarada**
- [x] Investigación de validación completada (`../Modulo 3/docs/mercado.md`, `overview.md`)
- [x] Investigación de crítica completada (`../Modulo 3/docs/critica.md`)
- [x] Investigación documentada en Markdown, con fuentes y las marcas [INTERNO] y [VERIFICAR]
- [x] Product Vision Board completado (este documento)
- [ ] `prd.md` co-creado a partir de este PVB, segmento por segmento
