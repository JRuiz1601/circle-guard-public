variable "namespace" {
  type    = string
  default = "monitoring"
}
variable "environment" {
  type = string
}
variable "prometheus_chart_version" {
  type    = string
  default = "61.7.2"
}
variable "loki_chart_version" {
  type    = string
  default = "2.10.2"
}
