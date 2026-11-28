#!/usr/bin/env python3
"""Verifica los manifiestos del backend (U08-B).

Uso:
  kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py
  kubectl kustomize deploy/base/backend  | python3 -I scripts/verificar-backend.py --base
  kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py --versiones   (solo versiones)
Imprime `OK backend: N objetos` y sale con 0, o una linea `FALLA <regla>: <detalle>` por fallo y sale con 1.
"""
import os
import re
import sys

try:
    import yaml
except ImportError:
    print("FALLA entrada: falta PyYAML")
    sys.exit(1)

NS = "synkra"
ETIQ = {"app.kubernetes.io/name": "synkra-backend", "app.kubernetes.io/part-of": "synkra"}
SEL = {"app.kubernetes.io/name": "synkra-backend"}
ENV_BASE = {
    "SYNKRA_DATA": "/data",
    "SYNKRA_PAISES": "co,pe",
    "MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED": "true",
    "JAVA_TOOL_OPTIONS": "-XX:MaxRAMPercentage=70",
}
RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def leer(ruta, patron):
    try:
        with open(os.path.join(RAIZ, ruta), encoding="utf-8") as f:
            m = re.search(patron, f.read(), re.M)
    except OSError:
        m = None
    if not m:
        print("FALLA entrada: no se pudo leer %s en %s" % (patron, ruta))
        sys.exit(1)
    return m.group(1)


# Origen unico de las versiones (el mismo que lee scripts/imagenes.sh): el overlay debe coincidir con ellas.
VERSION_BACKEND = leer("backend/build.gradle.kts", r'^version *= *"([^"]+)"')
IMAGEN_INIT = leer("backend/src/main/java/io/synkra/kubernetes/ConstructorObjetosPais.java",
                   r'IMAGEN_INIT_POR_DEFECTO *= *"([^"]+)"')
ENV_OVERLAY = {
    "SYNKRA_REGISTRO_PERMITIDO": "localhost:5001",
    "SYNKRA_KUBERNETES_IMAGEN_INIT": IMAGEN_INIT,
}
fallos = []


def falla(regla, detalle):
    fallos.append("FALLA %s: %s" % (regla, detalle))


def igual(regla, visto, esperado):
    if visto != esperado:
        falla(regla, "esperado %r / visto %r" % (esperado, visto))


def sonda(regla, c, clave, ruta, periodo, umbral):
    p = c.get(clave) or {}
    h = p.get("httpGet") or {}
    igual(regla + ".ruta", h.get("path"), ruta)
    igual(regla + ".puerto", h.get("port"), "http")
    igual(regla + ".periodo", p.get("periodSeconds"), periodo)
    igual(regla + ".umbral", p.get("failureThreshold"), umbral)


def solo_versiones(objs):
    """--versiones: el newTag del overlay y la imagen del init de backend-kind.yaml coinciden con el origen."""
    d = [o for o in objs if o.get("kind") == "Deployment"]
    if not d:
        print("FALLA entrada: no hay Deployment en la entrada")
        return 1
    c = d[0]["spec"]["template"]["spec"]["containers"][0]
    env = {e["name"]: e.get("value") for e in c.get("env") or []}
    tag = lambda i: (i or "").rsplit(":", 1)[-1]
    malos = []
    if tag(c.get("image")) != VERSION_BACKEND:
        malos.append("newTag del overlay %r, build.gradle.kts dice %r" % (tag(c.get("image")), VERSION_BACKEND))
    if env.get("SYNKRA_KUBERNETES_IMAGEN_INIT") != IMAGEN_INIT:
        malos.append("SYNKRA_KUBERNETES_IMAGEN_INIT %r, IMAGEN_INIT_POR_DEFECTO dice %r" % (env.get("SYNKRA_KUBERNETES_IMAGEN_INIT"), IMAGEN_INIT))
    if malos:
        print("\n".join("FALLA versiones: " + m for m in malos))
        return 1
    print("OK versiones: backend %s, init %s" % (VERSION_BACKEND, tag(IMAGEN_INIT)))
    return 0


def main():
    base = "--base" in sys.argv[1:]
    try:
        objs = [o for o in yaml.safe_load_all(sys.stdin.read()) if o]
    except yaml.YAMLError as e:
        print("FALLA entrada: YAML no valido: %s" % str(e).replace("\n", " "))
        return 1
    if not objs or not all(isinstance(o, dict) and "kind" in o for o in objs):
        print("FALLA entrada: no hay manifiestos en la entrada estandar")
        return 1

    if "--versiones" in sys.argv[1:]:
        return solo_versiones(objs)

    claves = sorted((o["kind"], (o.get("metadata") or {}).get("namespace"), (o.get("metadata") or {}).get("name")) for o in objs)
    esperados = [("Deployment", NS, "synkra-backend"), ("PersistentVolumeClaim", NS, "synkra-datos"), ("Service", NS, "synkra-backend")]
    if not base:
        esperados.append(("Ingress", NS, "synkra-backend"))
    esperados.sort()
    igual("objetos", claves, esperados)
    por = {o["kind"]: o for o in objs}

    for o in objs:
        m = o.get("metadata") or {}
        if o["kind"].startswith("Cluster"):
            falla("sin-cluster", o["kind"])
        if m.get("namespace") != NS:
            falla("namespace", "%s/%s en %r" % (o["kind"], m.get("name"), m.get("namespace")))
        et = m.get("labels") or {}
        for k, v in ETIQ.items():
            if et.get(k) != v:
                falla("etiquetas", "%s/%s sin %s=%s" % (o["kind"], m.get("name"), k, v))
    texto = yaml.safe_dump(objs)
    for prohibido in ("hostPath", "hostNetwork", "privileged: true", ":latest"):
        if prohibido in texto:
            falla("prohibido", prohibido)

    # PVC
    pvc = por.get("PersistentVolumeClaim")
    if pvc:
        s = pvc.get("spec", {})
        igual("pvc.acceso", s.get("accessModes"), ["ReadWriteOnce"])
        igual("pvc.clase", s.get("storageClassName"), "standard")
        igual("pvc.tamano", (s.get("resources") or {}).get("requests", {}).get("storage"), "1Gi")

    # Service
    sv = por.get("Service")
    if sv:
        s = sv.get("spec", {})
        igual("service.tipo", s.get("type"), "ClusterIP")
        igual("service.selector", s.get("selector"), SEL)
        igual("service.puertos", s.get("ports"), [{"name": "http", "port": 8080, "targetPort": "http", "protocol": "TCP"}])

    # Deployment
    d = por.get("Deployment")
    if d:
        s = d["spec"]
        igual("recreate", (s.get("strategy") or {}).get("type"), "Recreate")
        igual("replicas", s.get("replicas"), 1)
        igual("revisionHistoryLimit", s.get("revisionHistoryLimit"), 3)
        igual("deployment.selector", (s.get("selector") or {}).get("matchLabels"), SEL)
        igual("plantilla.etiquetas", (s["template"].get("metadata") or {}).get("labels"), ETIQ)
        p = s["template"]["spec"]
        igual("cuenta", p.get("serviceAccountName"), "deployer")
        igual("token", p.get("automountServiceAccountToken"), True)
        igual("sin-root.pod", p.get("securityContext"), {
            "runAsNonRoot": True, "runAsUser": 10001, "runAsGroup": 10001, "fsGroup": 10001,
            "fsGroupChangePolicy": "OnRootMismatch", "seccompProfile": {"type": "RuntimeDefault"}})
        cs = p.get("containers") or []
        igual("contenedores", [c.get("name") for c in cs], ["synkra-backend"])
        if len(cs) == 1:
            c = cs[0]
            igual("imagen", c.get("image"), "synkra-backend" if base else "localhost:5001/synkra-backend:" + VERSION_BACKEND)
            igual("pull", c.get("imagePullPolicy"), "IfNotPresent")
            igual("puerto", c.get("ports"), [{"name": "http", "containerPort": 8080, "protocol": "TCP"}])
            env = {e["name"]: e.get("value") for e in c.get("env") or []}
            esp = dict(ENV_BASE)
            if not base:
                esp.update(ENV_OVERLAY)
            igual("variables", env, esp)
            igual("recursos", c.get("resources"), {"requests": {"memory": "256Mi", "cpu": "250m"}, "limits": {"memory": "512Mi", "cpu": "500m"}})
            sonda("startupProbe", c, "startupProbe", "/actuator/health/liveness", 5, 36)
            sonda("livenessProbe", c, "livenessProbe", "/actuator/health/liveness", 10, 3)
            sonda("readinessProbe", c, "readinessProbe", "/actuator/health/readiness", 5, 3)
            igual("montajes", sorted((m["name"], m["mountPath"]) for m in c.get("volumeMounts") or []), [("datos", "/data"), ("tmp", "/tmp")])
            igual("sin-root.contenedor", c.get("securityContext"), {
                "allowPrivilegeEscalation": False, "readOnlyRootFilesystem": True, "capabilities": {"drop": ["ALL"]}})
        vols = {v["name"]: {k: x for k, x in v.items() if k != "name"} for v in p.get("volumes") or []}
        igual("volumenes", vols, {"datos": {"persistentVolumeClaim": {"claimName": "synkra-datos"}}, "tmp": {"emptyDir": {"sizeLimit": "256Mi"}}})

    # Ingress
    ing = por.get("Ingress")
    if ing:
        m = ing["metadata"]
        s = ing["spec"]
        igual("ingress.anotaciones", m.get("annotations"), {"nginx.ingress.kubernetes.io/proxy-body-size": "25m"})
        igual("ingress.clase", s.get("ingressClassName"), "nginx")
        igual("tls", s.get("tls"), [{"hosts": ["synkra.local"], "secretName": "synkra-local-tls"}])
        rutas = [(r.get("host"), [(p.get("path"), p.get("pathType"), p["backend"]["service"]["name"], p["backend"]["service"]["port"]) for p in r["http"]["paths"]]) for r in s.get("rules") or []]
        igual("ingress.rutas", rutas, [("synkra.local", [("/api", "Prefix", "synkra-backend", {"name": "http"}), ("/actuator/health", "Prefix", "synkra-backend", {"name": "http"})])])

    if fallos:
        print("\n".join(fallos))
        return 1
    print("OK backend: %d objetos" % len(objs))
    return 0


sys.exit(main())
