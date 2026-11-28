r() { echo "\$ $*"; eval "$@"; }
r 'kubectl kustomize deploy/overlays/kind > /dev/null; echo "kustomize=$?"'
r 'kubectl kustomize deploy/overlays/kind | python3 -I scripts/verificar-backend.py; echo "salida=$?"'
r "kubectl kustomize deploy/overlays/kind | python3 -I -c \"import sys,yaml; os_=[o for o in yaml.safe_load_all(sys.stdin) if o]; d=[o for o in os_ if o['kind']=='Deployment'][0]['spec']['template']['spec']['containers'][0]; print(d['image']); print(sorted((e['name'], e['value']) for e in d['env'])); i=[o for o in os_ if o['kind']=='Ingress'][0]['spec']; print(i['ingressClassName'], i['tls']); print([(r['host'], [(p['path'], p['pathType'], p['backend']['service']['name'], p['backend']['service']['port']) for p in r['http']['paths']]) for r in i['rules']])\""
r "grep -c 'synkra-front' deploy/overlays/kind/*.yaml"
