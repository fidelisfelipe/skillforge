#!/bin/bash
# create-kata.sh — Creates a kata template branch in skillforge-katas
#
# Usage:
#   ./scripts/create-kata.sh <KATA-ID> <ClassName>
#
# Examples:
#   ./scripts/create-kata.sh KATA-004A PaymentProcessor
#   ./scripts/create-kata.sh KATA-007A SalesReporter
#
# Requirements:
#   - GITHUB_TOKEN env var (or set in .env)
#   - git and mvn on PATH

set -e

KATA_ID=${1:?Usage: create-kata.sh <KATA-ID> <ClassName> [test-file-path]}
CLASS_NAME=${2:?Usage: create-kata.sh <KATA-ID> <ClassName> [test-file-path]}
TEST_FILE=${3:-}  # optional: path to a pre-generated test file

# Load .env if present
[ -f "$(dirname "$0")/../.env" ] && set -a && source "$(dirname "$0")/../.env" && set +a

GITHUB_TOKEN=${GITHUB_TOKEN:?GITHUB_TOKEN not set}

# Derive names
KATA_LOWER=$(echo "${KATA_ID}" | tr '[:upper:]' '[:lower:]')  # kata-004a
BRANCH="${KATA_LOWER}-template"                                 # kata-004a-template
PACKAGE="com.skillforge.kata"
REPO_URL="https://${GITHUB_TOKEN}@github.com/fidelisfelipe/skillforge-katas.git"
TEMP_DIR=$(mktemp -d)

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Creating kata: ${KATA_ID} → branch: ${BRANCH}"
echo "  Class: ${CLASS_NAME}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# ── 1. Clone repo (orphan branch) ──────────────────────────────────────────
echo "📥 Cloning skillforge-katas..."
git clone --depth 1 "${REPO_URL}" "${TEMP_DIR}/repo" 2>/dev/null || {
  # Repo empty or first branch — init fresh
  git init "${TEMP_DIR}/repo"
}
cd "${TEMP_DIR}/repo"
git checkout --orphan "${BRANCH}"
git rm -rf . 2>/dev/null || true

# ── 2. Maven project structure ─────────────────────────────────────────────
MAIN_PKG_DIR="src/main/java/com/skillforge/kata"
TEST_PKG_DIR="src/test/java/com/skillforge/kata"
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

# Skeleton implementation class
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
  echo "📋 Using generated test file: ${TEST_FILE}"
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
        // Arrange
        // TODO: setup

        // Act
        // TODO: call subject method

        // Assert
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

# ── 3. Commit e push ───────────────────────────────────────────────────────
git config user.name "fidelisfelipe"
git config user.email "atosfiel@gmail.com"
git add .
git commit -m "feat(${KATA_LOWER}): template ${CLASS_NAME}"

echo "📤 Pushing branch ${BRANCH}..."
git remote set-url origin "${REPO_URL}" 2>/dev/null || git remote add origin "${REPO_URL}"
git push origin "${BRANCH}" --force

echo ""
echo "✅ Branch created: https://github.com/fidelisfelipe/skillforge-katas/tree/${BRANCH}"
echo ""
echo "⚠️  Next: add tests to ${CLASS_NAME}Test.java and push again."
echo "    cd ${TEMP_DIR}/repo"

# ── 4. Cleanup ─────────────────────────────────────────────────────────────
# Temp dir kept so developer can edit tests if needed
echo "📁 Temp dir: ${TEMP_DIR}/repo"
