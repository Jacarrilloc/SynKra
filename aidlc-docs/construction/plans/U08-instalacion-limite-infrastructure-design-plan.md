# U08 · Instalación y límite: plan de Infrastructure Design

**Entradas:** `unit-of-work.md` §U08, `application-design.md` (frontera dentro/fuera del clúster,
D10, D13), Functional Design de U04, U06 y U07. **Salidas:**
`aidlc-docs/construction/U08-instalacion-limite/infrastructure-design/infrastructure-design.md` y
`deployment-architecture.md`.

## Pasos

- [ ] 1. Entorno: clúster, registro de imágenes y cómo se accede (preguntas 1 y 4)
- [ ] 2. Cómputo: recursos por pod (pregunta 2)
- [ ] 3. Almacenamiento: el PVC de SQLite y artefactos (pregunta 3)
- [ ] 4. Mensajería: N/A (no hay colas; la vigilancia es una tarea interna del backend, D8)
- [ ] 5. Red: Service interno para el init container, acceso de usuarios (pregunta 4)
- [ ] 6. Monitoreo: logs con `kubectl logs` y la salud de Actuator; Prometheus es Could (fuera)
- [ ] 7. Límite: cuenta `synkra:deployer`, `Role` por país, sin permisos de clúster; los pods de país sin token de Kubernetes
- [ ] 8. Cómo se instala (pregunta 5) y el diagrama de despliegue
- [ ] 9. Revisar AUTONOMIA-01 (todo se aplica solo al `kind` local) y AUTONOMIA-02

---

## Preguntas

Letra después de `[Answer]:`, o "aprobado" en el chat para aceptar las recomendaciones.

## Question 1 (Entorno)
¿Cómo es el clúster de la demo?

A) `kind` con un control plane y un worker, `extraPortMappings` 80 y 443, y un registro local `localhost:5001` conectado a la red de `kind` (patrón de la documentación de `kind`)

B) `kind` de un solo nodo, cargando las imágenes con `kind load docker-image`, sin registro

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** El registro local es el "registro permitido" del PRD (H-31) y la imagen del
core se referencia por digest, que `kind load` no da.

[Answer]: 

## Question 2 (Cómputo)
¿Qué recursos se reservan?

A) Backend 256 Mi a 512 Mi y 250m a 500m de CPU; front 128 Mi a 256 Mi; core de cada país 256 Mi a 512 Mi; init container 32 Mi a 64 Mi. Una réplica de cada uno

B) Sin límites en el demo

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Cabe en los 6 GiB que pide el curso, y los límites son también un Should
(H-32) que así ya queda medido.

[Answer]: 

## Question 3 (Almacenamiento)
¿Qué volumen usa el backend?

A) Un PVC de 1 Gi `ReadWriteOnce` con la StorageClass por defecto de `kind` (`standard`, local-path), montado en `/data`; Deployment con estrategia `Recreate`

B) Un `hostPath`

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Es el patrón del módulo 5 (PV, PVC, StorageClass) y sobrevive al borrado
del pod, que es lo que prueba H-18.

[Answer]: 

## Question 4 (Red y acceso)
¿Cómo se llega al front y a la API desde la máquina?

A) Ingress NGINX con el host `synkra.local`: `/` al front y `/api` al backend, con un certificado autofirmado que genera `make install`. Respaldo: `kubectl port-forward`

B) Solo `kubectl port-forward`, sin Ingress

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** El PRD usa `https://synkra.local/api` y el Ingress es tema del módulo 5;
el respaldo evita que la demo dependa de él. El init container siempre usa el Service interno
`synkra-backend.synkra.svc:8080`, no el Ingress.

[Answer]: 

## Question 5 (Cómo se instala)
¿Con qué se escriben y aplican los manifiestos de SynKra mismo?

A) YAML con Kustomize (`deploy/base` y `deploy/overlays/kind`), aplicados por `make install` con `kubectl apply -k`. El módulo 8 reconcilia esa misma carpeta con GitOps (H-34)

B) Un chart de Helm

C) YAML sueltos con `kubectl apply -f`

X) Other (please describe after [Answer]: tag below)

**Recomendación: A.** Kustomize sí encaja aquí: instala **SynKra mismo**, no los productos (que
siguen con Fabric8, Q9 de requisitos), y es lo que GitOps consume sin cambios.

[Answer]: 
