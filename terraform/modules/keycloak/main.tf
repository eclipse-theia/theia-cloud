variable "hostname" {
  description = "Hostname for the keycloak instance"
}

variable "keycloak_test_user_foo_password" {
  description = "Keycloak Foo Test User Password"
  sensitive   = true
}

variable "keycloak_test_user_bar_password" {
  description = "Keycloak Bar Test User Password"
  sensitive   = true
}

variable "valid_redirect_uri" {
  description = "URI for redirects."
}

resource "keycloak_realm" "theia-cloud" {
  realm   = "TheiaCloud"
  enabled = true
}

resource "keycloak_openid_client" "theia-cloud" {
  realm_id                                  = keycloak_realm.theia-cloud.id
  client_id                                 = "theia-cloud"
  enabled                                   = true
  access_type                               = "PUBLIC"
  client_authenticator_type                 = "client-secret"
  direct_access_grants_enabled              = true
  implicit_flow_enabled                     = false
  oauth2_device_authorization_grant_enabled = false
  service_accounts_enabled                  = false
  standard_flow_enabled                     = true
  use_refresh_tokens                        = true
  use_refresh_tokens_client_credentials     = false
  valid_post_logout_redirect_uris = [
    "${var.valid_redirect_uri}"
  ]
  valid_redirect_uris = [
    "${var.valid_redirect_uri}"
  ]
  web_origins = [
    "${var.valid_redirect_uri}"
  ]
}

resource "keycloak_group" "theia_cloud_admin" {
  realm_id = keycloak_realm.theia-cloud.id
  name     = "theia-cloud/admin"
}

resource "keycloak_openid_group_membership_protocol_mapper" "groups" {
  realm_id   = keycloak_realm.theia-cloud.id
  client_id  = keycloak_openid_client.theia-cloud.id
  name       = "groups"
  claim_name = "groups"
  # Disable full path for group names to just get the group name as configured
  # and avoid Keycloak prefixing them with a slash
  full_path = false
}

resource "keycloak_openid_audience_protocol_mapper" "audience" {
  realm_id                 = keycloak_realm.theia-cloud.id
  client_id                = keycloak_openid_client.theia-cloud.id
  name                     = "audience"
  included_custom_audience = "theia-cloud"
}

resource "keycloak_user" "test-user-foo" {
  realm_id       = keycloak_realm.theia-cloud.id
  username       = "foo"
  enabled        = true
  email          = "foo@theia-cloud.io"
  email_verified = true
  initial_password {
    value     = var.keycloak_test_user_foo_password
    temporary = false
  }
}

resource "keycloak_user" "test-user-bar" {
  realm_id       = keycloak_realm.theia-cloud.id
  username       = "bar"
  enabled        = true
  email          = "bar@theia-cloud.io"
  email_verified = true
  initial_password {
    value     = var.keycloak_test_user_bar_password
    temporary = false
  }
}
