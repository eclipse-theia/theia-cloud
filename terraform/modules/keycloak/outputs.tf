
output "realm" {
  value = {
    id   = keycloak_realm.theia-cloud.id
    name = keycloak_realm.theia-cloud.realm
  }
}

output "admin_group" {
  value = {
    id   = keycloak_group.theia_cloud_admin.id
    name = keycloak_group.theia_cloud_admin.name
  }
}

output "test_users" {
  value = {
    foo = {
      id       = keycloak_user.test-user-foo.id
      username = keycloak_user.test-user-foo.username
      email    = keycloak_user.test-user-foo.email
    }
    bar = {
      id       = keycloak_user.test-user-bar.id
      username = keycloak_user.test-user-bar.username
      email    = keycloak_user.test-user-bar.email
    }
  }
}
