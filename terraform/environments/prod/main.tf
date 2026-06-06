terraform {
  backend "remote" {
    organization = "circleguard-icesi"
    workspaces {
      name = "circleguard-prod"
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
  config_context = "minikube"
}

provider "helm" {
  kubernetes {
    config_path    = "~/.kube/config"
    config_context = "minikube"
  }
}

module "namespace" {
  source      = "../../modules/namespace"
  name        = "circleguard-prod"
  environment = "prod"
}

variable "environment" {
  type    = string
  default = "prod"
}

variable "image_tag" {
  type    = string
  default = "latest"
}

variable "replicas" {
  type    = number
  default = 2
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
    SPRING_PROFILES_ACTIVE                        = "prod"
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
    SPRING_PROFILES_ACTIVE                        = "prod"
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
    SPRING_PROFILES_ACTIVE = "prod"
    AUTH_SERVICE_URL       = "http://circleguard-auth-service:8081"
  }
}
