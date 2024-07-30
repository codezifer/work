#!/bin/bash
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