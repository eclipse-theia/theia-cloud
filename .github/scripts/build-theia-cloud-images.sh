#!/usr/bin/env bash
#
# Shared image-build helper for the e2e CI workflows.
#
# Builds the 8 Theia Cloud Docker images that the e2e Playwright suite
# depends on, tags them under a configurable registry prefix and tag,
# and (optionally) runs a post-build hook on each image. The hook is
# how the OpenShift workflow transfers each image into MicroShift's
# CRI-O containers-storage via skopeo without standing up a registry.
#
# Usage:
#   build-theia-cloud-images.sh [options]
#
# Options:
#   --tag <tag>                  Image tag (e.g. "minikube-ci-e2e",
#                                "microshift-ci-e2e"). Required.
#   --registry-prefix <prefix>   Prefix prepended to the per-image name
#                                (e.g. "theiacloud/theia-cloud-" or
#                                "localhost/theia-cloud-"). Required.
#   --no-cache                   Pass --no-cache to docker build.
#   --post-build <command>       Shell command run after each successful
#                                build. Receives LOCAL_TAG in env (the
#                                fully-qualified image reference). The
#                                command runs in the working directory
#                                of this script's caller.
#
# Must be invoked from the theia-cloud repository root (Dockerfiles use
# repo-root build contexts).

set -euo pipefail

TAG=""
REGISTRY_PREFIX=""
NO_CACHE=""
POST_BUILD=""

while [ $# -gt 0 ]; do
    case "$1" in
        --tag)
            TAG="$2"
            shift 2
            ;;
        --registry-prefix)
            REGISTRY_PREFIX="$2"
            shift 2
            ;;
        --no-cache)
            NO_CACHE="--no-cache"
            shift
            ;;
        --post-build)
            POST_BUILD="$2"
            shift 2
            ;;
        *)
            echo "::error::unknown argument: $1" >&2
            exit 2
            ;;
    esac
done

if [ -z "$TAG" ]; then
    echo "::error::--tag is required" >&2
    exit 2
fi
if [ -z "$REGISTRY_PREFIX" ]; then
    echo "::error::--registry-prefix is required" >&2
    exit 2
fi

build_image() {
    local name=$1
    local dockerfile=$2
    local context=$3
    local local_tag="${REGISTRY_PREFIX}${name}:${TAG}"

    echo "::group::Build ${local_tag}"
    # shellcheck disable=SC2086
    docker build $NO_CACHE -t "${local_tag}" -f "${dockerfile}" "${context}"
    echo "::endgroup::"

    if [ -n "$POST_BUILD" ]; then
        echo "::group::Post-build hook for ${local_tag}"
        LOCAL_TAG="${local_tag}" bash -c "${POST_BUILD}"
        echo "::endgroup::"
    fi
}

# Core Theia Cloud images (deployments under the theia-cloud namespace).
build_image service            dockerfiles/service/Dockerfile             .
build_image operator           dockerfiles/operator/Dockerfile            .
build_image landing-page       dockerfiles/landing-page/Dockerfile        .
build_image wondershaper       dockerfiles/wondershaper/Dockerfile        .
build_image conversion-webhook dockerfiles/conversion-webhook/Dockerfile  .

# Demo images (referenced by AppDefinitions).
build_image demo                demo/dockerfiles/demo-theia-docker/Dockerfile         demo/dockerfiles/demo-theia-docker/.
build_image activity-demo       demo/dockerfiles/demo-theia-monitor-vscode/Dockerfile demo/dockerfiles/demo-theia-monitor-vscode/.
build_image activity-demo-theia demo/dockerfiles/demo-theia-monitor-theia/Dockerfile  .
