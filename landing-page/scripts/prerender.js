/**
 * Post-build prerender script.
 * Creates static HTML shells for each SPA route with correct meta tags.
 * Googlebot sees proper <title>, <meta>, <canonical> per route without JS.
 */
import { readFileSync, writeFileSync, mkdirSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const distDir = resolve(__dirname, '../dist');
const indexHtml = readFileSync(resolve(distDir, 'index.html'), 'utf-8');

const routes = [
  {
    path: '/privacy',
    title: 'Privacy Policy - Contacts Cleaner',
    description: 'Privacy Policy for Contacts Cleaner. Learn how we protect your data — all contact processing happens locally on your device.',
  },
  {
    path: '/terms',
    title: 'Terms of Service - Contacts Cleaner',
    description: 'Terms of Service for Contacts Cleaner. Review the terms and conditions for using our AI-powered contact management app.',
  },
  {
    path: '/support',
    title: 'Support - Contacts Cleaner',
    description: 'Get help with Contacts Cleaner. Find answers to common questions, report issues, or contact our support team.',
  },
];

for (const route of routes) {
  let html = indexHtml;

  // Replace title
  html = html.replace(
    /<title>.*?<\/title>/,
    `<title>${route.title}</title>`
  );

  // Replace meta description
  html = html.replace(
    /<meta name="description" content=".*?" \/>/,
    `<meta name="description" content="${route.description}" />`
  );

  // Replace canonical
  html = html.replace(
    /<link rel="canonical" href=".*?" \/>/,
    `<link rel="canonical" href="https://contactscleaner.tech${route.path}" />`
  );

  // Replace OG URL
  html = html.replace(
    /<meta property="og:url" content=".*?" \/>/,
    `<meta property="og:url" content="https://contactscleaner.tech${route.path}" />`
  );

  // Replace OG title
  html = html.replace(
    /<meta property="og:title" content=".*?" \/>/,
    `<meta property="og:title" content="${route.title}" />`
  );

  // Write to route directory
  const routeDir = resolve(distDir, route.path.slice(1));
  mkdirSync(routeDir, { recursive: true });
  writeFileSync(resolve(routeDir, 'index.html'), html);
  console.log(`  Pre-rendered: ${route.path}/index.html`);
}

console.log('Pre-rendering complete.');
