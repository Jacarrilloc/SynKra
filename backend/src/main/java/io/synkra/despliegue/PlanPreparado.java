package io.synkra.despliegue;

/** Un plan recién guardado, con su resumen y sus cambios ya como objetos. */
record PlanPreparado(Plan plan, Resumen resumen, Cambios cambios) {}
