export type ViewState = 'home' | 'privacy' | 'terms' | 'support' | 'blog' | 'blog-post';

export interface TocHeading {
  level: number;
  text: string;
  id: string;
}

export interface BlogPostMeta {
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
  headings: TocHeading[];
}
