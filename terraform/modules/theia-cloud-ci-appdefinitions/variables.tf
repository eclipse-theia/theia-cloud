variable "image_theia" {
  description = "Container image for the Theia-based AppDefinitions (monitor-theia-* and theia-cloud-demo)."
  type        = string
  default     = "theiacloud/theia-cloud-activity-demo-theia:minikube-ci-e2e"
}

variable "image_vscode" {
  description = "Container image for the VS Code-based AppDefinitions (monitor-vscode-*)."
  type        = string
  default     = "theiacloud/theia-cloud-activity-demo:minikube-ci-e2e"
}

variable "image_pull_policy" {
  description = "imagePullPolicy applied to all AppDefinitions emitted by this module."
  type        = string
  default     = "IfNotPresent"
}

variable "eager_start" {
  description = "When true, sets minInstances=1 on the four monitor-* AppDefinitions (matches the e2e `eagerStart` matrix axis)."
  type        = bool
  default     = false
}
