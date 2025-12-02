output "cluster_host" {
  value = module.cluster.cluster_host
}

output "keycloak_url" {
  value = module.keycloak_setup.keycloak_url
}

output "keycloak_admin_username" {
  value = module.keycloak_setup.admin_username
}

output "hostname" {
  value = "${module.host.host}.nip.io"
}
