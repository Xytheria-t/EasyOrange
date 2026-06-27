import type { Product, PageResult } from '@/types';
import type {
  AdminProduct,
  AdminReport,
  AdminReportQuery,
  AdminProductQuery,
  ReportStatsResponse,
} from '@/admin/types/admin';

export function createMockProduct(overrides: Partial<Product> = {}): Product {
  return {
    id: '1',
    title: '测试商品',
    description: '这是一个测试商品描述',
    price: 100,
    originalPrice: 150,
    categoryId: 1,
    categoryName: '电子产品',
    condition: 1,
    conditionLevel: 1,
    status: 'ONLINE',
    images: ['https://example.com/image.jpg'],
    location: '北京',
    views: 50,
    favorites: 10,
    sellerId: 'user1',
    sellerName: '资产方小明',
    sellerAvatar: null,
    sellerRating: 4.5,
    createTime: '2026-05-16 10:00:00',
    updateTime: '2026-05-16 10:00:00',
    ...overrides,
  };
}

export function createMockProductPage(overrides: Partial<PageResult<Product>> = {}): PageResult<Product> {
  return {
    records: [createMockProduct()],
    total: 1,
    current: 1,
    size: 20,
    pages: 1,
    ...overrides,
  };
}

export function createMockAdminProduct(overrides: Partial<AdminProduct> = {}): AdminProduct {
  return {
    productId: 1,
    name: '测试商品',
    description: '描述',
    price: 100,
    originalPrice: 150,
    stock: 10,
    status: 4,
    statusDesc: '待审核',
    conditionLevel: 1,
    location: '北京',
    contactMethod: null,
    images: [],
    mainImage: null,
    categoryId: 1,
    categoryName: '电子产品',
    sellerId: 10,
    sellerName: '资产方小明',
    sellerAvatar: null,
    viewCount: 50,
    createTime: '2026-05-16 10:00:00',
    updateTime: '2026-05-16 10:00:00',
    ...overrides,
  };
}

export function createMockAdminReport(overrides: Partial<AdminReport> = {}): AdminReport {
  return {
    reportId: 1,
    productId: 100,
    productName: '被举报商品',
    productImage: null,
    reporterId: 10,
    reporterName: '举报人',
    reason: '虚假信息',
    status: 0,
    statusDesc: '待处理',
    handleResult: null,
    handleRemark: null,
    createTime: '2026-05-16 10:00:00',
    handleTime: null,
    ...overrides,
  };
}

export function createMockAdminProductQuery(overrides: Partial<AdminProductQuery> = {}): AdminProductQuery {
  return {
    pageNum: 1,
    pageSize: 20,
    ...overrides,
  };
}

export function createMockAdminReportQuery(overrides: Partial<AdminReportQuery> = {}): AdminReportQuery {
  return {
    pageNum: 1,
    pageSize: 20,
    ...overrides,
  };
}

export function createMockReportStats(overrides: Partial<ReportStatsResponse> = {}): ReportStatsResponse {
  return {
    totalReports: 10,
    pendingReports: 3,
    resolvedReports: 5,
    dismissedReports: 2,
    ...overrides,
  };
}
