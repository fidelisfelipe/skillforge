/**
 * Injects katas from catalog-additions.yml into the correct theme in catalog.yml.
 * Usage: node inject-katas.cjs <themeId>
 * Example: node inject-katas.cjs ch01-building-blocks
 */
const fs = require('fs');
const path = require('path');

const themeId = process.argv[2];
if (!themeId) { console.error('Usage: node inject-katas.cjs <themeId>'); process.exit(1); }

const repoRoot = path.resolve(__dirname, '..', '..');
const additionsPath = path.join(__dirname, 'output', 'catalog-additions.yml');
const catalogPath   = path.join(repoRoot, 'quests', 'dojo', 'java-21-certified', 'catalog.yml');

const additions = fs.readFileSync(additionsPath, 'utf-8');
let catalog     = fs.readFileSync(catalogPath, 'utf-8');

// Extract the katas block from additions (everything from "    katas:\n" onward)
const katasMatch = additions.match(/    katas:\n([\s\S]+)/);
if (!katasMatch) { console.error('No katas block found in catalog-additions.yml'); process.exit(1); }
const katasBlock = '    katas:\n' + katasMatch[1].trimEnd();

// Replace "katas: []" inside the target theme block
// Strategy: find the line "    katas: []" that appears after "  - id: <themeId>"
const themeStart = catalog.indexOf(`  - id: ${themeId}\n`);
if (themeStart === -1) { console.error(`Theme "${themeId}" not found in catalog.yml`); process.exit(1); }

const emptyKatas = '    katas: []';
const posInTheme = catalog.indexOf(emptyKatas, themeStart);
if (posInTheme === -1) { console.error(`"katas: []" not found in theme "${themeId}"`); process.exit(1); }

const updated = catalog.slice(0, posInTheme) + katasBlock + catalog.slice(posInTheme + emptyKatas.length);
fs.writeFileSync(catalogPath, updated);

const count = (katasBlock.match(/- id: KATA-/g) || []).length;
console.log(`✅ Inserted ${count} katas into theme "${themeId}"`);
