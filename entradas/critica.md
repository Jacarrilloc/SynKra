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

- Alucinación medida en preguntas sobre casos judiciales federales de EE.UU.: los modelos
  públicos alucinan al menos 58% de las veces según la versión publicada (Dahl, Magesh, Suzgun &
  Ho, "Large Legal Fictions", Journal of Legal Analysis, 2024,
  [Stanford Law](https://law.stanford.edu/publications/large-legal-fictions-profiling-legal-hallucinations-in-large-language-models));
  el preprint reporta un rango de 69% (GPT-3.5) a 88% (Llama 2)
  ([arXiv 2401.01301](https://arxiv.org/abs/2401.01301v1)).
- Herramientas legales comerciales con RAG (Lexis+ AI, Westlaw) alucinan entre 17% y 33%
  (Magesh et al., "Hallucination-Free?", Journal of Empirical Legal Studies 22:216-242, 2025,
  doi 10.1111/jels.12413,
  [Stanford Law](https://law.stanford.edu/publications/hallucination-free-assessing-the-reliability-of-leading-ai-legal-research-tools)).
- Ambos son benchmarks de derecho de EE.UU. con modelos de 2023-2024, no de código.
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
