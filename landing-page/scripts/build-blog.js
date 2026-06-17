import fs from 'fs';
import path from 'path';

import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const blogDir = path.resolve(__dirname, '../public/blog');
const blogContentDir = path.resolve(__dirname, '../content/blog');
const publicDir = path.resolve(__dirname, '../public');

// Ensure output directories exist
if (!fs.existsSync(blogDir)) {
  fs.mkdirSync(blogDir, { recursive: true });
}

// Ensure content directory exists (for dev/empty state)
if (!fs.existsSync(blogContentDir)) {
  fs.mkdirSync(blogContentDir, { recursive: true });
}

/**
 * Extracts reading time from content
 * @param {string} content Markdown content
 * @returns {number} Reading time in minutes
 */
function getReadingTime(content) {
  const wordsPerMinute = 200;
  const noOfWords = content.split(/\s/g).length;
  const minutes = noOfWords / wordsPerMinute;
  const readTime = Math.ceil(minutes);
  return readTime;
}

/**
 * Parse a markdown file with frontmatter manually since gray-matter depends on vulnerable js-yaml
 * @param {string} filePath
 */
function parseMatter(filePath) {
  const rawContent = fs.readFileSync(filePath, 'utf8');
  if (rawContent.startsWith('---')) {
    const endMatch = rawContent.indexOf('\n---', 3);
    if (endMatch !== -1) {
      const frontmatter = rawContent.substring(4, endMatch);
      const content = rawContent.substring(endMatch + 4).trim();

      const data = {};
      const lines = frontmatter.split('\n');
      for (const line of lines) {
        const colonIdx = line.indexOf(':');
        if (colonIdx !== -1) {
          const key = line.substring(0, colonIdx).trim();
          let value = line.substring(colonIdx + 1).trim();
          // very naive parsing for basic fields
          if (value.startsWith('"') && value.endsWith('"')) value = value.slice(1, -1);
          else if (value.startsWith("'") && value.endsWith("'")) value = value.slice(1, -1);

          if (key === 'date') {
              // we don't convert to Date right now to mimic gray-matter behavior exactly as used
              data[key] = new Date(value);
          } else {
              data[key] = value;
          }
        }
      }
      return { data, content };
    }
  }
  return { data: {}, content: rawContent };
}


/**
 * Builds the blog manifest and individual JSON files for the client
 */
function buildBlog() {
  const files = fs.readdirSync(blogContentDir).filter(file => file.endsWith('.md'));

  const posts = files.map(file => {
    const slug = file.replace(/\.md$/, '').replace(/[^a-zA-Z0-9-]/g, '');
    const filePath = path.join(blogContentDir, file);

    // Instead of gray-matter we use our naive parser to avoid js-yaml vulnerability while keeping the build script working
    const { data, content } = parseMatter(filePath);

    // Generate reading time
    const readTime = getReadingTime(content);

    // Handle date properly
    // gray-matter parses unquoted YAML dates into JS Date objects.
    let dateStr = '';
    if (data.date instanceof Date) {
      dateStr = data.date.toISOString().split('T')[0];
    } else if (typeof data.date === 'string') {
      dateStr = data.date;
    }

    const postData = {
      slug,
      title: data.title || 'Untitled',
      description: data.description || '',
      date: dateStr,
      author: data.author || 'Oga Bassey',
      readTime: data.readTime || readTime,
      tags: typeof data.tags === 'string' ? data.tags.split(',').map(t => t.trim()) : (data.tags || []),
      content: content
    };

    // Write individual post JSON
    fs.writeFileSync(
      path.join(blogDir, `${slug}.json`),
      JSON.stringify(postData)
    );

    return {
      slug,
      title: postData.title,
      description: postData.description,
      date: postData.date,
      readTime: postData.readTime,
      tags: postData.tags
    };
  });

  // Sort by date descending
  posts.sort((a, b) => {
    return new Date(b.date).getTime() - new Date(a.date).getTime();
  });

  // Write manifest
  fs.writeFileSync(
    path.join(blogDir, 'manifest.json'),
    JSON.stringify(posts)
  );

  console.log(`  Blog manifest: ${posts.length} posts`);

  // Also generate an RSS feed while we're at it
  generateRss(posts);
  generateSitemap(posts);
}

function generateRss(posts) {
  const siteUrl = 'https://contactscleaner.tech';

  let rss = `<?xml version="1.0" encoding="UTF-8" ?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
<channel>
  <title>Contacts Cleaner Blog</title>
  <description>Tips, updates, and guides for managing your contacts effectively.</description>
  <link>${siteUrl}/blog</link>
  <atom:link href="${siteUrl}/rss.xml" rel="self" type="application/rss+xml" />
`;

  posts.forEach(post => {
    // Basic XML escaping
    const title = post.title.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    const desc = post.description.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

    // RFC-822 date format
    const pubDate = new Date(post.date).toUTCString();

    rss += `  <item>
    <title>${title}</title>
    <description>${desc}</description>
    <link>${siteUrl}/blog/${post.slug}</link>
    <guid>${siteUrl}/blog/${post.slug}</guid>
    <pubDate>${pubDate}</pubDate>
  </item>\n`;
  });

  rss += `</channel>\n</rss>`;

  fs.writeFileSync(path.join(publicDir, 'rss.xml'), rss);
  console.log('  RSS feed: rss.xml');
}

function escapeXml(unsafe) {
    return unsafe.replace(/[<>&'"]/g, function (c) {
        switch (c) {
            case '<': return '&lt;';
            case '>': return '&gt;';
            case '&': return '&amp;';
            case '\'': return '&apos;';
            case '"': return '&quot;';
        }
    });
}
function generateSitemap(posts) {
  const siteUrl = 'https://contactscleaner.tech';
  const sitemapPath = path.join(publicDir, 'sitemap.xml');

  let sitemapContent = '';

  try {
    sitemapContent = fs.readFileSync(sitemapPath, 'utf8');

    // Check if blog URLs are already in the sitemap
    if (sitemapContent.includes('/blog/')) {
      buildFullSitemap(posts, siteUrl, sitemapPath);
      return;
    }

    let blogUrls = '';

    blogUrls += `
  <url>
    <loc>${siteUrl}/blog</loc>
    <changefreq>weekly</changefreq>
    <priority>0.8</priority>
  </url>`;

    posts.forEach(post => {
      blogUrls += `
  <url>
    <loc>${siteUrl}/blog/${escapeXml(post.slug)}</loc>
    <lastmod>${escapeXml(post.date)}</lastmod>
    <changefreq>monthly</changefreq>
    <priority>0.7</priority>
  </url>`;
    });

    sitemapContent = sitemapContent.replace('</urlset>', `${blogUrls}\n</urlset>`);
    fs.writeFileSync(sitemapPath, sitemapContent);
  } catch (err) {
    if (err.code === 'ENOENT') {
      buildFullSitemap(posts, siteUrl, sitemapPath);
    } else {
      throw err;
    }
  }

  console.log('  Sitemap updated with blog URLs');
}

function buildFullSitemap(posts, siteUrl, sitemapPath) {
  const today = new Date().toISOString().split('T')[0];

  let sitemap = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>${siteUrl}/</loc>
    <lastmod>${today}</lastmod>
    <changefreq>weekly</changefreq>
    <priority>1.0</priority>
  </url>
  <url>
    <loc>${siteUrl}/privacy</loc>
    <changefreq>monthly</changefreq>
    <priority>0.5</priority>
  </url>
  <url>
    <loc>${siteUrl}/terms</loc>
    <changefreq>monthly</changefreq>
    <priority>0.5</priority>
  </url>
  <url>
    <loc>${siteUrl}/support</loc>
    <changefreq>monthly</changefreq>
    <priority>0.6</priority>
  </url>
  <url>
    <loc>${siteUrl}/blog</loc>
    <changefreq>weekly</changefreq>
    <priority>0.8</priority>
  </url>`;

  posts.forEach(post => {
    sitemap += `
  <url>
    <loc>${siteUrl}/blog/${escapeXml(post.slug)}</loc>
    <lastmod>${escapeXml(post.date)}</lastmod>
    <changefreq>monthly</changefreq>
    <priority>0.7</priority>
  </url>`;
  });

  sitemap += `\n</urlset>`;

  fs.writeFileSync(sitemapPath, sitemap);
}

buildBlog();
console.log('Blog build complete.');
