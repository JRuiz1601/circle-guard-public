variable "name" {
  description = "Nombre del namespace de Kubernetes"
  type        = string
}

variable "environment" {
  description = "Ambiente al que pertenece el namespace (dev, stage, prod)"
  type        = string
}
