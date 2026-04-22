/**
 * @fileoverview 头像工具函数
 * @description 提供头像显示的统一处理逻辑，支持首字母/首汉字显示
 */

export function getUserInitial(name?: string | null): string {
    if (!name) { return 'U'; }
    const trimmed = name.trim();
    if (!trimmed) { return 'U'; }

    const firstChar = trimmed.charAt(0);

    if (/[a-zA-Z]/.test(firstChar)) {
        return firstChar.toUpperCase();
    }

    return firstChar;
}

export function createAvatarElement(
    name: string,
    avatarUrl?: string | null,
    size?: 'small' | 'medium' | 'large'
): HTMLElement {
    const container = document.createElement('div');
    container.className = `user-avatar ${size ? `user-avatar--${size}` : ''}`;

    if (avatarUrl) {
        const img = document.createElement('img');
        img.src = avatarUrl;
        img.alt = `${name}头像`;
        img.loading = 'lazy';
        img.addEventListener('error', () => {
            container.innerHTML = '';
            container.appendChild(createInitialElement(name));
        }, { once: true });
        container.appendChild(img);
        return container;
    }

    container.appendChild(createInitialElement(name));
    return container;
}

function createInitialElement(name: string): HTMLElement {
    const span = document.createElement('span');
    span.className = 'user-avatar-initial';
    span.textContent = getUserInitial(name);
    return span;
}