#!/usr/bin/env python3
"""Verifica deploy/base (salida de `kubectl kustomize deploy/base` por stdin).

Uso: kubectl kustomize deploy/base | python3 -I scripts/verificar-base.py
Imprime `OK base: <n> objetos` y sale con 0, o una linea `FALLA <regla>: <detalle>`
por cada fallo y sale con 1.
"""
import sys

try:
    import yaml
except ImportError:
    print("FALLA entrada: PyYAML no esta disponible")
    sys.exit(1)

PAISES = ("co", "pe")
VERBOS_ESCRITURA = ["get", "list", "watch", "create", "update", "patch"]
VERBOS_LECTURA = ["get", "list", "watch"]
PARTE = {"app.kubernetes.io/part-of": "synkra"}


def reglas(pais):
    return [
        {"apiGroups": ["apps"], "resources": ["deployments"], "verbs": VERBOS_ESCRITURA},
        {"apiGroups": [""], "resources": ["configmaps", "services"], "verbs": VERBOS_ESCRITURA},
        {"apiGroups": [""], "resources": ["pods", "pods/log", "events"], "verbs": VERBOS_LECTURA},
        {"apiGroups": [""], "resources": ["namespaces"], "resourceNames": ["synkra-" + pais],
         "verbs": ["get"]},
    ]


def esperados():
    e = {
        ("Namespace", "-", "synkra"): PARTE,
        ("ServiceAccount", "synkra", "deployer"): PARTE,
    }
    for p in PAISES:
        e[("Namespace", "-", "synkra-" + p)] = dict(PARTE, **{"synkra.io/pais": p})
        e[("Role", "synkra-" + p, "synkra-deployer")] = PARTE
        e[("RoleBinding", "synkra-" + p, "synkra-deployer")] = PARTE
    return e


def main():
    texto = sys.stdin.read()
    if not texto.strip():
        print("FALLA entrada: vacia")
        return 1
    try:
        docs = [d for d in yaml.safe_load_all(texto) if d]
    except yaml.YAMLError as ex:
        print("FALLA entrada: YAML invalido: %s" % str(ex).replace("\n", " "))
        return 1
    if not docs or not all(isinstance(d, dict) and "kind" in d and "metadata" in d for d in docs):
        print("FALLA entrada: no son objetos de Kubernetes")
        return 1
    fallas = []
    objs = {}
    for d in docs:
        m = d["metadata"]
        clave = (d["kind"], m.get("namespace", "-"), m.get("name"))
        if clave in objs:
            fallas.append("FALLA objetos: duplicado %s" % (clave,))
        objs[clave] = d
        if d["kind"].startswith("Cluster"):
            fallas.append("FALLA sin-cluster: %s" % (clave,))
    esp = esperados()
    for c in sorted(set(esp) - set(objs)):
        fallas.append("FALLA objetos: falta %s" % (c,))
    for c in sorted(set(objs) - set(esp), key=str):
        fallas.append("FALLA objetos: sobra %s" % (c,))
    for c, d in objs.items():
        if c not in esp:
            continue
        et = d["metadata"].get("labels") or {}
        if et != esp[c]:
            fallas.append("FALLA etiquetas: %s tiene %s y se esperaba %s" % (c, et, esp[c]))
        if c[0] == "Role":
            pais = c[1].split("-")[1]
            if d.get("rules") != reglas(pais):
                fallas.append("FALLA reglas: %s tiene %s y se esperaba %s" % (c, d.get("rules"), reglas(pais)))
        if c[0] == "RoleBinding":
            ref = d.get("roleRef")
            if ref != {"apiGroup": "rbac.authorization.k8s.io", "kind": "Role", "name": "synkra-deployer"}:
                fallas.append("FALLA roleRef: %s tiene %s" % (c, ref))
            sujetos = d.get("subjects")
            if sujetos != [{"kind": "ServiceAccount", "name": "deployer", "namespace": "synkra"}]:
                fallas.append("FALLA subjects: %s tiene %s" % (c, sujetos))
    # prohibidos, por si alguna regla se cuela
    for c, d in objs.items():
        for r in d.get("rules") or []:
            for campo, malo in (("verbs", {"*", "delete", "deletecollection", "escalate", "bind", "impersonate"}),
                                ("resources", {"*", "secrets"}), ("apiGroups", {"*"})):
                if malo & set(r.get(campo) or []):
                    fallas.append("FALLA prohibidos: %s usa %s en %s" % (c, sorted(malo & set(r[campo])), campo))
    if fallas:
        print("\n".join(fallas))
        return 1
    print("OK base: %d objetos" % len(objs))
    return 0


sys.exit(main())
