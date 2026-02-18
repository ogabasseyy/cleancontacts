import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeSlug from 'rehype-slug';
import rehypeAutolinkHeadings from 'rehype-autolink-headings';

interface BlogPostMeta {
  title: string;
  slug: string;
  date: string;
  lastModified: string;
  description: string;
  excerpt: string;
  readingTime: number;
  category: string;
  tags: string[];
  image: string;
}

const BlogPost: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const [meta, setMeta] = useState<BlogPostMeta | null>(null);
  const [content, setContent] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!slug) return;

    const controller = new AbortController();
    const { signal } = controller;

    Promise.all([
      fetch('/blog-manifest.json', { signal }).then(r => {
        if (!r.ok) throw new Error('Failed to load manifest');
        return r.json();
      }),
      fetch(`/blog/${slug}.md`, { signal }).then(r => {
        if (!r.ok) throw new Error('Not found');
        return r.text();
      }),
    ])
      .then(([manifest, md]: [BlogPostMeta[], string]) => {
        const post = manifest.find(p => p.slug === slug);
        if (!post) {
          setNotFound(true);
        } else {
          setMeta(post);
          setContent(md);
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
      <link rel="canonical" href={`https://contactscleaner.tech/blog/${meta.slug}`} />
      <meta property="og:type" content="article" />
      <meta property="og:title" content={meta.title} />
      <meta property="og:description" content={meta.description} />
      <meta property="og:image" content={`https://contactscleaner.tech${meta.image}`} />
      <meta property="og:url" content={`https://contactscleaner.tech/blog/${meta.slug}`} />
      <meta property="article:published_time" content={meta.date} />
      <meta property="article:modified_time" content={meta.lastModified} />

      <div className="glass-panel backdrop-blur-xl rounded-3xl p-8 md:p-12 shadow-2xl">
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
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-4 text-sm">
            <span className="px-2.5 py-0.5 rounded-full bg-brand/10 text-brand font-medium text-xs">
              {meta.category}
            </span>
            <span className="text-gray-500">{formatDate(meta.date)}</span>
            <span className="text-gray-600">·</span>
            <span className="text-gray-500">{meta.readingTime} min read</span>
          </div>
          <h1 className="text-3xl md:text-4xl font-bold text-gradient leading-tight">
            {meta.title}
          </h1>
        </div>

        {/* Markdown content */}
        <div className="prose prose-invert max-w-none prose-headings:font-bold prose-headings:text-white prose-p:text-gray-300 prose-p:leading-relaxed prose-a:text-brand prose-a:no-underline hover:prose-a:underline prose-strong:text-white prose-li:text-gray-300 prose-blockquote:border-brand/30 prose-blockquote:text-gray-400 prose-code:text-brand prose-code:bg-white/5 prose-code:px-1.5 prose-code:py-0.5 prose-code:rounded prose-code:before:content-[''] prose-code:after:content-[''] prose-th:text-white prose-td:text-gray-300 prose-hr:border-white/10">
          <ReactMarkdown
            remarkPlugins={[remarkGfm]}
            rehypePlugins={[
              rehypeSlug,
              [rehypeAutolinkHeadings, { behavior: 'wrap' }],
            ]}
            components={{
              img: ({ src, alt, ...props }) => (
                <img {...props} src={src} alt={alt || ''} loading="lazy" className="rounded-xl" />
              ),
              a: ({ href, children, ...props }) => {
                const isExternal = href?.startsWith('http');
                return (
                  <a
                    {...props}
                    href={href}
                    {...(isExternal ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
                  >
                    {children}
                  </a>
                );
              },
              table: ({ children, ...props }) => (
                <div className="overflow-x-auto -mx-4 md:mx-0">
                  <table {...props} className="min-w-full">
                    {children}
                  </table>
                </div>
              ),
            }}
          >
            {content}
          </ReactMarkdown>
        </div>

        {/* Tags */}
        {meta.tags.length > 0 && (
          <div className="mt-10 pt-6 border-t border-white/10">
            <div className="flex flex-wrap gap-2">
              {meta.tags.map(tag => (
                <span key={tag} className="text-xs text-gray-500 bg-white/5 px-3 py-1.5 rounded-full">
                  {tag}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* JSON-LD Article */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify({
            '@context': 'https://schema.org',
            '@type': 'Article',
            headline: meta.title,
            description: meta.description,
            image: `https://contactscleaner.tech${meta.image}`,
            datePublished: meta.date,
            dateModified: meta.lastModified,
            url: `https://contactscleaner.tech/blog/${meta.slug}`,
            author: {
              '@type': 'Organization',
              name: 'Contacts Cleaner',
              url: 'https://contactscleaner.tech',
            },
            publisher: {
              '@type': 'Organization',
              name: 'Contacts Cleaner',
              url: 'https://contactscleaner.tech',
              logo: {
                '@type': 'ImageObject',
                url: 'https://contactscleaner.tech/logo.png',
              },
            },
          }),
        }}
      />
    </div>
  );
};

export default BlogPost;
