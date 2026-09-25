#!/usr/bin/env bash

set +e

NAMESPACE="${1:-keycloak}"
EXTERNAL_URL="${2:-}"
INGRESS_KIND="${3:-}"
BASE_PATH="${KEYCLOAK_RELATIVE_PATH:-/keycloak}"
BASE_PATH="${BASE_PATH%/}"
POD=$(kubectl get pods -n "${NAMESPACE}" -l app=keycloak -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)

section() {
  echo ""
  echo "=== $1 ==="
}

request_in_pod() {
  local port="$1"
  local path="$2"

  if [ -z "${POD}" ]; then
    echo "Keycloak pod not found"
    return
  fi

  kubectl exec -n "${NAMESPACE}" "${POD}" -c keycloak -- bash -c \
    "exec 3<>/dev/tcp/127.0.0.1/${port} && printf 'GET ${path} HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n' >&3 && head -40 <&3" 2>&1
}

section "Keycloak operator CRDs"
kubectl get customresourcedefinition \
  keycloaks.k8s.keycloak.org \
  keycloakrealmimports.k8s.keycloak.org \
  keycloakoidcclients.k8s.keycloak.org \
  keycloaksamlclients.k8s.keycloak.org || true

section "Keycloak custom resource status"
kubectl get keycloak -n "${NAMESPACE}" keycloak \
  -o jsonpath='{.status.conditions}{"\n"}' || true

section "Keycloak workload probes and environment names"
kubectl get statefulset -n "${NAMESPACE}" keycloak \
  -o jsonpath='{range .spec.template.spec.containers[*]}container={.name}{"\n"}readinessProbe={.readinessProbe}{"\n"}livenessProbe={.livenessProbe}{"\n"}startupProbe={.startupProbe}{"\n"}{range .env[*]}env={.name}{"\n"}{end}{end}' || true

section "Keycloak service and endpoints"
kubectl get service -n "${NAMESPACE}" keycloak-service -o yaml || true
kubectl get endpoints -n "${NAMESPACE}" keycloak-service -o yaml || true

section "Keycloak pods"
kubectl get pods -n "${NAMESPACE}" -o wide || true
kubectl describe pods -n "${NAMESPACE}" -l app=keycloak \
  | sed '/KC_BOOTSTRAP_ADMIN_PASSWORD/d' \
  | tail -100 || true

section "Keycloak logs"
kubectl logs -n "${NAMESPACE}" -l app=keycloak -c keycloak --tail=300 || true

section "Keycloak log highlights"
kubectl logs -n "${NAMESPACE}" -l app=keycloak -c keycloak --tail=500 2>/dev/null \
  | grep -Ei 'started in|error|warn|503|hostname|bootstrap' || true

section "Keycloak operator logs"
kubectl logs -n "${NAMESPACE}" -l app.kubernetes.io/name=keycloak-operator --tail=150 || true

section "In-pod master realm response"
request_in_pod 8080 "${BASE_PATH}/realms/master"

section "In-pod readiness response"
request_in_pod 9000 "${BASE_PATH}/health/ready"

if [ -n "${EXTERNAL_URL}" ]; then
  EXTERNAL_URL="${EXTERNAL_URL%/}"
  section "External Keycloak response"
  curl -kisS --max-time 10 "${EXTERNAL_URL}/realms/master" | head -40 || true
fi

section "Ingress"
kubectl describe ingress -n "${NAMESPACE}" keycloak || true

section "OpenShift Route"
kubectl describe route -n "${NAMESPACE}" keycloak || true

case "${INGRESS_KIND}" in
  nginx)
    section "NGINX ingress logs"
    kubectl logs -n ingress-nginx deployment/ingress-nginx-controller --tail=300 2>/dev/null \
      | grep -i keycloak || true
    ;;
  haproxy)
    section "HAProxy ingress logs"
    kubectl logs -n ingress-haproxy deployment/haproxy-ingress --tail=300 2>/dev/null \
      | grep -i keycloak || true
    ;;
  openshift)
    section "OpenShift router logs"
    kubectl logs -n openshift-ingress deployment/router-default --tail=300 2>/dev/null \
      | grep -i keycloak || true
    ;;
esac

section "Keycloak certificates"
kubectl get certificate -n "${NAMESPACE}" -o wide || true

section "Recent Keycloak namespace events"
kubectl get events -n "${NAMESPACE}" --sort-by=.lastTimestamp | tail -50 || true
