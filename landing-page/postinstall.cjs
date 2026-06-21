const fs = require('fs');
const path = require('path');

const allowedDirs = [
  path.join(__dirname, 'node_modules', '.pnpm', 'gray-matter@4.0.3', 'node_modules', 'gray-matter', 'lib'),
  path.join(__dirname, 'node_modules', 'gray-matter', 'lib')
];

const files = [
  path.join(allowedDirs[0], 'engines.js'),
  path.join(allowedDirs[1], 'engines.js')
];

files.forEach(file => {
  // Check if file is within expected directory to prevent path traversal
  const isAllowed = allowedDirs.some(dir => file.startsWith(dir));
  if (isAllowed && fs.existsSync(file)) {
    let content = fs.readFileSync(file, 'utf8');
    content = content.replace('yaml.safeLoad.bind(yaml)', '(yaml.load || yaml.safeLoad).bind(yaml)');
    content = content.replace('yaml.safeDump.bind(yaml)', '(yaml.dump || yaml.safeDump).bind(yaml)');
    fs.writeFileSync(file, content);
  }
});
