# Comandos comunes de SynKra. Corren igual en Linux y en WSL2 (Ubuntu).
CLUSTER ?= synkra
COUNTRY ?= co

.PHONY: help setup doctor cluster cluster-delete status

help: ## Lista los comandos
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN{FS=":.*## "}{printf "  %-16s %s\n", $$1, $$2}'

setup: ## Instala las herramientas (Ubuntu/Debian/WSL2)
	./scripts/setup-linux.sh

doctor: ## Verifica herramientas y Docker
	./scripts/doctor.sh

cluster: ## Crea el clúster kind local
	@kind get clusters 2>/dev/null | grep -qx $(CLUSTER) || kind create cluster --name $(CLUSTER) --wait 180s
	kubectl wait --for=condition=Ready nodes --all --timeout=180s --context kind-$(CLUSTER)
	kubectl cluster-info --context kind-$(CLUSTER)

cluster-delete: ## Borra el clúster kind local
	kind delete cluster --name $(CLUSTER)

status: ## Muestra nodos y pods del clúster
	kubectl get nodes -o wide
	kubectl get pods -A
