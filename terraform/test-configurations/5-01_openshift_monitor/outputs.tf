output "service" {
  value = "https://service.${data.terraform_remote_state.openshift.outputs.hostname}"
}

output "instance" {
  value = "https://ws.${data.terraform_remote_state.openshift.outputs.hostname}"
}

output "landing" {
  value = "https://try.${data.terraform_remote_state.openshift.outputs.hostname}"
}
