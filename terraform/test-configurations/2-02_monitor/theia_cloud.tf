variable "enable_keycloak" {
  description = "Whether keycloak should be enabled"
  type        = bool
}

variable "use_vscode_extension" {
  description = "Whether the VSCode extension should be used."
  type        = bool
}

data "terraform_remote_state" "minikube" {
  backend = "local"

  config = {
    path = "${path.module}/../0_minikube-setup/terraform.tfstate"
  }
}

provider "helm" {
  kubernetes = {
    host                   = data.terraform_remote_state.minikube.outputs.host
    client_certificate     = data.terraform_remote_state.minikube.outputs.client_certificate
    client_key             = data.terraform_remote_state.minikube.outputs.client_key
    cluster_ca_certificate = data.terraform_remote_state.minikube.outputs.cluster_ca_certificate
  }
}

resource "helm_release" "theia-cloud" {
  name             = "theia-cloud"
  chart            = "../../../../theia-cloud-helm/charts/theia-cloud"
  namespace        = "theia-cloud"
  create_namespace = true

  values = [
    "${file("${path.module}/../../values/valuesMonitor.yaml")}"
  ]

  set = [
    {
      name  = "hosts.configuration.baseHost"
      value = data.terraform_remote_state.minikube.outputs.hostname
    },
    {
      name  = "hosts.configuration.service"
      value = "service"
    },
    {
      name  = "hosts.configuration.landing"
      value = "try"
    },
    {
      name  = "hosts.configuration.instance"
      value = "ws"
    },
    {
      name  = "keycloak.authUrl"
      value = "https://${data.terraform_remote_state.minikube.outputs.hostname}/keycloak/"
    },
    {
      name  = "operator.cloudProvider"
      value = "MINIKUBE"
    },
    {
      name  = "ingress.clusterIssuer"
      value = "theia-cloud-selfsigned-issuer"
    },
    {
      name  = "ingress.theiaCloudCommonName"
      value = true
    },
    {
      name  = "keycloak.enable"
      value = var.enable_keycloak
    },
    {
      name  = "demoApplication.name"
      value = var.use_vscode_extension ? "theiacloud/theia-cloud-activity-demo:1.2.0-next" : "theiacloud/theia-cloud-activity-demo-theia:1.2.0-next"
    },
    {
      name  = "demoApplication.monitor.port"
      value = var.use_vscode_extension ? 8081 : 3000
    }
  ]
}
