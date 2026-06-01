#!/usr/bin/env node
/**
 * generate-from-catalog.mjs
 * Reads existing catalog.yml and generates test code for all katas that don't yet have branches.
 *
 * Usage:
 *   node generate-from-catalog.mjs [--skip KATA-001A,KATA-002A] [--theme virtual-threads]
 *
 * Skips katas whose templateBranch already exists in skillforge-katas.
 * Batches by theme — one Claude API call per theme.
 */

import Anthropic from '@anthropic-ai/sdk';
import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';
import { parseArgs } from 'util';

const __dirname = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT  = resolve(__dirname, '../..');

const { values } = parseArgs({
  args: process.argv.slice(2),
  options: {
    skip:  { type: 'string', default: 'KATA-001A' },
    theme: { type: 'string', default: '' },
  }
});

const skipIds   = values.skip.split(',').map(s => s.trim()).filter(Boolean);
const onlyTheme = values.theme;

// ── Load catalog ─────────────────────────────────────────────────────────────

const catalogPath = resolve(REPO_ROOT, 'quests/dojo/java-21-certified/catalog.yml');
const catalogRaw  = readFileSync(catalogPath, 'utf-8');

// Simple YAML parser for our known structure
function parseCatalog(yaml) {
  const themes = [];
  let currentTheme = null;
  let currentKata  = null;
  let inSpec       = false;
  let specIndent   = 0;

  for (const raw of yaml.split('\n')) {
    const line = raw;
    const trimmed = line.trimStart();
    const indent  = line.length - trimmed.length;

    if (inSpec) {
      if (indent > specIndent || trimmed === '') {
        currentKata.spec += (trimmed === '' ? '' : line.slice(specIndent)) + '\n';
        continue;
      } else {
        inSpec = false;
        currentKata.spec = currentKata.spec.trim();
      }
    }

    if (trimmed.startsWith('- id:') && indent === 2) {
      currentTheme = { id: trimmed.slice(5).trim(), katas: [] };
      themes.push(currentTheme);
    } else if (trimmed.startsWith('name:') && currentTheme && indent === 4) {
      currentTheme.name = trimmed.slice(5).trim().replace(/^"|"$/g, '');
    } else if (trimmed.startsWith('description:') && currentTheme && indent === 4) {
      currentTheme.description = trimmed.slice(12).trim().replace(/^"|"$/g, '');
    } else if (trimmed.startsWith('difficulty:') && currentTheme && indent === 4) {
      currentTheme.difficulty = trimmed.slice(11).trim();
    } else if (trimmed.startsWith('certReference:') && currentTheme && indent === 4) {
      currentTheme.certReference = trimmed.slice(14).trim().replace(/^"|"$/g, '');
    } else if (trimmed.startsWith('- id:') && indent === 6) {
      currentKata = { id: trimmed.slice(5).trim(), spec: '' };
      currentTheme.katas.push(currentKata);
    } else if (trimmed.startsWith('title:') && currentKata && indent === 8) {
      currentKata.title = trimmed.slice(6).trim().replace(/^"|"$/g, '');
    } else if (trimmed.startsWith('difficulty:') && currentKata && indent === 8) {
      currentKata.difficulty = trimmed.slice(11).trim();
    } else if (trimmed.startsWith('xpReward:') && currentKata && indent === 8) {
      currentKata.xpReward = parseInt(trimmed.slice(9).trim());
    } else if (trimmed.startsWith('templateBranch:') && currentKata && indent === 8) {
      currentKata.templateBranch = trimmed.slice(15).trim();
    } else if (trimmed.startsWith('className:') && currentKata && indent === 8) {
      currentKata.className = trimmed.slice(10).trim();
    } else if (trimmed.startsWith('spec:') && currentKata && indent === 8) {
      inSpec = true;
      specIndent = indent + 2;
      currentKata.spec = '';
    }
  }
  if (inSpec && currentKata) currentKata.spec = currentKata.spec.trim();
  return themes;
}

const themes = parseCatalog(catalogRaw);

// Filter themes if --theme specified
const targetThemes = onlyTheme
  ? themes.filter(t => t.id === onlyTheme)
  : themes;

if (targetThemes.length === 0) {
  console.error(`❌ No themes found${onlyTheme ? ` matching "${onlyTheme}"` : ''}`);
  process.exit(1);
}

// Filter out skipped katas
const toGenerate = targetThemes.map(theme => ({
  ...theme,
  katas: theme.katas.filter(k => !skipIds.includes(k.id))
})).filter(t => t.katas.length > 0);

const total = toGenerate.reduce((s, t) => s + t.katas.length, 0);
console.log(`\n📚 Catalog: ${themes.length} themes | Generating tests for ${total} katas\n`);
if (skipIds.length) console.log(`   Skipping: ${skipIds.join(', ')}\n`);

// ── Setup output ──────────────────────────────────────────────────────────────

const outputDir  = resolve(__dirname, 'output');
const katasDir   = resolve(outputDir, 'katas');
mkdirSync(katasDir, { recursive: true });

const client = new Anthropic();
const allKatas = [];

// ── Generate per theme ────────────────────────────────────────────────────────

const systemPrompt = `You are a Java 21 OCP certification kata test generator for the SkillForge platform.
Given kata specs, generate complete JUnit 5 test files.

Rules:
- Tests use JUnit Jupiter only (junit-jupiter 5.10.0)
- 3–5 @Test methods per class with real assertions — never fail() as placeholder
- @DisplayName on class and each @Test in Brazilian Portuguese (pt-BR)
- Test method names in English snake_case
- @BeforeEach setUp() creating a new instance of the subject class
- The implementation class is empty — students add methods to pass the tests
- package is always: com.skillforge.kata

Return ONLY valid JSON, no markdown. Schema:
{
  "testFiles": {
    "KATA-XXX": "full Java source as string"
  }
}`;

for (const theme of toGenerate) {
  console.log(`\n🎯 Theme: ${theme.name} (${theme.katas.length} katas)`);

  const katasDesc = theme.katas.map(k => `
  - ID: ${k.id}
    className: ${k.className}
    title: ${k.title}
    difficulty: ${k.difficulty}
    spec: |
      ${k.spec.split('\n').join('\n      ')}`).join('\n');

  const userPrompt = `Generate test files for these katas from the theme "${theme.name}":
${katasDesc}

For each kata, return a complete test class where:
- The class name is exactly: <className>Test
- It tests the public methods that a student would implement to satisfy the spec
- Tests are concrete and runnable (all assertions, no TODOs)`;

  try {
    const message = await client.messages.create({
      model: 'claude-opus-4-7',
      max_tokens: 8192,
      messages: [{ role: 'user', content: userPrompt }],
      system: systemPrompt,
    });

    const raw = message.content[0].text.trim();
    let data;
    try {
      data = JSON.parse(raw);
    } catch {
      const match = raw.match(/\{[\s\S]*\}/);
      data = match ? JSON.parse(match[0]) : null;
    }

    if (!data?.testFiles) {
      console.error(`  ⚠️  No testFiles in response for theme ${theme.id}`);
      continue;
    }

    for (const kata of theme.katas) {
      const testCode = data.testFiles[kata.id];
      if (testCode) {
        const fileName = `${kata.id}_${kata.className}Test.java`;
        writeFileSync(resolve(katasDir, fileName), testCode);
        console.log(`  ✅ ${kata.id} — ${kata.className}Test.java`);
      } else {
        console.warn(`  ⚠️  ${kata.id} — no test file in response`);
      }
      allKatas.push({
        id:             kata.id,
        title:          kata.title,
        className:      kata.className,
        difficulty:     kata.difficulty,
        xpReward:       kata.xpReward,
        templateBranch: kata.templateBranch,
        hasTests:       !!testCode,
      });
    }
  } catch (err) {
    console.error(`  ❌ API error for theme ${theme.id}: ${err.message}`);
    for (const kata of theme.katas) {
      allKatas.push({ id: kata.id, title: kata.title, className: kata.className,
        difficulty: kata.difficulty, xpReward: kata.xpReward,
        templateBranch: kata.templateBranch, hasTests: false });
    }
  }
}

// ── Write summary ─────────────────────────────────────────────────────────────

writeFileSync(
  resolve(outputDir, 'summary.json'),
  JSON.stringify({ theme: { id: 'catalog', name: 'Full Catalog' }, katas: allKatas }, null, 2)
);

const ok   = allKatas.filter(k => k.hasTests).length;
const fail = allKatas.filter(k => !k.hasTests).length;

console.log(`\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`);
console.log(`  Tests generated: ${ok} ✅  |  Missing: ${fail} ⚠️`);
console.log(`  summary.json updated — run create-all-katas.sh --from-generator --local`);
console.log(`━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n`);
