output "service_name" {
  description = "Nombre del Service de Kubernetes creado (usado como DNS interno entre pods)"
  value       = kubernetes_service.this.metadata[0].name
}

output "namespace" {
  description = "Namespace donde fue desplegado el servicio"
  value       = kubernetes_service.this.metadata[0].namespace
}
