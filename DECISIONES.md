# Decisiones del módulo 5 — SynKra

## 1. Dos cosas que el marco me obligó a decidir y que el PRD no había decidido

- **Cómo localiza el agente el plugin.** El PRD decía que el agente "localiza" el plugin, pero no
  cómo. AI-DLC lo preguntó (requisitos, Q4) y decidí que la localización sea **determinista**: el
  país y el tema del cambio se cruzan con el manifiesto de cada plugin, y Claude solo redacta el
  informe y el PR. Consecuencia: el modelo nunca decide qué archivo ni qué rama se toca, pero el
  papel de la IA es menor de lo que el PRD prometía (riesgo R3).
- **Cómo sabe el kernel de qué país es una petición.** El PRD hablaba de "un overlay por país"
  sin decir qué pasa con una petición de otro país. Quedó: cada despliegue atiende **un solo
  país** por configuración, la ruta lleva el país (`/kyc/co/...`) y si no coincide responde 404
  (diseño de aplicación Q4 y seguimiento F1).

## 2. Una vez que pedí cambios

El 24 de octubre pedí en la sesión de AI-DLC (queda en `aidlc-docs/audit.md`):

> "Request Changes: en unidades-y-tareas.md agrega al final una sección "Tareas para la primera
> ola del módulo 6" con las 2 primeras tareas de U03 y de U04. Registra esta petición en audit.md."

**Por qué:** el consolidado listaba 91 tareas sin decir por dónde empezar el loop de agentes del
módulo 6. Pedí dejar explícita la primera ola en paralelo (plugins y agente) para que el
orquestador no tenga que deducirla. Al hacerlo se vio algo que no esperaba: las dos primeras
tareas de U04 son **[PERSONA]** (proteger `main` y crear la identidad del agente en GitHub), así
que el loop no las puede despachar a un codificador: las tengo que hacer yo.

## 3. Cómo quedó el grafo de dependencias

Cinco unidades sin ciclos: `U01 scaffold → U02 core → U03 plugins y U04 agent → U05 deploy`.
**U03 (plugins) y U04 (agente) se pueden construir en paralelo**: el agente solo usa los
manifiestos de los plugins como datos y puede arrancar con archivos de prueba. Esa es la ola en
paralelo que se usará en el módulo 6.

## 4. Qué riesgo de `critica.md` cambió el orden de entrega

El riesgo de **inyección indirecta y de que la IA proponga algo equivocado** (`critica.md` §3 y §5).
Por eso, dentro de U04, las tareas de **proteger `main` y crear la identidad del agente en GitHub
van primero** (marcadas `[PERSONA]`, las hace un humano), antes de cualquier tarea en la que el
agente abra un PR. Y el riesgo de **alcance frente a tiempo** (§6) hizo que se omitieran las
etapas de diseño NFR e Infrastructure de U04 y U05 para llegar al plan de tareas.
