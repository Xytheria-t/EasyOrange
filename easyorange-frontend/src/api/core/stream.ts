import { getStoredToken } from '@/features/auth/session';
import type { ChatStreamEvent } from '@/types/ai';

/**
 * SSE 流式消费（fetch + ReadableStream）— POST 请求可携带 Authorization 头，
 * 这是原生 EventSource（仅 GET、不能带头）无法满足的；逐帧解析 text/event-stream。
 * 事件协议与后端 SseEmitter 对齐：token / sources / done / error。
 */
export async function streamChat(
    endpoint: string,
    body: unknown,
    onEvent: (event: ChatStreamEvent) => void,
    signal?: AbortSignal
): Promise<void> {
    const response = await fetch(`/api${endpoint}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${getStoredToken() ?? ''}`,
        },
        credentials: 'include',
        body: JSON.stringify(body),
        signal,
    });

    if (!response.ok || !response.body) {
        throw new Error(`stream request failed: HTTP ${response.status}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';

    while (true) {
        const { done, value } = await reader.read();
        if (done) {
            break;
        }
        buffer += decoder.decode(value, { stream: true });
        let separator = buffer.indexOf('\n\n');
        while (separator >= 0) {
            const frame = buffer.slice(0, separator);
            buffer = buffer.slice(separator + 2);
            handleFrame(frame, onEvent);
            separator = buffer.indexOf('\n\n');
        }
    }
}

function handleFrame(frame: string, onEvent: (event: ChatStreamEvent) => void) {
    let eventName = 'message';
    const dataLines: string[] = [];
    for (const line of frame.split('\n')) {
        if (line.startsWith('event:')) {
            eventName = line.slice(6).trim();
        } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).trimStart());
        }
    }
    if (dataLines.length === 0) {
        return;
    }
    const raw = dataLines.join('\n');
    switch (eventName) {
        case 'token':
            onEvent({ type: 'token', data: parseRaw(raw) });
            break;
        case 'sources':
            onEvent({ type: 'sources', data: parseJsonArray(raw) });
            break;
        case 'done':
            onEvent({ type: 'done', data: parseRaw(raw) });
            break;
        case 'error':
            onEvent({ type: 'error', data: parseRaw(raw) });
            break;
        default:
            break;
    }
}

/** 后端 String 数据原样输出（非 JSON 引号），能 parse 就 parse，否则当原始文本 */
function parseRaw(raw: string): string {
    try {
        const parsed = JSON.parse(raw) as unknown;
        return typeof parsed === 'string' ? parsed : raw;
    } catch {
        return raw;
    }
}

function parseJsonArray(raw: string): string[] {
    try {
        const parsed = JSON.parse(raw) as unknown;
        if (Array.isArray(parsed)) {
            return parsed.map(String);
        }
    } catch {
        // fall through
    }
    return raw ? [raw] : [];
}
