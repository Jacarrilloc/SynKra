# Investigación de crítica (adversarial): SynKra

> El objetivo de este documento es **atacar la idea**: por qué fallaría, quién ya la da gratis
> y qué riesgo la mata. Corresponde al alcance vigente desde el 21-nov-2026 (plataforma de
> despliegue micro-kernel con front de arrastrar y soltar, sin IA) y sustituye la crítica del
> 17-oct-2026.
>
> **[INTERNO]** marca un juicio propio. **[VERIFICAR]** marca lo que no se pudo confirmar.
> Las fuentes se consultaron el 21-nov-2026.

## TL;DR: por qué SynKra podría fracasar

1. **El motor ya es gratis.** Kustomize components + Argo CD hacen la selección de plugins por
   variante, sin SynKra. Lo único nuevo es el front, y un front es fácil de copiar.
2. **El problema principal no es de herramientas.** La CNCF reporta que el reto n.º 1 es
   cultural (46%). Una herramienta más puede empeorar la complejidad que dice reducir.
3. **"Micro-kernel" puede ser una etiqueta.** Si los plugins son contenedores, SynKra es otro
   gestor de microservicios. Si son librerías, el despliegue implica reconstruir la imagen.
4. **Sin IA, no cumple una condición del curso.** Se declaró como desviación, pero pesa en el
   criterio de coherencia técnica.
5. **El alcance no cabe en el tiempo.** Cuatro fases de construcción: core, motor, front y
   migración de dos países.

## 1. Comoditización: ya ocurrió

- Argo CD soporta Kustomize components en la Application desde la v2.10.0
  ([docs](https://argo-cd.readthedocs.io/en/stable/user-guide/kustomize/)). "Elegir qué
  plugins tiene un país" se reduce a una lista en un YAML.
- Helm hace lo mismo con `condition` y `tags` en los subcharts
  ([Helm](https://helm.sh/docs/chart_best_practices/dependencies)).
- KubeVela ya ofrece entrega de aplicaciones por componentes, con una interfaz web para
  usuarios que no son expertos en Kubernetes, y está en la incubadora de la CNCF desde el
  27-feb-2023 ([CNCF](https://www.cncf.io/blog/2023/02/27/kubevela-brings-software-delivery-control-plane-capabilities-to-cncf-incubator/)).
- Port ofrece autoservicio gratis hasta 5 usuarios ([Port](https://port.io/pricing)).

**Consecuencia:** el valor de SynKra no puede estar en desplegar. Tiene que estar en el
**contrato de plugins y en la validación de compatibilidad**. Si eso no se construye bien, el
producto es un formulario bonito encima de un `kustomization.yaml`.

## 2. Una capa más contra la complejidad

- "Too complex to understand or run" es el reto n.º 1 al usar proyectos CNCF en producción
  (46%, y subiendo 13% interanual)
  ([CNCF Annual Survey 2024](https://www.cncf.io/wp-content/uploads/2025/04/cncf_annual_survey24_031225a.pdf)).
- Los portales de plataforma tienen un problema de adopción conocido: según TechTarget,
  "Spotify officials have estimated publicly that the average Backstage adoption rate is 10%"
  ([TechTarget, 7-nov-2023](https://techtarget.com/searchitoperations/news/366558592/Behind-the-scenes-Spotify-Backstage-a-work-in-progress)).
  Un análisis escrito por un vendor competidor afirma que mantenerlo exige "usually 4+, and
  often 15+ at scale" personas
  ([Earthly, 15-jul-2025](https://earthly.dev/blog/backstage-is-at-peak-hype/); fuente con
  conflicto de interés).

**Consecuencia:** SynKra añade un servicio, un front y un contrato más. Si el equipo no lo
adopta, es costo puro. El PRD tiene que medir la adopción, no solo si la plataforma funciona.

## 3. El micro-kernel, examinado en frío

La decisión de si un plugin es una librería o un contenedor sigue abierta, y las dos salidas
tienen un costo:

| Si el plugin es... | Qué pasa | Riesgo |
|---|---|---|
| **Librería** en el proceso del core (por ejemplo, PF4J) | Elegir plugins implica construir o montar una imagen por variante | El "despliegue por arrastrar y soltar" se vuelve un *build*. Más lento y más frágil |
| **Contenedor** aparte | Cada plugin es un Deployment que habla con el core por red | Es una arquitectura de microservicios con otro nombre. El "micro-kernel" es decorativo |

Además, el contrato de plugins se tiene que versionar. Un cambio incompatible en el core rompe
todos los plugins de todos los países a la vez **[INTERNO]**.

## 4. Sin IA: la desviación tiene un costo

El curso pone como condición no negociable que el producto use IA de forma defendible, y
evalúa que "Kubernetes e IA sean centrales, no decorativos" (módulo 2, instrucciones de
entrega). SynKra decidió **no** usar IA. Es una decisión honesta, pero:

- el criterio de **coherencia técnica** queda incumplido a medias, y
- el PVB (§6) y el PRD (segmento 11) tienen que explicar **por qué** una IA no aporta aquí, en
  lugar de dejar esas secciones vacías.

## 5. Riesgo de seguridad que el diseño no elimina

Una plataforma que escribe en Git y dispara despliegues tiene privilegios altos:

- Si el front o la API se comprometen, un atacante puede proponer un overlay que despliegue
  una imagen arbitraria.
- Si SynKra tiene credenciales para escribir en `main` o para aplicar en el clúster, se
  convierte en el punto único de falla.

**Mitigación que el PRD debe hacer verificable:** SynKra solo escribe en ramas de propuesta y
abre PRs; `main` está protegida; Argo CD es quien aplica, y SynKra no tiene un kubeconfig con
permisos de escritura. Cada punto se comprueba con un comando, no con una promesa.

## 6. Alcance frente a tiempo

Según el cronograma del curso, la sustentación del proyecto final es el sábado
**21-nov-2026**, la misma fecha en que se redefinió el alcance. Las fases 1 a 4 (core y
contrato, motor de despliegue, front y API, migración de CO y PE) son trabajo de semanas
**[INTERNO]**. Si no se recorta, el riesgo más probable es llegar con cuatro fases a medias
en lugar de una que funcione de punta a punta.

**Corte propuesto para el PRD:** un core, **dos** plugins y un front mínimo que genere el
overlay y abra el PR. Todo lo demás queda en Could o Won't.

## 7. Qué cambiaría del PVB

- La ventaja competitiva no es "desplegar con arrastrar y soltar". Es el contrato de plugins
  más la validación de compatibilidad.
- El segmento debe excluir explícitamente a los equipos que ya están contentos con Helm,
  Kustomize, KubeVela o Backstage.
- Las métricas deben incluir la **adopción** (cuántas variantes se arman con SynKra frente a
  editar YAML a mano).

## 8. Veredicto

La idea es **construible y defendible como proyecto académico** si se presenta como lo que es:
una capa delgada y opinada sobre Kustomize y Argo CD, cuyo aporte es el contrato micro-kernel
y la validación. **No es defendible como producto comercial** frente a KubeVela, Backstage y
Port sin evidencia de entrevistas que hoy no existe **[INTERNO]**.

## 9. Afirmaciones descartadas o sin verificar

- La cifra "Backstage tiene 89% de cuota de mercado" circula en foros, pero no se encontró una
  fuente primaria. No se usa **[VERIFICAR]**.
- La predicción de Gartner del 80% se refiere a organizaciones **grandes**. No vale para
  empresas medianas de LatAm.
- Las desventajas del micro-kernel según Richards no se pudieron leer (muro de pago).
