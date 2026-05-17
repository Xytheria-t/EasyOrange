import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useColumnCount } from './useColumnCount';

describe('useColumnCount', () => {
  beforeEach(() => {
    // Ensure we start with a clean window size
    Object.defineProperty(window, 'innerWidth', {
      value: 1400,
      configurable: true,
      writable: true,
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns 4 for desktop width > 1200', () => {
    Object.defineProperty(window, 'innerWidth', { value: 1400, configurable: true });
    const { result } = renderHook(() => useColumnCount());
    expect(result.current).toBe(4);
  });

  it('returns 3 for medium width (768 < width <= 1200)', () => {
    Object.defineProperty(window, 'innerWidth', { value: 1024, configurable: true });
    const { result } = renderHook(() => useColumnCount());
    expect(result.current).toBe(3);
  });

  it('returns 2 for small width (480 < width <= 768)', () => {
    Object.defineProperty(window, 'innerWidth', { value: 600, configurable: true });
    const { result } = renderHook(() => useColumnCount());
    expect(result.current).toBe(2);
  });

  it('returns 2 for mobile width (<= 480)', () => {
    Object.defineProperty(window, 'innerWidth', { value: 375, configurable: true });
    const { result } = renderHook(() => useColumnCount());
    expect(result.current).toBe(2);
  });

  it('updates on window resize', () => {
    Object.defineProperty(window, 'innerWidth', { value: 1400, configurable: true });
    const { result } = renderHook(() => useColumnCount());
    expect(result.current).toBe(4);

    act(() => {
      Object.defineProperty(window, 'innerWidth', { value: 700, configurable: true });
      window.dispatchEvent(new Event('resize'));
    });

    expect(result.current).toBe(2);
  });

  it('accepts custom breakpoints', () => {
    const customBreakpoints = [
      { maxWidth: 600, columns: 1 },
      { maxWidth: 1200, columns: 2 },
    ];

    Object.defineProperty(window, 'innerWidth', { value: 800, configurable: true });
    const { result } = renderHook(() => useColumnCount(customBreakpoints));
    expect(result.current).toBe(2);
  });
});
