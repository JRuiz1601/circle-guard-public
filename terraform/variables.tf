variable "environment" {
  description = "Nombre del ambiente (dev, stage, prod)"
  type        = string
}

variable "namespace" {
  description = "Namespace de Kubernetes donde se despliegan los recursos"
  type        = string
}

variable "image_tag" {
  description = "Tag de la imagen Docker a desplegar"
  type        = string
  default     = "latest"
}

variable "replicas" {
  description = "Número de réplicas para los deployments"
  type        = number
  default     = 1
}
