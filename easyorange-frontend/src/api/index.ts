/**
 * @fileoverview API 模块 - 统一导出
 * @description 从子模块重新导出所有 API，保持向后兼容
 */

import { userApi } from './userApi.js';
import { productApi } from './productApi.js';
import { orderApi } from './orderApi.js';
import { messageApi } from './messageApi.js';
import { favoriteApi } from './favoriteApi.js';
import { uploadApi, uploadFile, uploadFiles } from './uploadApi.js';
import { reviewApi } from './reviewApi.js';
import { activityApi, ACTIVITY_STATUS, ACTIVITY_TYPE_NAMES, ACTIVITY_STATUS_NAMES, type Activity, type ActivityType, type ActivityStatus, type CreateActivityRequest } from './activityApi.js';
import { insuranceApi, INSURANCE_TYPE_NAMES, type Insurance, type InsuranceType, type InsuranceStatus, type ClaimStatus, calculatePremiumLocal } from './insuranceApi.js';
import { paymentApi, type PaymentInfo, type CreatePaymentRequest, type PaymentResponse, type PaymentStatusResponse } from './paymentApi.js';
import { statsApi, type PlatformStats } from './statsApi.js';

export { ApiClientError } from './core/request.js';

// Re-export all APIs as named exports
export {
    userApi,
    productApi,
    orderApi,
    messageApi,
    favoriteApi,
    uploadApi,
    reviewApi,
    activityApi,
    insuranceApi,
    paymentApi,
    statsApi,
    uploadFile,
    uploadFiles,
    calculatePremiumLocal,
    ACTIVITY_STATUS,
    ACTIVITY_TYPE_NAMES,
    ACTIVITY_STATUS_NAMES,
    INSURANCE_TYPE_NAMES
};

export type {
    Activity,
    ActivityType,
    ActivityStatus,
    CreateActivityRequest,
    Insurance,
    InsuranceType,
    InsuranceStatus,
    ClaimStatus,
    PaymentInfo,
    CreatePaymentRequest,
    PaymentResponse,
    PaymentStatusResponse,
    PlatformStats
};

// Default export for backward compatibility
const api = {
    user: userApi,
    product: productApi,
    products: productApi,
    order: orderApi,
    orders: orderApi,
    message: messageApi,
    messages: messageApi,
    favorite: favoriteApi,
    favorites: favoriteApi,
    upload: uploadApi,
    review: reviewApi,
    reviews: reviewApi,
    activity: activityApi,
    activities: activityApi,
    insurance: insuranceApi,
    payment: paymentApi,
    stats: statsApi
};

export default api;
