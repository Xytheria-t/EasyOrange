/**
 * @fileoverview 工具函数模块 - React 版本
 * @description 只提供 React 应用需要的工具函数
 */

// 存储工具
export { storage, StorageUtils } from './storage.js';
export type { StorageItem } from './storage.js';

// 格式化工具
export {
  formatCurrency,
  formatDate,
  formatRelativeTime,
  formatPrice,
  formatCondition,
  escapeHtml,
  parseQueryString,
  buildQueryString
} from './format.js';

// 类名工具
export { cn } from './cn.js';

// 模态框管理
export { modalManager, ModalManager } from './modalManager.js';

// DOM 操作
export { dom, DomUtils } from './dom.js';

// 函数工具
export { debounce, throttle } from './function.js';
export type { DebounceOptions } from './function.js';

// 商品工具
export { calculateDiscount, getConditionNameFromString, isHotProduct, normalizeProduct } from './product.js';

// 元素创建工具
export {
  createElement,
  createImage,
  createButton,
  createSvgIcon,
  createFragment,
  appendChildren,
  clearChildren,
  createEmptyState
} from './element.js';
export type { ElementOptions } from './element.js';

// 头像工具
export { getUserInitial, createAvatarElement } from './avatar.js';

// 网络工具
export { network, NetworkUtils } from './network.js';

// 错误处理
export { errorHandler } from './errorHandler.js';

// 验证工具
export { validator, ValidatorUtils } from './validator.js';
