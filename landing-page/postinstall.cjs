const fs = require('fs');

const path1 = __dirname + '/node_modules/.pnpm/gray-matter@4.0.3/node_modules/gray-matter/lib/engines.js';
const path2 = __dirname + '/node_modules/gray-matter/lib/engines.js';

if (fs.existsSync(path1)) {
    let content = fs.readFileSync(path1, 'utf8');
    content = content.replace('yaml.safeLoad.bind(yaml)', '(yaml.load || yaml.safeLoad).bind(yaml)');
    content = content.replace('yaml.safeDump.bind(yaml)', '(yaml.dump || yaml.safeDump).bind(yaml)');
    fs.writeFileSync(path1, content);
}

if (fs.existsSync(path2)) {
    let content = fs.readFileSync(path2, 'utf8');
    content = content.replace('yaml.safeLoad.bind(yaml)', '(yaml.load || yaml.safeLoad).bind(yaml)');
    content = content.replace('yaml.safeDump.bind(yaml)', '(yaml.dump || yaml.safeDump).bind(yaml)');
    fs.writeFileSync(path2, content);
}
