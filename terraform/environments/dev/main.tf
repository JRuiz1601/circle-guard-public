terraform {
  backend "remote" {
    organization = "circleguard-icesi"
    workspaces {
      name = "circleguard-dev"
    }
  }

  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.35"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.17"
    }
  }
}

provider "kubernetes" {
  config_path    = "~/.kube/config"
  config_context = "kind-circleguard"
}

provider "helm" {
  kubernetes {
    config_path    = "~/.kube/config"
    config_context = "kind-circleguard"
  }
}

module "namespace" {
  source      = "../../modules/namespace"
  name        = "circleguard-dev"
  environment = "dev"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "image_tag" {
  type    = string
  default = "latest"
}

variable "replicas" {
  type    = number
  default = 1
}

module "auth_service" {
  source      = "../../modules/microservice"
  name        = "circleguard-auth-service"
  namespace   = module.namespace.name
  image       = "ghcr.io/jruiz1601/circleguard-auth-service:${var.image_tag}"
  port        = 8081
  replicas    = var.replicas
  environment = var.environment
  env_vars = {
    SERVER_PORT                                   = "8081"
    SPRING_PROFILES_ACTIVE                        = "dev"
    SPRING_DATASOURCE_USERNAME                    = "postgres"
    SPRING_DATASOURCE_PASSWORD                    = "postgres"
    SPRING_DATASOURCE_URL                         = "jdbc:postgresql://postgres:5432/circleguard_auth"
    CIRCLEGUARD_IDENTITY_MAP_URL                  = "http://circleguard-identity-service/api/v1/identities/map"
    JWT_SECRET                                    = "my-super-secret-dev-key-32-chars-long-12345678"
    KAFKA_BOOTSTRAP_SERVERS                       = "circleguard-kafka:29092"
  }
}

module "identity_service" {
  source      = "../../modules/microservice"
  name        = "circleguard-identity-service"
  namespace   = module.namespace.name
  image       = "ghcr.io/jruiz1601/circleguard-identity-service:${var.image_tag}"
  port        = 8082
  replicas    = var.replicas
  environment = var.environment
  env_vars = {
    SERVER_PORT                                   = "8082"
    SPRING_PROFILES_ACTIVE                        = "dev"
    SPRING_DATASOURCE_USERNAME                    = "postgres"
    SPRING_DATASOURCE_PASSWORD                    = "postgres"
    SPRING_DATASOURCE_URL                         = "jdbc:postgresql://postgres:5432/circleguard_identity"
    JWT_SECRET                                    = "my-super-secret-dev-key-32-chars-long-12345678"
    SPRING_KAFKA_PRODUCER_PROPERTIES_MAX_BLOCK_MS = "3000"
    KAFKA_BOOTSTRAP_SERVERS                       = "circleguard-kafka:29092"
  }
}

module "gateway_service" {
  source      = "../../modules/microservice"
  name        = "circleguard-gateway-service"
  namespace   = module.namespace.name
  image       = "ghcr.io/jruiz1601/circleguard-gateway-service:${var.image_tag}"
  port        = 8080
  replicas    = var.replicas
  environment = var.environment
  env_vars = {
    SERVER_PORT            = "8080"
    SPRING_PROFILES_ACTIVE = "dev"
    AUTH_SERVICE_URL       = "http://circleguard-auth-service:8081"
  }
}

module "form_service" {
  source      = "../../modules/microservice"
  name        = "circleguard-form-service"
  namespace   = module.namespace.name
  image       = "ghcr.io/jruiz1601/circleguard-form-service:${var.image_tag}"
  port        = 8083
  replicas    = var.replicas
  environment = var.environment
  env_vars = {
    SERVER_PORT                = "8083"
    SPRING_PROFILES_ACTIVE     = "dev"
    SPRING_DATASOURCE_USERNAME = "postgres"
    SPRING_DATASOURCE_PASSWORD = "postgres"
    SPRING_DATASOURCE_URL      = "jdbc:postgresql://postgres:5432/circleguard_form"
  }
}

module "file_service" {
  source      = "../../modules/microservice"
  name        = "circleguard-file-service"
  namespace   = module.namespace.name
  image       = "ghcr.io/jruiz1601/circleguard-file-service:${var.image_tag}"
  port        = 8084
  replicas    = var.replicas
  environment = var.environment
  env_vars = {
    SERVER_PORT            = "8084"
    SPRING_PROFILES_ACTIVE = "dev"
  }
}

module "dashboard_service" {
  source      = "../../modules/microservice"
  name        = "circleguard-dashboard-service"
  namespace   = module.namespace.name
  image       = "ghcr.io/jruiz1601/circleguard-dashboard-service:${var.image_tag}"
  port        = 8085
  replicas    = var.replicas
  environment = var.environment
  env_vars = {
    SERVER_PORT            = "8085"
    SPRING_PROFILES_ACTIVE = "dev"
  }
}

module "notification_service" {
  source      = "../../modules/microservice"
  name        = "circleguard-notification-service"
  namespace   = module.namespace.name
  image       = "ghcr.io/jruiz1601/circleguard-notification-service:${var.image_tag}"
  port        = 8082
  replicas    = var.replicas
  environment = var.environment
  env_vars = {
    SERVER_PORT             = "8082"
    SPRING_PROFILES_ACTIVE  = "dev"
    KAFKA_BOOTSTRAP_SERVERS = "circleguard-kafka:29092"
  }
}

module "promotion_service" {
  source      = "../../modules/microservice"
  name        = "circleguard-promotion-service"
  namespace   = module.namespace.name
  image       = "ghcr.io/jruiz1601/circleguard-promotion-service:${var.image_tag}"
  port        = 8088
  replicas    = var.replicas
  environment = var.environment
  env_vars = {
    SERVER_PORT             = "8088"
    SPRING_PROFILES_ACTIVE  = "dev"
    KAFKA_BOOTSTRAP_SERVERS = "circleguard-kafka:29092"
    NEO4J_URI               = "bolt://circleguard-neo4j:7687"
  }
}

module "observability" {
  source      = "../../modules/observability"
  namespace   = "monitoring"
  environment = var.environment
}
