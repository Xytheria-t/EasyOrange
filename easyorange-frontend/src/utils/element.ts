/**
 * @fileoverview DOM 元素创建工具模块
 * @description 提供常用的元素创建功能
 */

export interface ElementOptions {
    className?: string;
    id?: string;
    text?: string;
    html?: string;
    attributes?: Record<string, string>;
    children?: (HTMLElement | null)[];
    onClick?: (e: MouseEvent) => void;
}

export function createElement<K extends keyof HTMLElementTagNameMap>(
    tag: K,
    options: ElementOptions = {}
): HTMLElementTagNameMap[K] {
    const element = document.createElement(tag);
    if (options.className) {element.className = options.className;}
    if (options.id) {element.id = options.id;}
    if (options.text) {element.textContent = options.text;}
    if (options.html) {element.innerHTML = options.html;}
    if (options.attributes) {
        Object.entries(options.attributes).forEach(([key, value]) => {
            element.setAttribute(key, value);
        });
    }
    if (options.children) {
        options.children.forEach(child => { if (child) {element.appendChild(child);} });
    }
    if (options.onClick) {element.addEventListener('click', options.onClick as EventListener);}
    return element;
}

export function createImage(
    src: string,
    alt: string,
    options: { className?: string; lazyLoad?: boolean; onError?: () => void; onLoad?: () => void; } = {}
): HTMLImageElement {
    const img = document.createElement('img');
    img.alt = alt;
    if (options.lazyLoad) {
        img.dataset.src = src;
        img.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400"%3E%3Crect fill="%23f5f5f5" width="400" height="400"/%3E%3Crect fill="%23e0e0e0" x="150" y="150" width="100" height="100" rx="8"/%3E%3C/svg%3E';
        img.loading = 'lazy';
    } else {
        img.src = src;
    }
    if (options.className) {img.className = options.className;}
    img.onerror = () => {
        img.src = 'https://dummyimage.com/400x400/cccccc/999999.png&text=Image+Error';
        img.classList.add('image-error');
        options.onError?.();
    };
    img.onload = () => { options.onLoad?.(); };
    return img;
}

export function createButton(
    text: string,
    options: { className?: string; type?: 'button' | 'submit' | 'reset'; disabled?: boolean; onClick?: (e: MouseEvent) => void; icon?: HTMLElement; } = {}
): HTMLButtonElement {
    const button = document.createElement('button');
    button.type = options.type || 'button';
    button.textContent = text;
    if (options.className) {button.className = options.className;}
    if (options.disabled) {button.disabled = options.disabled;}
    if (options.icon) {button.insertBefore(options.icon, button.firstChild);}
    if (options.onClick) {button.addEventListener('click', options.onClick);}
    return button;
}

export function createSvgIcon(
    path: string,
    options: { className?: string; viewBox?: string; fill?: string; stroke?: string; strokeWidth?: number; } = {}
): SVGSVGElement {
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', options.viewBox || '0 0 24 24');
    if (options.className) {svg.classList.add(options.className);}
    if (options.fill) {svg.setAttribute('fill', options.fill);}
    if (options.stroke) {svg.setAttribute('stroke', options.stroke);}
    if (options.strokeWidth) {svg.setAttribute('stroke-width', options.strokeWidth.toString());}
    const pathEl = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    pathEl.setAttribute('d', path);
    svg.appendChild(pathEl);
    return svg;
}

export function createFragment(children?: (HTMLElement | null)[]): DocumentFragment {
    const fragment = document.createDocumentFragment();
    if (children) {
        children.forEach(child => { if (child) {fragment.appendChild(child);} });
    }
    return fragment;
}

export function appendChildren(parent: HTMLElement, children: (HTMLElement | null)[]): void {
    parent.appendChild(createFragment(children));
}

export function clearChildren(element: HTMLElement): void {
    while (element.firstChild) {element.removeChild(element.firstChild);}
}

export function createEmptyState(
    message: string,
    options: { icon?: string; className?: string } = {}
): HTMLElement {
    const { icon, className } = options;
    const container = createElement('div', {
        className: `empty-state ${className || ''}`.trim()
    });

    if (icon) {
        const iconContainer = createElement('div', {
            className: 'empty-state-icon',
            html: icon
        });
        container.appendChild(iconContainer);
    }

    const messageEl = createElement('p', {
        className: 'empty-state-message',
        text: message
    });
    container.appendChild(messageEl);

    return container;
}
