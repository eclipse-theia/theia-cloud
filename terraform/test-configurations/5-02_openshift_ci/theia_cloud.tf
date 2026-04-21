# OpenShift CI configuration. Mirror of `terraform/ci-configurations/`
# for the [E2E Tests] OpenShift workflow. Reads OpenShift connection
# state from `4_openshift-setup`, installs the three Theia Cloud helm
# releases, and emits the e2e AppDefinition CRs via the shared
# `theia-cloud-ci-appdefinitions` module.

data "terraform_remote_state" "openshift" {
  backend = "local"

  config = {
    path = "${path.module}/../4_openshift-setup/terraform.tfstate"
  }
}

variable "enable_keycloak" {
  description = "Whether the theia-cloud chart enables Keycloak login. Matches the e2e `keycloak` matrix axis."
  type        = bool
  default     = true
}

variable "use_ephemeral_storage" {
  description = "Whether sessions use ephemeral storage (no PVC). Matches the e2e `ephemeral` matrix axis."
  type        = bool
  default     = true
}

variable "eager_start" {
  description = "Whether to enable eager start for sessions. Matches the e2e `eagerStart` matrix axis."
  type        = bool
  default     = false
}

provider "helm" {
  kubernetes = {
    host     = data.terraform_remote_state.openshift.outputs.host
    token    = data.terraform_remote_state.openshift.outputs.token
    insecure = true
  }
}

provider "kubectl" {
  host             = data.terraform_remote_state.openshift.outputs.host
  token            = data.terraform_remote_state.openshift.outputs.token
  insecure         = true
  load_config_file = false
}

resource "helm_release" "theia-cloud-base" {
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
      name  = "issuerprod.enable"
      value = "false"
    }
  ]
}

resource "helm_release" "theia-cloud-crds" {
  depends_on = [helm_release.theia-cloud-base]

  name             = "theia-cloud-crds"
  chart            = "../../../../theia-cloud-helm/charts/theia-cloud-crds"
  namespace        = "theia-cloud"
  create_namespace = true

  set = [
    {
      name  = "conversion.image"
      value = "localhost/theia-cloud-conversion-webhook:microshift-ci-e2e"
    },
    {
      name  = "conversion.imagePullPolicy"
      value = "Never"
    }
  ]
}

resource "helm_release" "theia-cloud" {
  depends_on = [helm_release.theia-cloud-crds]

  name             = "theia-cloud"
  chart            = "../../../../theia-cloud-helm/charts/theia-cloud"
  namespace        = "theia-cloud"
  create_namespace = true

  values = [
    file("${path.module}/../../values/valuesE2ECI-base.yaml"),
    file("${path.module}/../../values/valuesE2ECI-openshift.yaml"),
  ]

  set = [
    {
      name  = "hosts.configuration.baseHost"
      value = data.terraform_remote_state.openshift.outputs.hostname
    },
    {
      name  = "keycloak.enable"
      value = tostring(var.enable_keycloak)
    },
    {
      name  = "keycloak.authUrl"
      value = "https://keycloak.${data.terraform_remote_state.openshift.outputs.hostname}/"
    },
    {
      name  = "landingPage.ephemeralStorage"
      value = tostring(var.use_ephemeral_storage)
    },
    {
      name  = "operator.eagerStart"
      value = tostring(var.eager_start)
    },
  ]
}

module "appdefinitions" {
  source = "../../modules/theia-cloud-ci-appdefinitions"

  depends_on = [helm_release.theia-cloud]

  image_theia       = "localhost/theia-cloud-activity-demo-theia:microshift-ci-e2e"
  image_vscode      = "localhost/theia-cloud-activity-demo:microshift-ci-e2e"
  image_pull_policy = "Never"
  eager_start       = var.eager_start
}
