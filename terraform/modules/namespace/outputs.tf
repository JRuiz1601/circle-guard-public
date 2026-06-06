output "name" {
  description = "Nombre del namespace creado"
  value       = kubernetes_namespace.this.metadata[0].name
}
