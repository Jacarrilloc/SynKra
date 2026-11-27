# U02-T04 — respuesta a la ronda 1
F-01: Corregido en 7172570: la prueba usa `- !!java.util.HashMap {nombre: kyc-co, version: 1.0.0}`, con control positivo (sin etiqueta arranca) y lee de vuelta el motivo (causa raíz ArranqueRechazado: "etiqueta de tipo 'tag:yaml.org,2002:java.util.HashMap' no permitida"). Rojo previo: 3 fallos contra el código anterior.
F-02: Corregido en 7172570: constantes documentadas CORE, MAX_BYTES, MAX_ALIAS y MAX_PROFUNDIDAD en ArranquePlugins (una sola fuente en core/); el lector aplica profundidad máxima 32 y rechaza etiquetas explícitas que no sean la implícita (como etiquetaExplicitaNoStr de U01). Pruebas: yamlDemasiadoAnidadoSeRechaza, etiquetaExplicitaQueNoEsStrSeRechaza, yamlDemasiadoGrandeSeRechaza. verificador/ intacto. Candidata (del orquestador): exponer el lector seguro desde verificador/.
F-03: No es un defecto del codificador: es de la especificación; el --stat se toma contra la base ceb914c.
F-04: Fuera de alcance: tocaría verificador/ o gradle/; candidata del orquestador. No se tocó.
F-05: Corregido en 7172570: import de Component en CargadorPlugins.
F-06: Corregido en 7172570: s4 comprueba "VERSION_INCOMPATIBLE plugin=kyc-pe" y s6 "PLUGIN_AUSENTE plugin=kyc-mx".
