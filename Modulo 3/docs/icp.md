# Ideal Customer Profile: SynKra

> Hipótesis explícita y falsable para el alcance vigente desde el 21-nov-2026. No es un
> hallazgo de investigación: se valida con entrevistas **[INTERNO]**.

## Beachhead

Equipos de plataforma o de entrega de **3 a 15 ingenieros**, en empresas de software que
despliegan **un mismo producto en 2 o más países** (o en variantes de cliente). Ya operan
Kubernetes y quieren dejar de mantener una rama o un repo por variante. Vertical inicial:
vendors de software financiero y fintechs con presencia en Colombia y Perú.

### Señales de calificación

1. Tienen Kubernetes en producción. No hace falta GitOps: SynKra despliega directo (ajuste del 21-nov-2026).
2. Hoy cada país es una rama, un repo o un fork con su propio pipeline.
3. Cuando alguien pregunta "qué tiene activado el país X", la respuesta está en la cabeza de
   una persona o en una hoja de cálculo.
4. Hay gente de producto o de soporte que necesita armar o revisar un despliegue sin escribir
   YAML.

### Fuera del ICP

- Productos de un solo país y una sola variante: no hay nada que componer.
- Equipos sin Kubernetes.
- Equipos que ya resolvieron la variación con Helm o Kustomize y no quieren otra capa.
- Organizaciones que ya invirtieron en Backstage o KubeVela y están contentas.

## Buyer personas

| Persona | Rol | Qué le importa | Poder |
|---|---|---|---|
| Head of Platform / SRE Lead | Dueño del clúster y de GitOps | "¿Esto añade una capa que tengo que operar?" | **Decisor y veto** |
| Arquitecto de software | Dueño del core y del contrato de plugins | Que un plugin no pueda romper el core | **Veto técnico** |
| Product Owner de país | Decide qué funciones tiene su país | Ver y armar su variante sin depender de un ingeniero | Usuario |
| Desarrollador de plugins | Construye un plugin | Un contrato claro, un entorno local que levanta en minutos | Usuario |

## Pains

- Un cambio en el core hay que replicarlo a mano en cada rama de país.
- Nadie tiene una vista única de qué plugin y qué versión corre en cada país.
- Armar una variante nueva toma días de YAML y de revisiones.

## Triggers de compra

- Se abre un país nuevo y hay que montar su variante.
- Un país se quedó con una versión vieja del core por un merge olvidado.
- Rota la persona que sabía cómo estaba armado cada país.

## Objeciones probables

| Objeción | Respuesta |
|---|---|
| "Eso ya lo hago con Kustomize components o con Helm" | Cierto: SynKra los usa por debajo. Añade el contrato de plugins, la validación de compatibilidad y la vista por país. Si eso no vale la pena para el equipo, no es ICP |
| "¿Por qué no KubeVela?" | KubeVela modela servicios con OAM. SynKra modela un core y sus plugins, con un contrato de versiones. Diferencia por validar en entrevistas **[VERIFICAR]** |
| "Yo despliego con GitOps y reviso PRs" | SynKra no pasa por Git: despliega artefactos ya compilados. Para un equipo que exige PRs, hoy no es la herramienta, y se dice así |
| "Es otra pieza con permisos de escritura en mi clúster" | Sus permisos se limitan a los namespaces `synkra-*`, solo despliega imágenes de un registro permitido y registra cada despliegue. Es verificable (ver `critica.md`, §5) |
