# U08-C — respuesta a la ronda 1

F-01: Corregido en la ronda 2 (commit "ca9 quita el texto entre comillas"): `ca9.sh` ya no descarta líneas con `|| { echo`; quita el texto entre comillas con `sed -E 's/"[^"]*"//g'` antes de buscar `kubectl ` sin `--kubeconfig`. Sobre HEAD da `sin-kubeconfig=1`; con el patrón del revisor (`kubectl apply -f "$TMP/secreto.yaml" || { echo "FALLA tls: apply"; exit 1; }` en una copia de `secreto-tls.sh`) da `sin-kubeconfig-mutado-patron-revisor=0` y muestra la línea; la mutación de `k()` también cae (`sin-kubeconfig-mutado-k=0`). Ver `bitacoras/U08-C-evidencia/ca9.txt`.
F-02: Fuera de alcance: candidata C-140 (por arbitraje del orquestador, para no invalidar la evidencia en el clúster).
F-03: Fuera de alcance: candidata C-140.
F-04: Fuera de alcance: candidata C-140.
F-05: Fuera de alcance: candidata C-140.
Notas de especificación (código 2 de make, grep de CA-9 frente a la decisión 11, grep de arquitectura, atestaciones, Ingress retirado): C-139.
