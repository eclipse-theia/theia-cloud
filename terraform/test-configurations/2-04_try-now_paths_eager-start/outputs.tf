output "service" {
  value = "https://${data.terraform_remote_state.minikube.outputs.hostname}/service"
}

output "instance" {
  value = "https://${data.terraform_remote_state.minikube.outputs.hostname}/instances"
}

output "keycloak" {
  value = "https://${data.terraform_remote_state.minikube.outputs.hostname}/keycloak/"
}

output "landing" {
  value = "https://${data.terraform_remote_state.minikube.outputs.hostname}/try"
}
