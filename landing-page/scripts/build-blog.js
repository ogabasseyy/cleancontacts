import { readFileSync, writeFileSync, mkdirSync, readdirSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';
import * as yaml from 'js-yaml';
import { unified } from 'unified';
import remarkParse from 'remark-parse';
import remarkGfm from 'remark-gfm';
import remarkRehype from 'remark-rehype';
import rehypeStringify from 'rehype-stringify';
import rehypeSlug from 'rehype-slug';
import rehypeAutolinkHeadings from 'rehype-autolink-headings';
import rehypePrettyCode from 'rehype-pretty-code';

function matter(raw) {
  const match = /^(?:---)\r?\n([\s\S]*?)\r?\n(?:---)\r?\n([\s\S]*)$/.exec(raw);
  if (match) {
    return { data: yaml.load(match[1]), content: match[2] };
  }
  return { data: {}, content: raw };
}

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
  const str = String(value).slice(0, 10);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(str)) {
    throw new Error(`Invalid date value: "${String(value)}"`);
  }
  return str;
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

/**
 * Extract h2/h3 headings from markdown for TOC generation.
 */
function extractHeadings(markdown) {
  const headings = [];
  const regex = /^(#{2,3})\s+(.+)$/gm;
  let match;
  while ((match = regex.exec(markdown)) !== null) {
    const text = match[2]
      .replace(/\*\*(.+?)\*\*/g, '$1')  // strip bold
      .replace(/\*(.+?)\*/g, '$1')      // strip italic
      .replace(/`(.+?)`/g, '$1')        // strip inline code
      .replace(/\[(.+?)\]\(.+?\)/g, '$1') // strip links
      .trim();
    const id = text
      .toLowerCase()
      .replace(/[^\w\s-]/g, '')
      .replace(/\s+/g, '-');
    headings.push({
      level: match[1].length,
      text,
      id,
    });
  }
  return headings;
}

/**
 * Warn about images without alt text in markdown.
 */
function warnMissingAltText(content, filename) {
  const emptyAltRegex = /!\[\s*\]\(/g;
  let match;
  while ((match = emptyAltRegex.exec(content)) !== null) {
    const line = content.substring(0, match.index).split('\n').length;
    console.warn(`  Warning: Image without alt text in ${filename} (line ${line})`);
  }
}

/**
 * Build the unified remark→rehype pipeline for markdown→HTML conversion.
 */
function createMarkdownProcessor() {
  return unified()
    .use(remarkParse)
    .use(remarkGfm)
    .use(remarkRehype, { allowDangerousHtml: false })
    .use(rehypeSlug)
    .use(rehypeAutolinkHeadings, { behavior: 'wrap' })
    .use(rehypePrettyCode, {
      theme: 'github-dark',
      keepBackground: true,
    })
    .use(rehypeStringify);
}

const processor = createMarkdownProcessor();

// Process all posts
const allPosts = [];
for (const file of files) {
  const raw = readFileSync(resolve(blogDir, file), 'utf-8');
  const { data, content } = matter(raw);

  // Validate required frontmatter
  if (!data.title || !data.slug || !data.date) {
    console.warn(`  Warning: ${file} missing required frontmatter (title, slug, date). Skipping.`);
    continue;
  }

  // Validate slug to prevent path traversal
  if (!isValidSlug(data.slug)) {
    console.warn(`  Warning: ${file} has invalid slug "${data.slug}". Skipping.`);
    continue;
  }

  // Draft filtering
  if (data.status === 'draft') {
    console.log(`  Skipping draft: ${file}`);
    continue;
  }

  // Warn about images without alt text
  warnMissingAltText(content, file);

  const date = normalizeDate(data.date);
  const lastModified = normalizeDate(data.lastModified || data.date);
  const excerpt = generateExcerpt(content);
  const headings = extractHeadings(content);

  // Convert markdown to HTML at build time
  const htmlResult = await processor.process(content);
  const html = String(htmlResult);

  // Write pre-rendered HTML to public/blog/
  writeFileSync(resolve(publicBlogDir, `${data.slug}.html`), html);

  // Publish a frontmatter-free markdown mirror for LLM-friendly access.
  // Preserve the original markdown body as-is when it already starts with a title.
  const mirror = content.endsWith('\n') ? content : `${content}\n`;
  writeFileSync(resolve(publicBlogDir, `${data.slug}.md`), mirror);

  allPosts.push({
    title: data.title,
    slug: data.slug,
    date,
    lastModified,
    description: data.description || excerpt,
    excerpt,
    readingTime: readingTime(content),
    category: data.category || 'General',
    tags: data.tags || [],
    image: data.image || '/og-image.png',
    headings,
  });
}

// Duplicate slug detection
const slugCounts = {};
for (const post of allPosts) {
  slugCounts[post.slug] = (slugCounts[post.slug] || 0) + 1;
}
const duplicates = Object.entries(slugCounts).filter(([, count]) => count > 1);
if (duplicates.length > 0) {
  console.error(`  Error: Duplicate slugs found: ${duplicates.map(([s]) => s).join(', ')}`);
  process.exit(1);
}

// Sort by date (newest first)
allPosts.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

// Write blog manifest (includes headings for TOC)
writeFileSync(
  resolve(publicDir, 'blog-manifest.json'),
  JSON.stringify(allPosts, null, 2)
);
console.log(`  Blog manifest: ${allPosts.length} posts`);

/**
 * Escape content for use inside CDATA sections.
 * Replaces "]]>" with "]]]]><![CDATA[>".
 */
function escapeCdata(str) {
  return String(str ?? '').replace(/]]>/g, ']]]]><![CDATA[>');
}

// Generate RSS 2.0 feed
const rssItems = allPosts.map(post => `    <item>
      <title><![CDATA[${escapeCdata(post.title)}]]></title>
      <link>${SITE_URL}/blog/${post.slug}</link>
      <guid isPermaLink="true">${SITE_URL}/blog/${post.slug}</guid>
      <description><![CDATA[${escapeCdata(post.description)}]]></description>
      <pubDate>${new Date(post.date + 'T00:00:00Z').toUTCString()}</pubDate>
      <category><![CDATA[${escapeCdata(post.category)}]]></category>
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
} catch (err) {
  if (err.code === 'ENOENT') {
    existingSitemap = `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n</urlset>`;
  } else {
    throw err;
  }
}

// Remove any previously generated blog entries (between markers)
let sitemapBase = existingSitemap.replace(
  /\s*<!-- BLOG_START -->[\s\S]*?<!-- BLOG_END -->/,
  ''
);

// Use the newest post date as lastmod for the blog index
const blogIndexLastmod = allPosts.length > 0 ? allPosts[0].lastModified : new Date().toISOString().slice(0, 10);

// Insert blog URLs before closing </urlset>
const blogUrls = [
  `  <!-- BLOG_START -->`,
  `  <url>`,
  `    <loc>${SITE_URL}/blog</loc>`,
  `    <lastmod>${blogIndexLastmod}</lastmod>`,
  `    <changefreq>weekly</changefreq>`,
  `    <priority>0.7</priority>`,
  `  </url>`,
  ...allPosts.map(post => [
    `  <url>`,
    `    <loc>${SITE_URL}/blog/${post.slug}</loc>`,
    `    <lastmod>${post.lastModified}</lastmod>`,
    `    <changefreq>monthly</changefreq>`,
    `    <priority>0.6</priority>`,
    `  </url>`,
  ].join('\n')),
  `  <!-- BLOG_END -->`,
].join('\n');

if (!sitemapBase.includes('</urlset>')) {
  console.error('  Error: sitemap.xml is missing closing </urlset> tag. Blog URLs not added.');
  process.exit(1);
}

const updatedSitemap = sitemapBase.replace(
  '</urlset>',
  `${blogUrls}\n</urlset>`
);

writeFileSync(sitemapPath, updatedSitemap);
console.log('  Sitemap updated with blog URLs');

console.log('Blog build complete.');
