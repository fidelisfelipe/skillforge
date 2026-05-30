#!/usr/bin/env bash
# Scaffold a new hero module from hero-template.
# Usage: ./scripts/new-hero.sh --id meu-hero [--skills "java,spring-boot"] [--name "Meu Nome"] [--port 8082]

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMPLATE_DIR="$REPO_ROOT/hero-template"
PARENT_POM="$REPO_ROOT/pom.xml"

# ── Parse args ──────────────────────────────────────────────────────────────

HERO_ID=""
SKILLS="skill-1,skill-2"
HERO_NAME=""
PORT=8082

while [[ $# -gt 0 ]]; do
  case "$1" in
    --id)     HERO_ID="$2";    shift 2 ;;
    --skills) SKILLS="$2";     shift 2 ;;
    --name)   HERO_NAME="$2";  shift 2 ;;
    --port)   PORT="$2";       shift 2 ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done

# ── Validate ─────────────────────────────────────────────────────────────────

if [[ -z "$HERO_ID" ]]; then
  echo "Usage: $0 --id <hero-id> [--skills \"skill1,skill2\"] [--name \"Hero Name\"] [--port 8082]"
  echo "Example: $0 --id code-sentinel --skills \"java,static-analysis\" --port 8083"
  exit 1
fi

if [[ ! "$HERO_ID" =~ ^[a-z0-9-]+$ ]]; then
  echo "Error: --id must be lowercase letters, digits and hyphens only (e.g. meu-hero)"
  exit 1
fi

TARGET_DIR="$REPO_ROOT/$HERO_ID"

if [[ -d "$TARGET_DIR" ]]; then
  echo "Error: directory '$TARGET_DIR' already exists."
  exit 1
fi

[[ -z "$HERO_NAME" ]] && HERO_NAME="$(echo "$HERO_ID" | sed 's/-/ /g' | awk '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1))substr($i,2); print}')"

# ── Build skills JSON array ───────────────────────────────────────────────────

SKILLS_JSON=$(echo "$SKILLS" | tr ',' '\n' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | jq -R . | jq -s .)

# ── Copy template ────────────────────────────────────────────────────────────

echo "Creating hero module: $HERO_ID"
cp -r "$TEMPLATE_DIR" "$TARGET_DIR"

# ── Substitute placeholders ──────────────────────────────────────────────────

# pom.xml
sed -i \
  -e "s|<artifactId>hero-template</artifactId>|<artifactId>$HERO_ID</artifactId>|g" \
  -e "s|<name>SkillForge Hero Template</name>|<name>SkillForge Hero $(echo "$HERO_NAME")</name>|g" \
  "$TARGET_DIR/pom.xml"

# application.yml
sed -i \
  -e "s|name: skillforge-hero|name: skillforge-$HERO_ID|g" \
  -e "s|id: hero-template|id: $HERO_ID|g" \
  -e "s|port: 8081|port: $PORT|g" \
  "$TARGET_DIR/src/main/resources/application.yml"

# manifest.json
cat > "$TARGET_DIR/src/main/resources/manifest.json" <<EOF
{
  "heroId": "$HERO_ID",
  "heroName": "$HERO_NAME",
  "heroClass": "Backend",
  "skills": $SKILLS_JSON,
  "endpoint": "http://localhost:$PORT",
  "model": "phi3:mini",
  "specialty": "Descreva em uma frase o que este hero faz"
}
EOF

# Rename .http test file
HTTP_FILE="$TARGET_DIR/src/test/resources/hero-template.http"
if [[ -f "$HTTP_FILE" ]]; then
  NEW_HTTP="$TARGET_DIR/src/test/resources/$HERO_ID.http"
  mv "$HTTP_FILE" "$NEW_HTTP"
  sed -i "s|hero-template|$HERO_ID|g" "$NEW_HTTP"
  sed -i "s|8081|$PORT|g" "$NEW_HTTP"
fi

# ── Register module in parent pom.xml ────────────────────────────────────────

if grep -q "<module>$HERO_ID</module>" "$PARENT_POM"; then
  echo "Module '$HERO_ID' already in parent pom.xml — skipping."
else
  # Insert before the closing </modules> tag
  sed -i "s|</modules>|    <module>$HERO_ID</module>\n    </modules>|" "$PARENT_POM"
  echo "Added <module>$HERO_ID</module> to parent pom.xml"
fi

# ── Done ─────────────────────────────────────────────────────────────────────

echo ""
echo "Hero '$HERO_ID' created at: $TARGET_DIR"
echo ""
echo "Next steps:"
echo "  1. Edit $TARGET_DIR/src/main/resources/manifest.json (update specialty)"
echo "  2. Implement your logic in $TARGET_DIR/src/main/java/com/skillforge/hero/service/SolveService.java"
echo "  3. Run locally:"
echo "     cd $TARGET_DIR && AMQP_URL=amqp://localhost DEV_MODE=true mvn spring-boot:run"
echo "  4. When ready, rename the branch and push:"
echo "     git branch -m local/$HERO_ID feature/$HERO_ID && git push origin feature/$HERO_ID"
