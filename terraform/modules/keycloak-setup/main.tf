resource "helm_release" "cert_manager" {
  count            = var.install_cert_manager ? 1 : 0
  name             = "cert-manager"
  repository       = "https://charts.jetstack.io"
  chart            = "cert-manager"
  version          = var.cert_manager_version
  namespace        = var.cert_manager_namespace
  create_namespace = true

  set = [
    {
      name  = "installCRDs"
      value = "true"
    }
  ]
}

resource "kubectl_manifest" "keycloak_selfsigned_issuer" {
  count      = var.install_selfsigned_issuer ? 1 : 0
  depends_on = [helm_release.cert_manager]

  yaml_body = yamlencode({
    apiVersion = "cert-manager.io/v1"
    kind       = "ClusterIssuer"
    metadata = {
      name = "keycloak-selfsigned-issuer"
    }
    spec = {
      selfSigned = {}
    }
  })
}

resource "kubernetes_namespace" "keycloak" {
  metadata {
    name = var.keycloak_namespace
  }

  depends_on = [helm_release.cert_manager]
}

data "http" "keycloak_crd" {
  url = "https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/${var.keycloak_version}/kubernetes/keycloaks.k8s.keycloak.org-v1.yml"
}

resource "kubectl_manifest" "keycloak_crd" {
  yaml_body = data.http.keycloak_crd.response_body
  depends_on = [
    kubernetes_namespace.keycloak
  ]
}

data "http" "keycloak_realm_import_crd" {
  url = "https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/${var.keycloak_version}/kubernetes/keycloakrealmimports.k8s.keycloak.org-v1.yml"
}

resource "kubectl_manifest" "keycloak_realm_import_crd" {
  yaml_body = data.http.keycloak_realm_import_crd.response_body
  depends_on = [
    kubernetes_namespace.keycloak
  ]
}

data "http" "keycloak_operator" {
  url = "https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/${var.keycloak_version}/kubernetes/kubernetes.yml"
}

locals {
  operator_manifests_raw = split("---", data.http.keycloak_operator.response_body)
  operator_manifests = [
    for doc in local.operator_manifests_raw :
    yamldecode(doc)
    if trimspace(doc) != "" && can(yamldecode(doc))
  ]
  operator_resources = {
    for idx, doc in local.operator_manifests :
    "${doc.kind}-${doc.metadata.name}-${idx}" => doc
  }
}

resource "kubectl_manifest" "keycloak_operator" {
  for_each           = local.operator_resources
  yaml_body          = yamlencode(each.value)
  override_namespace = var.keycloak_namespace
  depends_on = [
    kubectl_manifest.keycloak_crd,
    kubectl_manifest.keycloak_realm_import_crd
  ]
}

resource "kubernetes_secret" "postgres" {
  count = var.postgres_enabled ? 1 : 0

  metadata {
    name      = "postgres-credentials"
    namespace = kubernetes_namespace.keycloak.metadata[0].name
  }

  data = {
    username = var.postgres_username
    password = var.postgres_password
    database = var.postgres_database
  }

  type = "Opaque"
}

resource "kubernetes_persistent_volume_claim" "postgres" {
  count = var.postgres_enabled ? 1 : 0

  metadata {
    name      = "postgres-pvc"
    namespace = kubernetes_namespace.keycloak.metadata[0].name
  }

  spec {
    access_modes = ["ReadWriteOnce"]
    resources {
      requests = {
        storage = var.postgres_storage_size
      }
    }
    storage_class_name = var.postgres_storage_class != "" ? var.postgres_storage_class : null
  }
}

resource "kubernetes_deployment" "postgres" {
  count = var.postgres_enabled ? 1 : 0

  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace.keycloak.metadata[0].name
    labels = {
      app = "postgres"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "postgres"
      }
    }

    template {
      metadata {
        labels = {
          app = "postgres"
        }
      }

      spec {
        dynamic "init_container" {
          for_each = var.postgres_volume_permissions ? [1] : []
          content {
            name  = "volume-permissions"
            image = "busybox:latest"
            command = [
              "sh",
              "-c",
              "chown -R 999:999 /var/lib/postgresql/data"
            ]
            volume_mount {
              name       = "postgres-storage"
              mount_path = "/var/lib/postgresql/data"
            }
          }
        }

        container {
          name  = "postgres"
          image = var.postgres_image

          env {
            name = "POSTGRES_USER"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.postgres[0].metadata[0].name
                key  = "username"
              }
            }
          }

          env {
            name = "POSTGRES_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.postgres[0].metadata[0].name
                key  = "password"
              }
            }
          }

          env {
            name = "POSTGRES_DB"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.postgres[0].metadata[0].name
                key  = "database"
              }
            }
          }

          port {
            container_port = 5432
            name           = "postgres"
          }

          volume_mount {
            name       = "postgres-storage"
            mount_path = "/var/lib/postgresql/data"
          }

          resources {
            requests = {
              cpu    = "250m"
              memory = "512Mi"
            }
            limits = {
              cpu    = "500m"
              memory = "1Gi"
            }
          }
        }

        volume {
          name = "postgres-storage"
          persistent_volume_claim {
            claim_name = kubernetes_persistent_volume_claim.postgres[0].metadata[0].name
          }
        }
      }
    }
  }

  depends_on = [
    kubernetes_persistent_volume_claim.postgres
  ]
}

resource "kubernetes_service" "postgres" {
  count = var.postgres_enabled ? 1 : 0

  metadata {
    name      = "postgres"
    namespace = kubernetes_namespace.keycloak.metadata[0].name
  }

  spec {
    selector = {
      app = "postgres"
    }

    port {
      port        = 5432
      target_port = 5432
      protocol    = "TCP"
    }

    type = "ClusterIP"
  }
}



locals {
  tls_secret_name = var.ingress_tls_secret_name != "" ? var.ingress_tls_secret_name : "${var.hostname}-tls"

  keycloak_spec_base = {
    instances = var.keycloak_replicas
    http = {
      httpEnabled = true
      httpPort    = 8080
      tlsSecret   = var.ingress_tls_enabled ? local.tls_secret_name : null
    }
    hostname = {
      hostname = var.hostname
      strict   = false
    }
    http-relative-path = var.keycloak_http_relative_path
    additionalOptions = [
      {
        name  = "admin"
        value = var.keycloak_admin_username
      },
      {
        name  = "admin-password"
        value = var.keycloak_admin_password
      }
    ]
    resources = {
      requests = {
        cpu    = var.keycloak_resource_requests_cpu
        memory = var.keycloak_resource_requests_memory
      }
      limits = {
        cpu    = var.keycloak_resource_limits_cpu
        memory = var.keycloak_resource_limits_memory
      }
    }
  }

  keycloak_spec = merge(
    local.keycloak_spec_base,
    var.postgres_enabled ? {
      db = {
        vendor   = "postgres"
        host     = kubernetes_service.postgres[0].metadata[0].name
        port     = 5432
        database = var.postgres_database
        usernameSecret = {
          name = kubernetes_secret.postgres[0].metadata[0].name
          key  = "username"
        }
        passwordSecret = {
          name = kubernetes_secret.postgres[0].metadata[0].name
          key  = "password"
        }
      }
    } : { db = null }
  )
}

resource "kubectl_manifest" "keycloak_instance" {
  yaml_body = yamlencode({
    apiVersion = "k8s.keycloak.org/v2alpha1"
    kind       = "Keycloak"
    metadata = {
      name      = "keycloak"
      namespace = kubernetes_namespace.keycloak.metadata[0].name
    }
    spec = local.keycloak_spec
  })

  depends_on = [
    kubectl_manifest.keycloak_operator,
    kubernetes_service.postgres
  ]
}

resource "kubernetes_ingress_v1" "keycloak" {
  count = var.ingress_enabled ? 1 : 0

  metadata {
    name      = "keycloak"
    namespace = kubernetes_namespace.keycloak.metadata[0].name
    annotations = merge(
      {
        "nginx.ingress.kubernetes.io/proxy-buffer-size"       = "128k"
        "nginx.ingress.kubernetes.io/proxy-busy-buffers-size" = "128k"
      },
      var.ingress_tls_enabled ? {
        "cert-manager.io/cluster-issuer"                = var.ingress_cert_manager_cluster_issuer
        "cert-manager.io/common-name"                   = var.ingress_cert_manager_common_name != "" ? var.ingress_cert_manager_common_name : var.hostname
        "acme.cert-manager.io/http01-edit-in-place"     = "true"
        "acme.cert-manager.io/http01-ingress-path-type" = "ImplementationSpecific"
      } : {},
      var.ingress_annotations
    )
  }

  spec {
    ingress_class_name = var.ingress_class_name

    dynamic "tls" {
      for_each = var.ingress_tls_enabled ? [1] : []
      content {
        hosts       = [var.hostname]
        secret_name = local.tls_secret_name
      }
    }

    rule {
      host = var.hostname

      http {
        path {
          path      = var.keycloak_http_relative_path
          path_type = "ImplementationSpecific"

          backend {
            service {
              name = "keycloak-service"
              port {
                number = 8080
              }
            }
          }
        }
      }
    }
  }

  depends_on = [
    kubectl_manifest.keycloak_instance
  ]
}
