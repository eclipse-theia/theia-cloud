variable "ingress_ip" {
  description = "The IP of the running Minikube Cluster"
  type        = string
}

variable "use_paths" {
  description = "Whether to use Theia Cloud with paths or subdomains"
  type        = bool
}

variable "use_ephemeral_storage" {
  description = "Whether to use ephemeral storage"
  type        = bool
}

variable "enable_keycloak" {
  description = "Whether to enable keycloak"
  type        = bool
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}

provider "helm" {
  kubernetes = {
    config_path = "~/.kube/config"
  }
}

provider "kubectl" {
  load_config_file = true
  config_path      = "~/.kube/config"
}

resource "kubernetes_persistent_volume" "minikube" {
  metadata {
    name = "minikube-volume"
  }
  spec {
    storage_class_name = "manual"
    capacity = {
      storage = "16Gi"
    }
    access_modes = ["ReadWriteOnce"]
    persistent_volume_source {
      host_path {
        path = "/data/theiacloud"
      }
    }
  }
}

module "cluster_prerequisites" {
  source = "../modules/cluster-prerequisites"

  depends_on = [kubernetes_persistent_volume.minikube]

  hostname                            = "${var.ingress_ip}.nip.io"
  keycloak_admin_password             = "admin"
  postgres_password                   = "admin"
  install_cert_manager                = true
  install_selfsigned_issuer           = true
  cert_manager_issuer_email           = "jdoe@theia-cloud.io"
  ingress_cert_manager_cluster_issuer = "keycloak-selfsigned-issuer"
  ingress_cert_manager_common_name    = "${var.ingress_ip}.nip.io"
  postgres_storage_class              = "manual"
  postgres_volume_permissions         = true
  cloud_provider                      = "MINIKUBE"
}

provider "keycloak" {
  client_id                = "admin-cli"
  username                 = "admin"
  password                 = "admin"
  url                      = trimsuffix(module.cluster_prerequisites.keycloak_url, "/")
  tls_insecure_skip_verify = true # only for minikube self signed
  initial_login            = false
  client_timeout           = 60
}

module "keycloak" {
  source = "../modules/keycloak"

  depends_on = [module.cluster_prerequisites]

  hostname                        = "${var.ingress_ip}.nip.io"
  keycloak_test_user_foo_password = "foo"
  keycloak_test_user_bar_password = "bar"
  valid_redirect_uri              = "*"
}

resource "helm_release" "theia-cloud-crds" {
  depends_on = [module.keycloak]

  name             = "theia-cloud-crds"
  chart            = "../../../theia-cloud-helm/charts/theia-cloud-crds"
  namespace        = "theia-cloud"
  create_namespace = true

  set = [
    {
      name  = "conversion.image"
      value = "theiacloud/theia-cloud-conversion-webhook:minikube-ci-e2e"
    }
  ]
}

resource "helm_release" "theia-cloud-base" {
  depends_on = [module.keycloak]

  name             = "theia-cloud-base"
  chart            = "../../../theia-cloud-helm/charts/theia-cloud-base"
  namespace        = "theia-cloud"
  create_namespace = true

  set = [
    {
      name  = "issuer.email"
      value = "jdoe@theia-cloud.io"
    }
  ]
}

resource "helm_release" "theia-cloud" {
  depends_on = [helm_release.theia-cloud-crds, helm_release.theia-cloud-base]

  name             = "theia-cloud"
  chart            = "../../../theia-cloud-helm/charts/theia-cloud"
  namespace        = "theia-cloud"
  create_namespace = true

  values = [
    "${file("${path.module}/valuesE2ECI.yaml")}"
  ]

  set = [{
    name  = "hosts.usePaths"
    value = var.use_paths
    },
    {
      name  = "hosts.configuration.baseHost"
      value = "${var.ingress_ip}.nip.io"
    },
    {
      name  = "landingPage.ephemeralStorage"
      value = var.use_ephemeral_storage
    },
    {
      name  = "keycloak.enable"
      value = var.enable_keycloak
    },
    {
      name  = "keycloak.authUrl"
      value = "https://${var.ingress_ip}.nip.io/keycloak/"
    }
  ]
}

resource "kubectl_manifest" "theia-cloud-monitor-theia" {
  depends_on = [helm_release.theia-cloud]
  yaml_body  = <<-EOF
  apiVersion: theia.cloud/v1beta10
  kind: AppDefinition
  metadata:
    name: theia-cloud-monitor-theia
    namespace: theia-cloud
  spec:
    name: theia-cloud-monitor-theia
    image: theiacloud/theia-cloud-activity-demo-theia:minikube-ci-e2e
    imagePullPolicy: IfNotPresent
    uid: 101
    port: 3000
    ingressname: theia-cloud-demo-ws-ingress
    ingressHostnamePrefixes: []
    minInstances: 0
    maxInstances: 10
    timeout: 6
    requestsMemory: 1000M
    requestsCpu: 100m
    limitsMemory: 1200M
    limitsCpu: "2"
    downlinkLimit: 30000
    uplinkLimit: 30000
    mountPath: /home/project/persisted
    monitor:
      port: 3000
      activityTracker:
        timeoutAfter: 4
        notifyAfter: 2
  EOF
}

resource "kubectl_manifest" "theia-cloud-monitor-vscode" {
  depends_on = [helm_release.theia-cloud]
  yaml_body  = <<-EOF
  apiVersion: theia.cloud/v1beta10
  kind: AppDefinition
  metadata:
    name: theia-cloud-monitor-vscode
    namespace: theia-cloud
  spec:
    name: theia-cloud-monitor-vscode
    image: theiacloud/theia-cloud-activity-demo:minikube-ci-e2e
    imagePullPolicy: IfNotPresent
    uid: 101
    port: 3000
    ingressname: theia-cloud-demo-ws-ingress
    ingressHostnamePrefixes: []
    minInstances: 0
    maxInstances: 10
    timeout: 6
    requestsMemory: 1000M
    requestsCpu: 100m
    limitsMemory: 1200M
    limitsCpu: "2"
    downlinkLimit: 30000
    uplinkLimit: 30000
    mountPath: /home/project/persisted
    monitor:
      port: 8081
      activityTracker:
        timeoutAfter: 4
        notifyAfter: 2
  EOF
}
