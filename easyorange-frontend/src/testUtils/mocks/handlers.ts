import { http, HttpResponse } from 'msw';

export const handlers = [
  // 商品列表
  http.get('/api/products', () => {
    return HttpResponse.json({
      code: 'A0000',
      message: 'success',
      data: {
        records: [],
        total: 0,
        current: 1,
        size: 20,
        pages: 0,
      },
      timestamp: Date.now(),
    });
  }),

  // 商品搜索
  http.get('/api/products/search', () => {
    return HttpResponse.json({
      code: 'A0000',
      message: 'success',
      data: {
        records: [],
        total: 0,
        current: 1,
        size: 20,
        pages: 0,
      },
      timestamp: Date.now(),
    });
  }),

  // 搜索建议
  http.get('/api/products/search/suggestions', () => {
    return HttpResponse.json({
      code: 'A0000',
      message: 'success',
      data: [],
      timestamp: Date.now(),
    });
  }),

  // 热搜
  http.get('/api/products/search/hot', () => {
    return HttpResponse.json({
      code: 'A0000',
      message: 'success',
      data: [],
      timestamp: Date.now(),
    });
  }),

  // Admin 商品列表
  http.get('/api/admin/products', () => {
    return HttpResponse.json({
      code: 'A0000',
      message: 'success',
      data: {
        records: [],
        total: 0,
        current: 1,
        size: 20,
        pages: 0,
      },
      timestamp: Date.now(),
    });
  }),

  // Admin 举报列表
  http.get('/api/admin/reports', () => {
    return HttpResponse.json({
      code: 'A0000',
      message: 'success',
      data: {
        records: [],
        total: 0,
        current: 1,
        size: 20,
        pages: 0,
      },
      timestamp: Date.now(),
    });
  }),

  // Admin 举报统计
  http.get('/api/admin/reports/stats', () => {
    return HttpResponse.json({
      code: 'A0000',
      message: 'success',
      data: {
        totalReports: 0,
        pendingReports: 0,
        resolvedReports: 0,
        dismissedReports: 0,
      },
      timestamp: Date.now(),
    });
  }),

  // Admin 处理举报
  http.put('/api/admin/reports/:id/handle', () => {
    return HttpResponse.json({
      code: 'A0000',
      message: 'success',
      data: null,
      timestamp: Date.now(),
    });
  }),

  // Admin 单个举报详情
  http.get('/api/admin/reports/:id', () => {
    return HttpResponse.json({
      code: 'A0000',
      message: 'success',
      data: {
        reportId: 1,
        productId: 100,
        productName: '测试商品',
        productImage: null,
        reporterId: 10,
        reporterName: '测试用户',
        reason: '虚假信息',
        status: 0,
        statusDesc: '待处理',
        handleResult: null,
        handleRemark: null,
        createTime: '2026-05-16 10:00:00',
        handleTime: null,
      },
      timestamp: Date.now(),
    });
  }),

  // Admin 审核日志
  http.get('/api/admin/products/:id/audit-logs', () => {
    return HttpResponse.json({
      code: 'A0000',
      message: 'success',
      data: [],
      timestamp: Date.now(),
    });
  }),
];
