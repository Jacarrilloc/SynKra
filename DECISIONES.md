# Decisiones del módulo 5

> Borrador para que el responsable del proyecto lo revise y lo ajuste con sus palabras.

## 1. Dos cosas que el marco me obligó a decidir y que el PRD no había decidido

- **Quién crea los namespaces de los países.** El PRD decía que SynKra despliega en
  `synkra-<país>`, pero no decía quién crea ese namespace. Al diseñar el RBAC salió el problema:
  Kubernetes no deja limitar `create namespaces` por nombre. Si SynKra pudiera crear namespaces,
  podría crear cualquiera. Decidí que los namespaces se crean al instalar (supuesto S1). Así,
  `synkra:deployer` tiene un Role en cada país y ningún permiso de clúster, y
  `make verificar-limite` lo comprueba.
- **Quién puede revertir.** La pregunta de las personas dejó ver una contradicción. El §3 del PRD
  dice que el SRE es "lector más reversión", y el §9.2 dice que el Lector no cambia nada. Apliqué
  el §9.2: revertir es un despliegue más y exige el rol Desplegador. Un SRE que revierte tiene los
  dos roles.

## 2. Una vez que pedí cambios

En las historias, el marco les puso nombres propios a las personas. Pedí: *"saque esos nombres,
solo dejalo general, no queremos nombres propios, sin nombres"*. Lo pedí porque es un ejercicio
académico: los nombres sugerían usuarios reales que no existen, y el rol ya dice todo lo que
importa. Se quitaron de `personas.md`, de `stories.md` y de los journeys del PRD. También pedí
*"cambia maven por gradle"* en Application Design, y se cambiaron los comandos de todos los
criterios.

## 3. Cómo quedó el grafo de dependencias

El grafo tiene 11 unidades y 21 dependencias, y `scripts/verificar-grafo.py` confirma que no hay
ciclos. Hay dos raíces que se pueden construir en paralelo: **U01 Verificador** y **U03
Plataforma del backend**. Después, U02, U04 y U05 tampoco dependen entre sí. El camino crítico es
U03 → U05 → U06 → U07 → U08 → U09.

## 4. Qué riesgo de `critica.md` cambió el orden de entrega

En la sección 3 de la crítica, el riesgo "la validación solo ocurre al arrancar" decía que el
error de un plugin incompatible aparece en el pod y no antes, y que el motor de despliegue debía
hacer la misma verificación **antes** de desplegar (R1 en el PRD). Por eso el verificador es una
librería aparte, es la primera unidad y no depende de nada: el core (U02) y el despliegue (U07)
la consumen en vez de copiar la regla. Además, el riesgo de alcance de la sección 6 (R9) dejó las
Should (U10 y U11) al final, en unidades que se pueden cortar sin tocar la demo.
