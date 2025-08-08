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

  set = [
    {
      name = "hosts.usePaths"
      # Need to hand in boolean as string as terraform converts boolean to 1 resp. 0.
      # See https://github.com/hashicorp/terraform-provider-helm/issues/208
      value = "true"
    },
    {
      name  = "ingress.addTLSSecretName"
      value = "true"
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
      value = "instances"
    },
    {
      name  = "hosts.configuration.baseHost"
      value = data.terraform_remote_state.minikube.outputs.hostname
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
      name  = "operator.eagerStart"
      value = true
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
      name  = "imagePullPolicy"
      value = "IfNotPresent"
    }
  ]
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
