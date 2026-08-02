export const AVATAR_GRADIENTS = [
    'linear-gradient(135deg, #F97316, #FB923C)',
    'linear-gradient(135deg, #FB7185, #C39BD3)',
    'linear-gradient(135deg, #34D399, #10B981)',
    'linear-gradient(135deg, #FBBF24, #F97316)',
    'linear-gradient(135deg, #C39BD3, #D8B4FE)',
] as const;

/**
 * 基于字符串 ID 稳定地选取头像渐变色。
 * userId 为 UUID v7 字符串，不可用 Number() 转换（会得到 NaN）。
 */
export function pickAvatarGradient(id: string): string {
    const hash = Array.from(id).reduce((acc, ch) => acc + ch.charCodeAt(0), 0);
    return AVATAR_GRADIENTS[hash % AVATAR_GRADIENTS.length];
}
