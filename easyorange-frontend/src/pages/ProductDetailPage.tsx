import { useParams } from 'react-router-dom';
import { useProduct } from '@/hooks';

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: product, isLoading } = useProduct(Number(id));

  if (isLoading) return <div className="flex items-center justify-center py-12 text-gray-500">加载中...</div>;
  if (!product) return <div className="flex items-center justify-center py-12 text-gray-500">商品不存在</div>;

  return (
    <div className="rounded-xl bg-white p-6 shadow-sm">
      <h1 className="text-2xl font-bold">{product.title}</h1>
      <div className="mt-4 text-3xl font-bold text-primary-500">¥{product.price.toFixed(2)}</div>
      <p className="mt-4 text-gray-600">{product.description}</p>
      <div className="mt-6 flex items-center gap-4 text-sm text-gray-500">
        <span>卖家：{product.sellerName}</span>
        <span>位置：{product.location}</span>
      </div>
    </div>
  );
}
