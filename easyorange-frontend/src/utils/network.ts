/**
 * @fileoverview 网络工具模块
 * @description 提供网络状态检测功能
 */

export type NetworkChangeCallback = (online: boolean) => void;
export type Unsubscribe = () => void;

class NetworkUtils {
    private _online: boolean = navigator.onLine;
    private _listeners: NetworkChangeCallback[] = [];
    private _checkInterval: ReturnType<typeof setInterval> | null = null;
    private _initialized = false;

    private _init(): void {
        if (this._initialized) {return;}
        this._initialized = true;

        window.addEventListener('online', () => {
            this._online = true;
            this._notifyListeners(true);
        });

        window.addEventListener('offline', () => {
            this._online = false;
            this._notifyListeners(false);
        });

        // 页面可见时才启动轮询，减少后台资源占用
        const startPolling = () => {
            if (this._checkInterval) clearInterval(this._checkInterval);
            this._checkInterval = setInterval(() => {
                this.checkConnectivity();
            }, 30000);
        };

        const stopPolling = () => {
            if (this._checkInterval) {
                clearInterval(this._checkInterval);
                this._checkInterval = null;
            }
        };

        document.addEventListener('visibilitychange', () => {
            if (document.hidden) {
                stopPolling();
            } else {
                startPolling();
                this.checkConnectivity();
            }
        });

        startPolling();
    }

    private _notifyListeners(online: boolean): void {
        this._listeners.forEach(callback => {
            try { callback(online); } catch (e) { /* 网络监听器错误静默处理 */ }
        });
    }

    isOnline(): boolean {
        return this._online;
    }

    async checkStatus(): Promise<boolean> {
        return this.checkConnectivity();
    }

    async checkConnectivity(): Promise<boolean> {
        try {
            const response = await fetch('/api/health', {
                method: 'HEAD',
                cache: 'no-store',
                signal: AbortSignal.timeout(5000)
            });
            const wasOffline = !this._online;
            this._online = response.ok;

            if (wasOffline && this._online) {
                this._notifyListeners(true);
            }

            return this._online;
        } catch {
            const wasOnline = this._online;
            this._online = false;

            if (wasOnline) {
                this._notifyListeners(false);
            }

            return false;
        }
    }

    onChange(callback: NetworkChangeCallback): Unsubscribe {
        this._init();
        this._listeners.push(callback);
        return () => {
            const index = this._listeners.indexOf(callback);
            if (index > -1) {this._listeners.splice(index, 1);}
        };
    }

    destroy(): void {
        if (this._checkInterval) {
            clearInterval(this._checkInterval);
            this._checkInterval = null;
        }
        this._listeners = [];
        this._initialized = false;
    }
}

export const network = new NetworkUtils();
export { NetworkUtils };
export default network;
