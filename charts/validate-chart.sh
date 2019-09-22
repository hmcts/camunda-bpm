#!/usr/bin/env bash

CHART="camunda-bpm"
RELEASE="camunda-bpm-release"
NAMESPACE="money-claims"

helm install ${CHART} --name ${RELEASE} \
  --namespace ${NAMESPACE} \
  --debug --dry-run \
  -f ci-values.yaml --wait --timeout 160

