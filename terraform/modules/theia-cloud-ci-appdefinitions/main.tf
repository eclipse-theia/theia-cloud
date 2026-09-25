# Shared AppDefinitions used by the e2e Playwright suite. Consumed by
# both the minikube CI config (terraform/ci-configurations/) and the
# OpenShift CI config (terraform/test-configurations/5-02_openshift_ci/).
#
# The image references differ between the two: minikube reuses the
# locally-built docker tags directly (`theiacloud/*:minikube-ci-e2e`,
# IfNotPresent), while OpenShift transfers the same images into
# MicroShift's CRI-O storage under `localhost/*:microshift-ci-e2e` and
# uses `Never` so the kubelet does not attempt a registry pull.

resource "kubectl_manifest" "theia-cloud-monitor-theia-popup" {
  yaml_body = <<-EOF
  apiVersion: theia.cloud/v1beta10
  kind: AppDefinition
  metadata:
    name: theia-cloud-monitor-theia-popup
    namespace: theia-cloud
  spec:
    name: theia-cloud-monitor-theia-popup
    image: ${var.image_theia}
    imagePullPolicy: ${var.image_pull_policy}
    uid: 101
    port: 3000
    ingressname: theia-cloud-demo-ws-ingress
    ingressHostnamePrefixes: []
    minInstances: ${var.eager_start ? 1 : 0}
    maxInstances: 10
    timeout: 15
    requestsMemory: 1000M
    requestsCpu: 100m
    limitsMemory: 1200M
    limitsCpu: "2"
    downlinkLimit: 30000
    uplinkLimit: 30000
    mountPath: /home/project/persisted
    monitor:
      port: 3000
      activityTracker:
        timeoutAfter: 15
        notifyAfter: 2
  EOF
}

resource "kubectl_manifest" "theia-cloud-monitor-theia-timeout" {
  yaml_body = <<-EOF
  apiVersion: theia.cloud/v1beta10
  kind: AppDefinition
  metadata:
    name: theia-cloud-monitor-theia-timeout
    namespace: theia-cloud
  spec:
    name: theia-cloud-monitor-theia-timeout
    image: ${var.image_theia}
    imagePullPolicy: ${var.image_pull_policy}
    uid: 101
    port: 3000
    ingressname: theia-cloud-demo-ws-ingress
    ingressHostnamePrefixes: []
    minInstances: ${var.eager_start ? 1 : 0}
    maxInstances: 10
    timeout: 15
    requestsMemory: 1000M
    requestsCpu: 100m
    limitsMemory: 1200M
    limitsCpu: "2"
    downlinkLimit: 30000
    uplinkLimit: 30000
    mountPath: /home/project/persisted
    monitor:
      port: 3000
      activityTracker:
        timeoutAfter: 4
        notifyAfter: 15
  EOF
}

resource "kubectl_manifest" "theia-cloud-monitor-vscode-popup" {
  yaml_body = <<-EOF
  apiVersion: theia.cloud/v1beta10
  kind: AppDefinition
  metadata:
    name: theia-cloud-monitor-vscode-popup
    namespace: theia-cloud
  spec:
    name: theia-cloud-monitor-vscode-popup
    image: ${var.image_vscode}
    imagePullPolicy: ${var.image_pull_policy}
    uid: 101
    port: 3000
    ingressname: theia-cloud-demo-ws-ingress
    ingressHostnamePrefixes: []
    minInstances: ${var.eager_start ? 1 : 0}
    maxInstances: 10
    timeout: 15
    requestsMemory: 1000M
    requestsCpu: 100m
    limitsMemory: 1200M
    limitsCpu: "2"
    downlinkLimit: 30000
    uplinkLimit: 30000
    mountPath: /home/project/persisted
    monitor:
      port: 8081
      activityTracker:
        timeoutAfter: 15
        notifyAfter: 2
  EOF
}

resource "kubectl_manifest" "theia-cloud-monitor-vscode-timeout" {
  yaml_body = <<-EOF
  apiVersion: theia.cloud/v1beta10
  kind: AppDefinition
  metadata:
    name: theia-cloud-monitor-vscode-timeout
    namespace: theia-cloud
  spec:
    name: theia-cloud-monitor-vscode-timeout
    image: ${var.image_vscode}
    imagePullPolicy: ${var.image_pull_policy}
    uid: 101
    port: 3000
    ingressname: theia-cloud-demo-ws-ingress
    ingressHostnamePrefixes: []
    minInstances: ${var.eager_start ? 1 : 0}
    maxInstances: 10
    timeout: 15
    requestsMemory: 1000M
    requestsCpu: 100m
    limitsMemory: 1200M
    limitsCpu: "2"
    downlinkLimit: 30000
    uplinkLimit: 30000
    mountPath: /home/project/persisted
    monitor:
      port: 8081
      activityTracker:
        timeoutAfter: 4
        notifyAfter: 15
  EOF
}

resource "kubectl_manifest" "theia-cloud-demo" {
  yaml_body = <<-EOF
  apiVersion: theia.cloud/v1beta10
  kind: AppDefinition
  metadata:
    name: theia-cloud-demo
    namespace: theia-cloud
  spec:
    name: theia-cloud-demo
    image: ${var.image_theia}
    imagePullPolicy: ${var.image_pull_policy}
    uid: 101
    port: 3000
    ingressname: theia-cloud-demo-ws-ingress
    ingressHostnamePrefixes: []
    minInstances: 0
    maxInstances: 10
    timeout: 2
    requestsMemory: 1000M
    requestsCpu: 100m
    limitsMemory: 1200M
    limitsCpu: "2"
    downlinkLimit: 30000
    uplinkLimit: 30000
    mountPath: /home/project/persisted
    monitor:
      port: 3000
      activityTracker:
        timeoutAfter: 30
        notifyAfter: 30
  EOF
}
