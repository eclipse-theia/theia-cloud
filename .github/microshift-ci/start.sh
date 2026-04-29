#!/usr/bin/env bash
#
# Host-side helper for Spike A: start a MicroShift container, wait until
# the API server is reachable and the node is Ready, then export a
# kubeconfig that targets it via 127.0.0.1.
#
# Usage:
#   start.sh PULL_SECRET_PATH [IMAGE]
#
# Arguments:
#   PULL_SECRET_PATH  Path to a Red Hat pull-secret JSON file. REQUIRED.
#                     Obtain from console.redhat.com/openshift/install/pull-secret
#                     (free Red Hat developer account). Bind-mounted into
#                     the container at /etc/crio/openshift-pull-secret so
#                     MicroShift can pull subscription-gated images
#                     (OVN, DNS, router, etc.) from
#                     quay.io/openshift-release-dev/ocp-v4.0-art-dev.
#   IMAGE             Local Docker image tag to run (default: microshift-ci:local).
#
# Environment overrides:
#   MICROSHIFT_CONTAINER_NAME  Container name (default: microshift)
#   WAIT_TIMEOUT               Seconds to wait for Ready (default: 600)
#   KUBECONFIG_OUT             Output kubeconfig path
#                              (default: $HOME/.kube/microshift-config)
#   NODE_NOT_READY_GRACE       Seconds the node may stay NotReady after
#                              registering before we dump pod diagnostics
#                              (default: 120; Spike A run 4 lesson).

set -euo pipefail

PULL_SECRET_PATH="${1:-}"
IMAGE="${2:-microshift-ci:local}"
NAME="${MICROSHIFT_CONTAINER_NAME:-microshift}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-600}"
KUBECONFIG_OUT="${KUBECONFIG_OUT:-$HOME/.kube/microshift-config}"
NODE_NOT_READY_GRACE="${NODE_NOT_READY_GRACE:-120}"

log() { printf '[microshift-ci] %s\n' "$*"; }

# Validate the pull secret BEFORE any docker operations. Without it,
# microshift's bundled OVN/DNS/router/pause images cannot be pulled
# (Spike A run 4 lesson) and the node never reaches Ready. Failing
# fast here saves ~10 min of pointless polling on misconfigured runs.
if [ -z "$PULL_SECRET_PATH" ]; then
    log "ERROR: missing PULL_SECRET_PATH (first argument)."
    log "       MicroShift requires a Red Hat pull secret to pull its"
    log "       subscription-gated control-plane images. In CI, set the"
    log "       REDHAT_PULL_SECRET GitHub Actions secret on this repo,"
    log "       write it to a file, and pass that path as the first arg."
    log "       See: console.redhat.com/openshift/install/pull-secret"
    exit 2
fi
if [ ! -f "$PULL_SECRET_PATH" ]; then
    log "ERROR: pull secret file does not exist: $PULL_SECRET_PATH"
    exit 2
fi
if [ ! -s "$PULL_SECRET_PATH" ]; then
    log "ERROR: pull secret file is empty: $PULL_SECRET_PATH"
    log "       (likely the REDHAT_PULL_SECRET GH secret is unset or empty)"
    exit 2
fi

dump_diagnostics() {
    if ! docker inspect "$NAME" >/dev/null 2>&1; then
        log "(no container to inspect)"
        return
    fi
    log "--- container status ---"
    docker ps -a --filter "name=^/${NAME}$" || true
    log "--- container logs (tail) ---"
    docker logs --tail 200 "$NAME" 2>&1 || true
    log "--- microshift journal (tail) ---"
    docker exec "$NAME" journalctl -u microshift --no-pager 2>&1 | tail -200 || true
    log "--- crio journal (tail) ---"
    docker exec "$NAME" journalctl -u crio --no-pager 2>&1 | tail -200 || true
}

trap 'rc=$?; if [ "$rc" -ne 0 ]; then dump_diagnostics; fi; exit "$rc"' EXIT

# Remove any stale container from a previous run.
if docker inspect "$NAME" >/dev/null 2>&1; then
    log "Removing existing container $NAME"
    docker rm -f "$NAME" >/dev/null
fi

log "Starting MicroShift container ($IMAGE)"
log "  pull secret: $PULL_SECRET_PATH"
docker run -d \
    --name "$NAME" \
    --privileged \
    --hostname microshift \
    --cgroupns=host \
    -v /sys/fs/cgroup:/sys/fs/cgroup:rw \
    -v /lib/modules:/lib/modules:ro \
    -v "${PULL_SECRET_PATH}:/etc/crio/openshift-pull-secret:ro" \
    --tmpfs /run \
    --tmpfs /tmp \
    -p 6443:6443 \
    -p 80:80 \
    -p 443:443 \
    "$IMAGE" >/dev/null

deadline=$(( $(date +%s) + WAIT_TIMEOUT ))

# systemd rate-limits unit restarts after 5 failures in 10 seconds and
# gives up. Polling for a kubeconfig that will never appear past that
# point just burns the full WAIT_TIMEOUT (Spike A run 3 lesson). Check
# the unit state on every iteration and bail fast on `failed`.
check_microshift_failed() {
    local state
    state=$(docker exec "$NAME" systemctl is-failed microshift.service 2>/dev/null || true)
    if [ "$state" = "failed" ]; then
        log "microshift.service entered 'failed' state — bailing out"
        log "--- microshift unit status ---"
        docker exec "$NAME" systemctl status --no-pager microshift.service 2>&1 || true
        log "--- microshift + crio journal (current boot) ---"
        docker exec "$NAME" journalctl -u microshift -u crio --no-pager -b 2>&1 || true
        return 1
    fi
    return 0
}

log "Waiting for kubeconfig to appear inside the container"
while ! docker exec "$NAME" test -f /var/lib/microshift/resources/kubeadmin/kubeconfig 2>/dev/null; do
    if ! check_microshift_failed; then
        exit 1
    fi
    if (( $(date +%s) > deadline )); then
        log "Timed out waiting for kubeconfig (${WAIT_TIMEOUT}s)"
        exit 1
    fi
    sleep 5
done

mkdir -p "$(dirname "$KUBECONFIG_OUT")"
docker cp "${NAME}:/var/lib/microshift/resources/kubeadmin/kubeconfig" "$KUBECONFIG_OUT"
chmod 600 "$KUBECONFIG_OUT"

# Point the kubeconfig at the published port on the runner host. The
# server certificate is signed for in-cluster names, so we mark the
# cluster as insecure-skip-tls-verify for spike-level connectivity
# tests (the production workflow can replace this with proper SAN
# configuration later).
cluster_name=$(kubectl --kubeconfig "$KUBECONFIG_OUT" config view --raw -o jsonpath='{.clusters[0].name}')
if [ -z "$cluster_name" ]; then
    log "Could not determine cluster name from kubeconfig"
    cat "$KUBECONFIG_OUT"
    exit 1
fi
kubectl --kubeconfig "$KUBECONFIG_OUT" config set-cluster "$cluster_name" \
    --server=https://127.0.0.1:6443 >/dev/null
kubectl --kubeconfig "$KUBECONFIG_OUT" config set \
    "clusters.${cluster_name}.insecure-skip-tls-verify" true >/dev/null
kubectl --kubeconfig "$KUBECONFIG_OUT" config unset \
    "clusters.${cluster_name}.certificate-authority-data" >/dev/null 2>&1 || true

log "Wrote kubeconfig to $KUBECONFIG_OUT"

# Detect the "node registered but stuck NotReady" failure mode and
# dump the diagnostics that have surfaced real failures in past runs:
#
#   - run 4: subscription-gated image pulls   -> visible in `describe pods`
#   - run 5: rshared mount-propagation error  -> visible in pod events
#   - run 6: OVS modprobe / OVN setup hang    -> visible in OVN pod logs,
#                                                 OVS journals, and the
#                                                 missing CNI config file
#   - run 7: cri-o EACCES exec'ing OVN CNI    -> visible in /run mount
#           binary (because /run is noexec)     options, ls of the binary,
#                                                 and a direct --help exec
#
# Run 6 specifically taught us that filtering on `phase != Running`
# misses the case where OVN pods are Running-but-not-functional, so we
# describe ALL pods (capped to head -200 to keep the workflow log
# readable).
dump_node_diagnostics() {
    log "--- pods (all namespaces) ---"
    kubectl --kubeconfig "$KUBECONFIG_OUT" get pods -A -o wide || true

    log "--- describe pods -A (head -200) ---"
    kubectl --kubeconfig "$KUBECONFIG_OUT" describe pods -A 2>&1 | head -200 || true

    log "--- ovnkube-node logs (all containers, tail 200) ---"
    kubectl --kubeconfig "$KUBECONFIG_OUT" logs \
        -n openshift-ovn-kubernetes -l app=ovnkube-node \
        --all-containers --tail=200 2>&1 || true

    log "--- ovnkube-master logs (all containers, tail 200) ---"
    kubectl --kubeconfig "$KUBECONFIG_OUT" logs \
        -n openshift-ovn-kubernetes -l app=ovnkube-master \
        --all-containers --tail=200 2>&1 || true

    log "--- openvswitch / microshift-ovs-init unit status ---"
    docker exec "$NAME" systemctl status --no-pager \
        openvswitch microshift-ovs-init 2>&1 || true

    log "--- openvswitch / microshift-ovs-init journal (tail 100) ---"
    docker exec "$NAME" journalctl -u openvswitch -u microshift-ovs-init \
        --no-pager 2>&1 | tail -100 || true

    log "--- /etc/cni/net.d/ (CNI handoff marker) ---"
    docker exec "$NAME" ls -la /etc/cni/net.d/ 2>&1 || true

    # Run 7 layer: cri-o exec'ing the OVN CNI plugin returns EACCES
    # because /run is noexec. The next iteration of these dumps
    # distinguishes between (a) /run still mounted noexec (entrypoint
    # remount didn't take effect), (b) the binary missing or not
    # executable (run 6-style OVN setup incomplete), and (c) AppArmor
    # blocking exec from /run despite --privileged.
    log "--- /run/cni/bin/ contents ---"
    docker exec "$NAME" ls -la /run/cni/bin/ 2>&1 || true

    log "--- /run mount options (looking for noexec) ---"
    docker exec "$NAME" sh -c "cat /proc/mounts | grep -E ' /run( |\b)'" 2>&1 || true

    log "--- direct exec of OVN CNI binary (--help) ---"
    docker exec "$NAME" /run/cni/bin/ovn-k8s-cni-overlay --help 2>&1 | head -10 || true

    log "--- SELinux mode (getenforce) ---"
    docker exec "$NAME" getenforce 2>&1 || true

    log "--- OVN CNI binary stat (mode/owner) ---"
    docker exec "$NAME" stat -c '%A %u %g %n' /run/cni/bin/ovn-k8s-cni-overlay 2>&1 || true
}

log "Waiting for node to report Ready"
node_first_seen_at=0
diagnostics_dumped=0
while true; do
    if kubectl --kubeconfig "$KUBECONFIG_OUT" get nodes 2>/dev/null \
            | awk 'NR>1 && $2=="Ready" {found=1} END {exit !found}'; then
        break
    fi

    # Track the moment the node first appears in `get nodes` output, so
    # we can measure how long it has been registered-but-NotReady.
    if [ "$node_first_seen_at" = "0" ]; then
        if kubectl --kubeconfig "$KUBECONFIG_OUT" get nodes 2>/dev/null \
                | awk 'NR>1' | grep -q .; then
            node_first_seen_at=$(date +%s)
            log "Node registered, waiting for Ready"
        fi
    fi

    if [ "$diagnostics_dumped" = "0" ] && [ "$node_first_seen_at" != "0" ]; then
        if (( $(date +%s) - node_first_seen_at > NODE_NOT_READY_GRACE )); then
            log "Node has been registered for >${NODE_NOT_READY_GRACE}s but is still NotReady"
            log "Dumping node diagnostics to surface the underlying error"
            dump_node_diagnostics
            diagnostics_dumped=1
        fi
    fi

    if ! check_microshift_failed; then
        exit 1
    fi
    if (( $(date +%s) > deadline )); then
        log "Timed out waiting for Ready node (${WAIT_TIMEOUT}s)"
        kubectl --kubeconfig "$KUBECONFIG_OUT" get nodes -o wide || true
        # Final node dump if we haven't already.
        if [ "$diagnostics_dumped" = "0" ]; then
            dump_node_diagnostics
        fi
        exit 1
    fi
    sleep 5
done

log "MicroShift is up:"
kubectl --kubeconfig "$KUBECONFIG_OUT" get nodes -o wide

# Expose the kubeconfig to subsequent steps when running under GitHub Actions.
if [ -n "${GITHUB_ENV:-}" ]; then
    echo "KUBECONFIG=$KUBECONFIG_OUT" >> "$GITHUB_ENV"
fi
