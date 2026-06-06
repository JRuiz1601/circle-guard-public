resource "kubernetes_deployment" "this" {
  metadata {
    name      = var.name
    namespace = var.namespace
    labels = {
      app         = var.name
      environment = var.environment
    }
  }

  wait_for_rollout = false

  spec {
    replicas = var.replicas

    selector {
      match_labels = {
        app = var.name
      }
    }

    template {
      metadata {
        labels = {
          app         = var.name
          environment = var.environment
        }
      }

      spec {
        image_pull_secrets {
          name = "ghcr-secret"
        }

        container {
          name              = var.name
          image             = var.image
          image_pull_policy = "Always"

          port {
            container_port = var.port
          }

          dynamic "env" {
            for_each = var.env_vars
            content {
              name  = env.key
              value = env.value
            }
          }

          liveness_probe {
            http_get {
              path = "/actuator/health"
              port = var.port
            }
            initial_delay_seconds = 120
            period_seconds        = 10
            failure_threshold     = 5
          }

          readiness_probe {
            http_get {
              path = "/actuator/health/readiness"
              port = var.port
            }
            initial_delay_seconds = 120
            period_seconds        = 5
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "this" {
  metadata {
    name      = var.name
    namespace = var.namespace
  }

  spec {
    selector = {
      app = var.name
    }

    type = "ClusterIP"

    port {
      port        = var.port
      target_port = var.port
    }
  }
}

resource "kubernetes_config_map" "this" {
  metadata {
    name      = "${var.name}-config"
    namespace = var.namespace
  }

  data = var.config_data
}
