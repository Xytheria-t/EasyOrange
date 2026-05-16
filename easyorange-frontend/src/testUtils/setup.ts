import '@testing-library/jest-dom/vitest';

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
