#!/bin/bash
#
# Container entrypoint for the MicroShift CI image.
#
# Two filesystem fixes are needed BEFORE systemd/microshift/ovn start,
# because once nested containers are running it is too late.
#
# 1. Mount-propagation (Spike A run 5)
#
# Docker mounts the container's root filesystem as `private` propagation
# by default, even with `--privileged`. Several OpenShift system pods
# (notably ovnkube-master, but also the OVS-aware bits of multus and
# the ingress router) bind-mount the host root with `rshared`
# propagation so they can publish OVS sockets and netns mounts visible
# to peer pods. Linux refuses an `rshared` child of a `private`
# parent and runc fails with:
#
#   path "/" is mounted on "/" but it is not a shared or slave mount
#
# Remounting / as rshared once at PID-1 startup fixes this for every
# nested container started afterwards. Same workaround `kind` and
# `k3s-in-docker` apply.
#
# 2. /run must allow exec (Spike A run 7)
#
# OVN-Kubernetes drops its CNI plugin binary at
# /run/cni/bin/ovn-k8s-cni-overlay; cri-o validates the CNI config by
# exec'ing that binary. The runner's Docker mounts /run as tmpfs with
# `noexec` by default, so the exec returns EACCES, cri-o silently
# discards the OVN network in favour of crio-bridge + loopback, and
# kubelet reports "no CNI configuration file" -- confusing because
# the file IS there; it just can't be loaded. Remounting /run with
# exec is idempotent if it already permits exec.
set -e

mount --make-rshared /
mount -o remount,exec /run

exec /sbin/init
