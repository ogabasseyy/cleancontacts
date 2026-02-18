/**
 * Build-time blog processor.
 * Reads markdown posts from blog/, generates:
 * - public/blog-manifest.json (post metadata for client)
 * - public/blog/*.md (raw markdown for client-side rendering)
 * - public/rss.xml (RSS 2.0 feed)
 * - Updates public/sitemap.xml with blog URLs
 */
import { readFileSync, writeFileSync, mkdirSync, readdirSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';
import matter from 'gray-matter';

const __dirname = dirname(fileURLToPath(import.meta.url));
const blogDir = resolve(__dirname, '../blog');
const publicDir = resolve(__dirname, '../public');
const publicBlogDir = resolve(publicDir, 'blog');
const SITE_URL = 'https://contactscleaner.tech';

// Ensure output directories exist
mkdirSync(publicBlogDir, { recursive: true });

// Read all markdown files (gracefully handle missing blog/ directory)
let files;
try {
  files = readdirSync(blogDir).filter(f => f.endsWith('.md'));
} catch (err) {
  if (err.code === 'ENOENT') {
    files = [];
  } else {
    throw err;
  }
}

if (files.length === 0) {
  console.log('No blog posts found. Skipping blog build.');
  process.exit(0);
}

/**
 * Strip markdown syntax for plain text excerpt.
 */
function stripMarkdown(md) {
  return md
    .replace(/^```[\s\S]*?```$/gm, '')   // fenced code blocks (backticks)
    .replace(/^~~~[\s\S]*?~~~$/gm, '')    // fenced code blocks (tildes)
    .replace(/^\s*\|.*\|\s*$/gm, '')      // table rows
    .replace(/^\s*\|?\s*-{3,}.*$/gm, '')  // table separator lines
    .replace(/^#{1,6}\s+/gm, '')          // headings
    .replace(/\*\*(.+?)\*\*/g, '$1')      // bold
    .replace(/\*(.+?)\*/g, '$1')          // italic
    .replace(/\[(.+?)\]\(.+?\)/g, '$1')   // links
    .replace(/!\[.*?\]\(.+?\)/g, '')      // images
    .replace(/`{1,3}[^`]*`{1,3}/g, '')   // inline code
    .replace(/>\s+/g, '')                 // blockquotes
    .replace(/[-*+]\s+/g, '')            // list items
    .replace(/\d+\.\s+/g, '')            // ordered list items
    .replace(/\n{2,}/g, ' ')             // multiple newlines
    .replace(/\n/g, ' ')                 // single newlines
    .trim();
}

/**
 * Normalize a Date object or string to YYYY-MM-DD.
 * gray-matter parses unquoted YAML dates into JS Date objects.
 */
function normalizeDate(value) {
  if (value instanceof Date) return value.toISOString().slice(0, 10);
  return String(value);
}

/**
 * Validate slug contains only safe characters (alphanumerics, hyphens, underscores).
 */
function isValidSlug(slug) {
  return /^[a-z0-9][a-z0-9-_]*$/.test(slug);
}

/**
 * Calculate reading time (words / 200 wpm).
 */
function readingTime(content) {
  const words = stripMarkdown(content).split(/\s+/).filter(Boolean).length;
  return Math.max(1, Math.ceil(words / 200));
}

/**
 * Generate auto-excerpt from content (first 160 chars).
 */
function generateExcerpt(content) {
  const plain = stripMarkdown(content);
  if (plain.length <= 160) return plain;
  return plain.slice(0, 157) + '...';
}

// Process all posts
const posts = files.map(file => {
  const raw = readFileSync(resolve(blogDir, file), 'utf-8');
  const { data, content } = matter(raw);

  // Validate required frontmatter
  if (!data.title || !data.slug || !data.date) {
    console.warn(`  Warning: ${file} missing required frontmatter (title, slug, date). Skipping.`);
    return null;
  }

  // Validate slug to prevent path traversal
  if (!isValidSlug(data.slug)) {
    console.warn(`  Warning: ${file} has invalid slug "${data.slug}". Skipping.`);
    return null;
  }

  const date = normalizeDate(data.date);
  const lastModified = normalizeDate(data.lastModified || data.date);

  // Copy raw markdown to public/blog/
  writeFileSync(resolve(publicBlogDir, `${data.slug}.md`), content);

  return {
    title: data.title,
    slug: data.slug,
    date,
    lastModified,
    description: data.description || generateExcerpt(content),
    excerpt: generateExcerpt(content),
    readingTime: readingTime(content),
    category: data.category || 'General',
    tags: data.tags || [],
    image: data.image || '/og-image.png',
  };
}).filter(Boolean);

// Sort by date (newest first)
posts.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

// Write blog manifest
writeFileSync(
  resolve(publicDir, 'blog-manifest.json'),
  JSON.stringify(posts, null, 2)
);
console.log(`  Blog manifest: ${posts.length} posts`);

// Generate RSS 2.0 feed
const rssItems = posts.map(post => `    <item>
      <title><![CDATA[${post.title}]]></title>
      <link>${SITE_URL}/blog/${post.slug}</link>
      <guid isPermaLink="true">${SITE_URL}/blog/${post.slug}</guid>
      <description><![CDATA[${post.description}]]></description>
      <pubDate>${new Date(post.date + 'T00:00:00Z').toUTCString()}</pubDate>
      <category><![CDATA[${post.category}]]></category>
    </item>`).join('\n');

const rss = `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
  <channel>
    <title>Contacts Cleaner Blog</title>
    <link>${SITE_URL}/blog</link>
    <description>Tips, guides, and news about contact management for iPhone and Android.</description>
    <language>en-us</language>
    <lastBuildDate>${new Date().toUTCString()}</lastBuildDate>
    <atom:link href="${SITE_URL}/rss.xml" rel="self" type="application/rss+xml" />
${rssItems}
  </channel>
</rss>`;

writeFileSync(resolve(publicDir, 'rss.xml'), rss);
console.log('  RSS feed: rss.xml');

// Update sitemap.xml with blog URLs
const sitemapPath = resolve(publicDir, 'sitemap.xml');
let existingSitemap;
try {
  existingSitemap = readFileSync(sitemapPath, 'utf-8');
} catch {
  existingSitemap = `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n</urlset>`;
}

// Remove any previously generated blog entries (between markers)
let sitemapBase = existingSitemap.replace(
  /\s*<!-- BLOG_START -->[\s\S]*?<!-- BLOG_END -->/,
  ''
);

// Use the newest post date as lastmod for the blog index
const blogIndexLastmod = posts.length > 0 ? posts[0].lastModified : new Date().toISOString().slice(0, 10);

// Insert blog URLs before closing </urlset>
const blogUrls = [
  `  <!-- BLOG_START -->`,
  `  <url>`,
  `    <loc>${SITE_URL}/blog</loc>`,
  `    <lastmod>${blogIndexLastmod}</lastmod>`,
  `    <changefreq>weekly</changefreq>`,
  `    <priority>0.7</priority>`,
  `  </url>`,
  ...posts.map(post => [
    `  <url>`,
    `    <loc>${SITE_URL}/blog/${post.slug}</loc>`,
    `    <lastmod>${post.lastModified}</lastmod>`,
    `    <changefreq>monthly</changefreq>`,
    `    <priority>0.6</priority>`,
    `  </url>`,
  ].join('\n')),
  `  <!-- BLOG_END -->`,
].join('\n');

const updatedSitemap = sitemapBase.replace(
  '</urlset>',
  `${blogUrls}\n</urlset>`
);

writeFileSync(sitemapPath, updatedSitemap);
console.log('  Sitemap updated with blog URLs');

console.log('Blog build complete.');
