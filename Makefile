# Comandos comunes de SynKra. Corren igual en Linux y en WSL2 (Ubuntu).
# El unico cluster permitido es kind-synkra, con el kubeconfig del proyecto (AUTONOMIA-01).
KUBECONFIG_SYNKRA ?= $(CURDIR)/.kube/kind-synkra.yaml
export KUBECONFIG_SYNKRA
export KUBECONFIG := $(KUBECONFIG_SYNKRA)
COUNTRY ?= co

.PHONY: help setup doctor guardia cluster registro aplicar-base install status cluster-delete

help: ## Lista los comandos
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN{FS=":.*## "}{printf "  %-16s %s\n", $$1, $$2}'

setup: ## Instala las herramientas (Ubuntu/Debian/WSL2)
	./scripts/setup-linux.sh

doctor: ## Verifica herramientas y Docker
	./scripts/doctor.sh

guardia: ## Comprueba que el kubeconfig es el de kind-synkra
	@scripts/guardia-contexto.sh

cluster: ## Crea (o reexporta) el clúster kind-synkra
	@scripts/guardia-contexto.sh --antes-de-crear
	@mkdir -p -m 0700 "$(dir $(KUBECONFIG_SYNKRA))"
	@if kind get kubeconfig --name synkra >/dev/null 2>&1; then \
	  kind export kubeconfig --name synkra --kubeconfig "$(KUBECONFIG_SYNKRA)"; \
	else \
	  kind create cluster --name synkra --config deploy/kind/cluster.yaml --kubeconfig "$(KUBECONFIG_SYNKRA)" --wait 180s; \
	fi
	@scripts/guardia-contexto.sh
	kubectl --kubeconfig "$(KUBECONFIG_SYNKRA)" wait --for=condition=Ready nodes --all --timeout=180s

registro: guardia ## Levanta el registro local en 127.0.0.1:5001
	scripts/registro-local.sh

aplicar-base: guardia ## Aplica namespaces y RBAC (deploy/base)
	kubectl --kubeconfig "$(KUBECONFIG_SYNKRA)" apply -k deploy/base

install: ## Clúster, registro y base
	@scripts/guardia-contexto.sh --antes-de-crear
	$(MAKE) cluster
	$(MAKE) registro
	$(MAKE) aplicar-base

status: guardia ## Muestra nodos y pods del clúster
	kubectl --kubeconfig "$(KUBECONFIG_SYNKRA)" get nodes -o wide
	kubectl --kubeconfig "$(KUBECONFIG_SYNKRA)" get pods -A

cluster-delete: ## Borra el clúster kind-synkra y su registro
	kind delete cluster --name synkra --kubeconfig "$(KUBECONFIG_SYNKRA)"
	@if docker inspect kind-registry >/dev/null 2>&1; then \
	  if [ "$$(docker inspect -f '{{index .Config.Labels "io.synkra.registro"}}' kind-registry)" = true ]; then \
	    docker rm --force kind-registry; \
	  else \
	    echo "kind-registry existe y no es de SynKra: no se toca"; \
	  fi; \
	fi
