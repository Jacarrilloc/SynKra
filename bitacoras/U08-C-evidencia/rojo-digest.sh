#!/usr/bin/env bash
# Rojo del hallazgo de ronda 1 (digest del pod frente al del registro). Desde la raiz del worktree, con SynKra instalado.
KC=$PWD/.kube/kind-synkra.yaml; R=127.0.0.1:5001
A='application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.oci.image.manifest.v1+json, application/vnd.docker.distribution.manifest.v2+json'
echo "# reconstruir y subir la misma imagen dos veces (mismo contenido)"
make imagenes > /dev/null 2>&1; echo "imagenes-1=$?"
d1=$(curl -sfI -H "Accept: $A" "http://$R/v2/synkra-backend/manifests/0.1.0" | tr -d '\r' | awk -F': ' 'tolower($1)=="docker-content-digest"{print $2}')
make imagenes > /dev/null 2>&1; echo "imagenes-2=$?"
d2=$(curl -sfI -H "Accept: $A" "http://$R/v2/synkra-backend/manifests/0.1.0" | tr -d '\r' | awk -F': ' 'tolower($1)=="docker-content-digest"{print $2}')
echo "registro tras la 1a subida: $d1"; echo "registro tras la 2a subida: $d2"; [ "$d1" = "$d2" ] && echo "mismo-digest" || echo "digest-distinto-con-el-mismo-contenido"
echo "# make desplegar"
make desplegar 2>&1 | tail -4; echo "desplegar=${PIPESTATUS[0]}"
echo "# lo que ve el pod y lo que tiene el nodo (solo lectura)"
kubectl --kubeconfig "$KC" -n synkra get pods -l app.kubernetes.io/name=synkra-backend -o jsonpath='{.items[0].status.containerStatuses[0].imageID}'; echo
docker exec synkra-worker crictl inspecti localhost:5001/synkra-backend:0.1.0 | jq -c '{id: .status.id, repoDigests: .status.repoDigests}'
