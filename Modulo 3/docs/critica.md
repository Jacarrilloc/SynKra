# Crítica adversarial — por qué SynKra podría fracasar

> Investigación en contra del producto. Viene de las secciones 4, 6 y 9 del PVB y se amplía
> con la nueva dirección micro-kernel. Esta crítica **no** se resuelve a favor del optimismo.

## 1. El micro-kernel le quita parte de la razón de ser al agente

El PVB vendía el agente para **propagar** cambios entre ramas por país. Con un kernel y plugins,
un cambio en el kernel llega a todos los países sin propagar nada. Lo que queda es localizar
**qué plugin** tocar, que es un problema más pequeño y que un buen equipo resuelve leyendo el
código. Si el número de plugins es bajo (2 a 5), el agente aporta poco.

## 2. Comoditización (PVB riesgo #1)

- Rovo crea work items desde documentos; Copilot y Cursor hacen ticket → PR.
- Kustomize, Argo CD ApplicationSets y PF4J resuelven gratis el despliegue y los plugins.
- Un equipo competente conecta un LLM al repositorio y al tracker en un mes (PVB riesgo #2).
- Harness tiene la mitad de despliegue y solo le falta el frente de entrada.

**Lo que queda propio:** el contrato de puntos de extensión regulatorios del kernel y un agente
con límite de autonomía verificable. Es poco, y hay que decirlo.

## 3. La IA se equivoca y alguien lo aprueba

- Alucinación medida en preguntas legales: 58% GPT-4, 88% Llama 2 (Dahl et al., "Large Legal
  Fictions", Journal of Legal Analysis 16(1), 2024); herramientas legales con RAG fallan entre
  17% y 33% (Magesh et al., JELS 22, 2025). **[VERIFICAR: enlace]** Son benchmarks de derecho
  de EE.UU., no de código.
- Fatiga de aprobación: el revisor aprueba en bloque y el "humano en el bucle" se vuelve ficción.
- Falso negativo silencioso: el agente no reporta un plugin afectado y nadie lo nota.

## 4. Usar Claude contradice la ventaja de Trust

El PVB dice que el código nunca sale del perímetro. Llamar a la API de Claude envía código y
texto a un proveedor externo. Para un banco real eso es un veto del CISO. El MVP del curso lo
acepta, pero **el argumento de venta del PVB no se cumple en modo Claude**.

## 5. Inyección indirecta de prompts

El agente lee commits, issues y documentos que un tercero puede escribir. Un texto malicioso
puede intentar que el agente modifique otra cosa. Mitigación estructural: el agente no puede
fusionar ni escribir fuera de `synkra/*`.

## 6. Alcance frente a tiempo

Quedan 5 semanas hasta la sustentación (21-nov-2026) y no hay código. El PVB proponía CRDs con
controladores, GPU, OpenTelemetry y multi-tenancy. Intentar todo eso garantiza no terminar.
