/** 头像渐变：取值来自 admin.css 的 `--admin-gradient-*`，这里只做引用。 */
export const AVATAR_GRADIENTS = [
    'var(--admin-gradient-1)',
    'var(--admin-gradient-2)',
    'var(--admin-gradient-3)',
    'var(--admin-gradient-4)',
    'var(--admin-gradient-5)',
] as const;

/**
 * 基于字符串 ID 稳定地选取头像渐变色。
 * userId 为 UUID v7 字符串，不可用 Number() 转换（会得到 NaN）。
 */
export function pickAvatarGradient(id: string): string {
    const hash = Array.from(id).reduce((acc, ch) => acc + ch.charCodeAt(0), 0);
    return AVATAR_GRADIENTS[hash % AVATAR_GRADIENTS.length];
}
