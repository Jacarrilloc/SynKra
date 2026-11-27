# U05-T09: fronteras del módulo `kubernetes`

**Unidad:** U05: `Pod del país`
**Historias que implementa:** H-12 (contribuye), H-13 (contribuye)
**Depende de:** U05-T06 (toda la API del módulo ya existe)

---

## Alcance

**Dentro** (una línea, concreta):

> Cerrar la frontera del módulo `kubernetes`: ningún otro módulo puede hablar con la API de Kubernetes sin pasar por `AdaptadorKubernetes` (y por su guardia), la API pública `kubernetes.api` no expone tipos de Fabric8, y `FronterasModulosTest` lo comprueba con reglas que fallan si alguien lo rompe. Recoge C-70, C-73, C-68 y C-79.

**Fuera** (lo que el codificador debe rechazar aunque lo vea roto):

- Cambiar el comportamiento de `construir`, `aplicar`, `estado`, `contextoFallo` o `leerPais` más allá de cambiar tipos en su firma (C-73). Si una prueba de comportamiento tiene que cambiar, es solo por el tipo.
- Reintentos del cliente (C-76) y todo lo de U07.
- Cambios fuera de `backend/`.

---

## Archivos de contexto

- `aidlc-docs/construction/plans/U05-pod-pais-code-generation-plan.md` (U05-T09)
- `backend/src/test/java/io/synkra/**/FronterasModulosTest.java` (U03-T05 y U04-T11: Spring Modulith, módulo compartido `verificador`, prueba que muerde con un módulo de juguete)
- `backend/src/main/java/io/synkra/kubernetes/**` y `kubernetes/api/**`
- `CANDIDATAS.md` (C-68, C-70, C-73, C-79)

---

## Decisiones del orquestador

1. **C-70: el cliente no es un bean visible.** El `KubernetesClient` deja de ser un bean que otro módulo pueda inyectar: se crea dentro de `ConfiguracionKubernetes` y solo se publica `AdaptadorKubernetes`. Si Spring necesita gestionar su cierre, se hace con el ciclo de vida del adaptador.
2. **C-70: regla de dependencias.** `FronterasModulosTest` añade una regla (ArchUnit, que ya trae Spring Modulith, o la API de `ApplicationModules`) que falla si una clase fuera de `io.synkra.kubernetes..` depende de `io.fabric8..`. La regla se prueba que muerde con una clase de juguete en `src/test` (como el caso de U03-T05).
3. **C-73: `ObjetosPais` sin Fabric8 en su API.** `ObjetosPais` deja de exponer `Deployment`, `ConfigMap` y `Service` de Fabric8 en métodos públicos. Lo que necesite U07 (por ejemplo el YAML de cada objeto o sus nombres) se expone como texto o como tipos propios; los objetos de Fabric8 quedan en una clase del paquete interno. Una segunda regla comprueba que ninguna firma pública de `kubernetes.api` menciona `io.fabric8`.
4. **C-73: imagen del init.** `RegistroPermitido` (catálogo) y `exigirImagenInit` (kubernetes) analizan referencias de imagen con reglas distintas. Aquí solo se documenta con una prueba de caracterización qué acepta cada uno; unificarlos sería cambiar comportamiento de U04 y queda en la candidata.
5. **C-68 y C-79 (pruebas).** `laUrlNoSaleDeLasPropiedadesDeSpring` registra unas `VariablesPod` vacías en vez de depender del entorno; `sinLaCaDeLaCuentaLaConexionTlsFalla` exige la excepción concreta; `"443"` pasa a constante; `peticiones()` de `AplicarTest` falla con mensaje si `takeRequest` devuelve `null`; `namespaceComun` rechaza un `ObjetosPais` incompleto con un error con nombre. El `disponible()` fijo tras el arranque se documenta, no se cambia.

---

## Criterios de aceptación

- [ ] **CA-1**: las fronteras se cumplen y la prueba muerde
  ```bash
  ./gradlew :backend:test --rerun --tests '*FronterasModulosTest'
  grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' backend/build/test-results/test/TEST-*FronterasModulosTest.xml
  grep -rn "io.fabric8" backend/src/main/java --include=*.java | grep -v "/kubernetes/"; echo "fuera=$?"
  grep -rn "io.fabric8" backend/src/main/java/io/synkra/kubernetes/api; echo "api=$?"
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos, con al menos dos pruebas nuevas que muerden (una clase de juguete fuera de `kubernetes` que importa Fabric8, y una firma pública de `kubernetes.api` con un tipo de Fabric8, deben hacer fallar las reglas); `fuera=1` y `api=1` (ningún resultado).

- [ ] **CA-2**: el cliente no se puede inyectar desde fuera (lectura de vuelta del contexto)
  ```bash
  ./gradlew :backend:test --rerun --tests '*ConfiguracionKubernetesTest*'
  ```
  Esperado: una prueba que arranca el contexto de la aplicación y comprueba que `getBeanNamesForType(KubernetesClient.class)` está vacío y que `AdaptadorKubernetes` sí está. Sin saltos ni fallos.

- [ ] **CA-3**: todo el backend sigue pasando
  ```bash
  ./gradlew :backend:test --rerun :backend:check
  for f in backend/build/test-results/test/TEST-*.xml; do grep -Ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' "$f"; done
  ```
  Esperado: `BUILD SUCCESSFUL`, sin saltos ni fallos.

- [ ] **CA-4**: el diff queda acotado
  ```bash
  git rev-parse --short HEAD; git diff --stat <base-del-despacho>...HEAD
  ```
  Esperado: `backend/src/main/java/io/synkra/kubernetes/**`, `backend/src/test/**`, la bitácora y `revisiones/U05-T09/`. Se anota el sha.

---

**Rojo primero:** las reglas nuevas antes que el arreglo; con el código actual deben fallar (C-70 y C-73 existen hoy).
