#!/bin/bash

# Script para rodar MVP de Kata
# Configura variáveis de ambiente e inicia Hub + Hero-Template

set -e

echo "🥋 SkillForge Kata MVP Bootstrap"
echo "=================================="
echo ""

# === CONFIGURAÇÃO ===

# Defaults (sobrescreva no seu shell)
RABBITMQ_HOST=${RABBITMQ_HOST:-localhost}
RABBITMQ_PORT=${RABBITMQ_PORT:-5672}
RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-skillforge-dev}
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-skillforge-dev}
RABBITMQ_VHOST=${RABBITMQ_VHOST:-/skillforge}

HERO_ID=${HERO_ID:-alice}
HERO_NAME=${HERO_NAME:-Alice}

GITHUB_TOKEN=${GITHUB_TOKEN:-}
GITHUB_OWNER=${GITHUB_OWNER:-}
GITHUB_REPO=${GITHUB_REPO:-}

# === VALIDAÇÃO ===

echo "📋 Validando configuração..."

if [ -z "$RABBITMQ_HOST" ]; then
  echo "❌ RABBITMQ_HOST não definido"
  exit 1
fi

if [ -z "$HERO_ID" ]; then
  echo "❌ HERO_ID não definido"
  exit 1
fi

echo "✅ RabbitMQ: $RABBITMQ_HOST:$RABBITMQ_PORT"
echo "✅ VHost: $RABBITMQ_VHOST"
echo "✅ Hero ID: $HERO_ID ($HERO_NAME)"
echo ""

# === CRIAR DIRETÓRIOS ===

mkdir -p ~/.skillforge/heroes/$HERO_ID
echo "✅ Diretórios criados"
echo ""

# === FUNÇÃO: Iniciar Hub ===

start_hub() {
  echo "🚀 Iniciando Hub..."
  cd ~/skillforge/hub

  mvn spring-boot:run \
    --spring-boot.run.arguments="--spring.profiles.active=kata" \
    -Dspring.rabbitmq.host=$RABBITMQ_HOST \
    -Dspring.rabbitmq.port=$RABBITMQ_PORT \
    -Dspring.rabbitmq.username=$RABBITMQ_USERNAME \
    -Dspring.rabbitmq.password=$RABBITMQ_PASSWORD \
    -Dspring.rabbitmq.virtual-host=$RABBITMQ_VHOST \
    -DGITHUB_TOKEN=$GITHUB_TOKEN \
    -DGITHUB_OWNER=$GITHUB_OWNER \
    -DGITHUB_REPO=$GITHUB_REPO
}

# === FUNÇÃO: Iniciar Hero ===

start_hero() {
  echo "🚀 Iniciando Hero-Template..."
  cd ~/skillforge/heroes/hero-template

  mvn spring-boot:run \
    --spring-boot.run.arguments="--spring.profiles.active=kata --server.port=8081" \
    -Dspring.rabbitmq.host=$RABBITMQ_HOST \
    -Dspring.rabbitmq.port=$RABBITMQ_PORT \
    -Dspring.rabbitmq.username=$RABBITMQ_USERNAME \
    -Dspring.rabbitmq.password=$RABBITMQ_PASSWORD \
    -Dspring.rabbitmq.virtual-host=$RABBITMQ_VHOST \
    -DHERO_ID=$HERO_ID \
    -DHERO_NAME=$HERO_NAME
}

# === EXECUTAR ===

case "${1:-all}" in
  hub)
    start_hub
    ;;
  hero)
    start_hero
    ;;
  all)
    echo "Para rodar Hub + Hero em paralelo, abra 2 terminais:"
    echo ""
    echo "  Terminal 1: $0 hub"
    echo "  Terminal 2: $0 hero"
    echo ""
    echo "Ou abra ambos:"
    echo ""
    start_hub &
    sleep 3
    start_hero &
    wait
    ;;
  *)
    echo "Uso: $0 [hub|hero|all]"
    echo ""
    echo "Exemplos:"
    echo "  $0 hub        # Inicia apenas Hub"
    echo "  $0 hero       # Inicia apenas Hero"
    echo "  $0 all        # Inicia Hub + Hero em paralelo"
    exit 1
    ;;
esac
