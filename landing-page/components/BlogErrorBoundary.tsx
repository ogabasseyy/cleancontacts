import React from 'react';
import { Link } from 'react-router';

interface Props {
  children: React.ReactNode;
}

interface State {
  hasError: boolean;
}

class BlogErrorBoundary extends React.Component<Props, State> {
  declare props: Props;
  declare state: State;

  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl text-center">
          <div className="glass-panel backdrop-blur-xl rounded-3xl p-8 md:p-12 shadow-2xl" role="alert">
            <h1 className="text-2xl font-bold mb-4">Something went wrong</h1>
            <p className="text-gray-400 mb-6">
              There was an error loading this page. Please try again.
            </p>
            <Link to="/blog" className="text-brand hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-dark rounded-sm">
              Back to Blog
            </Link>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default BlogErrorBoundary;
