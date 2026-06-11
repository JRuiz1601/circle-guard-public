output "namespace" {
  value = var.namespace
}
output "prometheus_release_name" {
  value = helm_release.prometheus_stack.name
}
output "loki_release_name" {
  value = helm_release.loki_stack.name
}
