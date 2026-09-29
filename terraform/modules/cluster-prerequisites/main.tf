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

resource "helm_release" "ingress_nginx" {
  count            = var.install_ingress_controller && var.ingress_controller_type == "nginx" ? 1 : 0
  name             = "nginx-ingress-controller"
  repository       = "https://kubernetes.github.io/ingress-nginx"
  chart            = "ingress-nginx"
  version          = var.ingress_controller_version
  namespace        = var.ingress_controller_namespace
  create_namespace = true

  set = [
    {
      name  = "fullnameOverride"
      value = "ingress-nginx"
    },
    {
      name  = "controller.service.loadBalancerIP"
      value = var.load_balancer_ip
    },
    {
      name  = "controller.allowSnippetAnnotations"
      value = true
    },
    # Below two are added for backward compatibility with 1.1.1 which used Prefix pathType at some places. After 1.2.0 we should check if we may remove them again
    {
      name  = "controller.admissionWebhooks.enabled"
      value = false
    },
    {
      name  = "controller.config.enable-snippet"
      value = "true"
    }
  ]
}

resource "helm_release" "haproxy-ingress-controller" {
  count            = var.install_ingress_controller && var.ingress_controller_type == "haproxy" ? 1 : 0
  name             = "haproxy-ingress"
  repository       = "https://haproxy-ingress.github.io/charts"
  chart            = "haproxy-ingress"
  version          = "0.16.2"
  namespace        = "ingress-haproxy"
  create_namespace = true

  set = [
    {
      name  = "controller.ingressClassResource.enabled"
      value = true
    },
    {
      name  = "controller.service.loadBalancerIP"
      value = var.load_balancer_ip
    }
  ]
}

resource "kubectl_manifest" "keycloak_selfsigned_issuer" {
  count      = var.install_selfsigned_issuer ? 1 : 0
  depends_on = [helm_release.cert_manager, helm_release.ingress_nginx]

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

resource "kubernetes_namespace_v1" "keycloak" {
  metadata {
    name = var.keycloak_namespace
  }

  depends_on = [helm_release.cert_manager]
}

data "http" "keycloak_crd" {
  url = "https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/${var.keycloak_version}/kubernetes/keycloaks.k8s.keycloak.org-v1.yml"

  lifecycle {
    postcondition {
      condition     = self.status_code == 200
      error_message = "Failed to download the Keycloak CRD for version ${var.keycloak_version}."
    }
  }
}

resource "kubectl_manifest" "keycloak_crd" {
  yaml_body = data.http.keycloak_crd.response_body
  depends_on = [
    kubernetes_namespace_v1.keycloak
  ]
}

data "http" "keycloak_realm_import_crd" {
  url = "https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/${var.keycloak_version}/kubernetes/keycloakrealmimports.k8s.keycloak.org-v1.yml"

  lifecycle {
    postcondition {
      condition     = self.status_code == 200
      error_message = "Failed to download the KeycloakRealmImport CRD for version ${var.keycloak_version}."
    }
  }
}

resource "kubectl_manifest" "keycloak_realm_import_crd" {
  yaml_body = data.http.keycloak_realm_import_crd.response_body
  depends_on = [
    kubernetes_namespace_v1.keycloak
  ]
}

data "http" "keycloak_oidc_client_crd" {
  count = local.keycloak_client_crds_available ? 1 : 0
  url   = "https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/${var.keycloak_version}/kubernetes/keycloakoidcclients.k8s.keycloak.org-v1.yml"

  lifecycle {
    postcondition {
      condition     = self.status_code == 200
      error_message = "Failed to download the KeycloakOIDCClient CRD for version ${var.keycloak_version}."
    }
  }
}

resource "kubectl_manifest" "keycloak_oidc_client_crd" {
  count     = local.keycloak_client_crds_available ? 1 : 0
  yaml_body = data.http.keycloak_oidc_client_crd[0].response_body
  depends_on = [
    kubernetes_namespace_v1.keycloak
  ]
}

data "http" "keycloak_saml_client_crd" {
  count = local.keycloak_client_crds_available ? 1 : 0
  url   = "https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/${var.keycloak_version}/kubernetes/keycloaksamlclients.k8s.keycloak.org-v1.yml"

  lifecycle {
    postcondition {
      condition     = self.status_code == 200
      error_message = "Failed to download the KeycloakSAMLClient CRD for version ${var.keycloak_version}."
    }
  }
}

resource "kubectl_manifest" "keycloak_saml_client_crd" {
  count     = local.keycloak_client_crds_available ? 1 : 0
  yaml_body = data.http.keycloak_saml_client_crd[0].response_body
  depends_on = [
    kubernetes_namespace_v1.keycloak
  ]
}

locals {
  keycloak_ns = kubernetes_namespace_v1.keycloak.metadata[0].name

  keycloak_version_components    = try(regex("^v?([0-9]+)\\.([0-9]+)", var.keycloak_version), ["999", "999"])
  keycloak_client_crds_available = tonumber(local.keycloak_version_components[0]) > 26 || (tonumber(local.keycloak_version_components[0]) == 26 && tonumber(local.keycloak_version_components[1]) >= 7)

  # local_exec_quotes is a helper function to deal with different handling of
  # quotes between linux and windows. On linux, it will output "'". On windows,
  # it will output "".
  local_exec_quotes = startswith(abspath(path.module), "/") ? "'" : ""
  jsonpatch = jsonencode([{
    "op"    = "add",
    "path"  = "/spec/template/spec/containers/0/args/-",
    "value" = "--default-ssl-certificate=keycloak/${var.hostname}-tls"
  }])
}

resource "terraform_data" "keycloak_operator" {
  input = {
    namespace = var.keycloak_namespace
    version   = var.keycloak_version
  }

  triggers_replace = [
    var.keycloak_namespace,
    var.keycloak_version,
    local.keycloak_client_crds_available
  ]

  provisioner "local-exec" {
    command = <<-EOT
      set -e
      kubectl wait customresourcedefinition/keycloaks.k8s.keycloak.org --for=condition=Established --timeout=2m
      kubectl wait customresourcedefinition/keycloakrealmimports.k8s.keycloak.org --for=condition=Established --timeout=2m
      if [ "${local.keycloak_client_crds_available}" = "true" ]; then
        kubectl wait customresourcedefinition/keycloakoidcclients.k8s.keycloak.org --for=condition=Established --timeout=2m
        kubectl wait customresourcedefinition/keycloaksamlclients.k8s.keycloak.org --for=condition=Established --timeout=2m
      fi
      kubectl apply -n ${var.keycloak_namespace} -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/${var.keycloak_version}/kubernetes/kubernetes.yml
      kubectl patch clusterrolebinding keycloak-operator-clusterrole-binding --type='json' -p='[{"op": "replace", "path": "/subjects/0/namespace", "value":"${var.keycloak_namespace}"}]'
      kubectl rollout status deployment/keycloak-operator -n ${var.keycloak_namespace} --timeout=5m
    EOT
  }

  provisioner "local-exec" {
    when    = destroy
    command = <<-EOT
      kubectl delete -n ${self.input.namespace} -f https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/${self.input.version}/kubernetes/kubernetes.yml --ignore-not-found=true || true
    EOT
  }

  depends_on = [
    kubernetes_namespace_v1.keycloak,
    kubectl_manifest.keycloak_crd,
    kubectl_manifest.keycloak_realm_import_crd,
    kubectl_manifest.keycloak_oidc_client_crd,
    kubectl_manifest.keycloak_saml_client_crd
  ]
}

resource "kubernetes_secret_v1" "postgres" {
  count = var.postgres_enabled ? 1 : 0

  metadata {
    name      = "postgres-credentials"
    namespace = local.keycloak_ns
  }

  data = {
    username = var.postgres_username
    password = var.postgres_password
    database = var.postgres_database
  }

  type = "Opaque"
}

resource "kubernetes_persistent_volume_claim_v1" "postgres" {
  count = var.postgres_enabled ? 1 : 0

  metadata {
    name      = "postgres-pvc"
    namespace = local.keycloak_ns
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

  # Don't wait for the PVC to be bound - with WaitForFirstConsumer storage classes
  # (like GKE's standard-rwo), the PVC won't bind until a pod is scheduled to use it
  wait_until_bound = false
}

resource "kubernetes_deployment_v1" "postgres" {
  count = var.postgres_enabled ? 1 : 0

  metadata {
    name      = "postgres"
    namespace = local.keycloak_ns
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
                name = kubernetes_secret_v1.postgres[0].metadata[0].name
                key  = "username"
              }
            }
          }

          env {
            name = "POSTGRES_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.postgres[0].metadata[0].name
                key  = "password"
              }
            }
          }

          env {
            name = "POSTGRES_DB"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.postgres[0].metadata[0].name
                key  = "database"
              }
            }
          }

          # Use a subdirectory for PGDATA to avoid issues with lost+found directory
          # on freshly formatted volumes (common on cloud providers like GKE)
          env {
            name  = "PGDATA"
            value = "/var/lib/postgresql/data/pgdata"
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
            claim_name = kubernetes_persistent_volume_claim_v1.postgres[0].metadata[0].name
          }
        }
      }
    }
  }

  depends_on = [
    kubernetes_persistent_volume_claim_v1.postgres
  ]
}

resource "kubernetes_service_v1" "postgres" {
  count = var.postgres_enabled ? 1 : 0

  metadata {
    name      = "postgres"
    namespace = local.keycloak_ns
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

  keycloak_protocol       = var.ingress_tls_enabled ? "https://" : "http://"
  keycloak_base_path      = trimsuffix(var.keycloak_http_relative_path, "/")
  keycloak_realm_path     = "${local.keycloak_base_path}/realms/master"
  keycloak_health_path    = "${local.keycloak_base_path}/health/ready"
  keycloak_external_url   = "${local.keycloak_protocol}${var.hostname}"
  keycloak_ready_attempts = ceil(var.keycloak_ready_timeout_seconds / 5)

  ingress_controller_annotations = var.ingress_controller_type == "nginx" ? {
    "nginx.ingress.kubernetes.io/proxy-buffer-size"       = "128k"
    "nginx.ingress.kubernetes.io/proxy-busy-buffers-size" = "128k"
    } : var.ingress_controller_type == "haproxy" ? {
    "haproxy-ingress.github.io/proxy-body-size"      = "128k"
    "haproxy-ingress.github.io/timeout-http-request" = "30s"
  } : {}

  ingress_tls_annotations = var.ingress_tls_enabled ? {
    "cert-manager.io/cluster-issuer"                = var.ingress_cert_manager_cluster_issuer
    "cert-manager.io/common-name"                   = var.ingress_cert_manager_common_name != "" ? var.ingress_cert_manager_common_name : var.hostname
    "acme.cert-manager.io/http01-edit-in-place"     = "true"
    "acme.cert-manager.io/http01-ingress-path-type" = "ImplementationSpecific"
  } : {}

  keycloak_spec_base = {
    instances = var.keycloak_replicas
    http = {
      httpEnabled = true
      httpPort    = 8080
    }
    ingress = {
      enabled = false
    }
    hostname = {
      # hostname v2 including protocol and path
      hostname = "${local.keycloak_protocol}${var.hostname}${var.keycloak_http_relative_path}"
      strict   = true
    }
    # Use "unsupported" pod template to set admin credentials via environment variables.
    # Despite the name "unsupported", this is officially supported by the Keycloak Operator,
    # see https://www.keycloak.org/operator/advanced-configuration#_pod_template
    # For the admin credentials see https://www.keycloak.org/server/configuration#_creating_the_initial_admin_user
    unsupported = {
      podTemplate = {
        spec = {
          containers = [
            {
              name = "keycloak"
              env = [
                {
                  name  = "KC_BOOTSTRAP_ADMIN_USERNAME"
                  value = var.keycloak_admin_username
                },
                {
                  name  = "KC_BOOTSTRAP_ADMIN_PASSWORD"
                  value = var.keycloak_admin_password
                }
              ]
            }
          ]
        }
      }
    }

    additionalOptions = [
      # Use X-Forwarded-* headers from the ingress controller (works for both nginx and haproxy).
      {
        name  = "proxy-headers"
        value = "xforwarded"
      },
      {
        name  = "http-relative-path"
        value = var.keycloak_http_relative_path
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
        host     = kubernetes_service_v1.postgres[0].metadata[0].name
        port     = 5432
        database = var.postgres_database
        usernameSecret = {
          name = kubernetes_secret_v1.postgres[0].metadata[0].name
          key  = "username"
        }
        passwordSecret = {
          name = kubernetes_secret_v1.postgres[0].metadata[0].name
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
      namespace = local.keycloak_ns
    }
    spec = local.keycloak_spec
  })

  depends_on = [
    terraform_data.keycloak_operator,
    kubernetes_service_v1.postgres
  ]
}

resource "kubernetes_ingress_v1" "keycloak" {
  count = var.ingress_enabled ? 1 : 0

  metadata {
    name      = "keycloak"
    namespace = local.keycloak_ns
    annotations = merge(
      local.ingress_controller_annotations,
      local.ingress_tls_annotations,
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
    kubernetes_namespace_v1.keycloak,
    helm_release.ingress_nginx,
    helm_release.haproxy-ingress-controller
  ]
}

resource "kubectl_manifest" "keycloak_route" {
  count = var.cloud_provider == "OPENSHIFT" ? 1 : 0

  yaml_body = yamlencode({
    apiVersion = "route.openshift.io/v1"
    kind       = "Route"
    metadata = {
      name      = "keycloak"
      namespace = local.keycloak_ns
    }
    spec = {
      host = var.hostname
      to = {
        kind   = "Service"
        name   = "keycloak-service"
        weight = 100
      }
      port = {
        targetPort = 8080
      }
      wildcardPolicy = "None"
      tls = {
        termination                   = "edge"
        insecureEdgeTerminationPolicy = "Redirect"
      }
    }
  })

  depends_on = [
    kubectl_manifest.keycloak_instance
  ]
}

# Kubernetes readiness can become true before Keycloak 26.x has finished
# initializing the master realm. Wait for the application endpoint as well.
resource "terraform_data" "wait_for_keycloak_instance" {
  provisioner "local-exec" {
    command = <<-EOT
      set -e
      echo "Waiting for Keycloak resource to report ready..."
      kubectl wait keycloak/keycloak -n ${local.keycloak_ns} --for=condition=Ready --timeout=3m
      echo "Waiting for Keycloak pods to be ready..."
      kubectl wait pods -n ${local.keycloak_ns} -l app=keycloak --for=condition=Ready --timeout=3m
      echo "Waiting for Keycloak service endpoint..."
      kubectl wait --for=jsonpath='{.subsets[0].addresses[0].ip}' endpoints/keycloak-service -n ${local.keycloak_ns} --timeout=2m

      if [ "${var.keycloak_ready_check_in_cluster}" != "true" ]; then
        echo "Skipping the in-cluster Keycloak application readiness check."
        exit 0
      fi

      keycloak_status() {
        PORT=$1
        REQUEST_PATH=$2
        POD_NAME=$3
        STATUS=$(kubectl exec -n ${local.keycloak_ns} "$POD_NAME" -c keycloak -- bash -c "exec 3<>/dev/tcp/127.0.0.1/$PORT && printf 'GET $REQUEST_PATH HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n' >&3 && head -n1 <&3" 2>/dev/null | tr -d '\r' | cut -d' ' -f2)
        if [ -n "$STATUS" ]; then
          echo "$STATUS"
        else
          echo "000"
        fi
      }

      echo "Waiting up to ${var.keycloak_ready_timeout_seconds}s for Keycloak to serve ${local.keycloak_realm_path} in-cluster..."
      for i in $(seq 1 ${local.keycloak_ready_attempts}); do
        POD=$(kubectl get pods -n ${local.keycloak_ns} -l app=keycloak -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)
        if [ -z "$POD" ]; then
          echo "[$i/${local.keycloak_ready_attempts}] in-cluster: Keycloak pod not found"
          sleep 5
          continue
        fi

        REALM_STATUS=$(keycloak_status 8080 ${local.keycloak_realm_path} "$POD")
        HEALTH_STATUS=$(keycloak_status 9000 ${local.keycloak_health_path} "$POD")
        echo "[$i/${local.keycloak_ready_attempts}] in-cluster: realms/master=$REALM_STATUS health/ready=$HEALTH_STATUS"
        if [ "$REALM_STATUS" = "200" ]; then
          echo "Keycloak application is ready in-cluster."
          kubectl logs -n ${local.keycloak_ns} "$POD" -c keycloak | grep -m1 'started in' || true
          exit 0
        fi
        sleep 5
      done

      echo "Timed out waiting for Keycloak application readiness" >&2
      kubectl get keycloak -n ${local.keycloak_ns} keycloak -o jsonpath='{.status.conditions}' || true
      echo
      kubectl get pods -n ${local.keycloak_ns} -o wide || true
      POD=$(kubectl get pods -n ${local.keycloak_ns} -l app=keycloak -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)
      if [ -n "$POD" ]; then
        kubectl logs -n ${local.keycloak_ns} "$POD" -c keycloak --tail=200 || true
      fi
      exit 1
    EOT
  }

  depends_on = [
    kubectl_manifest.keycloak_instance
  ]
}

moved {
  from = terraform_data.wait_for_keycloak_route
  to   = terraform_data.wait_for_keycloak_endpoint
}

resource "terraform_data" "wait_for_keycloak_endpoint" {
  count = var.keycloak_ready_check_external && (var.cloud_provider == "OPENSHIFT" || var.ingress_enabled) ? 1 : 0

  provisioner "local-exec" {
    command = <<-EOT
      echo "Waiting up to ${var.keycloak_ready_timeout_seconds}s for Keycloak at ${local.keycloak_external_url}..."
      for i in $(seq 1 ${local.keycloak_ready_attempts}); do
        HTTP_CODE=$(curl -skL -o /dev/null -w '%%{http_code}' --max-time 10 "${local.keycloak_external_url}${local.keycloak_realm_path}" || true)
        if [ -z "$HTTP_CODE" ]; then
          HTTP_CODE="000"
        fi
        echo "[$i/${local.keycloak_ready_attempts}] external ${local.keycloak_external_url}${local.keycloak_realm_path} -> HTTP $HTTP_CODE"
        if [ "$HTTP_CODE" = "200" ]; then
          echo "Keycloak is reachable through the ingress or Route."
          exit 0
        fi
        sleep 5
      done

      echo "Timed out waiting for the external Keycloak endpoint" >&2
      curl -kisS --max-time 10 "${local.keycloak_external_url}${local.keycloak_realm_path}" | head -40 || true
      kubectl get endpoints -n ${local.keycloak_ns} keycloak-service -o yaml || true
      kubectl describe ingress -n ${local.keycloak_ns} keycloak || true
      kubectl describe route -n ${local.keycloak_ns} keycloak || true
      kubectl logs -n ${local.keycloak_ns} -l app=keycloak -c keycloak --tail=100 || true
      exit 1
    EOT
  }

  depends_on = [
    terraform_data.wait_for_keycloak_instance,
    kubectl_manifest.keycloak_route,
    kubernetes_ingress_v1.keycloak,
    terraform_data.wait_for_certificate,
    terraform_data.patch_ingress_controller
  ]
}

resource "terraform_data" "wait_for_certificate" {
  count = var.ingress_enabled && var.ingress_tls_enabled ? 1 : 0

  provisioner "local-exec" {
    command = "kubectl wait certificate -n ${local.keycloak_ns} ${local.tls_secret_name} --for=condition=Ready --timeout=3m"
  }

  depends_on = [
    kubernetes_ingress_v1.keycloak
  ]
}

resource "terraform_data" "patch_ingress_controller" {
  count = var.ingress_enabled && var.ingress_tls_enabled && var.ingress_controller_type == "nginx" ? 1 : 0

  provisioner "local-exec" {
    command = "kubectl patch deploy ingress-nginx-controller --type=${local.local_exec_quotes}json${local.local_exec_quotes} -n ${var.ingress_controller_namespace} -p ${local.local_exec_quotes}${local.jsonpatch}${local.local_exec_quotes} && kubectl -n ${var.ingress_controller_namespace} rollout status deploy/ingress-nginx-controller --timeout=180s"
  }

  depends_on = [
    terraform_data.wait_for_certificate
  ]
}
