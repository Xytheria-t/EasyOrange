export type UserType = '00' | '01';

export type UserStatus = 0 | 1 | 2;

export type ProductStatus = 0 | 1 | 2 | 3;

export interface AdminUser {
  userId: string;
  username: string;
  nickname?: string;
  email: string;
  phone: string | null;
  userType: UserType;
  status: UserStatus;
  createTime: string;
  productCount: number;
  orderCount: number;
}

export interface DashboardStats {
  totalUsers: number;
  newUsersToday: number;
  totalProducts: number;
  pendingReview: number;
  totalOrders: number;
  userGrowth: number;
  productGrowth: number;
  orderGrowth: number;
}

export interface PendingItems {
  pendingProducts: number;
  pendingReports: number;
  pendingRefunds: number;
}

export interface RecentUser {
  userId: string;
  username: string;
  nickname?: string;
  createTime: string;
}

export interface RecentProduct {
  id: string;
  name: string;
  createTime: string;
}

export interface AdminProduct {
  id: string;
  name: string;
  price: number;
  originalPrice?: number;
  conditionLevel?: number;
  images: string[];
  categoryName: string;
  sellerId: string;
  sellerName: string;
  status: ProductStatus;
  createTime: string;
  viewCount: number;
  favoriteCount: number;
  description?: string;
  location?: string;
}

export interface AdminUserQuery {
  page: number;
  size: number;
  status?: UserStatus;
  keyword?: string;
  sortBy?: 'createTime' | 'productCount' | 'orderCount';
}

export interface AdminProductQuery {
  page: number;
  size: number;
  status?: ProductStatus;
  categoryId?: string;
  keyword?: string;
}

export interface UpdateStatusRequest {
  status: number;
  reason?: string;
}

export interface PageResponse<T> {
  code: number;
  data: {
    records: T[];
    total: number;
    page: number;
    size: number;
    pages: number;
  };
}

export interface ActionResponse {
  code: number;
  data: {
    success: boolean;
    message: string;
  };
}
