import { Link } from 'react-router-dom';
import { Home, ArrowLeft } from 'lucide-react';

export function NotFoundPage() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center px-4 text-center">
      <div className="mb-6 flex h-24 w-24 items-center justify-center rounded-full bg-primary-50">
        <span className="text-5xl font-bold text-primary-600">404</span>
      </div>
      <h1 className="text-2xl font-bold text-gray-900">页面未找到</h1>
      <p className="mt-2 max-w-md text-sm text-gray-500">
        抱歉，您访问的页面不存在或已被移除。请检查地址是否正确，或返回首页。
      </p>
      <div className="mt-8 flex gap-4">
        <button
          className="btn btn-outline"
          onClick={() => window.history.back()}
        >
          <ArrowLeft size={16} />
          返回上页
        </button>
        <Link to="/" className="btn btn-primary">
          <Home size={16} />
          回到首页
        </Link>
      </div>
    </div>
  );
}
