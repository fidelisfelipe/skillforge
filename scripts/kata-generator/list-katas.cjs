// Reads summary.json and prints: KATA-ID|ClassName, one per line
// Usage: node list-katas.js <path-to-summary.json>
const { readFileSync } = require('fs');
const file = process.argv[2];
if (!file) { process.stderr.write('Usage: node list-katas.js <summary.json>\n'); process.exit(1); }
const { katas } = JSON.parse(readFileSync(file, 'utf-8'));
katas.forEach(k => process.stdout.write(k.id + '|' + k.className + '\n'));
