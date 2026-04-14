output "service" {
  value = "http://service.${data.terraform_remote_state.openshift.outputs.hostname}"
}

output "instance" {
  value = "http://ws.${data.terraform_remote_state.openshift.outputs.hostname}"
}

output "landing" {
  value = "http://try.${data.terraform_remote_state.openshift.outputs.hostname}"
}
