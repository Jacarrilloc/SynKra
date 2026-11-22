# U08 · Arquitectura de despliegue

```mermaid
flowchart TB
    DEV["Maquina del operador: make install, curl, navegador"]
    REG[("Registro local localhost:5001")]

    subgraph KIND["Cluster kind: control plane + worker"]
        ING["Ingress NGINX synkra.local"]
        subgraph NS["namespace synkra"]
            FE["Deployment synkra-front"]
            BE["Deployment synkra-backend, SA deployer, Recreate"]
            PVC[("PVC synkra-datos 1Gi en /data")]
        end
        subgraph CO["namespace synkra-co"]
            RCO["Role y RoleBinding synkra-deployer"]
            PCO["Pod core: init descarga + core"]
        end
        subgraph PE["namespace synkra-pe"]
            RPE["Role y RoleBinding synkra-deployer"]
            PPE["Pod core: init descarga + core"]
        end
    end

    DEV -->|"https"| ING
    ING -->|"/"| FE
    ING -->|"/api"| BE
    FE -->|"http interno"| BE
    BE --- PVC
    BE -->|"API de Kubernetes, solo con su Role"| CO
    BE -->|"API de Kubernetes, solo con su Role"| PE
    PCO -->|"GET /api/artefactos por hash"| BE
    PPE -->|"GET /api/artefactos por hash"| BE
    DEV -->|"docker push"| REG
    PCO -.->|"imagen por digest"| REG
    PPE -.->|"imagen por digest"| REG
```

_Texto alternativo:_ desde la máquina del operador se instala y se usa SynKra por el Ingress
`synkra.local`, que manda `/` al front y `/api` al backend, ambos en el namespace `synkra`. El
backend monta el PVC y usa la cuenta `deployer`, que solo tiene un `Role` en `synkra-co` y en
`synkra-pe`. Los pods de cada país descargan sus plugins del backend por hash y su imagen del
registro local por digest.

## Orden de `make install`

1. `kind create cluster --config deploy/kind/cluster.yaml` (si no existe) y el registro local.
2. Ingress NGINX para `kind`; certificado autofirmado para `synkra.local`.
3. `docker build` y `docker push` del backend, el core y el init container (y el front si existe).
4. `kubectl apply -k deploy/overlays/kind`.
5. `kubectl wait` hasta que el backend esté listo.
6. `make verificar-limite`.
