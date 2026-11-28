#!/usr/bin/env bash
# CA-2: U09-T07, el criterio literal del plan y la imagen con version. Desde la raiz del worktree, con JAVA_HOME exportado.
docker image inspect --format '{{json .RepoDigests}}' mcr.microsoft.com/dotnet/aspnet:10.0.15 mcr.microsoft.com/dotnet/sdk:10.0.404
docker build -t localhost:5001/synkra-front front/ > /dev/null 2>&1 && docker inspect -f '{{.Config.User}}' localhost:5001/synkra-front; echo "plan=$?"
docker rmi localhost:5001/synkra-front:latest
bash scripts/imagenes.sh; echo "imagenes=$?"
docker image inspect -f '{{index .RepoTags 0}} user={{.Config.User}} arch={{.Architecture}}' localhost:5001/synkra-front:0.1.0
docker version -f '{{.Server.Arch}}'
grep -n '^FROM' front/Dockerfile; grep -c '^FROM' front/Dockerfile
