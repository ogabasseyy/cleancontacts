import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

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

const BlogList: React.FC = () => {
  const [posts, setPosts] = useState<BlogPostMeta[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

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

  const formatDate = (dateStr: string) => {
    // Append T00:00:00 to avoid UTC midnight shifting the day in negative offsets
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
      <link rel="canonical" href="https://contactscleaner.tech/blog" />

      <div className="glass-panel backdrop-blur-xl rounded-3xl p-8 md:p-12 shadow-2xl">
        <h1 className="text-3xl md:text-4xl font-bold mb-2 text-gradient">Blog</h1>
        <p className="text-gray-400 mb-10">Tips, guides, and news about contact management.</p>

        {loading ? (
          <div className="space-y-6">
            {[1, 2, 3].map(i => (
              <div key={i} className="animate-pulse rounded-2xl bg-white/5 h-48" />
            ))}
          </div>
        ) : error ? (
          <p className="text-gray-500">Failed to load posts. Please try again later.</p>
        ) : posts.length === 0 ? (
          <p className="text-gray-500">No posts yet. Check back soon!</p>
        ) : (
          <div className="space-y-6">
            {posts.map(post => (
              <Link
                key={post.slug}
                to={`/blog/${post.slug}`}
                className="block group rounded-2xl border border-white/5 bg-white/[0.02] hover:bg-white/[0.05] hover:border-brand/20 transition-all duration-300 p-6 md:p-8"
              >
                <div className="flex items-center gap-3 mb-3 text-sm">
                  <span className="px-2.5 py-0.5 rounded-full bg-brand/10 text-brand font-medium text-xs">
                    {post.category}
                  </span>
                  <span className="text-gray-500">{formatDate(post.date)}</span>
                  <span className="text-gray-600">·</span>
                  <span className="text-gray-500">{post.readingTime} min read</span>
                </div>

                <h2 className="text-xl md:text-2xl font-bold text-white group-hover:text-brand transition-colors mb-3">
                  {post.title}
                </h2>

                <p className="text-gray-400 leading-relaxed">
                  {post.description}
                </p>

                <div className="mt-4 flex flex-wrap gap-2">
                  {post.tags.slice(0, 4).map(tag => (
                    <span key={tag} className="text-xs text-gray-500 bg-white/5 px-2 py-1 rounded">
                      {tag}
                    </span>
                  ))}
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>

      {/* JSON-LD CollectionPage */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify({
            '@context': 'https://schema.org',
            '@type': 'CollectionPage',
            name: 'Contacts Cleaner Blog',
            description: 'Tips, guides, and comparisons for managing your phone contacts.',
            url: 'https://contactscleaner.tech/blog',
            publisher: {
              '@type': 'Organization',
              name: 'Contacts Cleaner',
              url: 'https://contactscleaner.tech',
            },
          }),
        }}
      />
    </div>
  );
};

export default BlogList;
