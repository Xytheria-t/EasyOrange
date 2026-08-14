import { afterEach, describe, expect, it, vi } from 'vitest';
import { streamChat } from './stream';

function sseResponse(frames: string[]): Response {
    const encoder = new TextEncoder();
    const body = new ReadableStream<Uint8Array>({
        start(controller) {
            for (const frame of frames) {
                controller.enqueue(encoder.encode(frame));
            }
            controller.close();
        },
    });
    return new Response(body, { status: 200, headers: { 'Content-Type': 'text/event-stream' } });
}

afterEach(() => {
    vi.unstubAllGlobals();
});

describe('streamChat (SSE 流式消费)', () => {
    it('逐帧解析 token / sources / done 事件', async () => {
        vi.stubGlobal(
            'fetch',
            vi
                .fn()
                .mockResolvedValue(
                    sseResponse([
                        'event: token\ndata: 你\n\n',
                        'event: token\ndata: 好\n\n',
                        'event: sources\ndata: ["退款规则"]\n\n',
                        'event: done\ndata: 你好\n\n',
                    ])
                )
        );

        const events: string[] = [];
        await streamChat('/ai/chat/stream', { question: '怎么退款？' }, event => {
            events.push(`${event.type}:${JSON.stringify(event.data)}`);
        });

        expect(events).toEqual(['token:"你"', 'token:"好"', 'sources:["退款规则"]', 'done:"你好"']);
    });

    it('error 事件与跨帧切分（数据分两次到达）', async () => {
        const encoder = new TextEncoder();
        const body = new ReadableStream<Uint8Array>({
            start(controller) {
                controller.enqueue(encoder.encode('event: token\ndata: 部'));
                controller.enqueue(encoder.encode('分\n\nevent: error\ndata: 服务不可用\n\n'));
                controller.close();
            },
        });
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(body, { status: 200 })));

        const events: string[] = [];
        await streamChat('/ai/chat/stream', {}, event => {
            events.push(`${event.type}:${JSON.stringify(event.data)}`);
        });

        expect(events).toEqual(['token:"部分"', 'error:"服务不可用"']);
    });

    it('HTTP 非 2xx -> 抛错', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 429 })));

        await expect(streamChat('/ai/chat/stream', {}, () => {})).rejects.toThrow('429');
    });
});
