#!/bin/bash

VERSION="1.0.0-SNAPSHOT"

kubectl delete deployment backend
kubectl delete service backend
kubectl delete ingress backend

kubectl delete deployment frontend
kubectl delete service frontend
kubectl delete ingress frontend

minikube image rm "de.carsten.spring-react.backend:$VERSION"
minikube image load "de.carsten.spring-react.backend:$VERSION"

minikube image rm "de.carsten.spring-react.frontend:$VERSION"
minikube image load "de.carsten.spring-react.frontend:$VERSION"

kubectl apply -f src/main/k8s/backend/deployment.yaml
kubectl apply -f src/main/k8s/backend/service.yaml

kubectl apply -f src/main/k8s/frontend/deployment.yaml
kubectl apply -f src/main/k8s/frontend/service.yaml
