const fs = require('fs');
const packageJsonPath = './package.json';
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));

if (!packageJson.pnpm) {
    packageJson.pnpm = {};
}
if (!packageJson.pnpm.overrides) {
    packageJson.pnpm.overrides = {};
}
packageJson.pnpm.overrides['js-yaml'] = '^4.2.0';

fs.writeFileSync(packageJsonPath, JSON.stringify(packageJson, null, 2) + '\n');
