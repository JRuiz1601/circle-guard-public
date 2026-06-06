resource "kubernetes_namespace" "this" {
  metadata {
    name = var.name
    labels = {
      environment = var.environment
      managed-by  = "terraform"
    }
  }
}
