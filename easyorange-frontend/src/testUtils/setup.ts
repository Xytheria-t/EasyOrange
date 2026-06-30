import { afterEach } from 'vitest';
import '@testing-library/jest-dom/vitest';
import { server } from './mocks/server';

// 全局 MSW 生命周期：所有测试自动 mock HTTP 请求
server.listen({ onUnhandledRequest: 'error' });
afterEach(() => server.resetHandlers());

// Node.js 26 内置 localStorage 需要 --localstorage-file flag，jsdom 环境不兼容
// 手动注入 localStorage mock 以确保兼容性
if (typeof globalThis.localStorage === 'undefined') {
  const storage = new Map<string, string>();
  Object.defineProperty(globalThis, 'localStorage', {
    value: {
      getItem: (key: string) => storage.get(key) ?? null,
      setItem: (key: string, value: string) => storage.set(key, value),
      removeItem: (key: string) => storage.delete(key),
      clear: () => storage.clear(),
      get length() { return storage.size; },
      key: (index: number) => Array.from(storage.keys())[index] ?? null,
    },
    writable: false,
    configurable: true,
  });
}

// jsdom does not implement PointerEvent capture APIs used by Radix UI primitives
if (typeof Element.prototype.hasPointerCapture !== 'function') {
  Element.prototype.hasPointerCapture = () => false;
}
if (typeof Element.prototype.setPointerCapture !== 'function') {
  Element.prototype.setPointerCapture = () => {};
}
if (typeof Element.prototype.releasePointerCapture !== 'function') {
  Element.prototype.releasePointerCapture = () => {};
}

// jsdom does not implement scrollIntoView — used by Radix Select/Dialog for focus management
if (typeof Element.prototype.scrollIntoView !== 'function') {
  Element.prototype.scrollIntoView = () => {};
}

// jsdom does not implement ResizeObserver — used by Radix Checkbox/Tooltip primitives
if (typeof globalThis.ResizeObserver === 'undefined') {
  globalThis.ResizeObserver = class ResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
}

if (typeof globalThis.sessionStorage === 'undefined') {
  const storage = new Map<string, string>();
  Object.defineProperty(globalThis, 'sessionStorage', {
    value: {
      getItem: (key: string) => storage.get(key) ?? null,
      setItem: (key: string, value: string) => storage.set(key, value),
      removeItem: (key: string) => storage.delete(key),
      clear: () => storage.clear(),
      get length() { return storage.size; },
      key: (index: number) => Array.from(storage.keys())[index] ?? null,
    },
    writable: false,
    configurable: true,
  });
}
