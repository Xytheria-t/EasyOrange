/**
 * @fileoverview 表单验证工具模块
 * @description 提供常用的表单验证功能
 * @version 1.0.0
 */

/**
 * 表单验证工具类
 */
class ValidatorUtils {
    /**
     * 验证邮箱格式
     */
    isValidEmail(email: string): boolean {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    /**
     * 验证密码强度
     */
    isStrongPassword(password: string): boolean {
        return password.length >= 6;
    }

    /**
     * 获取密码强度等级
     */
    getPasswordStrength(password: string): 0 | 1 | 2 | 3 | 4 {
        if (password.length === 0) {return 0;}
        if (password.length < 6) {return 1;}
        if (password.length < 8) {return 2;}
        if (password.length < 12) {return 3;}
        return 4;
    }

    /**
     * 验证用户名
     */
    isValidUsername(username: string): boolean {
        return username.length >= 3 && username.length <= 20;
    }

    /**
     * 验证学号
     */
    isValidStudentId(studentId: string): boolean {
        const trimmed = studentId?.trim() ?? '';
        return trimmed.length > 0 && /^\d+$/.test(trimmed);
    }

    /**
     * 验证真实姓名
     */
    isValidRealName(realName: string): boolean {
        const trimmed = realName?.trim() ?? '';
        return trimmed.length >= 2 && trimmed.length <= 10;
    }

    /**
     * 验证手机号
     */
    isValidPhone(phone: string): boolean {
        const phoneRegex = /^1[3-9]\d{9}$/;
        return phoneRegex.test(phone);
    }

    /**
     * 获取验证错误消息
     */
    getErrorMessage(field: string, value: string): string {
        const trimmedValue = value?.trim() ?? '';

        switch (field) {
            case 'username':
                if (!trimmedValue) {return '用户名不能为空';}
                if (trimmedValue.length < 3) {return '用户名至少需要 3 个字符';}
                if (trimmedValue.length > 20) {return '用户名不能超过 20 个字符';}
                return '';

            case 'password':
                if (!value) {return '密码不能为空';}
                if (value.length < 6) {return '密码至少需要 6 个字符';}
                return '';

            case 'email':
                if (!trimmedValue) {return '邮箱不能为空';}
                if (!this.isValidEmail(trimmedValue)) {return '请输入有效的邮箱地址';}
                return '';

            case 'studentId':
                if (!trimmedValue) {return '学号不能为空';}
                if (!/^\d+$/.test(trimmedValue)) {return '学号必须为数字';}
                return '';

            case 'realName':
                if (!trimmedValue) {return '真实姓名不能为空';}
                if (trimmedValue.length < 2) {return '真实姓名至少需要 2 个字符';}
                if (trimmedValue.length > 10) {return '真实姓名不能超过 10 个字符';}
                return '';

            case 'phone':
                if (!trimmedValue) {return '手机号不能为空';}
                if (!this.isValidPhone(trimmedValue)) {return '请输入有效的手机号';}
                return '';

            default:
                return '';
        }
    }
}

// 导出单例
const validator = new ValidatorUtils();

export { ValidatorUtils, validator };
export default validator;
