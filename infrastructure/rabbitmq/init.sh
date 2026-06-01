#!/bin/bash

# RabbitMQ Initialization Script
# Cria queues, exchanges e bindings para o sistema de Kata

echo "🐰 Initializing RabbitMQ for SkillForge Kata System..."

# Aguardar RabbitMQ estar pronto
until rabbitmq-diagnostics -q ping; do
  echo "⏳ Aguardando RabbitMQ..."
  sleep 2
done

echo "✅ RabbitMQ está pronto. Configurando..."

# === QUEUES ===

# Fila: Requisições de tema/kata (Hero → Hub)
rabbitmqctl declare_queue name=kata.requests durable=true

# Fila: Respostas de tema/kata (Hub → Hero)
rabbitmqctl declare_queue name=kata.responses durable=true

# Fila: Submissão de kata para validação (Hero → Hub)
rabbitmqctl declare_queue name=kata.submission durable=true

# Fila: Resultados de validação (Hub → Hero)
rabbitmqctl declare_queue name=kata.validation.results durable=true

# === EXCHANGES ===

# Topic Exchange: Para broadcast de skill validated (Hub → Guilda)
rabbitmqctl declare_exchange name=skill.events type=topic durable=true

# Direct Exchange: Para respostas específicas por hero
rabbitmqctl declare_exchange name=kata.responses.exchange type=direct durable=true

# === BINDINGS ===

# Bind: kata.responses queue ao exchange
rabbitmqctl declare_binding source_name=kata.responses.exchange destination_name=kata.responses type=queue routing_key=kata.responses.*

# === VIRTUAL HOST ===

# Criar vhost se não existir
rabbitmqctl add_vhost /skillforge || true

# Dar permissões
rabbitmqctl set_permissions -p /skillforge skillforge ".*" ".*" ".*"

echo "✅ RabbitMQ initialization complete!"
echo ""
echo "📊 Queues:"
echo "  - kata.requests              (Hero → Hub: requisições de tema/kata)"
echo "  - kata.responses             (Hub → Hero: respostas)"
echo "  - kata.submission            (Hero → Hub: submissão para validação)"
echo "  - kata.validation.results    (Hub → Hero: resultados)"
echo ""
echo "📡 Exchanges:"
echo "  - skill.events (topic)       (Hub → Guilda: broadcast de skill validated)"
echo ""
echo "🔗 Management UI: http://localhost:15672"
echo "   User: skillforge"
echo "   Pass: skillforge-dev"
