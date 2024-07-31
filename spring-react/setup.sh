#!/bin/bash
kubectl apply -f src/main/k8s/backend/volumes.yaml
kubectl apply -f src/main/k8s/backend/secret.yaml
kubectl apply -f src/main/k8s/backend/configmap.yaml
kubectl apply -f src/main/k8s/backend/deployment.yaml
kubectl apply -f src/main/k8s/backend/service.yaml

kubectl apply -f src/main/k8s/frontend/deployment.yaml
kubectl apply -f src/main/k8s/frontend/service.yaml

kubectl apply -f src/main/k8s/keycloak/secret.yaml
kubectl apply -f src/main/k8s/keycloak/deployment.yaml
kubectl apply -f src/main/k8s/keycloak/service.yaml
