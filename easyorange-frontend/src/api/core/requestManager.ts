interface PendingRequest {
    controller: AbortController;
    timestamp: number;
}

export const requestManager = {
    pendingRequests: new Map<string, PendingRequest>(),
    dedupeWindow: 100,

    generateKey(
        endpoint: string,
        options: { method?: string; body?: unknown; params?: Record<string, unknown> } = {}
    ): string {
        const method = options.method || 'GET';
        const body = options.body ? JSON.stringify(options.body) : '';
        // params 必须进key：同端点不同筛选的 GET 是两次不同的请求，
        // 不参与 key 会被 100ms 窗口内的第二次请求误判成重复（mutation retry 0 时用户直接看到「重复请求已取消」）
        const params = options.params ? JSON.stringify(options.params) : '';
        return `${method}:${endpoint}:${params}:${body}`;
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
};
