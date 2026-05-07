/**
 * @fileoverview API 模块 - 统一导出
 * @description 从子模块重新导出所有 API，保持向后兼容
 */

import { userApi } from './userApi';
import { productApi } from './productApi';
import { orderApi } from './orderApi';
import { messageApi } from './messageApi';
import { favoriteApi } from './favoriteApi';
import { uploadApi, uploadFile, uploadFiles } from './uploadApi';
import { reviewApi } from './reviewApi';
import { activityApi, ACTIVITY_STATUS, ACTIVITY_TYPE_NAMES, ACTIVITY_STATUS_NAMES, type Activity, type ActivityType, type ActivityStatus, type CreateActivityRequest } from './activityApi';
import { insuranceApi, INSURANCE_TYPE_NAMES, type Insurance, type InsuranceType, type InsuranceStatus, type ClaimStatus, calculatePremiumLocal } from './insuranceApi';
import { paymentApi, type PaymentInfo, type CreatePaymentRequest, type PaymentResponse, type PaymentStatusResponse } from './paymentApi';
import { statsApi, type PlatformStats } from './statsApi';

export { ApiClientError } from './core/request';

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
