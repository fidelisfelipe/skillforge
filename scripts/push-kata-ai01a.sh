#!/bin/bash
# push-kata-ai01a.sh — Publica o branch kata-ai01a-template em skillforge-katas
#
# Usage: ./scripts/push-kata-ai01a.sh
#
# Requer: GITHUB_TOKEN env var (ou .env na raiz)

set -e

[ -f "$(dirname "$0")/../.env" ] && set -a && source "$(dirname "$0")/../.env" && set +a

GITHUB_TOKEN=${GITHUB_TOKEN:?GITHUB_TOKEN não definido}

BRANCH="kata-ai01a-template"
REPO_URL="https://${GITHUB_TOKEN}@github.com/fidelisfelipe/skillforge-katas.git"
TEMPLATE_DIR="$(cd "$(dirname "$0")/templates/kata-ai01a" && pwd)"
TEMP_DIR=$(mktemp -d)

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Publicando KATA-AI01A → branch: ${BRANCH}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo "📥 Clonando skillforge-katas..."
git clone --depth 1 "${REPO_URL}" "${TEMP_DIR}/repo" 2>/dev/null || git init "${TEMP_DIR}/repo"

cd "${TEMP_DIR}/repo"
git checkout --orphan "${BRANCH}"
git rm -rf . 2>/dev/null || true

echo "📋 Copiando template..."
cp -r "${TEMPLATE_DIR}/." .

cat > .gitignore << 'EOF'
target/
*.class
*.jar
.idea/
*.iml
.DS_Store
EOF

git config user.name "fidelisfelipe"
git config user.email "atosfiel@gmail.com"
git add .
git commit -m "feat(kata-ai01a): template IssueTriager"

echo "📤 Publicando branch ${BRANCH}..."
git remote set-url origin "${REPO_URL}" 2>/dev/null || git remote add origin "${REPO_URL}"
git push origin "${BRANCH}" --force

echo ""
echo "✅ https://github.com/fidelisfelipe/skillforge-katas/tree/${BRANCH}"
