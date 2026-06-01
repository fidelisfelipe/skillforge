#!/bin/bash
# create-kata-local.sh — Creates a kata template branch directly in a local repo (no push)
#
# Usage:
#   ./scripts/create-kata-local.sh <KATA-ID> <ClassName> [test-file-path]
#
# The branch is created in $SKILLFORGE_KATAS_DIR (default: sibling directory skillforge-katas)

set -e

KATA_ID=${1:?Usage: create-kata-local.sh <KATA-ID> <ClassName> [test-file-path]}
CLASS_NAME=${2:?Usage: create-kata-local.sh <KATA-ID> <ClassName> [test-file-path]}
TEST_FILE=${3:-}

KATA_LOWER=$(echo "${KATA_ID}" | tr '[:upper:]' '[:lower:]')
BRANCH="${KATA_LOWER}-template"
PACKAGE="com.skillforge.kata"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOCAL_REPO="${SKILLFORGE_KATAS_DIR:-$(cd "${SCRIPT_DIR}/../../skillforge-katas" 2>/dev/null && pwd)}"

if [ -z "${LOCAL_REPO}" ] || [ ! -d "${LOCAL_REPO}/.git" ]; then
  echo "❌ skillforge-katas repo not found. Set SKILLFORGE_KATAS_DIR or place it alongside skillforge."
  exit 1
fi

MAIN_PKG_DIR="src/main/java/com/skillforge/kata"
TEST_PKG_DIR="src/test/java/com/skillforge/kata"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  ${KATA_ID} → branch: ${BRANCH}"
echo "  Class: ${CLASS_NAME}"
echo "  Repo:  ${LOCAL_REPO}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

cd "${LOCAL_REPO}"

# Save current branch to restore later
ORIGINAL_BRANCH=$(git branch --show-current 2>/dev/null || echo "main")

# Check if branch already exists locally
if git show-ref --verify --quiet "refs/heads/${BRANCH}"; then
  echo "⚠️  Branch ${BRANCH} already exists locally — deleting and recreating"
  git branch -D "${BRANCH}"
fi

# Create orphan branch and clear working tree
git checkout --orphan "${BRANCH}"
git rm -rf . 2>/dev/null || true
rm -rf src pom.xml README.md .gitignore 2>/dev/null || true

# ── Create Maven structure ──────────────────────────────────────────────────
mkdir -p "${MAIN_PKG_DIR}" "${TEST_PKG_DIR}"

# pom.xml
cat > pom.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.skillforge.kata</groupId>
  <artifactId>${KATA_LOWER}</artifactId>
  <version>1.0.0</version>

  <properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>5.10.0</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.2.5</version>
      </plugin>
    </plugins>
  </build>
</project>
EOF

# Skeleton implementation class (intentionally empty)
cat > "${MAIN_PKG_DIR}/${CLASS_NAME}.java" << EOF
package ${PACKAGE};

/**
 * ${KATA_ID}: ${CLASS_NAME}
 *
 * TODO: Implemente os métodos abaixo para fazer os testes passarem.
 * Execute: mvn verify
 */
public class ${CLASS_NAME} {

    // TODO: adicione os métodos necessários

}
EOF

# Test class — use generated file if provided, otherwise placeholder
if [ -n "${TEST_FILE}" ] && [ -f "${TEST_FILE}" ]; then
  echo "📋 Using generated test: $(basename "${TEST_FILE}")"
  cp "${TEST_FILE}" "${TEST_PKG_DIR}/${CLASS_NAME}Test.java"
else
  cat > "${TEST_PKG_DIR}/${CLASS_NAME}Test.java" << EOF
package ${PACKAGE};

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("${KATA_ID}: ${CLASS_NAME}")
class ${CLASS_NAME}Test {

    private ${CLASS_NAME} subject;

    @BeforeEach
    void setUp() {
        subject = new ${CLASS_NAME}();
    }

    @Test
    @DisplayName("TODO: descreva o primeiro critério de aceite")
    void test_1() {
        fail("Implemente este teste de acordo com a spec do kata");
    }

    @Test
    @DisplayName("TODO: descreva o segundo critério de aceite")
    void test_2() {
        fail("Implemente este teste de acordo com a spec do kata");
    }
}
EOF
fi

# .gitignore
cat > .gitignore << 'EOF'
target/
*.class
*.jar
.idea/
*.iml
.DS_Store
EOF

# README.md
cat > README.md << EOF
# ${KATA_ID}: ${CLASS_NAME}

> Consulte \`quests/dojo/java-21-certified/catalog.yml\` no repo \`skillforge\` para a spec completa.

## Como resolver

1. Abra \`src/main/java/com/skillforge/kata/${CLASS_NAME}.java\`
2. Leia os testes em \`src/test/java/com/skillforge/kata/${CLASS_NAME}Test.java\`
3. Implemente até todos os testes passarem
4. Verifique: \`mvn verify\`

## Como submeter

\`\`\`bash
git checkout -b ${KATA_LOWER}-{heroId}-solution
git commit -am "${KATA_LOWER}: implement ${CLASS_NAME}"
git push origin ${KATA_LOWER}-{heroId}-solution
# Abra PR com body: heroId: {heroId}
\`\`\`
EOF

# ── Commit ──────────────────────────────────────────────────────────────────
git config user.name  "fidelisfelipe"
git config user.email "atosfiel@gmail.com"
git add .
git commit -m "feat(${KATA_LOWER}): template ${CLASS_NAME}"

echo "✅ Branch ${BRANCH} created locally"

# Return to original branch
git checkout "${ORIGINAL_BRANCH}" 2>/dev/null || git checkout -b "${ORIGINAL_BRANCH}" 2>/dev/null || true
