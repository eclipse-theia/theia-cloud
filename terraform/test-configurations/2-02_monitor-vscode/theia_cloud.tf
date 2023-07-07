variable "enable_keycloak" {
  description = "Whether keycloak should be enabled"
}

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

resource "helm_release" "theia-cloud" {
  name             = "theia-cloud"
  chart            = "../../../../theia-cloud-helm/charts/theia.cloud"
  namespace        = "theiacloud"
  create_namespace = true

  values = [
    "${file("${path.module}/../../../helm/theia.cloud/valuesMonitor.yaml")}"
  ]

  set {
    name  = "hosts.paths.baseHost"
    value = data.terraform_remote_state.minikube.outputs.hostname
  }

  set {
    name  = "keycloak.enable"
    value = var.enable_keycloak
  }

  set {
    name  = "keycloak.authUrl"
    value = "https://${data.terraform_remote_state.minikube.outputs.hostname}/keycloak/"
  }

  # Comment in to only pull missing images. This is needed to use images built locally in Minikube
  # set {
  #   name  = "imagePullPolicy"
  #   value = "IfNotPresent"
  # }
}
