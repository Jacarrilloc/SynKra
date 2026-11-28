#!/usr/bin/env bash
# CA-3: la imagen verificada, sin SDK, y la etapa de compilacion solo restauro el paquete declarado. Desde la raiz del worktree.
bash scripts/verificar-imagenes.sh; echo "verificar=$?"
N=synkra-u09d-build-$$; trap 'docker rmi "$N:etapa" >/dev/null 2>&1' EXIT
docker build -q --target build -t "$N:etapa" front/ > /dev/null; echo "etapa=$?"
docker run --pull=never --rm --entrypoint cat "$N:etapa" /src/obj/project.assets.json | python3 -I -c 'import json,sys; d=json.load(sys.stdin); print(sorted(d["libraries"]))'
docker run --pull=never --rm --entrypoint dotnet localhost:5001/synkra-front:0.1.0 --list-sdks | wc -l
docker run --pull=never --rm --entrypoint dotnet localhost:5001/synkra-front:0.1.0 --list-runtimes
docker run --pull=never --rm --entrypoint sh localhost:5001/synkra-front:0.1.0 -c 'ls -ln /app | head -5; touch /app/x 2>&1; echo "escribe=$?"'
