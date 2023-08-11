output "try_now" {
  description = "Try Now URL."
  value       = "https://${module.cluster.loadbalancer_ip}.sslip.io/trynow/"
}
