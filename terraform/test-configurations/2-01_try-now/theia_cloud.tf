data "terraform_remote_state" "minikube" {
  backend = "local"

  config = {
    path = "${path.module}/../0_minikube-setup/terraform.tfstate"
  }
}

provider "helm" {
  kubernetes {
    host                   = data.terraform_remote_state.minikube.outputs.host
    client_certificate     = data.terraform_remote_state.minikube.outputs.client_certificate
    client_key             = data.terraform_remote_state.minikube.outputs.client_key
    cluster_ca_certificate = data.terraform_remote_state.minikube.outputs.cluster_ca_certificate
  }
}

provider "kubectl" {
  load_config_file       = false
  host                   = data.terraform_remote_state.minikube.outputs.host
  client_certificate     = data.terraform_remote_state.minikube.outputs.client_certificate
  client_key             = data.terraform_remote_state.minikube.outputs.client_key
  cluster_ca_certificate = data.terraform_remote_state.minikube.outputs.cluster_ca_certificate
}

resource "helm_release" "theia-cloud" {
  name             = "theia-cloud"
  chart            = "../../../../theia-cloud-helm/charts/theia.cloud"
  namespace        = "theiacloud"
  create_namespace = true

  values = [
    "${file("${path.module}/../../../helm/theia.cloud/valuesGKETryNow.yaml")}"
  ]

  set {
    name  = "hosts.service"
    value = "service.${data.terraform_remote_state.minikube.outputs.hostname}"
  }

  set {
    name  = "hosts.landing"
    value = "try.${data.terraform_remote_state.minikube.outputs.hostname}"
  }

  set {
    name  = "hosts.instance"
    value = "ws.${data.terraform_remote_state.minikube.outputs.hostname}"
  }

  set {
    name  = "keycloak.authUrl"
    value = "https://${data.terraform_remote_state.minikube.outputs.hostname}/keycloak/"
  }

  set {
    name  = "operator.cloudProvider"
    value = "MINIKUBE"
  }

  set {
    name  = "ingress.clusterIssuer"
    value = "theia-cloud-selfsigned-issuer"
  }

  set {
    name  = "ingress.theiaCloudCommonName"
    value = true
  }

  # Comment in to only pull missing images. This is needed to use images built locally in Minikube
  # set {
  #   name  = "imagePullPolicy"
  #   value = "IfNotPresent"
  # }
}

resource "kubectl_manifest" "cdt-cloud-demo" {
  depends_on = [helm_release.theia-cloud]
  yaml_body  = <<-EOF
  apiVersion: theia.cloud/v8beta
  kind: AppDefinition
  metadata:
    name: cdt-cloud-demo
    namespace: theiacloud
  spec:
    downlinkLimit: 30000
    image: theiacloud/cdt-cloud:v1.34.4
    imagePullPolicy: IfNotPresent
    ingressname: theia-cloud-demo-ws-ingress
    limitsCpu: "2"
    limitsMemory: 1200M
    maxInstances: 10
    minInstances: 0
    name: cdt-cloud-demo
    port: 3000
    requestsCpu: 100m
    requestsMemory: 1000M
    timeout:
      limit: 30
      strategy: FIXEDTIME
    uid: 101
    uplinkLimit: 30000
    mountPath: /home/project/persisted
    monitor:
      port: 8081
      activityTracker:
        timeoutAfter: 30
        notifyAfter: 25
  EOF
}


resource "kubectl_manifest" "coffee-editor" {
  depends_on = [helm_release.theia-cloud]
  yaml_body  = <<-EOF
  apiVersion: theia.cloud/v8beta
  kind: AppDefinition
  metadata:
    name: coffee-editor
    namespace: theiacloud
  spec:
    downlinkLimit: 30000
    image: eu.gcr.io/kubernetes-238012/coffee-editor:v0.7.17
    imagePullPolicy: IfNotPresent
    ingressname: theia-cloud-demo-ws-ingress
    limitsCpu: "2"
    limitsMemory: 2400M
    maxInstances: 4
    minInstances: 0
    name: coffee-editor
    port: 3000
    requestsCpu: 100m
    requestsMemory: 1000M
    timeout:
      limit: 30
      strategy: FIXEDTIME
    uid: 1001
    uplinkLimit: 30000
    mountPath: /home/project/persisted
    monitor:
      port: 8081
      activityTracker:
        timeoutAfter: 30
        notifyAfter: 25
  EOF
}
