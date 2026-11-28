docker tag registry@sha256:1be55279f18a2fe1a74edf2664cac61c1bea305b7b4642dab412e7affdcb3e33 localhost:5001/synkra-prueba-registro:1
docker push localhost:5001/synkra-prueba-registro:1; echo "push=$?"
curl -sf http://127.0.0.1:5001/v2/synkra-prueba-registro/tags/list
for n in synkra-control-plane synkra-worker; do docker exec "$n" crictl pull localhost:5001/synkra-prueba-registro:1; echo "$n pull=$?"; done
for n in synkra-control-plane synkra-worker; do docker exec "$n" crictl rmi localhost:5001/synkra-prueba-registro:1; done; docker rmi localhost:5001/synkra-prueba-registro:1
