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
  chart            = "../../../../theia-cloud-helm/charts/theia-cloud"
  namespace        = "theia-cloud"
  create_namespace = true

  values = [
    "${file("${path.module}/../../values/valuesDemo.yaml")}"
  ]

  set {
    name = "hosts.usePaths"
    # Need to hand in boolean as string as terraform converts boolean to 1 resp. 0.
    # See https://github.com/hashicorp/terraform-provider-helm/issues/208
    value = "true"
  }

  set {
    name  = "ingress.addTLSSecretName"
    value = "true"
  }

  set {
    name  = "hosts.configuration.service"
    value = "service"
  }

  set {
    name  = "hosts.configuration.landing"
    value = "try"
  }

  set {
    name  = "hosts.configuration.instance"
    value = "instances"
  }


  set {
    name  = "hosts.configuration.baseHost"
    value = data.terraform_remote_state.minikube.outputs.hostname
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
    name  = "operator.eagerStart"
    value = true
  }

  set {
    name  = "ingress.clusterIssuer"
    value = "theia-cloud-selfsigned-issuer"
  }

  set {
    name  = "ingress.theiaCloudCommonName"
    value = true
  }

  # Only pull missing images. This is needed to use images built locally in Minikube
  set {
    name  = "imagePullPolicy"
    value = "IfNotPresent"
  }
}

resource "kubectl_manifest" "cdt-cloud-demo" {
  depends_on = [helm_release.theia-cloud]
  yaml_body  = <<-EOF
  apiVersion: theia.cloud/v1beta10
  kind: AppDefinition
  metadata:
    name: cdt-cloud-demo
    namespace: theia-cloud
  spec:
    downlinkLimit: 30000
    image: theiacloud/cdt-cloud:v1.43.1
    imagePullPolicy: IfNotPresent
    ingressname: theia-cloud-demo-ws-ingress
    limitsCpu: "2"
    limitsMemory: 1200M
    maxInstances: 10
    minInstances: 1
    name: cdt-cloud-demo
    port: 3000
    requestsCpu: 100m
    requestsMemory: 1000M
    timeout: 30
    uid: 101
    uplinkLimit: 30000
  EOF
}
