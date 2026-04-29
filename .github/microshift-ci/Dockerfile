# syntax=docker/dockerfile:1
#
# MicroShift in a privileged container for CI.
#
# This image bundles upstream MicroShift on top of CentOS Stream 9 and
# runs systemd as PID 1. Intended to be launched with:
#
#   docker run -d --privileged --cgroupns=host \
#       -v /sys/fs/cgroup:/sys/fs/cgroup:rw \
#       -v /lib/modules:/lib/modules:ro \
#       --tmpfs /run --tmpfs /tmp \
#       -p 6443:6443 -p 80:80 -p 443:443 \
#       microshift-ci:local
#
# See .github/microshift-ci/start.sh for the host-side wrapper used in CI.
#
# Notes from Spike A runs 1–7 (2026-04-29):
#   - The `@redhat-et/microshift` Copr is dead (last build 2021).
#   - The `@microshift/` Copr group on Fedora Copr is empty (no projects,
#     no builds), so any `@microshift/microshift-X.Y` reference fails at
#     `dnf copr enable`.
#   - The verified working RPM source is the official OpenShift mirror
#     at `mirror.openshift.com`, which serves the same MicroShift bits
#     Red Hat ships under subscription, on two anonymous yum repos.
#     No GPG keys are published alongside, so `gpgcheck=0` is required.
#   - Reproducibility: pin to a specific RC tag (e.g. `4.18.0-rc.10`),
#     NOT the moving `latest-4.18` symlink.
#   - microshift shells out to `/bin/hostname` at startup; the package
#     must be installed explicitly because it is not in the minimal
#     base nor pulled transitively by any microshift-* dep.
#   - cri-o reads `/etc/crio/openshift-pull-secret` to authenticate to
#     `quay.io/openshift-release-dev/ocp-v4.0-art-dev`. The path is
#     bind-mounted from the runner by `start.sh`; without it the
#     OVN/DNS/router pods fail to pull and the node never goes Ready.
#   - Docker's default `private` mount propagation on `/` blocks the
#     `rshared` bind mounts that OVN-Kubernetes (and other openshift-*
#     system pods) need. We wrap /sbin/init with entrypoint.sh which
#     runs `mount --make-rshared /` first.
#   - The runner's Docker mounts `/run` as tmpfs with `noexec`. cri-o
#     validates a CNI config by exec'ing the plugin binary; OVN drops
#     `/run/cni/bin/ovn-k8s-cni-overlay` there, the exec returns
#     EACCES, and cri-o falls back to crio-bridge + loopback (node
#     never goes Ready). entrypoint.sh remounts /run with `exec` to
#     fix this.

ARG BASE_IMAGE=quay.io/centos/centos:stream9
# MicroShift release tag to pin (e.g. 4.18.0-rc.10). Browse
# https://mirror.openshift.com/pub/openshift-v4/x86_64/microshift/ocp/
# for available RC directories. There are no GA dirs in the public
# mirror; only `*-rc.*` and (in `…/ocp-dev-preview/…`) `*-ec.*`.
ARG MICROSHIFT_RELEASE=4.18.0-rc.10
# Minor line corresponding to MICROSHIFT_RELEASE. Used both in the local
# repo `[id]` and in the dependencies-repo URL.
ARG MICROSHIFT_LINE=4.18

FROM ${BASE_IMAGE}

ARG MICROSHIFT_RELEASE
ARG MICROSHIFT_LINE

# Base packages: systemd as PID 1, plus userspace required by
# microshift's startup checks.
#
#   - `hostname` is mandatory (Spike A run 3 lesson): microshift shells
#     out to /bin/hostname during config defaulting; without it the
#     daemon dies in <1 s. The package is NOT in CentOS Stream 9's
#     minimal base image and is NOT pulled transitively by any
#     microshift-* dep.
#   - `iputils`, `iproute`, `procps-ng` are listed explicitly even
#     though they happen to be pulled by microshift-networking today.
#     Same class of silent breakage as `hostname` if a future minor
#     changes its deps; cheap to defend against.
RUN dnf -y install \
        systemd \
        ca-certificates \
        hostname \
        iputils \
        iproute \
        procps-ng && \
    dnf clean all

# Mask units that are not relevant inside a container and tend to fail
# noisily, polluting `journalctl` output. Validated working in
# Spike A runs 1–7.
RUN systemctl mask \
        systemd-firstboot.service \
        systemd-udevd.service \
        systemd-udev-trigger.service \
        systemd-hwdb-update.service \
        systemd-tmpfiles-setup.service \
        systemd-tmpfiles-setup-dev.service \
        systemd-tmpfiles-clean.service \
        systemd-tmpfiles-clean.timer \
        getty.target \
        getty@.service \
        console-getty.service

# Configure the two anonymous mirror.openshift.com yum repos that ship
# MicroShift and its dependencies (cri-o, openshift-clients, openvswitch3,
# etc.). Shell-level `:=` defaults guard against an empty `--build-arg`
# from the workflow (defense in depth — with literal mirror URLs an
# empty value would surface as a 404, but pinning is too important to
# rely on that alone).
RUN : "${MICROSHIFT_RELEASE:=4.18.0-rc.10}" && \
    : "${MICROSHIFT_LINE:=4.18}" && \
    cat > /etc/yum.repos.d/microshift.repo <<EOF
[microshift-${MICROSHIFT_LINE}]
name=MicroShift ${MICROSHIFT_LINE}
baseurl=https://mirror.openshift.com/pub/openshift-v4/x86_64/microshift/ocp/${MICROSHIFT_RELEASE}/el9/os/
enabled=1
gpgcheck=0

[microshift-${MICROSHIFT_LINE}-deps]
name=MicroShift ${MICROSHIFT_LINE} Dependencies
baseurl=https://mirror.openshift.com/pub/openshift-v4/x86_64/dependencies/rpms/${MICROSHIFT_LINE}-el9-beta/
enabled=1
gpgcheck=0
EOF

# Install MicroShift, CRI-O, and the openshift-clients package (oc CLI).
# `skopeo` is bundled in the same dependencies repo and is required by
# Spike C's registry-free image hand-off (`docker save | skopeo copy
# docker-archive:- containers-storage:...`).
RUN dnf -y install \
        cri-o cri-tools \
        microshift microshift-networking microshift-greenboot \
        microshift-release-info microshift-selinux \
        openshift-clients \
        skopeo && \
    dnf clean all

# Enable services so they start under PID-1 systemd.
RUN systemctl enable crio.service microshift.service

ENV KUBECONFIG=/var/lib/microshift/resources/kubeadmin/kubeconfig

# systemd uses SIGRTMIN+3 for clean container shutdown.
STOPSIGNAL SIGRTMIN+3

EXPOSE 6443 80 443

# Wrap /sbin/init in entrypoint.sh which makes two filesystem fixes
# that MUST happen before any nested container runs:
#   - `mount --make-rshared /`  for OVN-Kubernetes rshared bind mounts
#                               (Spike A run 5 lesson)
#   - `mount -o remount,exec /run`  so cri-o can exec the OVN CNI
#                                   plugin binary (Spike A run 7 lesson)
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh
CMD ["/usr/local/bin/entrypoint.sh"]
