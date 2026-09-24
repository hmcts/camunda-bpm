#!/usr/bin/env bash

# Proves that a restarted pod resolves a mutable tag to its current ACR digest.
# Run only against a disposable Preview, Plum, or Toffee deployment.
set -euo pipefail

required_variables=(
  ACR_NAME
  AZURE_SUBSCRIPTION
  REPOSITORY
  TEMP_TAG
  SOURCE_DIGEST_ONE
  SOURCE_DIGEST_TWO
  NAMESPACE
  DEPLOYMENT
  CONTAINER
  POD_SELECTOR
)

for variable in "${required_variables[@]}"; do
  if [[ -z "${!variable:-}" ]]; then
    echo "${variable} must be set" >&2
    exit 2
  fi
done

if [[ "${TEMP_TAG}" != image-pull-policy-test-* ]]; then
  echo 'TEMP_TAG must start with image-pull-policy-test-' >&2
  exit 2
fi

if [[ "${SOURCE_DIGEST_ONE}" == "${SOURCE_DIGEST_TWO}" ]]; then
  echo 'SOURCE_DIGEST_ONE and SOURCE_DIGEST_TWO must differ' >&2
  exit 2
fi

registry="${ACR_NAME}.azurecr.io"
image="${registry}/${REPOSITORY}:${TEMP_TAG}"
deployment_changed=false
temporary_tag_created=false
original_image=''

cleanup() {
  if [[ "${deployment_changed}" == true ]]; then
    kubectl -n "${NAMESPACE}" set image "deployment/${DEPLOYMENT}" \
      "${CONTAINER}=${original_image}" || true
  fi

  if [[ "${temporary_tag_created}" == true ]]; then
    az acr repository untag --name "${ACR_NAME}" --subscription "${AZURE_SUBSCRIPTION}" \
      --image "${REPOSITORY}:${TEMP_TAG}" --only-show-errors || true
  fi
}
trap cleanup EXIT

if az acr repository show --name "${ACR_NAME}" --subscription "${AZURE_SUBSCRIPTION}" \
  --image "${REPOSITORY}:${TEMP_TAG}" --only-show-errors >/dev/null 2>&1; then
  echo "Temporary tag already exists: ${REPOSITORY}:${TEMP_TAG}" >&2
  exit 2
fi

container_spec="$(kubectl -n "${NAMESPACE}" get deployment "${DEPLOYMENT}" -o json \
  | jq -er --arg container "${CONTAINER}" '.spec.template.spec.containers[] | select(.name == $container)')"
image_pull_policy="$(jq -er '.imagePullPolicy' <<<"${container_spec}")"
original_image="$(jq -er '.image' <<<"${container_spec}")"

if [[ "${image_pull_policy}" != Always ]]; then
  echo "Expected ${DEPLOYMENT}/${CONTAINER} to use imagePullPolicy Always; got ${image_pull_policy}" >&2
  exit 1
fi

set_tag_to_digest() {
  local expected_digest="$1"

  az acr import --name "${ACR_NAME}" --subscription "${AZURE_SUBSCRIPTION}" \
    --source "${registry}/${REPOSITORY}@${expected_digest}" \
    --image "${REPOSITORY}:${TEMP_TAG}" --force --only-show-errors
  temporary_tag_created=true

  local actual_digest
  actual_digest="$(az acr repository show --name "${ACR_NAME}" --subscription "${AZURE_SUBSCRIPTION}" \
    --image "${REPOSITORY}:${TEMP_TAG}" --query digest -o tsv)"
  [[ "${actual_digest}" == "${expected_digest}" ]] || {
    echo "Temporary tag resolved to ${actual_digest}, expected ${expected_digest}" >&2
    exit 1
  }
}

pod_image_id() {
  local pod
  pod="$(kubectl -n "${NAMESPACE}" get pods -l "${POD_SELECTOR}" -o json \
    | jq -er '.items | map(select(.status.phase == "Running")) | max_by(.status.startTime) | .metadata.name')"
  kubectl -n "${NAMESPACE}" get pod "${pod}" -o json \
    | jq -er --arg container "${CONTAINER}" \
      '.status.containerStatuses[] | select(.name == $container) | .imageID'
}

set_tag_to_digest "${SOURCE_DIGEST_ONE}"
kubectl -n "${NAMESPACE}" set image "deployment/${DEPLOYMENT}" "${CONTAINER}=${image}"
deployment_changed=true
kubectl -n "${NAMESPACE}" rollout status "deployment/${DEPLOYMENT}" --timeout=10m
first_image_id="$(pod_image_id)"
echo "First pod imageID: ${first_image_id}"

set_tag_to_digest "${SOURCE_DIGEST_TWO}"
kubectl -n "${NAMESPACE}" rollout restart "deployment/${DEPLOYMENT}"
kubectl -n "${NAMESPACE}" rollout status "deployment/${DEPLOYMENT}" --timeout=10m
second_image_id="$(pod_image_id)"
echo "Second pod imageID: ${second_image_id}"

if [[ "${first_image_id}" == "${second_image_id}" ]]; then
  echo 'Restarted pod retained the first imageID after the tag moved' >&2
  exit 1
fi

echo "imagePullPolicy Always verified: the restarted pod used the moved tag's second image."
