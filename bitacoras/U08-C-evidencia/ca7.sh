#!/usr/bin/env bash
# CA-7: un ClusterRoleBinding de prueba (y un RoleBinding en synkra) hacen fallar el limite; al quitarlos, vuelve a pasar.
KC=${KC:-$PWD/.kube/kind-synkra.yaml}
quitar() { kubectl --kubeconfig "$KC" delete clusterrolebinding synkra-prueba-limite --ignore-not-found; kubectl --kubeconfig "$KC" -n synkra delete rolebinding synkra-prueba-limite --ignore-not-found; }
trap quitar EXIT                                                   # registrado antes de crear nada
kubectl --kubeconfig "$KC" create clusterrolebinding synkra-prueba-limite --clusterrole=edit --serviceaccount=synkra:deployer
kubectl --kubeconfig "$KC" get clusterrolebinding synkra-prueba-limite -o jsonpath='{.roleRef.name} {.subjects[0].namespace}/{.subjects[0].name}'; echo
make verificar-limite; echo "con-crb=$?"
kubectl --kubeconfig "$KC" delete clusterrolebinding synkra-prueba-limite
kubectl --kubeconfig "$KC" get clusterrolebinding synkra-prueba-limite; echo "crb-borrado=$?"
kubectl --kubeconfig "$KC" -n synkra create rolebinding synkra-prueba-limite --clusterrole=edit --serviceaccount=synkra:deployer
make verificar-limite; echo "con-rb=$?"
quitar
kubectl --kubeconfig "$KC" -n synkra get rolebinding synkra-prueba-limite; echo "rb-borrado=$?"
make verificar-limite; echo "limpio=$?"
