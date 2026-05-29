import { describe, it, expect } from 'vitest';
import { cn } from './cn';

describe('cn', () => {
  it('merges class names', () => {
    expect(cn('foo', 'bar')).toBe('foo bar');
  });

  it('handles conditional classes', () => {
    const isHidden = false;
    expect(cn('base', isHidden && 'hidden', 'visible')).toBe('base visible');
  });

  it('handles array of classes', () => {
    expect(cn(['a', 'b'], 'c')).toBe('a b c');
  });

  it('resolves tailwind conflicts via twMerge', () => {
    const result = cn('px-4', 'px-2');
    expect(result).toBe('px-2');
  });

  it('returns empty string for no args', () => {
    expect(cn()).toBe('');
  });
});
