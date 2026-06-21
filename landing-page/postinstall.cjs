const fs = require('fs');
const file = 'node_modules/.pnpm/gray-matter@4.0.3/node_modules/gray-matter/lib/engines.js';
if (fs.existsSync(file)) {
  let content = fs.readFileSync(file, 'utf8');
  content = content.replace('yaml.safeLoad.bind(yaml)', '(yaml.load || yaml.safeLoad).bind(yaml)');
  content = content.replace('yaml.safeDump.bind(yaml)', '(yaml.dump || yaml.safeDump).bind(yaml)');
  fs.writeFileSync(file, content);
}
