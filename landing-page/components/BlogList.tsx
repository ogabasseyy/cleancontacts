import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import type { BlogPostMeta } from '../types';

const SITE_URL = 'https://contactscleaner.tech';

/** Safely serialize JSON-LD for embedding in <script> tags (prevents XSS via </script> injection) */
const safeJsonLd = (data: unknown): string =>
  JSON.stringify(data).replace(/</g, '\\u003c').replace(/>/g, '\\u003e');

const BlogList: React.FC = () => {
  const [posts, setPosts] = useState<BlogPostMeta[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const titleRef = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    const controller = new AbortController();
    fetch('/blog-manifest.json', { signal: controller.signal })
      .then(res => {
        if (!res.ok) throw new Error('Failed to load posts');
        return res.json();
      })
      .then((data: BlogPostMeta[]) => {
        const sorted = [...data].sort(
          (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()
        );
        setPosts(sorted);
        setLoading(false);
      })
      .catch((err) => {
        if (err.name !== 'AbortError') {
          setError(true);
          setLoading(false);
        }
      });
    return () => controller.abort();
  }, []);

  // Focus management: move focus to h1 after loading for screen readers
  useEffect(() => {
    if (!loading && !error && titleRef.current) {
      titleRef.current.focus();
    }
  }, [loading, error]);

  const formatDate = (dateStr: string) => {
    return new Date(dateStr + 'T00:00:00').toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  return (
    <div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl">
      <title>Blog - Contacts Cleaner</title>
      <meta name="description" content="Tips, guides, and comparisons for managing your phone contacts. Learn how to remove duplicates, detect WhatsApp users, and keep your address book clean." />
      <link rel="canonical" href={`${SITE_URL}/blog`} />

      <div className="glass-panel backdrop-blur-xl rounded-3xl p-8 md:p-12 shadow-2xl">
        <h1
          ref={titleRef}
          tabIndex={-1}
          className="text-3xl md:text-4xl font-bold mb-2 text-gradient outline-none"
        >
          Blog
        </h1>
        <p className="text-gray-400 mb-10">Tips, guides, and news about contact management.</p>

        {loading ? (
          <div
            className="space-y-6"
            role="status"
            aria-live="polite"
            aria-busy="true"
            aria-label="Loading blog posts"
          >
            <span className="sr-only">Loading blog posts</span>
            {[1, 2, 3].map(i => (
              <div key={i} className="animate-pulse rounded-2xl bg-white/5 h-48" aria-hidden="true" />
            ))}
          </div>
        ) : error ? (
          <p className="text-gray-400" role="alert">Failed to load posts. Please try again later.</p>
        ) : posts.length === 0 ? (
          <p className="text-gray-400" role="status">No posts yet. Check back soon!</p>
        ) : (
          <ul className="space-y-6">
            {posts.map(post => (
              <li key={post.slug}>
              <Link
                to={`/blog/${post.slug}`}
                className="block group rounded-2xl border border-white/5 bg-white/[0.02] hover:bg-white/[0.05] hover:border-brand/20 transition-all duration-300 p-6 md:p-8 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-dark focus-visible:bg-white/[0.05] focus-visible:border-brand/20 relative overflow-hidden"
              >
                <div className="flex items-center gap-3 mb-3 text-sm">
                  <span className="px-2.5 py-0.5 rounded-full bg-brand/10 text-brand font-medium text-xs">
                    {post.category}
                  </span>
                  <time dateTime={post.date} className="text-gray-400">
                    {formatDate(post.date)}
                  </time>
                  <span className="text-gray-600" aria-hidden="true">·</span>
                  <span className="text-gray-400">{post.readingTime} min read</span>
                </div>

                <h2 className="text-xl md:text-2xl font-bold text-white group-hover:text-brand group-focus-visible:text-brand transition-colors mb-3">
                  {post.title}
                </h2>

                <p className="text-gray-400 leading-relaxed">
                  {post.description}
                </p>

                <div className="mt-4 flex flex-wrap items-center justify-between gap-4">
                  <div className="flex flex-wrap gap-2">
                    {post.tags.slice(0, 4).map(tag => (
                      <span key={tag} className="text-xs text-gray-400 bg-white/5 px-2 py-1 rounded">
                        {tag}
                      </span>
                    ))}
                  </div>
                  <span className="flex items-center gap-2 text-sm font-medium text-brand opacity-0 translate-x-4 group-hover:opacity-100 group-hover:translate-x-0 group-focus-visible:opacity-100 group-focus-visible:translate-x-0 transition-all duration-300" aria-hidden="true">
                    Read article
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true" focusable="false">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
                    </svg>
                  </span>
                </div>
              </Link>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* JSON-LD CollectionPage with mainEntity */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: safeJsonLd({
            '@context': 'https://schema.org',
            '@type': 'CollectionPage',
            name: 'Contacts Cleaner Blog',
            description: 'Tips, guides, and comparisons for managing your phone contacts.',
            url: `${SITE_URL}/blog`,
            publisher: {
              '@type': 'Organization',
              name: 'Contacts Cleaner',
              url: SITE_URL,
            },
            mainEntity: posts.map(post => ({
              '@type': 'BlogPosting',
              headline: post.title,
              url: `${SITE_URL}/blog/${post.slug}`,
              datePublished: post.date,
            })),
          }),
        }}
      />
    </div>
  );
};

export default BlogList;
