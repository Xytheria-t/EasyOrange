import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';
import { favoriteApi } from '@/api/favoriteApi';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';

/** 后端 batch-check 单次 id 上限（BatchCheckRequest @Size(max = 100)），超限整体 400。 */
const BATCH_CHECK_MAX_SIZE = 100;

export function useFavoriteCheck() {
    const { token } = useAuthStore();
    const queryClient = useQueryClient();
    const addToast = useUIStore(s => s.addToast);
    const [favoriteMap, setFavoriteMap] = useState<Record<string, boolean>>(() => ({}));

    const checkFavorites = useCallback(
        async (productIds: string[]) => {
            if (!token || productIds.length === 0) {
                setFavoriteMap({});
                return;
            }

            // 商品列表无限滚动会累计上百个 id，必须去重后按后端上限分批提交
            const uniqueIds = [...new Set(productIds)];
            const batches: string[][] = [];
            for (let start = 0; start < uniqueIds.length; start += BATCH_CHECK_MAX_SIZE) {
                batches.push(uniqueIds.slice(start, start + BATCH_CHECK_MAX_SIZE));
            }

            try {
                const responses = await Promise.all(batches.map(batch => favoriteApi.batchCheck(batch)));
                // 合并而非覆盖：不同页面各自调用本方法，覆盖会让先前查到的收藏状态整体丢失
                setFavoriteMap(prev =>
                    responses.reduce<Record<string, boolean>>(
                        (merged, response) => Object.assign(merged, response.data ?? {}),
                        { ...prev }
                    )
                );
            } catch {
                // 保留上一次结果：清空会让已收藏商品的心形整体变空心
            }
        },
        [token]
    );

    const toggleFavorite = useCallback(
        async (productId: string, shouldFavorite: boolean) => {
            if (!token) {
                return false;
            }
            try {
                if (shouldFavorite) {
                    await favoriteApi.addFavorite(productId);
                    addToast({ type: 'success', message: '已收藏' });
                } else {
                    await favoriteApi.removeFavorite(productId);
                    addToast({ type: 'success', message: '已取消收藏' });
                }
                setFavoriteMap(prev => ({ ...prev, [productId]: shouldFavorite }));
                queryClient.invalidateQueries({ queryKey: ['favorites'] });
                return true;
            } catch {
                addToast({ type: 'error', message: shouldFavorite ? '收藏失败' : '取消收藏失败' });
                return false;
            }
        },
        [token, addToast, queryClient]
    );

    const effectiveFavoriteMap = useMemo(() => {
        return token ? favoriteMap : {};
    }, [token, favoriteMap]);

    const isFavorited = useCallback(
        (productId: string): boolean => {
            return effectiveFavoriteMap[productId] ?? false;
        },
        [effectiveFavoriteMap]
    );

    return { favoriteMap: effectiveFavoriteMap, checkFavorites, isFavorited, toggleFavorite };
}
