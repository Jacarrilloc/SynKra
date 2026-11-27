# U02-T06 — respuesta a la ronda 2
F-06: Corregido en d9f9e0c: caso «a, b que falla, c» espera exactamente [iniciar:a, iniciar:b, detener:a]; el mutante sin `return` ahora muere (línea 169, `ronda3-mutante-sin-return.txt`).
F-07: Corregido en d9f9e0c: `Health.outOfService()` y aserción sobre el cuerpo (OUT_OF_SERVICE en readiness, UP en liveness 200); el mutante `Health.down()` muere (línea 138). El agregado /actuator/health refleja el fallo por decisión del orquestador, documentado en bitácora y Javadoc, y afirmado en la prueba (503).
F-08: Corregido en el commit de bitácora de esta ronda: cada cita de evidencia borrada lleva «en d2554a3».
F-09: Corregido: `ca1.txt` regenerado sobre el código final e incluye la salida del grep del XML (tests="7" skipped="0" failures="0" errors="0").
F-10: Corregido en d9f9e0c: constante `DETALLE_FALLO` y el detalle nombra el plugin que falló.
Candidata del agregado: resuelta por la decisión del orquestador. Candidata de cierre de contexto (detener a los iniciados): sigue abierta, fuera de alcance.
