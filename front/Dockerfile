# Imagen del front de SynKra (U09-T07): dos etapas, sin root, puerto 8080. Contexto: front/.
# Etapa de compilacion: SDK 10.0.404, el mismo de scripts/dotnet-docker.sh.
# Digest = RepoDigests de `docker image inspect mcr.microsoft.com/dotnet/sdk:10.0.404` (anotado por U09-A).
FROM mcr.microsoft.com/dotnet/sdk:10.0.404@sha256:8c6edfb26c85153f236d26efebd9ad349aec403583f343f201ee67838b23658d AS build
ENV DOTNET_CLI_TELEMETRY_OPTOUT=1 DOTNET_NOLOGO=1
WORKDIR /src
# Primero solo el .csproj: la restauracion queda en cache mientras no cambien los paquetes.
COPY Synkra.Front.csproj .
RUN dotnet restore
COPY . .
RUN dotnet publish -c Release -o /app --no-restore

# Etapa final: runtime de ASP.NET 10.0.15, sin SDK.
# Digest = indice multiarquitectura de `docker image inspect mcr.microsoft.com/dotnet/aspnet:10.0.15` (permiso del responsable, audit.md).
FROM mcr.microsoft.com/dotnet/aspnet:10.0.15@sha256:aac50ceef10391e78a899e3d2ccd00bbafa2dcb56ccf4586cbccf00edb84fc21
WORKDIR /app
# Archivos de dueño root y sin escritura para el usuario de la aplicacion.
COPY --from=build /app .
# UID_CONTENEDORES y GID_CONTENEDORES de ConstructorObjetosPais (C-17), como las otras tres imagenes.
USER 10001:10001
# El puerto 8080 lo fija la base con ASPNETCORE_HTTP_PORTS.
EXPOSE 8080
ENTRYPOINT ["dotnet", "Synkra.Front.dll"]
