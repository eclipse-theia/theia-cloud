data "terraform_remote_state" "openshift" {
  backend = "local"

  config = {
    path = "${path.module}/../4_openshift-setup/terraform.tfstate"
  }
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
}

resource "helm_release" "theia-cloud" {
  depends_on = [helm_release.theia-cloud-crds]

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
