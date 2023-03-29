output "try_now" {
  description = "Try Now URL."
  value       = "https://${google_compute_address.host_ip.address}.sslip.io/trynow/"
}

