/**
 * Post-build prerender script.
 * Creates static HTML shells for each SPA route with correct meta tags.
 * Googlebot sees proper <title>, <meta>, <canonical>, JSON-LD per route without JS.
 */
import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const distDir = resolve(__dirname, '../dist');
const indexHtml = readFileSync(resolve(distDir, 'index.html'), 'utf-8');
const SITE_URL = 'https://contactscleaner.tech';

/**
 * Escape a string for safe use inside an HTML attribute value.
 */
function escapeAttr(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

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
  {
    path: '/blog',
    title: 'Blog - Contacts Cleaner',
    description: 'Tips, guides, and comparisons for managing your phone contacts. Learn how to remove duplicates, detect WhatsApp users, and keep your address book clean.',
  },
];

// Load blog manifest for individual post routes
const manifestPath = resolve(distDir, 'blog-manifest.json');
if (existsSync(manifestPath)) {
  const posts = JSON.parse(readFileSync(manifestPath, 'utf-8'));
  for (const post of posts) {
    routes.push({
      path: `/blog/${post.slug}`,
      title: `${post.title} - Contacts Cleaner Blog`,
      description: post.description,
      isBlogPost: true,
      date: post.date,
      lastModified: post.lastModified,
      image: post.image,
      jsonLd: [
        // BlogPosting schema
        {
          '@context': 'https://schema.org',
          '@type': 'BlogPosting',
          headline: post.title,
          description: post.description,
          image: `${SITE_URL}${post.image}`,
          datePublished: post.date,
          dateModified: post.lastModified,
          url: `${SITE_URL}/blog/${post.slug}`,
          mainEntityOfPage: `${SITE_URL}/blog/${post.slug}`,
          author: {
            '@type': 'Organization',
            name: 'Contacts Cleaner',
            url: SITE_URL,
          },
          publisher: {
            '@type': 'Organization',
            name: 'Contacts Cleaner',
            url: SITE_URL,
            logo: {
              '@type': 'ImageObject',
              url: `${SITE_URL}/logo.png`,
            },
          },
        },
        // BreadcrumbList schema
        {
          '@context': 'https://schema.org',
          '@type': 'BreadcrumbList',
          itemListElement: [
            { '@type': 'ListItem', position: 1, name: 'Home', item: SITE_URL },
            { '@type': 'ListItem', position: 2, name: 'Blog', item: `${SITE_URL}/blog` },
            { '@type': 'ListItem', position: 3, name: post.title, item: `${SITE_URL}/blog/${post.slug}` },
          ],
        },
      ],
    });
  }
  console.log(`  Found ${posts.length} blog posts to prerender`);
}

function prerenderRoute(route) {
  let html = indexHtml;

  // Replace title
  html = html.replace(
    /<title>.*?<\/title>/,
    `<title>${escapeAttr(route.title)}</title>`
  );

  // Replace meta description
  html = html.replace(
    /<meta name="description" content=".*?" \/>/,
    `<meta name="description" content="${escapeAttr(route.description)}" />`
  );

  // Replace canonical
  html = html.replace(
    /<link rel="canonical" href=".*?" \/>/,
    `<link rel="canonical" href="${SITE_URL}${route.path}" />`
  );

  // Replace OG URL
  html = html.replace(
    /<meta property="og:url" content=".*?" \/>/,
    `<meta property="og:url" content="${SITE_URL}${route.path}" />`
  );

  // Replace OG title
  html = html.replace(
    /<meta property="og:title" content=".*?" \/>/,
    `<meta property="og:title" content="${escapeAttr(route.title)}" />`
  );

  // Replace OG description
  html = html.replace(
    /<meta property="og:description" content=".*?" \/>/,
    `<meta property="og:description" content="${escapeAttr(route.description)}" />`
  );

  // Blog post specific: og:type → article, article timestamps, twitter card per post
  if (route.isBlogPost) {
    html = html.replace(
      /<meta property="og:type" content=".*?" \/>/,
      `<meta property="og:type" content="article" />`
    );

    // Add article timestamps before </head>
    const articleMeta = [
      `  <meta property="article:published_time" content="${route.date}" />`,
      `  <meta property="article:modified_time" content="${route.lastModified}" />`,
    ].join('\n');
    html = html.replace('</head>', `${articleMeta}\n  </head>`);

    // Replace twitter card tags per post
    html = html.replace(
      /<meta name="twitter:title" content=".*?" \/>/,
      `<meta name="twitter:title" content="${escapeAttr(route.title)}" />`
    );
    html = html.replace(
      /<meta name="twitter:description" content=".*?" \/>/,
      `<meta name="twitter:description" content="${escapeAttr(route.description)}" />`
    );
  }

  // Inject JSON-LD schemas for blog posts (array of schemas)
  if (route.jsonLd) {
    const schemas = route.jsonLd
      .map(schema => `  <script type="application/ld+json">\n    ${JSON.stringify(schema).replace(/</g, '\\u003c')}\n    </script>`)
      .join('\n');
    html = html.replace('</head>', `${schemas}\n  </head>`);
  }

  // Write to route directory
  const routeDir = resolve(distDir, route.path.slice(1));
  mkdirSync(routeDir, { recursive: true });
  writeFileSync(resolve(routeDir, 'index.html'), html);
  console.log(`  Pre-rendered: ${route.path}/index.html`);
}

for (const route of routes) {
  prerenderRoute(route);
}

console.log('Pre-rendering complete.');
