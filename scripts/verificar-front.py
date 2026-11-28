#!/usr/bin/env python3
"""Verifica los manifiestos del front (U09-D).

Uso:
  kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-front.py          (Deployment, Service e Ingress del front)
  kubectl kustomize deploy/base/front    | python3 -I scripts/verificar-front.py --base   (Deployment y Service, sin nada de kind)
Imprime `OK front: N objetos` y sale con 0, o una linea `FALLA <regla>: <detalle>` por fallo y sale con 1.
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
NOMBRE = "synkra-front"
ETIQ = {"app.kubernetes.io/name": NOMBRE, "app.kubernetes.io/part-of": "synkra"}
SEL = {"app.kubernetes.io/name": NOMBRE}
URL_API = "http://synkra-backend.synkra.svc:8080/"
RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
fallos = []


def falla(regla, detalle):
    fallos.append("FALLA %s: %s" % (regla, detalle))


def igual(regla, visto, esperado):
    if visto != esperado:
        falla(regla, "esperado %r / visto %r" % (esperado, visto))


def version_front():
    try:
        with open(os.path.join(RAIZ, "front/Synkra.Front.csproj"), encoding="utf-8") as f:
            m = re.search(r"<Version>([^<]+)</Version>", f.read())
    except OSError:
        m = None
    if not m:
        print("FALLA entrada: front/Synkra.Front.csproj no tiene <Version>")
        sys.exit(1)
    return m.group(1)


def sonda(regla, c, clave, periodo, umbral):
    p = c.get(clave) or {}
    h = p.get("httpGet") or {}
    igual(regla + ".ruta", h.get("path"), "/app.css")
    igual(regla + ".puerto", h.get("port"), "http")
    igual(regla + ".periodo", p.get("periodSeconds"), periodo)
    igual(regla + ".umbral", p.get("failureThreshold"), umbral)


def deployment(d, imagen, pull):
    s = d["spec"]
    igual("estrategia", (s.get("strategy") or {}).get("type", "RollingUpdate"), "RollingUpdate")
    igual("replicas", s.get("replicas"), 1)
    igual("revisionHistoryLimit", s.get("revisionHistoryLimit"), 3)
    igual("deployment.selector", (s.get("selector") or {}).get("matchLabels"), SEL)
    igual("plantilla.etiquetas", (s["template"].get("metadata") or {}).get("labels"), ETIQ)
    p = s["template"]["spec"]
    igual("cuenta", p.get("serviceAccountName"), None)
    igual("token", p.get("automountServiceAccountToken"), False)
    igual("sin-root.pod", p.get("securityContext"), {
        "runAsNonRoot": True, "runAsUser": 10001, "runAsGroup": 10001, "seccompProfile": {"type": "RuntimeDefault"}})
    cs = p.get("containers") or []
    igual("contenedores", [c.get("name") for c in cs], [NOMBRE])
    if len(cs) == 1:
        c = cs[0]
        igual("imagen", c.get("image"), imagen)
        igual("pull", c.get("imagePullPolicy"), pull)
        igual("puerto", c.get("ports"), [{"name": "http", "containerPort": 8080, "protocol": "TCP"}])
        igual("variables", c.get("env"), [{"name": "Synkra__ApiUrl", "value": URL_API}])
        igual("recursos", c.get("resources"), {"requests": {"memory": "128Mi", "cpu": "100m"}, "limits": {"memory": "256Mi", "cpu": "500m"}})
        sonda("startupProbe", c, "startupProbe", 2, 30)
        sonda("livenessProbe", c, "livenessProbe", 10, 3)
        sonda("readinessProbe", c, "readinessProbe", 5, 3)
        igual("montajes", c.get("volumeMounts"), [{"name": "tmp", "mountPath": "/tmp"}])
        igual("sin-root.contenedor", c.get("securityContext"), {
            "allowPrivilegeEscalation": False, "readOnlyRootFilesystem": True, "capabilities": {"drop": ["ALL"]}})
    igual("volumenes", p.get("volumes"), [{"name": "tmp", "emptyDir": {"sizeLimit": "64Mi"}}])


def servicio(sv):
    s = sv.get("spec", {})
    igual("service.tipo", s.get("type"), "ClusterIP")
    igual("service.selector", s.get("selector"), SEL)
    igual("service.puertos", s.get("ports"), [{"name": "http", "port": 8080, "targetPort": "http", "protocol": "TCP"}])


def ingress(ing):
    s = ing["spec"]
    igual("ingress.anotaciones", ing["metadata"].get("annotations") or {}, {})
    igual("ingress.clase", s.get("ingressClassName"), "nginx")
    igual("tls", s.get("tls"), [{"hosts": ["synkra.local"], "secretName": "synkra-local-tls"}])
    rutas = [(r.get("host"), [(p.get("path"), p.get("pathType"), p["backend"]["service"]["name"], p["backend"]["service"]["port"])
                              for p in r["http"]["paths"]]) for r in s.get("rules") or []]
    igual("ingress.rutas", rutas, [("synkra.local", [("/", "Prefix", NOMBRE, {"name": "http"})])])


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
    clave = lambda o: (o["kind"], (o.get("metadata") or {}).get("namespace"), (o.get("metadata") or {}).get("name"))
    esperados = [("Deployment", NS, NOMBRE), ("Service", NS, NOMBRE)] + ([] if base else [("Ingress", NS, NOMBRE)])
    esperados.sort()
    propios = [o for o in objs if clave(o) in esperados] if not base else objs
    igual("objetos", sorted(clave(o) for o in propios), esperados)
    por = {o["kind"]: o for o in propios}

    for o in objs:
        if o["kind"].startswith("Cluster"):
            falla("sin-cluster", o["kind"])
    for o in propios:
        m = o.get("metadata") or {}
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

    imagen, pull = (NOMBRE, "IfNotPresent") if base else ("localhost:5001/%s:%s" % (NOMBRE, version_front()), "Always")
    if "Deployment" in por:
        deployment(por["Deployment"], imagen, pull)
    if "Service" in por:
        servicio(por["Service"])
    if "Ingress" in por:
        ingress(por["Ingress"])
    if not base:
        # Ningun otro Ingress del overlay se queda con la ruta /: el del backend sigue en /api y /actuator/health.
        for o in objs:
            if o["kind"] == "Ingress" and clave(o) not in esperados:
                for r in (o.get("spec") or {}).get("rules") or []:
                    for p in r["http"]["paths"]:
                        if p.get("path") == "/":
                            falla("ruta-raiz", "el Ingress %s tambien tiene la ruta /" % o["metadata"].get("name"))

    if fallos:
        print("\n".join(fallos))
        return 1
    print("OK front: %d objetos" % len(propios))
    return 0


sys.exit(main())
