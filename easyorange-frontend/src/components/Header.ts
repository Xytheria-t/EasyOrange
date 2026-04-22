/**
 * @fileoverview 公共导航栏组件 - 悬浮岛屿设计
 * @description 统一管理所有页面的导航栏，包含用户菜单功能
 * @version 3.0.0
 */

import { storage, toast } from '../utils/index.js';
import { navigation } from '../app/navigation.js';
import {
    AUTH_SESSION_CHANGE_EVENT,
    getStoredToken,
    getStoredUser,
    logout as logoutSession,
    type AuthSessionDetail
} from '../app/authSession.js';

export interface User {
    username?: string;
    nickname?: string;
    [key: string]: unknown;
}

interface HeaderElements {
    userMenu: HTMLElement | null;
    userAvatarBtn: HTMLElement | null;
    userAvatarImg: HTMLImageElement | null;
    userName: HTMLElement | null;
    userDropdown: HTMLElement | null;
    logoutBtn: HTMLElement | null;
    loginBtn: HTMLElement | null;
    publishBtn: HTMLElement | null;
    notificationBtn: HTMLElement | null;
    navLinks: NodeListOf<HTMLElement> | null;
}

const FLOATING_NAV_TEMPLATE = `
<nav class="floating-nav" role="navigation" aria-label="主导航">
    <div class="floating-nav__ambient"></div>
    <div class="floating-nav__glow"></div>
    <div class="floating-nav__inner">
        <a href="/" class="floating-nav__brand" aria-label="EasyOrange首页">
            <div class="floating-nav__logo">
                <svg viewBox="0 0 40 40" fill="none">
                    <circle cx="20" cy="20" r="18" stroke="url(#logoGradientNav)" stroke-width="2"/>
                    <path d="M12 20c0-4.4 3.6-8 8-8s8 3.6 8 8-3.6 8-8 8" stroke="url(#logoGradientNav)" stroke-width="2" stroke-linecap="round"/>
                    <circle cx="20" cy="20" r="3" fill="url(#logoGradientNav)"/>
                    <defs>
                        <linearGradient id="logoGradientNav" x1="0%" y1="0%" x2="100%" y2="100%">
                            <stop offset="0%" stop-color="#F97316"/>
                            <stop offset="50%" stop-color="#EA580C"/>
                            <stop offset="100%" stop-color="#F43F5E"/>
                        </linearGradient>
                    </defs>
                </svg>
            </div>
            <span class="floating-nav__brand-name">EasyOrange</span>
        </a>

        <div class="floating-nav__divider"></div>

        <div class="floating-nav__links">
            <a href="/" class="floating-nav__link active" data-nav="home">
                <span class="floating-nav__link-text">首页</span>
            </a>
            <a href="/products.html" class="floating-nav__link" data-nav="products">
                <span class="floating-nav__link-text">商品</span>
            </a>
        </div>

        <div class="floating-nav__actions">
            <button class="floating-nav__icon-btn" id="notificationBtn" aria-label="通知">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                    <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                </svg>
                <span class="notification-dot"></span>
            </button>

            <div class="floating-nav__user" id="userMenu">
                <button class="floating-nav__user-btn" id="userAvatarBtn">
                    <div class="floating-nav__user-avatar" id="userAvatar">
                        <span id="userInitial"></span>
                    </div>
                    <span class="floating-nav__user-name" id="userName"></span>
                    <svg class="floating-nav__user-dropdown-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="6 9 12 15 18 9"/>
                    </svg>
                </button>
                <div class="floating-nav__user-menu" id="userDropdown">
                    <a href="/profile.html" class="floating-nav__menu-item">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                            <circle cx="12" cy="7" r="4"/>
                        </svg>
                        <span>个人中心</span>
                    </a>
                    <a href="/favorites.html" class="floating-nav__menu-item">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                        </svg>
                        <span>我的收藏</span>
                    </a>
                    <div class="floating-nav__menu-divider"></div>
                    <button class="floating-nav__menu-item floating-nav__menu-item--logout" id="logoutBtn">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                            <polyline points="16 17 21 12 16 7"/>
                            <line x1="21" y1="12" x2="9" y2="12"/>
                        </svg>
                        <span>退出登录</span>
                    </button>
                </div>
            </div>

            <button class="floating-nav__login-btn is-hidden" id="loginBtn">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                </svg>
                <span>登录</span>
            </button>

            <a href="/publish.html" class="floating-nav__publish-btn">
                <svg class="plus-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="12" y1="5" x2="12" y2="19"/>
                    <line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
                <span>发布</span>
            </a>
        </div>
    </div>
</nav>
`;

class Header {
    private initialized: boolean;
    private _documentClickHandler: ((e: MouseEvent) => void) | null;
    private _authSessionHandler: ((e: Event) => void) | null;

    constructor() {
        this.initialized = false;
        this._documentClickHandler = null;
        this._authSessionHandler = null;
    }

    private cacheElements(): void {
        const publishBtn = document.querySelector('.floating-nav__publish-btn');
        const elements: HeaderElements = {
            userMenu: document.getElementById('userMenu'),
            userAvatarBtn: document.getElementById('userAvatarBtn'),
            userAvatarImg: document.getElementById('userAvatarImg') as HTMLImageElement | null,
            userName: document.getElementById('userName'),
            userDropdown: document.getElementById('userDropdown'),
            logoutBtn: document.getElementById('logoutBtn'),
            loginBtn: document.getElementById('loginBtn'),
            publishBtn: publishBtn as HTMLElement | null,
            notificationBtn: document.getElementById('notificationBtn'),
            navLinks: document.querySelectorAll('.floating-nav__link')
        };
        void elements;
    }

    init(): void {
        if (this.initialized) {return;}

        if (document.querySelector('.floating-nav')) {
            this.cacheElements();
            this.bindEvents();
            this.checkLoginStatus();
            this.initialized = true;
            return;
        }

        if (document.querySelector('.unified-header')) {
            document.querySelector('.unified-header')?.remove();
        }

        this.injectFloatingNav();
        this.cacheElements();
        this.bindEvents();
        this.checkLoginStatus();
        this.initialized = true;
    }

    private injectFloatingNav(): void {
        const container = document.createElement('div');
        container.id = 'floating-nav-container';
        container.innerHTML = FLOATING_NAV_TEMPLATE;
        document.body.appendChild(container);
    }

    private checkLoginStatus(): void {
        const token = getStoredToken();
        const user = getStoredUser() as User | null;

        if (token && user) {
            this.showLoggedInState(user);
        } else {
            this.showLoggedOutState();
        }
    }

    private showLoggedInState(user: User): void {
        const userMenu = document.querySelector('.floating-nav__user');
        const loginBtn = document.getElementById('loginBtn');
        const userName = document.getElementById('userName');
        const userInitial = document.getElementById('userInitial');

        if (userMenu) {
            userMenu.classList.remove('is-hidden');
        }
        if (loginBtn) {
            loginBtn.classList.add('is-hidden');
        }
        if (userName) {
            const displayName = user.username || user.nickname || '用户';
            userName.textContent = displayName;
            if (userInitial) {
                userInitial.textContent = (displayName.charAt(0)).toUpperCase();
            }
        }
    }

    private showLoggedOutState(): void {
        const userMenu = document.querySelector('.floating-nav__user');
        const loginBtn = document.getElementById('loginBtn');

        if (userMenu) {
            userMenu.classList.add('is-hidden');
        }
        if (loginBtn) {
            loginBtn.classList.remove('is-hidden');
        }
    }

    private bindEvents(): void {
        const notificationBtn = document.getElementById('notificationBtn');
        if (notificationBtn) {
            notificationBtn.addEventListener('click', () => {
                navigation.go('messages');
            });
        }

        const userAvatarBtn = document.getElementById('userAvatarBtn');
        const userMenu = document.getElementById('userMenu');

        if (userAvatarBtn && userMenu) {
            userAvatarBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                const isActive = userMenu.classList.toggle('active');
                userAvatarBtn.setAttribute('aria-expanded', String(isActive));
            });

            this._documentClickHandler = (e: MouseEvent) => {
                if (userMenu && !userMenu.contains(e.target as Node)) {
                    userMenu.classList.remove('active');
                    userAvatarBtn.setAttribute('aria-expanded', 'false');
                }
            };
            document.addEventListener('click', this._documentClickHandler);
        }

        if (!this._authSessionHandler) {
            this._authSessionHandler = (event: Event) => {
                const detail = (event as CustomEvent<AuthSessionDetail>).detail;
                if (detail?.isAuthenticated && detail.user) {
                    this.showLoggedInState(detail.user as User);
                    return;
                }
                this.showLoggedOutState();
            };
            window.addEventListener(AUTH_SESSION_CHANGE_EVENT, this._authSessionHandler);
        }

        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', async (e) => {
                e.preventDefault();
                await this.logout();
            });
        }

        const loginBtn = document.getElementById('loginBtn');
        if (loginBtn) {
            loginBtn.addEventListener('click', () => {
                const authContainer = document.getElementById('authContainer');
                if (authContainer) {
                    authContainer.classList.add('active');
                }
            });
        }
    }

    private async logout(): Promise<void> {
        await logoutSession();
        toast.success('已退出登录');
        navigation.replace('home');
    }

    setActiveNav(navKey: string): void {
        const navLinks = document.querySelectorAll('.floating-nav__link');
        navLinks.forEach(link => {
            const isActive = (link as HTMLElement).dataset.nav === navKey;
            link.classList.toggle('active', isActive);
        });
    }

    updateUser(user: User): void {
        if (user) {
            storage.set('user', user);
            this.showLoggedInState(user);
        }
    }

    destroy(): void {
        if (this._documentClickHandler) {
            document.removeEventListener('click', this._documentClickHandler);
            this._documentClickHandler = null;
        }
        if (this._authSessionHandler) {
            window.removeEventListener(AUTH_SESSION_CHANGE_EVENT, this._authSessionHandler);
            this._authSessionHandler = null;
        }

        const navContainer = document.getElementById('floating-nav-container');
        if (navContainer) {
            navContainer.remove();
        }

        this.initialized = false;
    }
}

export const header = new Header();
export default header;
