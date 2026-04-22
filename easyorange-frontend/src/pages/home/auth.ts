/**
 * @fileoverview 首页认证模块
 * @description 处理用户登录、注册和认证相关逻辑
 */

import { userApi, ApiClientError } from '../../api/index.js';
import { storage, toast, validator, dom } from '../../utils/index.js';
import { navigation } from '../../app/navigation.js';
import { getStoredToken, getStoredUser, setSession } from '../../app/authSession.js';
import Modal from '../../components/Modal.js';

/** 用户信息接口 */
export interface UserInfo {
    id: number;
    username: string;
    email: string;
    phone: string;
    studentId: string;
    realName: string;
    [key: string]: unknown;
}

/** 登录响应数据 */
interface LoginResponseData {
    token: string;
    user: UserInfo;
}

/** 认证模块 DOM 元素 */
interface AuthElements {
    loginBtn: HTMLElement | null;
    authContainer: HTMLElement | null;
    loginForm: HTMLFormElement | null;
    registerForm: HTMLFormElement | null;
    authClose: HTMLElement | null;
}

/**
 * 认证管理器
 */
export class AuthManager {
    private elements: AuthElements;
    private initialized: boolean;

    constructor() {
        this.elements = {
            loginBtn: null,
            authContainer: null,
            loginForm: null,
            registerForm: null,
            authClose: null
        };
        this.initialized = false;
    }

    /**
     * 初始化认证模块
     */
    init(): void {
        if (this.initialized) {return;}
        
        this.cacheElements();
        this.bindEvents();
        this.checkAuthStatus();
        this.initialized = true;
        
        // 暴露全局函数供 index.html 的 onclick 调用
        (window as Window & { __openAuth?: () => void }).__openAuth = () => this.showAuthInterface('login');
    }

    /**
     * 缓存 DOM 元素
     */
    private cacheElements(): void {
        this.elements = {
            loginBtn: dom.get('#loginBtn'),
            authContainer: dom.get('#authContainer'),
            loginForm: dom.get('#loginForm') as HTMLFormElement | null,
            registerForm: dom.get('#registerForm') as HTMLFormElement | null,
            authClose: dom.get('.auth-close')
        };
    }

    /**
     * 绑定事件监听器
     */
    private bindEvents(): void {
        const { loginForm, registerForm, authClose } = this.elements;

        if (loginForm) {
            loginForm.addEventListener('submit', (e) => this.handleLogin(e));
        }

        if (registerForm) {
            registerForm.addEventListener('submit', (e) => this.handleRegister(e));
        }

        if (authClose) {
            authClose.addEventListener('click', (e) => {
                e.stopPropagation();
                this.hideAuthInterface();
            });
        }

        // Tab switcher
        document.addEventListener('click', (e) => {
            const target = e.target as HTMLElement;
            const tab = target.closest('.auth-tab');
            if (tab) {
                const tabTarget = (tab as HTMLElement).dataset?.tab as 'login' | 'register' | undefined;
                if (tabTarget) {
                    e.preventDefault();
                    e.stopPropagation();
                    this.showAuthInterface(tabTarget);
                }
            }
        });

        // 键盘 ESC 关闭认证界面
        window.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.elements.authContainer?.classList.contains('active')) {
                e.preventDefault();
                this.hideAuthInterface();
            }
        });

        // 点击 overlay 关闭
        document.addEventListener('click', (e) => {
            const target = e.target as HTMLElement;
            if (target.classList.contains('auth-overlay')) {
                e.preventDefault();
                e.stopPropagation();
                this.hideAuthInterface();
            }
        });

        // 切换链接
        document.addEventListener('click', (e) => {
            const target = e.target as HTMLElement;
            const switchLink = target.closest('.switch-form') as HTMLAnchorElement | null;
            if (!switchLink) {return;}

            e.preventDefault();
            e.stopPropagation();
            const formTarget = switchLink.dataset?.form as 'login' | 'register' | undefined;
            if (formTarget) {
                this.showAuthInterface(formTarget);
            }
        });
    }

    /**
     * 绑定登录按钮点击事件（在 Header 初始化后调用）
     */
    bindLoginButtons(): void {
        // 使用事件委托，确保无论按钮是静态还是动态注入都能响应
        document.addEventListener('click', (e) => {
            const target = e.target as HTMLElement;
            const loginBtn = target.closest('.login-btn');
            if (loginBtn) {
                e.preventDefault();
                e.stopPropagation();
                this.showAuthInterface('login');
            }
        });
    }

    /**
     * 更新 tab 激活状态
     */
    private updateTabs(type: 'login' | 'register'): void {
        const tabs = document.querySelectorAll('.auth-tab');
        tabs.forEach(tab => {
            const tabType = (tab as HTMLElement).dataset.tab;
            tab.classList.toggle('auth-tab--active', tabType === type);
        });
    }

    /**
     * 显示认证界面
     * @param type - 认证类型：login 或 register
     */
    showAuthInterface(type: 'login' | 'register' = 'login'): void {
        const { authContainer, loginForm, registerForm } = this.elements;

        if (!authContainer) {return;}

        authContainer.classList.add('active');
        this.updateTabs(type);

        if (loginForm && registerForm) {
            loginForm.classList.toggle('auth-form--hidden', type !== 'login');
            registerForm.classList.toggle('auth-form--hidden', type !== 'register');
        }

        Modal.create('#authContainer');
    }

    /**
     * 隐藏认证界面
     */
    hideAuthInterface = (): void => {
        const { authContainer, loginForm, registerForm } = this.elements;

        if (!authContainer) {return;}

        authContainer.classList.remove('active');

        // 重置为登录表单
        if (loginForm && registerForm) {
            loginForm.classList.remove('auth-form--hidden');
            registerForm.classList.add('auth-form--hidden');
        }
        this.updateTabs('login');

        // 清空表单
        this.elements.loginForm?.reset();
        this.elements.registerForm?.reset();
    }

    /**
     * 处理登录
     */
    private async handleLogin(e: Event): Promise<void> {
        e.preventDefault();

        const { loginForm } = this.elements;
        if (!loginForm) {return;}

        const loginUsername = dom.get('#loginUsername', loginForm) as HTMLInputElement;
        const loginPassword = dom.get('#loginPassword', loginForm) as HTMLInputElement;

        const username = loginUsername?.value.trim() || '';
        const password = loginPassword?.value || '';

        // 验证输入
        const usernameError = validator.getErrorMessage('username', username);
        if (usernameError) {
            toast.error(usernameError);
            loginUsername?.focus();
            return;
        }

        const passwordError = validator.getErrorMessage('password', password);
        if (passwordError) {
            toast.error(passwordError);
            loginPassword?.focus();
            return;
        }

        // 显示加载状态
        const submitBtn = loginForm.querySelector('button[type="submit"]') as HTMLButtonElement;
        if (submitBtn) {
            submitBtn.classList.add('loading');
            submitBtn.disabled = true;
        }

        try {
            const response = await userApi.login({ account: username, password });
            const { token, user } = response.data as LoginResponseData;

            // 保存用户信息和 token
            setSession(token, user);

            toast.success('登录成功');
            this.hideAuthInterface();
            this.updateUIAfterLogin();
        } catch (error) {
            if (error instanceof ApiClientError) {
                toast.error(error.message);
            } else if (error instanceof Error) {
                toast.error(error.message);
            } else {
                toast.error('登录失败，请检查用户名和密码');
            }
        } finally {
            // 恢复按钮状态
            if (submitBtn) {
                submitBtn.classList.remove('loading');
                submitBtn.disabled = false;
            }
        }
    }

    /**
     * 处理注册 - 简化注册（渐进式注册第一步）
     */
    private async handleRegister(e: Event): Promise<void> {
        e.preventDefault();

        const { registerForm } = this.elements;
        if (!registerForm) {return;}

        const usernameInput = dom.get('#registerUsername', registerForm) as HTMLInputElement;
        const passwordInput = dom.get('#registerPassword', registerForm) as HTMLInputElement;
        const confirmPasswordInput = dom.get('#confirmPassword', registerForm) as HTMLInputElement;
        const agreeTermsInput = dom.get('#agreeTerms', registerForm) as HTMLInputElement;

        const username = usernameInput?.value.trim() || '';
        const password = passwordInput?.value || '';
        const confirmPassword = confirmPasswordInput?.value || '';

        if (!username || !password || !confirmPassword) {
            toast.error('请填写完整信息');
            return;
        }

        if (!agreeTermsInput?.checked) {
            toast.error('请同意服务条款和隐私政策');
            return;
        }

        if (password !== confirmPassword) {
            toast.error('两次输入的密码不一致');
            confirmPasswordInput?.focus();
            return;
        }

        const submitBtn = registerForm.querySelector('button[type="submit"]') as HTMLButtonElement;
        if (submitBtn) {
            submitBtn.classList.add('loading');
            submitBtn.disabled = true;
        }

        try {
            await userApi.register({ username, password });

            toast.success('注册成功！正在登录...');

            const response = await userApi.login({ account: username, password });
            const { token, user } = response.data as LoginResponseData;

            setSession(token, user);
            storage.set('needCompleteProfile', 'true');

            this.hideAuthInterface();
            this.updateUIAfterLogin();

            setTimeout(() => {
                navigation.go('profile', { query: { firstLogin: '1' } });
            }, 500);
        } catch (error) {
            if (error instanceof ApiClientError) {
                toast.error(error.message);
            } else if (error instanceof Error) {
                toast.error(error.message);
            } else {
                toast.error('注册失败，请稍后重试');
            }
        } finally {
            if (submitBtn) {
                submitBtn.classList.remove('loading');
                submitBtn.disabled = false;
            }
        }
    }

    /**
     * 检查认证状态
     */
    checkAuthStatus(): void {
        const token = getStoredToken();
        const user = getStoredUser() as UserInfo | null;

        if (token && user) {
            this.updateUIAfterLogin();
        }
    }

    /**
     * 登录后更新 UI
     */
    private updateUIAfterLogin(): void {
        const user = getStoredUser() as UserInfo | null;

        window.dispatchEvent(new CustomEvent('user-login', { detail: { user } }));

        if (window.location.search.includes('redirect=')) {
            navigation.loginRedirect();
        }
    }

    /**
     * 销毁模块，清理资源
     */
    destroy(): void {
        this.initialized = false;
        this.elements = {
            loginBtn: null,
            authContainer: null,
            loginForm: null,
            registerForm: null,
            authClose: null
        };
    }
}

export default new AuthManager();
