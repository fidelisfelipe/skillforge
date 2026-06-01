#!/bin/bash
# create-all-katas.sh — Creates kata branches from generator output OR from hardcoded list
#
# Usage:
#   # From generator output, local repo only (no push):
#   ./scripts/create-all-katas.sh --from-generator --local
#
#   # From generator output, push to GitHub:
#   ./scripts/create-all-katas.sh --from-generator
#
#   # Full catalog list, local repo only:
#   ./scripts/create-all-katas.sh --local
#
#   # Full catalog list, push to GitHub (skip existing):
#   ./scripts/create-all-katas.sh --skip-existing

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

[ -f "${SCRIPT_DIR}/../.env" ] && set -a && source "${SCRIPT_DIR}/../.env" && set +a

FROM_GENERATOR=false
SKIP_EXISTING=false
LOCAL=false

for arg in "$@"; do
  [[ "$arg" == "--from-generator" ]] && FROM_GENERATOR=true
  [[ "$arg" == "--skip-existing"  ]] && SKIP_EXISTING=true
  [[ "$arg" == "--local"          ]] && LOCAL=true
done

if [ "$LOCAL" = false ]; then
  GITHUB_TOKEN=${GITHUB_TOKEN:?GITHUB_TOKEN not set (use --local for local-only mode)}
fi

SUMMARY_FILE="${SCRIPT_DIR}/kata-generator/output/summary.json"

# ── Mode: from generator output ─────────────────────────────────────────────
if [ "$FROM_GENERATOR" = true ]; then
  if [ ! -f "${SUMMARY_FILE}" ]; then
    echo "❌ No generator output found at ${SUMMARY_FILE}"
    echo "   Run: cd scripts/kata-generator && npm install && node generate.mjs --theme '...' --count 3"
    exit 1
  fi

  echo "📦 Reading generator output from ${SUMMARY_FILE}..."

  KATAS_OUTPUT_DIR="${SCRIPT_DIR}/kata-generator/output/katas"
  SUCCESS=0
  FAILED=0

  KATAS_OUTPUT_DIR="${SCRIPT_DIR}/kata-generator/output/katas"
  LIST_SCRIPT="${SCRIPT_DIR}/kata-generator/list-katas.cjs"

  # Convert path for Node.js (Windows binary needs Windows-style paths)
  WIN_SUMMARY=$(cygpath -w "${SUMMARY_FILE}" 2>/dev/null || echo "${SUMMARY_FILE}")

  # Read id|className pairs from summary.json via dedicated helper script
  while IFS='|' read -r KATA_ID CLASS_NAME; do
    TEST_FILE="${KATAS_OUTPUT_DIR}/${KATA_ID}_${CLASS_NAME}Test.java"
    echo ""
    echo "▶ ${KATA_ID} — ${CLASS_NAME}"

    if [ "$LOCAL" = true ]; then
      CMD="bash \"${SCRIPT_DIR}/create-kata-local.sh\" \"${KATA_ID}\" \"${CLASS_NAME}\" \"${TEST_FILE}\""
    else
      CMD="bash \"${SCRIPT_DIR}/create-kata.sh\" \"${KATA_ID}\" \"${CLASS_NAME}\" \"${TEST_FILE}\""
    fi

    if eval "$CMD"; then
      echo "✅ ${KATA_ID} done"
      SUCCESS=$((SUCCESS + 1))
    else
      echo "❌ ${KATA_ID} FAILED"
      FAILED=$((FAILED + 1))
    fi
  done < <(node "${LIST_SCRIPT}" "${WIN_SUMMARY}")

  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  Done: ${SUCCESS} created | ${FAILED} failed"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  exit 0
fi

# ── Mode: hardcoded full catalog list ────────────────────────────────────────
declare -a KATAS=(
  "KATA-001B BlockingIoService"
  "KATA-001C AggregatorService"
  "KATA-002A MoneyRecord"
  "KATA-002B Temperature"
  "KATA-002C ExprEvaluator"
  "KATA-003A ShapeClassifier"
  "KATA-003B ShapeAnalyzer"
  "KATA-003C EventHandler"
  "KATA-004A PaymentProcessor"
  "KATA-004B ResultHandler"
  "KATA-005A TemplateRenderer"
  "KATA-006A DayClassifier"
  "KATA-006B PricingEngine"
  "KATA-007A SalesReporter"
  "KATA-007B LibraryCatalog"
  "KATA-007C RunningAverageCollector"
  "KATA-008A DataPipeline"
  "KATA-008B TriFunction"
  "KATA-009A UserProfileService"
  "KATA-010A NumberStats"
  "KATA-010B MultiIndexMap"
  "KATA-011A AsyncOrderPipeline"
  "KATA-011B BoundedBlockingCache"
  "KATA-012A ModuleConsumer"
  "KATA-013A PaymentException"
)

TOTAL=${#KATAS[@]}
SUCCESS=0
FAILED=0
SKIPPED=0

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Creating ${TOTAL} kata branches (catalog list)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

for entry in "${KATAS[@]}"; do
  KATA_ID=$(echo "$entry"    | awk '{print $1}')
  CLASS_NAME=$(echo "$entry" | awk '{print $2}')
  BRANCH="$(echo "${KATA_ID}" | tr '[:upper:]' '[:lower:]')-template"

  if [ "$SKIP_EXISTING" = true ]; then
    EXISTS=$(git ls-remote "https://${GITHUB_TOKEN}@github.com/fidelisfelipe/skillforge-katas.git" \
      "refs/heads/${BRANCH}" 2>/dev/null | wc -l)
    if [ "$EXISTS" -gt 0 ]; then
      echo "⏭️  Skipping ${KATA_ID} — branch ${BRANCH} already exists"
      SKIPPED=$((SKIPPED + 1))
      continue
    fi
  fi

  echo ""
  echo "▶ [${KATA_ID}] ${CLASS_NAME} → ${BRANCH}"

  if [ "$LOCAL" = true ]; then
    CREATE_CMD="bash \"${SCRIPT_DIR}/create-kata-local.sh\" \"${KATA_ID}\" \"${CLASS_NAME}\""
  else
    CREATE_CMD="bash \"${SCRIPT_DIR}/create-kata.sh\" \"${KATA_ID}\" \"${CLASS_NAME}\""
  fi

  if eval "$CREATE_CMD"; then
    echo "✅ ${KATA_ID} done"
    SUCCESS=$((SUCCESS + 1))
  else
    echo "❌ ${KATA_ID} FAILED"
    FAILED=$((FAILED + 1))
  fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Done: ${SUCCESS} created | ${SKIPPED} skipped | ${FAILED} failed"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
