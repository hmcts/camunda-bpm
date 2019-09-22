#!/usr/bin/env bash

CHART="camunda-bpm"
RELEASE="camunda-bpm-release"
NAMESPACE="money-claims"

helm upgrade ${RELEASE} ${CHART}  \
  -f ci-values.yaml \
  --wait --timeout 160 \
  --namespace ${NAMESPACE}


