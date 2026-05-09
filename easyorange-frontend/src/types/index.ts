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
} from './product';

export type {
    Order,
    OrderStatus,
    PaymentMethod,
    CreateOrderRequest,
    OrderQueryParams,
    OrderDetail,
} from './order';

export type { ChatSession, ChatMessage } from './message';

export type { RequestOptions } from './api';

export {
    CONDITION_LABEL_MAP,
    STATUS_LABEL_MAP,
    PRODUCT_STATUS_CODE,
} from '@/constants/product';

export {
    ORDER_STATUS_CODE,
    ORDER_STATUS_LABEL,
    getOrderStatusLabel,
    getOrderStatusFromCode,
} from '@/constants/order';
