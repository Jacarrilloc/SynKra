# Overview: panorama del dominio de SynKra

> Insumo del PRD (módulo 3). Describe el dominio del **alcance vigente desde el
> 21-nov-2026**: una plataforma de despliegue de aplicaciones micro-kernel sobre Kubernetes.
>
> Convenciones: toda cifra lleva fuente y fecha. **[INTERNO]** marca una proyección o un
> supuesto propio. **[VERIFICAR]** marca lo que no se pudo confirmar con una fuente primaria.
> Las fuentes se consultaron el 21-nov-2026, salvo las que indican "contrastada el 17-oct-2026".

## 1. Qué es SynKra, en una frase

Un **core** con las funciones básicas, y **plugins** (micro-kernels) que se eligen en un
front de arrastrar y soltar. La plataforma valida la combinación, genera la configuración de
despliegue y la aplica en un clúster de Kubernetes. El caso de uso de partida es un mismo
producto desplegado en varios países (Colombia y Perú), cada uno con solo los plugins que
necesita.

## 2. El dominio: un producto, varias variantes

Una empresa que despliega el mismo producto en varios mercados no mantiene un solo sistema:
mantiene un núcleo común más las diferencias de cada variante. Evidencia de que el patrón es
real (contrastada el 17-oct-2026):

- **Oracle FLEXCUBE** ofrece "localized clusters" por país sobre un núcleo común, y su guía de
  instalación separa los objetos del **kernel** de los del **cluster** de cada país
  ([Oracle](https://www.oracle.com/financial-services/banking/flexcube/core-banking-software);
  [guía de instalación FCUBS](https://docs.oracle.com/cd/F20443_01/PDF/Installation/FLEXCUBE_UBS_Installation/FCUBS%20Components/FCUBS/FCUBS_DB_Setup.pdf)).
  En la práctica es el patrón micro-kernel.
- **Temenos** vende "Country Model Banks" con funcionalidad regional
  ([Temenos](https://www.temenos.com/?p=6398)). Es material de marketing del propio vendor.
- **BBVA** creó un área global de software para que sus soluciones se escalen y compartan entre
  países
  ([BBVA](https://www.bbva.com/es/innovacion/bbva-crea-un-area-de-desarrollo-de-software-para-impulsar-las-soluciones-globales-y-escalables/)).

## 3. El patrón de arquitectura: micro-kernel

Mark Richards lo describe como "a flexible and extensible architecture that allows a developer
or end user to easily add additional functionality", formada por "a core system and plug-in
modules"
([Software Architecture Patterns, 2.ª ed., cap. 4, O'Reilly, jul-2022](https://www.oreilly.com/library/view/software-architecture-patterns/9781098134280/ch04.html)).

Frente a la alternativa *clone-and-own* (una rama o un repo por variante), que Thoughtworks
desaconseja por los riesgos de las ramas de larga vida
([Thoughtworks Radar, Gitflow en "Hold"](https://www.thoughtworks.com/radar/techniques/gitflow)),
el micro-kernel concentra lo común en un solo lugar. Su costo es que hay que diseñar y
versionar bien el **contrato** entre el core y los plugins. Las desventajas que Richards
detalla en la sección "Considerations and Analysis" no se pudieron leer porque están tras un
muro de pago **[VERIFICAR]**.

En Java existe soporte maduro para plugins dentro de un proceso: **PF4J**, "an open source
(Apache license) lightweight (around 100 KB) plugin framework for java", usado entre otros
por Netflix Spinnaker y Apache Tika ([GitHub pf4j/pf4j](https://github.com/pf4j/pf4j)).

> **Decisión tomada (21-nov-2026): el plugin es un contenedor lógico, no un contenedor de
> Docker.** Cada plugin es un artefacto compilado (un módulo empaquetado) que viaja junto al
> core. Al arrancar, el core **detecta** qué plugins trae el compilado, **verifica** que la versión
> del core compatible de cada plugin coincida con la del core, y los compara con la lista de plugins que el despliegue pide activar. Si el
> despliegue pide un plugin que no está, o uno incompatible, el core no arranca y dice por qué.
> El overlay de Kustomize solo lleva esa lista, así que cambiar los plugins de un país no obliga
> a reconstruir la imagen.

## 4. Dónde se monta: Kubernetes y GitOps

Las piezas de bajo nivel ya existen y son gratuitas:

- **Kustomize con components.** Argo CD soporta components de Kustomize directamente en la
  Application desde la v2.10.0 (`spec.source.kustomize.components`)
  ([docs de Argo CD](https://argo-cd.readthedocs.io/en/stable/user-guide/kustomize/)). Un
  plugin puede ser un component que se activa o no por variante.
- **Helm con condiciones.** La guía oficial indica que "Conditions or tags should be added to
  any dependencies that *are optional*"
  ([Helm, buenas prácticas de dependencias](https://helm.sh/docs/chart_best_practices/dependencies)).
- **Argo CD** corre en cerca del 60% de los clústeres de Kubernetes encuestados
  ([CNCF, 24-jul-2025](https://cncf.io/announcements/2025/07/24/cncf-end-user-survey-finds-argo-cd-as-majority-adopted-gitops-solution-for-kubernetes);
  contrastada el 17-oct-2026).

SynKra no reemplaza esas piezas: se monta **encima** de Kubernetes y puede usar Kustomize para
generar los manifiestos. A diferencia del flujo GitOps, **SynKra no pasa por Git** (decisión del
21-nov-2026): parte de un proyecto ya compilado, traduce "este core con estos plugins para este
país" en un despliegue y lo aplica directo en el clúster.

## 5. Dónde se ubica en la categoría: platform engineering

- Gartner predice que "By 2026, 80% of large software engineering organizations will establish
  platform engineering teams", frente al 45% de 2022
  ([Gartner](https://www.gartner.com/en/articles/what-is-platform-engineering); la página no
  muestra fecha de publicación). La predicción habla de organizaciones **grandes**.
- El portal de referencia es **Backstage**, en la incubadora de la CNCF desde el 15-mar-2022,
  con "Software Templates for quickly spinning up new projects"
  ([CNCF](https://cncf.io/blog/2022/03/15/backstage-project-joins-the-cncf-incubator)).
- El caso más cercano a SynKra es **KubeVela**, "an application delivery engine built with the
  Kubernetes control plane", basado en el Open Application Model, con una interfaz web
  (VelaUX) pensada para "non-k8s-expert users"
  ([CNCF, 27-feb-2023](https://www.cncf.io/blog/2023/02/27/kubevela-brings-software-delivery-control-plane-capabilities-to-cncf-incubator/)).
  Sigue en estado Incubating ([CNCF](https://www.cncf.io/projects/KubeVela)).

## 6. Marco de ejemplo para los plugins (CO y PE)

Los plugins de país del MVP (Fase 4) usan reglas de ejemplo:

| País | Protección de datos | Documento de identidad |
|---|---|---|
| Colombia | Ley 1581 de 2012 (Habeas Data) | Cédula de ciudadanía |
| Perú | Ley 29733 de 2011 (Protección de Datos Personales) | DNI |

Las reglas concretas son **simuladas** en el MVP, y así se declara **[VERIFICAR]**.

## 7. IA: fuera del producto (desviación declarada)

El curso pide que el producto use IA de forma defendible. **SynKra no la usa**: así se decidió
el 21-nov-2026. Ninguna función del producto depende de un modelo, y el PVB (§6) y el PRD
(segmento 11) lo declaran de forma explícita, en lugar de añadir una IA decorativa.

## 8. Qué no se pudo verificar

1. Las desventajas del micro-kernel según Richards (contenido tras muro de pago).
2. Si VelaUX permite componer aplicaciones arrastrando componentes: la fuente solo dice que es
   una interfaz web para usuarios que no son expertos en Kubernetes.
3. Cuántas empresas del segmento usan micro-kernel frente a ramas por país (sin dato público).
