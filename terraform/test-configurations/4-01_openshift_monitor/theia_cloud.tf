data "terraform_remote_state" "openshift" {
  backend = "local"

  config = {
    path = "${path.module}/../0_openshift-setup/terraform.tfstate"
  }
}

provider "helm" {
  kubernetes = {
    host     = data.terraform_remote_state.openshift.outputs.host
    token    = data.terraform_remote_state.openshift.outputs.token
    insecure = true
  }
}

resource "helm_release" "theia-cloud-crds" {
  name             = "theia-cloud-crds"
  chart            = "../../../../theia-cloud-helm/charts/theia-cloud-crds"
  namespace        = "theia-cloud"
  create_namespace = true
}

resource "helm_release" "theia-cloud-base" {
  depends_on = [helm_release.theia-cloud-crds]

  name             = "theia-cloud-base"
  chart            = "../../../../theia-cloud-helm/charts/theia-cloud-base"
  namespace        = "theia-cloud"
  create_namespace = true

  set = [
    {
      name  = "operator.cloudProvider"
      value = "OPENSHIFT"
    },
    {
      name  = "issuerca.enable"
      value = "false"
    },
    {
      name  = "issuerprod.enable"
      value = "false"
    }
  ]
}

resource "helm_release" "theia-cloud" {
  depends_on = [helm_release.theia-cloud-base]

  name             = "theia-cloud"
  chart            = "../../../../theia-cloud-helm/charts/theia-cloud"
  namespace        = "theia-cloud"
  create_namespace = true

  values = [
    "${file("${path.module}/../../values/valuesOpenShiftMonitor.yaml")}"
  ]

  set = [
    {
      name  = "hosts.configuration.baseHost"
      value = data.terraform_remote_state.openshift.outputs.hostname
    },
    {
      name  = "operator.cloudProvider"
      value = "OPENSHIFT"
    }
  ]
}
