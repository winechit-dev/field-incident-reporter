const fs = require('node:fs');
const path = require('node:path');
const { createApp } = require('./app');

const root = path.join(__dirname, '..');
const dbFile = process.env.DB_FILE || path.join(root, 'db.json');
if (!fs.existsSync(dbFile)) fs.copyFileSync(path.join(root, 'seed.json'), dbFile);

const port = Number(process.env.PORT || 3000);
createApp({ dbFile, uploadsDir: path.join(root, 'uploads') }).listen(port, '0.0.0.0', () => {
  console.log(`Field Incident Reporter API on http://localhost:${port}`);
  console.log('Android emulator: http://10.0.2.2:' + port);
});
