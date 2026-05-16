import { describe, it, expect } from 'vitest';
import { validator } from './validator';

describe('ValidatorUtils', () => {
  describe('isValidEmail', () => {
    it('returns true for valid email', () => {
      expect(validator.isValidEmail('test@example.com')).toBe(true);
      expect(validator.isValidEmail('user+tag@domain.co')).toBe(true);
    });

    it('returns false for invalid email', () => {
      expect(validator.isValidEmail('')).toBe(false);
      expect(validator.isValidEmail('not-email')).toBe(false);
      expect(validator.isValidEmail('@domain.com')).toBe(false);
    });
  });

  describe('isStrongPassword', () => {
    it('returns true for strong password', () => {
      expect(validator.isStrongPassword('Abc123')).toBe(true);
      expect(validator.isStrongPassword('Str0ng!Pass')).toBe(true);
    });

    it('returns false for weak password', () => {
      expect(validator.isStrongPassword('abc')).toBe(false);
      expect(validator.isStrongPassword('123456')).toBe(false);
      expect(validator.isStrongPassword('abcdef')).toBe(false);
      expect(validator.isStrongPassword('ABCDEF')).toBe(false);
    });
  });

  describe('getPasswordStrength', () => {
    it('returns 0 for empty password', () => {
      expect(validator.getPasswordStrength('')).toBe(0);
    });

    it('returns 1 for password with length < 6', () => {
      expect(validator.getPasswordStrength('ab')).toBe(1);
    });

    it('returns 2 for password with length < 8', () => {
      expect(validator.getPasswordStrength('abc123')).toBe(2);
    });

    it('returns 3 for password with length < 12', () => {
      expect(validator.getPasswordStrength('abc12345')).toBe(3);
    });

    it('returns 4 for password with length >= 12', () => {
      expect(validator.getPasswordStrength('abc123456789')).toBe(4);
    });
  });

  describe('isValidUsername', () => {
    it('returns true for valid username', () => {
      expect(validator.isValidUsername('user_123')).toBe(true);
      expect(validator.isValidUsername('abc')).toBe(true);
    });

    it('returns false for invalid username', () => {
      expect(validator.isValidUsername('ab')).toBe(false);
      expect(validator.isValidUsername('user name')).toBe(false);
      expect(validator.isValidUsername('user@name')).toBe(false);
    });
  });

  describe('isValidPhone', () => {
    it('returns true for valid phone number', () => {
      expect(validator.isValidPhone('13800138000')).toBe(true);
      expect(validator.isValidPhone('15912345678')).toBe(true);
    });

    it('returns false for invalid phone number', () => {
      expect(validator.isValidPhone('123')).toBe(false);
      expect(validator.isValidPhone('12345678901')).toBe(false);
      expect(validator.isValidPhone('')).toBe(false);
    });
  });

  describe('isValidStudentId', () => {
    it('returns true for numeric student ID', () => {
      expect(validator.isValidStudentId('2024001')).toBe(true);
    });

    it('returns false for empty or non-numeric', () => {
      expect(validator.isValidStudentId('')).toBe(false);
      expect(validator.isValidStudentId('abc')).toBe(false);
    });
  });

  describe('getErrorMessage', () => {
    it('returns empty string for valid data', () => {
      expect(validator.getErrorMessage('username', 'valid_user')).toBe('');
      expect(validator.getErrorMessage('email', 'test@example.com')).toBe('');
    });

    it('returns error for empty required field', () => {
      expect(validator.getErrorMessage('username', '')).toBe('用户名不能为空');
    });

    it('returns error for short username', () => {
      expect(validator.getErrorMessage('username', 'ab')).toBe('用户名至少需要 3 个字符');
    });

    it('returns error for invalid email', () => {
      expect(validator.getErrorMessage('email', 'bad')).toBe('请输入有效的邮箱地址');
    });

    it('returns error for weak password', () => {
      expect(validator.getErrorMessage('password', 'abc')).toBe('密码至少需要 6 个字符');
    });

    it('returns empty for unknown field', () => {
      expect(validator.getErrorMessage('unknown', 'value')).toBe('');
    });
  });
});
