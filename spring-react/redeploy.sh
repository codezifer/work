#!/bin/bash

VERSION="1.0.0-SNAPSHOT"

#eval $(minikube docker-env)

kubectl delete deployment backend
kubectl delete service backend
kubectl delete ingress backend

kubectl delete deployment postgres
kubectl delete secret postgres
kubectl delete configmap postgres
kubectl delete pvc postgres-pvc
kubectl delete pv postgres-pv
kubectl delete service postgres

kubectl delete deployment react-frontend
kubectl delete configmap frontend
kubectl delete service frontend
kubectl delete ingress frontend

#minikube image rm "de.carsten.spring-react.backend:$VERSION"
#minikube image load "de.carsten.spring-react.backend:$VERSION"

#minikube image rm "de.carsten.spring-react.frontend:$VERSION"
#minikube image load "de.carsten.spring-react.frontend:$VERSION"

kubectl apply -f src/main/k8s/backend/volumes.yaml
kubectl apply -f src/main/k8s/backend/secret.yaml
kubectl apply -f src/main/k8s/backend/configmap.yaml
kubectl apply -f src/main/k8s/backend/deployment.yaml
kubectl apply -f src/main/k8s/backend/service.yaml

kubectl apply -f src/main/k8s/frontend/configmap.yaml
kubectl apply -f src/main/k8s/frontend/deployment.yaml
kubectl apply -f src/main/k8s/frontend/service.yaml
