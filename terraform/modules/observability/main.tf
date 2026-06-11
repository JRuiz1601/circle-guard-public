resource "helm_release" "prometheus_stack" {
  name             = "monitoring"
  repository       = "https://prometheus-community.github.io/helm-charts"
  chart            = "kube-prometheus-stack"
  namespace        = var.namespace
  create_namespace = true
  version          = var.prometheus_chart_version
  values           = [file("${path.root}/../../../observability/helm/prometheus-values.yaml")]
}

resource "helm_release" "loki_stack" {
  name       = "loki"
  repository = "https://grafana.github.io/helm-charts"
  chart      = "loki-stack"
  namespace  = var.namespace
  version    = var.loki_chart_version
  values     = [file("${path.root}/../../../observability/helm/loki-values.yaml")]
}
