export type { Result, PageResult, ApiCode } from './common';
export { isSuccessCode } from './common';

export type { User, LoginRequest, RegisterRequest, LoginResponse } from './user';

export type {
    Product,
    ProductStatus,
    ProductQueryParams,
    CreateProductRequest,
    UpdateProductRequest,
    Category,
    Favorite,
    FavoriteProduct,
    FacetBucket,
    ProductSearchResult,
    ProductSearchParams,
} from './product';

export type {
    Order,
    OrderStatus,
    PaymentMethod,
    OrderItemVO,
    CreateOrderRequest,
    OrderQueryParams,
    OrderDetail,
} from './order';

export type { ChatSession, ChatMessage } from './message';

export type { RequestOptions } from './api';

export type { NotificationItem, UnreadCount } from './notification';
