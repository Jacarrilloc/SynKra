# U08-C — respuesta a la ronda 2

F-06: Corregido en el commit "ca9 parte cada linea en ordenes" (ronda 3): `ca9.sh` ya no borra comillas a ciegas. Quita solo los argumentos de `echo` y los comentarios, parte cada línea en órdenes (por `$(`, `&&`, `||`, `;`, `|`, `{`, `(` y comilla invertida; un `python3 -I` dentro del guion) y exige `--kubeconfig` en cada orden que empieza por `kubectl`. Sobre HEAD da `sin-kubeconfig=1`; caen, cada una en su copia, `k()`, el patrón `|| { echo`, `v1-canI` (`verificar-limite.sh:23`), `v2-tipo` (`secreto-tls.sh:37`), `v3-encluster` (`:41`), `v4-jsonpath-dq` y `v6-kc-en-mensaje` (todas `sin-kubeconfig-mutado-<nombre>=0`). Ver `bitacoras/U08-C-evidencia/ca9.txt`.
F-07: Corregido en el mismo commit: al partir por órdenes, `v5-dos-ordenes` (`kubectl --kubeconfig … get ns && kubectl apply …`) da `sin-kubeconfig-mutado-v5-dos-ordenes=0`.
Candidata C-140: mover la regla a un script versionado único (no copiada en la evidencia).
