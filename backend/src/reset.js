const fs = require('node:fs');
const path = require('node:path');

const root = path.join(__dirname, '..');
fs.copyFileSync(path.join(root, 'seed.json'), path.join(root, 'db.json'));
const uploads = path.join(root, 'uploads');
for (const f of fs.readdirSync(uploads)) {
  if (f !== '.gitkeep') fs.rmSync(path.join(uploads, f));
}
console.log('db.json reset from seed.json, uploads cleared');
