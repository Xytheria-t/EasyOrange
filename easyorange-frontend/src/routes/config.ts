export type RouteName =
    | 'home'
    | 'products'
    | 'publish'
    | 'search'
    | 'profile'
    | 'favorites'
    | 'messages'
    | 'orders'
    | 'orderDetail'
    | 'payment'
    | 'paymentResult'
    | 'notifications'
    | 'credit';

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
    search: {
        path: '/search',
        title: '搜索',
        requiresAuth: false
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
    orderDetail: {
        path: '/orders/:id',
        title: '订单详情',
        requiresAuth: true
    },
    payment: {
        path: '/payment',
        title: '收银台',
        requiresAuth: true
    },
    paymentResult: {
        path: '/payment/result',
        title: '支付结果',
        requiresAuth: true
    },
    notifications: {
        path: '/notifications',
        title: '通知中心',
        requiresAuth: true
    },
    credit: {
        path: '/credit',
        title: '我的信用',
        requiresAuth: true
    }
};
