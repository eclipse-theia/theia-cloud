output "service" {
  value = "https://service.${data.terraform_remote_state.minikube.outputs.hostname}"
}

output "instance" {
  value = "https://ws.${data.terraform_remote_state.minikube.outputs.hostname}"
}

output "keycloak" {
  value = "https://${data.terraform_remote_state.minikube.outputs.hostname}/keycloak/"
}

output "landing" {
  value = "https://try.${data.terraform_remote_state.minikube.outputs.hostname}"
}
