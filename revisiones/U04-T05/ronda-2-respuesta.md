# U04-T05: respuesta a la ronda 2

- F-05: Corregido en d05e8d0.
  - **Qué cambió:** la autorreferencia se calcula ahora en cada lectura, no sobre el nombre crudo. Una lectura no vacía hecha solo de `.` y `/` es autorreferencia; en la lectura con `\` como separador, `./\` ya es `./`. El `startsWith` sigue siendo estricto.
  - **Casos fijos:** `./\`, `.\` y `./\/` quedan como `@Example autoReferenciasConBarraInvertida_seAceptanYElOraculoLasDaDentro`. Antes del arreglo esa prueba estaba en rojo: `[el oráculo da dentro a './\'] Expecting value to be true but was false`. Se añade `@Example elNombreVacioNoEstaDentro`.
  - **Semilla `-4345582717168822446`, fijada en copias:** con b7dcce7 falla, reducida a `'./\/'`; con d05e8d0 pasa, `tests="4" failures="0"`.
  - **Estabilidad:** 30 corridas de CA-1 en el worktree, borrando `backend/.jqwik-database` entre una y otra, y las 30 en verde.
  - **P7:** sigue muriendo, reducido a `'' (0x7075 '')`.
- F-06: Corregido en d05e8d0. Antes y después de cada intento, `recibir` lista `java.io.tmpdir` filtrado a `recepcion-*` y exige que no aparezca ninguno nuevo.
  - **r3-D3 muere:** el temporal movido al abuelo de `datos` hace fallar las 2 propiedades con `recepcion-* nuevos en java.io.tmpdir, fuera de la caja`.
  - **Paralelismo:** solo cuentan los nombres nuevos, así que lo que ya había en `/tmp` no molesta. El backend corre sus pruebas en una sola JVM y en serie, y ninguna otra prueba crea `recepcion-*` directo en `/tmp` (todas usan su propio `synkra.data`).
  - **Riesgo que queda:** otro proceso que, a la vez, deje `recepcion-*` directo en `/tmp` (por ejemplo, el mutante D3 de otra sesión) haría fallar la propiedad. Ese fallo apuntaría a un defecto real de ese proceso. Redirigir el `java.io.tmpdir` de la JVM de pruebas sigue como candidata del revisor, porque es configuración de `build.gradle.kts` y queda fuera de CA-4.
