variable "name" {
  description = "Nombre del microservicio (usado como nombre del Deployment, Service y ConfigMap)"
  type        = string
}

variable "namespace" {
  description = "Namespace de Kubernetes donde se despliega el servicio"
  type        = string
}

variable "image" {
  description = "Imagen Docker completa incluyendo registry y tag (ej: ghcr.io/jruiz1601/circleguard-auth-service:latest)"
  type        = string
}

variable "port" {
  description = "Puerto en el que escucha el contenedor (SERVER_PORT del servicio Spring Boot)"
  type        = number
}

variable "replicas" {
  description = "Número de réplicas del Deployment"
  type        = number
  default     = 1
}

variable "environment" {
  description = "Ambiente de despliegue (dev, stage, prod)"
  type        = string
}

variable "env_vars" {
  description = "Variables de entorno a inyectar en el contenedor"
  type        = map(string)
  default     = {}
}

variable "config_data" {
  description = "Datos para el ConfigMap del servicio (clave-valor)"
  type        = map(string)
  default     = {}
}
