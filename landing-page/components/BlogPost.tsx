import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import type { BlogPostMeta, TocHeading } from '../types';

const SITE_URL = 'https://contactscleaner.tech';

// Module-level manifest cache — avoids re-fetching on each post navigation
let cachedManifest: BlogPostMeta[] | null = null;

const TableOfContents: React.FC<{ headings: TocHeading[] }> = ({ headings }) => {
  const h2s = headings.filter(h => h.level === 2);
  if (h2s.length < 3) return null;

  return (
    <nav aria-label="Table of contents" className="mb-8 p-4 rounded-xl border border-white/10 bg-white/[0.02]">
      <p className="text-sm font-semibold text-gray-400 mb-3">In this article</p>
      <ul className="space-y-1.5">
        {h2s.map(heading => (
          <li key={heading.id}>
            <a
              href={`#${heading.id}`}
              className="text-sm text-gray-400 hover:text-brand transition-colors"
            >
              {heading.text}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
};

const BlogPost: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const [meta, setMeta] = useState<BlogPostMeta | null>(null);
  const [html, setHtml] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const titleRef = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    if (!slug) return;

    const controller = new AbortController();
    const { signal } = controller;

    const fetchManifest = cachedManifest
      ? Promise.resolve(cachedManifest)
      : fetch('/blog-manifest.json', { signal })
          .then(r => {
            if (!r.ok) throw new Error('Failed to load manifest');
            return r.json();
          })
          .then((data: BlogPostMeta[]) => {
            cachedManifest = data;
            return data;
          });

    Promise.all([
      fetchManifest,
      fetch(`/blog/${slug}.html`, { signal }).then(r => {
        if (!r.ok) throw new Error('Not found');
        return r.text();
      }),
    ])
      .then(([manifest, htmlContent]) => {
        const post = manifest.find(p => p.slug === slug);
        if (!post) {
          setNotFound(true);
        } else {
          setMeta(post);
          setHtml(htmlContent);
        }
        setLoading(false);
      })
      .catch((err) => {
        if (err.name !== 'AbortError') {
          setNotFound(true);
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [slug]);

  // Focus management: move focus to h1 after content loads for screen readers
  useEffect(() => {
    if (!loading && meta && titleRef.current) {
      titleRef.current.focus();
    }
  }, [loading, meta]);

  const formatDate = (dateStr: string) => {
    return new Date(dateStr + 'T00:00:00').toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  if (loading) {
    return (
      <div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl">
        <div className="glass-panel backdrop-blur-xl rounded-3xl p-8 md:p-12 shadow-2xl animate-pulse">
          <div className="h-8 bg-white/5 rounded w-3/4 mb-4" />
          <div className="h-4 bg-white/5 rounded w-1/2 mb-8" />
          <div className="space-y-3">
            {[1, 2, 3, 4, 5].map(i => (
              <div key={i} className="h-4 bg-white/5 rounded" />
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (notFound || !meta) {
    return (
      <div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl text-center">
        <div className="glass-panel backdrop-blur-xl rounded-3xl p-8 md:p-12 shadow-2xl">
          <h1 className="text-2xl font-bold mb-4">Post Not Found</h1>
          <p className="text-gray-400 mb-6">The blog post you're looking for doesn't exist.</p>
          <Link to="/blog" className="text-brand hover:underline">
            Back to Blog
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl">
      <title>{meta.title} - Contacts Cleaner Blog</title>
      <meta name="description" content={meta.description} />
      <link rel="canonical" href={`${SITE_URL}/blog/${meta.slug}`} />
      <meta property="og:type" content="article" />
      <meta property="og:title" content={meta.title} />
      <meta property="og:description" content={meta.description} />
      <meta property="og:image" content={`${SITE_URL}${meta.image}`} />
      <meta property="og:url" content={`${SITE_URL}/blog/${meta.slug}`} />
      <meta property="article:published_time" content={meta.date} />
      <meta property="article:modified_time" content={meta.lastModified} />

      <article className="glass-panel backdrop-blur-xl rounded-3xl p-8 md:p-12 shadow-2xl">
        {/* Back link */}
        <Link
          to="/blog"
          className="inline-flex items-center gap-2 text-sm text-gray-400 hover:text-brand transition-colors mb-8"
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M10 12L6 8L10 4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          Back to Blog
        </Link>

        {/* Post header */}
        <header className="mb-8">
          <div className="flex items-center gap-3 mb-4 text-sm">
            <span className="px-2.5 py-0.5 rounded-full bg-brand/10 text-brand font-medium text-xs">
              {meta.category}
            </span>
            <time dateTime={meta.date} className="text-gray-400">
              {formatDate(meta.date)}
            </time>
            <span className="text-gray-600" aria-hidden="true">·</span>
            <span className="text-gray-400">{meta.readingTime} min read</span>
          </div>
          <h1
            ref={titleRef}
            tabIndex={-1}
            className="text-3xl md:text-4xl font-bold text-gradient leading-tight outline-none"
          >
            {meta.title}
          </h1>
        </header>

        {/* Table of Contents */}
        <TableOfContents headings={meta.headings} />

        {/* Pre-rendered HTML content (build-time markdown→HTML, safe: our own output) */}
        <div
          className="prose prose-invert max-w-none prose-headings:font-bold prose-headings:text-white prose-p:text-gray-300 prose-p:leading-relaxed prose-a:text-brand prose-a:no-underline hover:prose-a:underline prose-strong:text-white prose-li:text-gray-300 prose-blockquote:border-brand/30 prose-blockquote:text-gray-400 prose-code:text-brand prose-code:bg-white/5 prose-code:px-1.5 prose-code:py-0.5 prose-code:rounded prose-code:before:content-[''] prose-code:after:content-[''] prose-th:text-white prose-td:text-gray-300 prose-hr:border-white/10 prose-table:border-white/10 prose-thead:border-white/10 prose-tr:border-white/10"
          dangerouslySetInnerHTML={{ __html: html }}
        />

        {/* Tags */}
        {meta.tags.length > 0 && (
          <footer className="mt-10 pt-6 border-t border-white/10">
            <div className="flex flex-wrap gap-2">
              {meta.tags.map(tag => (
                <span key={tag} className="text-xs text-gray-400 bg-white/5 px-3 py-1.5 rounded-full">
                  {tag}
                </span>
              ))}
            </div>
          </footer>
        )}
      </article>

      {/* JSON-LD BlogPosting */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify({
            '@context': 'https://schema.org',
            '@type': 'BlogPosting',
            headline: meta.title,
            description: meta.description,
            image: `${SITE_URL}${meta.image}`,
            datePublished: meta.date,
            dateModified: meta.lastModified,
            url: `${SITE_URL}/blog/${meta.slug}`,
            mainEntityOfPage: `${SITE_URL}/blog/${meta.slug}`,
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
          }),
        }}
      />

      {/* JSON-LD BreadcrumbList */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify({
            '@context': 'https://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
              {
                '@type': 'ListItem',
                position: 1,
                name: 'Home',
                item: SITE_URL,
              },
              {
                '@type': 'ListItem',
                position: 2,
                name: 'Blog',
                item: `${SITE_URL}/blog`,
              },
              {
                '@type': 'ListItem',
                position: 3,
                name: meta.title,
                item: `${SITE_URL}/blog/${meta.slug}`,
              },
            ],
          }),
        }}
      />
    </div>
  );
};

export default BlogPost;
