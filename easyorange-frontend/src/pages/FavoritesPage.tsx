import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Heart, Package, Trash2, Loader2 } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { favoriteApi } from '@/api/favoriteApi';
import { useUIStore } from '@/store/uiStore';
import type { Favorite } from '@/types';

export function FavoritesPage() {
  const navigate = useNavigate();
  const addToast = useUIStore((s) => s.addToast);
  const queryClient = useQueryClient();
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());

  const { data: favoritesData, isLoading, error } = useQuery({
    queryKey: ['favorites'],
    queryFn: async () => {
      const response = await favoriteApi.getList();
      return response.data;
    },
    staleTime: 30 * 1000,
  });

  const removeMutation = useMutation({
    mutationFn: async (productId: number) => {
      await favoriteApi.remove(productId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['favorites'] });
      addToast({ type: 'success', message: '已取消收藏' });
    },
    onError: () => {
      addToast({ type: 'error', message: '取消收藏失败' });
    },
  });

  const removeManyMutation = useMutation({
    mutationFn: async (ids: number[]) => {
      await favoriteApi.removeMany(ids);
    },
    onSuccess: () => {
      setSelectedIds(new Set());
      queryClient.invalidateQueries({ queryKey: ['favorites'] });
      addToast({ type: 'success', message: '已批量取消收藏' });
    },
    onError: () => {
      addToast({ type: 'error', message: '批量取消收藏失败' });
    },
  });

  const favorites = favoritesData?.records ?? [];

  const toggleSelect = (id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const handleBatchRemove = () => {
    if (selectedIds.size === 0) return;
    removeManyMutation.mutate(Array.from(selectedIds));
  };

  if (isLoading) {
    return (
      <div className="py-6">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">我的收藏</h1>
          <p className="mt-1 text-sm text-gray-500">您收藏的商品</p>
        </div>
        <div className="flex items-center justify-center py-20">
          <Loader2 size={32} className="animate-spin text-primary-600" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="py-6">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">我的收藏</h1>
        </div>
        <div className="rounded-2xl bg-white p-8 shadow-sm ring-1 ring-gray-200/50 text-center">
          <p className="text-red-500">加载收藏列表失败，请稍后重试</p>
          <button
            className="mt-4 btn btn-outline"
            onClick={() => queryClient.invalidateQueries({ queryKey: ['favorites'] })}
          >
            重新加载
          </button>
        </div>
      </div>
    );
  }

  if (favorites.length === 0) {
    return (
      <div className="py-6">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">我的收藏</h1>
          <p className="mt-1 text-sm text-gray-500">您收藏的商品</p>
        </div>
        <div className="rounded-2xl bg-white p-8 shadow-sm ring-1 ring-gray-200/50">
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <div className="flex h-20 w-20 items-center justify-center rounded-full bg-gray-100">
              <Heart size={36} className="text-gray-400" />
            </div>
            <h3 className="mt-4 text-lg font-semibold text-gray-900">暂无收藏商品</h3>
            <p className="mt-2 max-w-sm text-sm text-gray-500">
              还没有收藏任何商品，快去逛逛发现喜欢的宝贝吧！
            </p>
            <Link to="/products" className="mt-6 rounded-xl bg-primary-600 px-6 py-3 font-semibold text-white shadow-sm hover:bg-primary-700 transition-all">
              <span className="flex items-center gap-2">
                <Package size={18} />
                开始购物
              </span>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="py-6">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">我的收藏</h1>
          <p className="mt-1 text-sm text-gray-500">共 {favorites.length} 件商品</p>
        </div>
        {selectedIds.size > 0 && (
          <button
            className="btn btn-outline text-red-600 hover:bg-red-50"
            onClick={handleBatchRemove}
            disabled={removeManyMutation.isPending}
          >
            <Trash2 size={16} />
            删除选中 ({selectedIds.size})
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {favorites.map((item) => {
          const fav = item as unknown as Favorite;
          const product = fav.product;
          return (
            <div
              key={fav.id}
              className="group relative rounded-2xl bg-white p-4 shadow-sm ring-1 ring-gray-200/50 hover:shadow-md transition-shadow"
            >
              <label className="absolute left-3 top-3 z-10 flex items-center">
                <input
                  type="checkbox"
                  checked={selectedIds.has(fav.id)}
                  onChange={() => toggleSelect(fav.id)}
                  className="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                />
              </label>
              {product && (
                <div
                  className="cursor-pointer"
                  onClick={() => navigate(`/products/${product.id}`)}
                >
                  <div className="aspect-square overflow-hidden rounded-xl bg-gray-100">
                    {product.images?.[0] ? (
                      <img
                        src={product.images[0]}
                        alt={product.title}
                        className="h-full w-full object-cover group-hover:scale-105 transition-transform"
                      />
                    ) : (
                      <div className="flex h-full w-full items-center justify-center">
                        <Package size={40} className="text-gray-300" />
                      </div>
                    )}
                  </div>
                  <div className="mt-3">
                    <h3 className="text-sm font-medium text-gray-900 line-clamp-2">{product.title}</h3>
                    <div className="mt-2 flex items-center justify-between">
                      <span className="text-lg font-bold text-primary-600">¥{product.price?.toFixed(2)}</span>
                      <span className="text-xs text-gray-400">{product.categoryName}</span>
                    </div>
                  </div>
                </div>
              )}
              <button
                className="absolute right-3 top-3 rounded-full bg-white/80 p-1.5 text-gray-400 hover:text-red-500 hover:bg-white shadow-sm backdrop-blur-sm transition-colors"
                onClick={() => removeMutation.mutate(fav.productId)}
                disabled={removeMutation.isPending}
              >
                <Heart size={16} fill="currentColor" />
              </button>
            </div>
          );
        })}
      </div>
    </div>
  );
}
