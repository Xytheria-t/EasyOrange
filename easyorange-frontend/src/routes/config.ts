export type RouteName =
    | 'home'
    | 'products'
    | 'publish'
    | 'profile'
    | 'favorites'
    | 'messages'
    | 'orders'
    | 'review'
    | 'orderDetail';

export interface RouteConfig {
    path: string;
    title: string;
    navKey?: string;
    requiresAuth: boolean;
}

export const routes: Record<RouteName, RouteConfig> = {
    home: {
        path: '/',
        title: '首页',
        navKey: 'home',
        requiresAuth: false
    },
    products: {
        path: '/products.html',
        title: '商品',
        navKey: 'products',
        requiresAuth: false
    },
    publish: {
        path: '/publish.html',
        title: '发布商品',
        requiresAuth: true
    },
    profile: {
        path: '/profile.html',
        title: '个人中心',
        requiresAuth: true
    },
    favorites: {
        path: '/favorites.html',
        title: '我的收藏',
        requiresAuth: true
    },
    messages: {
        path: '/messages.html',
        title: '消息',
        requiresAuth: true
    },
    orders: {
        path: '/orders.html',
        title: '我的订单',
        requiresAuth: true
    },
    review: {
        path: '/review.html',
        title: '评价',
        requiresAuth: true
    },
    orderDetail: {
        path: '/order-detail.html',
        title: '订单详情',
        requiresAuth: true
    }
};
