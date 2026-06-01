#!/usr/bin/env node
/**
 * Kata Generator — uses Claude API to generate kata specs, test code and catalog entries.
 *
 * Usage:
 *   node generate.mjs --theme "Exception Handling" --difficulty intermediate --count 3
 *   node generate.mjs --theme "Reactive Streams" --difficulty advanced --count 2 --cert "Oracle Java 21 - Module 12"
 */

import Anthropic from '@anthropic-ai/sdk';
import { readFileSync, writeFileSync, mkdirSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';
import { parseArgs } from 'util';

const __dirname = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(__dirname, '../..');

const { values } = parseArgs({
  args: process.argv.slice(2),
  options: {
    theme:      { type: 'string' },
    difficulty: { type: 'string', default: 'intermediate' },
    count:      { type: 'string', default: '3' },
    cert:       { type: 'string', default: '' },
  }
});

if (!values.theme) {
  console.error('Usage: node generate.mjs --theme "<name>" [--difficulty intermediate] [--count 3] [--cert "<ref>"]');
  process.exit(1);
}

const { theme, difficulty, cert } = values;
const count = Math.min(Math.max(parseInt(values.count) || 3, 1), 5);

// ── Find next KATA number ────────────────────────────────────────────────────

const catalogPath = resolve(REPO_ROOT, 'quests/dojo/java-21-certified/catalog.yml');
const catalogContent = readFileSync(catalogPath, 'utf-8');

const existingNums = [...catalogContent.matchAll(/^\s+- id:\s+KATA-(\d+)/gm)]
  .map(m => parseInt(m[1]));
const maxNum   = existingNums.length > 0 ? Math.max(...existingNums) : 0;
const nextNum  = maxNum + 1;
const numStr   = nextNum.toString().padStart(3, '0');
const letters  = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';

const newKataIds = Array.from({ length: count }, (_, i) => `KATA-${numStr}${letters[i]}`);

const themeId = theme.toLowerCase()
  .normalize('NFD').replace(/[̀-ͯ]/g, '')
  .replace(/[^a-z0-9\s]/g, '')
  .replace(/\s+/g, '-')
  .replace(/-+/g, '-')
  .trim();

// ── Call Claude ──────────────────────────────────────────────────────────────

const client = new Anthropic();

const systemPrompt = `You are a Java 21 OCP certification kata generator for the SkillForge platform.
Your job: produce kata exercises that teach Java 21 features through Test-Driven Development.

Rules for generated katas:
- Practical and focused: solvable in 30–90 minutes
- Specs in Brazilian Portuguese (pt-BR)
- Test @DisplayName in pt-BR; test method names in English snake_case
- Tests use JUnit Jupiter only (junit-jupiter 5.10.0, already on classpath)
- Each test class has 3–5 @Test methods with real assertions — never fail() as placeholder
- The implementation class is intentionally empty; students add methods to make tests pass
- XP range: beginner=60-80, intermediate=80-110, advanced=110-130

Return ONLY valid JSON. No markdown fences. No extra text. Use this exact schema:
{
  "theme": {
    "id": "string",
    "name": "string",
    "description": "string (English, one line)",
    "difficulty": "beginner|intermediate|advanced",
    "certReference": "string",
    "katas": [
      {
        "id": "KATA-XXXL",
        "title": "string",
        "difficulty": "beginner|intermediate|advanced",
        "xpReward": 90,
        "templateBranch": "kata-xxxl-template",
        "className": "PascalCase",
        "spec": "multi-line string in pt-BR"
      }
    ]
  },
  "testFiles": {
    "KATA-XXXL": "full Java source as string with real tests"
  }
}`;

const userPrompt = `Generate ${count} kata(s) for the theme "${theme}" at difficulty "${difficulty}".
${cert ? `Certification reference: "${cert}"` : ''}

The kata IDs MUST be exactly: ${newKataIds.join(', ')}
The theme ID MUST be exactly: ${themeId}
Template branch names: ${newKataIds.map(id => id.replace('KATA-', 'kata-').toLowerCase() + '-template').join(', ')}

Each Java test file must follow this structure:
\`\`\`java
package com.skillforge.kata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("<kata title in pt-BR>")
class <ClassName>Test {

    private <ClassName> subject;

    @BeforeEach
    void setUp() {
        subject = new <ClassName>();
    }

    @Test
    @DisplayName("<acceptance criterion in pt-BR>")
    void <snake_case_test_name>() {
        // real assertions using JUnit 5
    }
    // 2–4 more @Test methods
}
\`\`\`

Style reference from existing catalog:
${catalogContent.slice(0, 2000)}`;

console.log(`\n🤖 Calling Claude API — generating ${count} kata(s) for theme "${theme}"...\n`);

const message = await client.messages.create({
  model: 'claude-opus-4-7',
  max_tokens: 8192,
  messages: [{ role: 'user', content: userPrompt }],
  system: systemPrompt,
});

const raw = message.content[0].text.trim();

// ── Parse JSON response ──────────────────────────────────────────────────────

let data;
try {
  data = JSON.parse(raw);
} catch {
  const match = raw.match(/\{[\s\S]*\}/);
  if (!match) {
    console.error('❌ Could not parse JSON from Claude response:');
    console.error(raw.slice(0, 500));
    process.exit(1);
  }
  data = JSON.parse(match[0]);
}

// ── Write output files ───────────────────────────────────────────────────────

const outputDir  = resolve(__dirname, 'output');
const katasDir   = resolve(outputDir, 'katas');
mkdirSync(katasDir, { recursive: true });

// Build catalog YAML block
const td = data.theme;
let catalogYaml = `\n  - id: ${td.id}\n`;
catalogYaml += `    name: "${td.name}"\n`;
catalogYaml += `    description: "${td.description}"\n`;
catalogYaml += `    difficulty: ${td.difficulty}\n`;
catalogYaml += `    certReference: "${td.certReference || cert}"\n`;
catalogYaml += `    katas:\n`;

for (const k of td.katas) {
  catalogYaml += `      - id: ${k.id}\n`;
  catalogYaml += `        title: "${k.title}"\n`;
  catalogYaml += `        difficulty: ${k.difficulty}\n`;
  catalogYaml += `        xpReward: ${k.xpReward}\n`;
  catalogYaml += `        templateBranch: ${k.templateBranch}\n`;
  catalogYaml += `        className: ${k.className}\n`;
  catalogYaml += `        spec: |\n`;
  for (const line of k.spec.trim().split('\n')) {
    catalogYaml += `          ${line}\n`;
  }
}

writeFileSync(resolve(outputDir, 'catalog-additions.yml'), catalogYaml);

// Write test files and summary
const summary = [];
for (const k of td.katas) {
  const testCode = data.testFiles?.[k.id];
  if (testCode) {
    writeFileSync(resolve(katasDir, `${k.id}_${k.className}Test.java`), testCode);
  }
  summary.push({
    id:             k.id,
    title:          k.title,
    className:      k.className,
    difficulty:     k.difficulty,
    xpReward:       k.xpReward,
    templateBranch: k.templateBranch,
    hasTests:       !!testCode,
  });
}

writeFileSync(
  resolve(outputDir, 'summary.json'),
  JSON.stringify({ theme: td, katas: summary }, null, 2)
);

// ── Print summary ────────────────────────────────────────────────────────────

console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
console.log(`  Theme: ${td.name} (${td.id})`);
console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
for (const k of summary) {
  const testMark = k.hasTests ? '✅ tests' : '⚠️  no tests';
  console.log(`  ${k.id}  ${k.difficulty.padEnd(14)} ${k.xpReward}xp  ${k.className}  ${testMark}`);
}
console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
console.log(`\n📁 Output: scripts/kata-generator/output/`);
console.log(`   catalog-additions.yml  — append to quests catalog`);
console.log(`   katas/                 — test Java files`);
console.log(`   summary.json           — machine-readable metadata`);
