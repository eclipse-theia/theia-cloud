data "terraform_remote_state" "openshift" {
  backend = "local"

  config = {
    path = "${path.module}/../4_openshift-setup/terraform.tfstate"
  }
}

# CI-only knobs. All defaulted to empty so local CRC development keeps
# using the upstream chart values + valuesOpenShiftMonitor.yaml as-is.
# The MicroShift e2e CI workflow passes these via -var to:
#  - swap the primary values file to a CI-specific one with the right
#    `app.name`, multi-app `additionalApps`, etc. (`values_file`)
#  - layer additional Helm overrides on top (`extra_helm_values`)
#  - point the conversion-webhook image at a locally imported tag
#    (`conversion_webhook_image[_pull_policy]`).
variable "values_file" {
  description = "Path to the primary Helm values file for the theia-cloud release. Empty = use valuesOpenShiftMonitor.yaml (local CRC dev default)."
  type        = string
  default     = ""
}

variable "extra_helm_values" {
  description = "Path to an additional Helm values file applied last to the theia-cloud release. Empty disables."
  type        = string
  default     = ""
}

variable "conversion_webhook_image" {
  description = "Override image for the theia-cloud-crds conversion webhook. Empty = chart default."
  type        = string
  default     = ""
}

variable "conversion_webhook_image_pull_policy" {
  description = "imagePullPolicy for the conversion webhook. Empty = chart default."
  type        = string
  default     = ""
}

provider "helm" {
  kubernetes = {
    host     = data.terraform_remote_state.openshift.outputs.host
    token    = data.terraform_remote_state.openshift.outputs.token
    insecure = true
  }
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

  # Optional CI image overrides (see variables above). Empty list when
  # both vars are unset.
  set = concat(
    var.conversion_webhook_image == "" ? [] : [
      { name = "conversion.image", value = var.conversion_webhook_image }
    ],
    var.conversion_webhook_image_pull_policy == "" ? [] : [
      { name = "conversion.imagePullPolicy", value = var.conversion_webhook_image_pull_policy }
    ]
  )
}

resource "helm_release" "theia-cloud" {
  depends_on = [helm_release.theia-cloud-crds]

  name             = "theia-cloud"
  chart            = "../../../../theia-cloud-helm/charts/theia-cloud"
  namespace        = "theia-cloud"
  create_namespace = true

  values = concat(
    # Primary values file: CI overrides via `values_file`, otherwise
    # the local-dev valuesOpenShiftMonitor.yaml. `coalesce` skips empty
    # strings, so the default "" falls through to the second arg.
    [file(coalesce(var.values_file, "${path.module}/../../values/valuesOpenShiftMonitor.yaml"))],
    var.extra_helm_values == "" ? [] : ["${file(var.extra_helm_values)}"]
  )

  set = [
    {
      name  = "hosts.configuration.baseHost"
      value = data.terraform_remote_state.openshift.outputs.hostname
    },
    {
      name  = "operator.cloudProvider"
      value = "OPENSHIFT"
    },
    {
      name  = "keycloak.enable"
      value = "true"
    },
    {
      name  = "keycloak.authUrl"
      value = "https://keycloak.${data.terraform_remote_state.openshift.outputs.hostname}/"
    },
    # Uncomment to use locally built images (see openshift.md)
    # {
    #   name  = "operator.image"
    #   value = "image-registry.openshift-image-registry.svc:5000/theia-cloud/theia-cloud-operator:dev"
    # },
    # {
    #   name  = "operator.imagePullPolicy"
    #   value = "Always"
    # },
    # {
    #   name  = "service.image"
    #   value = "image-registry.openshift-image-registry.svc:5000/theia-cloud/theia-cloud-service:dev"
    # },
    # {
    #   name  = "service.imagePullPolicy"
    #   value = "Always"
    # },
    # {
    #   name  = "landingPage.image"
    #   value = "image-registry.openshift-image-registry.svc:5000/theia-cloud/theia-cloud-landing-page:dev"
    # },
    # {
    #   name  = "landingPage.imagePullPolicy"
    #   value = "Always"
    # },
  ]
}
