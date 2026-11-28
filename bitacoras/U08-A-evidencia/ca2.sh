make registro; echo "salida=$?"
docker inspect -f '{{.Id}}' kind-registry
docker inspect -f '{{index .Config.Labels "io.synkra.registro"}} {{.Config.Image}} {{.HostConfig.RestartPolicy.Name}} {{range $k,$v := .NetworkSettings.Networks}}{{$k}} {{end}}' kind-registry
docker port kind-registry
curl -sf http://127.0.0.1:5001/v2/; echo " v2=$?"
for n in synkra-control-plane synkra-worker; do docker exec "$n" cat /etc/containerd/certs.d/localhost:5001/hosts.toml; done
make registro; echo "salida=$?"
docker inspect -f '{{.Id}}' kind-registry
