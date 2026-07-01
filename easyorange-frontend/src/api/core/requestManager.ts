interface PendingRequest {
    controller: AbortController;
    timestamp: number;
}

export const requestManager = {
    pendingRequests: new Map<string, PendingRequest>(),
    dedupeWindow: 100,

    generateKey(endpoint: string, options: { method?: string; body?: unknown } = {}): string {
        const method = options.method || 'GET';
        const body = options.body ? JSON.stringify(options.body) : '';
        return `${method}:${endpoint}:${body}`;
    },

    isDuplicate(key: string): boolean {
        const pending = this.pendingRequests.get(key);
        if (!pending) {
            return false;
        }
        return Date.now() - pending.timestamp < this.dedupeWindow;
    },

    startTracking(key: string, controller: AbortController): void {
        const existing = this.pendingRequests.get(key);
        if (existing?.controller) {
            existing.controller.abort();
        }
        this.pendingRequests.set(key, { controller, timestamp: Date.now() });
    },

    stopTracking(key: string): void {
        this.pendingRequests.delete(key);
    },

    cancel(key: string, reason = '请求已取消'): void {
        const pending = this.pendingRequests.get(key);
        if (pending?.controller) {
            pending.controller.abort(reason);
            this.pendingRequests.delete(key);
        }
    },

    cancelAll(reason = '所有请求已取消'): void {
        this.pendingRequests.forEach(pending => {
            pending.controller?.abort(reason);
        });
        this.pendingRequests.clear();
    },

    cancelByPattern(pattern: RegExp | string, reason = '请求已取消'): void {
        const regex = pattern instanceof RegExp ? pattern : new RegExp(pattern);
        this.pendingRequests.forEach((pending, key) => {
            if (regex.test(key)) {
                pending.controller?.abort(reason);
                this.pendingRequests.delete(key);
            }
        });
    },
};
