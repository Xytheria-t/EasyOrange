/**
 * @fileoverview 工具函数模块 - React 版本
 * @description 只提供 React 应用需要的工具函数
 */

// 存储工具
export { storage, StorageUtils } from './storage';
export type { StorageItem } from './storage';

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
} from './format';

// 类名工具
export { cn } from './cn';

// 模态框管理
export { modalManager, ModalManager } from './modalManager';

// DOM 操作
export { dom, DomUtils } from './dom';

// 函数工具
export { debounce, throttle } from './function';
export type { DebounceOptions } from './function';

// 商品工具
export { calculateDiscount, getConditionNameFromString, isHotProduct, normalizeProduct } from './product';

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
} from './element';
export type { ElementOptions } from './element';

// 错误处理
export { errorHandler } from './errorHandler';

// 验证工具
export { validator, ValidatorUtils } from './validator';
