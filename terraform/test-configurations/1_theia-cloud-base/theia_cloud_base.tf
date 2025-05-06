variable "cert_manager_issuer_email" {
  description = "EMail address used to create certificates."
  default     = "tester@theia-cloud.io"
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

resource "helm_release" "theia-cloud-crds" {
  name             = "theia-cloud-crds"
  chart            = "../../../../theia-cloud-helm/charts/theia-cloud-crds"
  namespace        = "theia-cloud"
  create_namespace = true
}

resource "helm_release" "theia-cloud-base" {
  name             = "theia-cloud-base"
  chart            = "../../../../theia-cloud-helm/charts/theia-cloud-base"
  namespace        = "theia-cloud"
  create_namespace = true

  set {
    name  = "issuer.email"
    value = var.cert_manager_issuer_email
  }
}
