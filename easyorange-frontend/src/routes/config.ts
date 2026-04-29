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
        path: '/products',
        title: '商品',
        navKey: 'products',
        requiresAuth: false
    },
    publish: {
        path: '/publish',
        title: '发布商品',
        requiresAuth: true
    },
    profile: {
        path: '/profile',
        title: '个人中心',
        requiresAuth: true
    },
    favorites: {
        path: '/favorites',
        title: '我的收藏',
        requiresAuth: true
    },
    messages: {
        path: '/messages',
        title: '消息',
        requiresAuth: true
    },
    orders: {
        path: '/orders',
        title: '我的订单',
        requiresAuth: true
    },
    review: {
        path: '/review',
        title: '评价',
        requiresAuth: true
    },
    orderDetail: {
        path: '/order-detail',
        title: '订单详情',
        requiresAuth: true
    }
};
