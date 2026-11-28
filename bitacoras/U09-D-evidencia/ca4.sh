#!/usr/bin/env bash
# CA-4: la imagen arranca sin root, con la raiz de solo lectura, y no arranca con una URL mala. Desde la raiz del worktree.
I=localhost:5001/synkra-front:0.1.0; N=synkra-u09d-front-$$; M=synkra-u09d-malo-$$
trap 'docker rm -f "$N" "$M" >/dev/null 2>&1' EXIT          # registrado antes de los docker run
docker run --pull=never -d --name "$N" --user 10001:10001 --read-only --tmpfs /tmp --cap-drop ALL --security-opt no-new-privileges \
  -e Synkra__ApiUrl=http://127.0.0.1:9/ -m 256m --cpus 0.5 -p 127.0.0.1::8080 "$I" || { echo "FALLA ca4: docker run"; exit 1; }
P=$(docker port "$N" 8080/tcp | head -1 | cut -d: -f2)
timeout 60 bash -c "until curl -sf 127.0.0.1:$P/app.css >/dev/null; do sleep 1; done"; echo "espera=$?"
curl -s -o /dev/null -w '%{http_code}\n' 127.0.0.1:$P/app.css
curl -s -o /dev/null -w 'blazor.web.js %{http_code}\n' 127.0.0.1:$P/_framework/blazor.web.js
curl -s 127.0.0.1:$P/ | grep -o -e '<h1>Vista general</h1>' -e 'API_NO_DISPONIBLE: No se pudo conectar con la API de SynKra' | sort -u
docker exec "$N" id -u
docker logs "$N" 2>&1 | grep -ciE 'read-only file system|permission denied|unhandled exception'
docker run --pull=never --name "$M" --user 10001:10001 --read-only --tmpfs /tmp -e Synkra__ApiUrl=ftp://h/ "$I" > /dev/null 2>&1; echo "malo=$?"
docker logs "$M" 2>&1 | grep -c 'Synkra:ApiUrl falta o no es una URL absoluta http o https'
