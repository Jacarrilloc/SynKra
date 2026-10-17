# Decisiones del módulo 5 — SynKra

> BORRADOR para revisión de Julian. Reescríbelo con tus palabras antes de entregarlo; la guía pide
> que sea "de tu puño y letra".

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

PENDIENTE: completar después de pedir el cambio sobre `unidades-y-tareas.md` en la sesión de
AI-DLC. Copiar aquí el texto exacto que se pidió y por qué.

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
