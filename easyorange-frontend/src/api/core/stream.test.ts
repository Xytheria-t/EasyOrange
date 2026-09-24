import { afterEach, describe, expect, it, vi } from 'vitest';
import { StreamAuthError, streamChat } from './stream';

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
                        'event: sources\ndata: [{"type":"knowledge","id":"kb-2","title":"退款规则"}]\n\n',
                        'event: done\ndata: 你好\n\n',
                    ])
                )
        );

        const events: string[] = [];
        await streamChat('/ai/chat/stream', { question: '怎么退款？' }, event => {
            events.push(`${event.type}:${JSON.stringify(event.data)}`);
        });

        expect(events).toEqual([
            'token:"你"',
            'token:"好"',
            'sources:[{"type":"knowledge","id":"kb-2","title":"退款规则"}]',
            'done:"你好"',
        ]);
    });

    it('sources 载荷不是对象数组时降级为空数组（不把原始文本当来源渲染）', async () => {
        vi.stubGlobal(
            'fetch',
            vi
                .fn()
                .mockResolvedValue(
                    sseResponse(['event: sources\ndata: ["退款规则"]\n\n', 'event: done\ndata: 回答\n\n'])
                )
        );

        const sources: unknown[] = [];
        await streamChat('/ai/chat/stream', {}, event => {
            if (event.type === 'sources') {
                sources.push(event.data);
            }
        });

        expect(sources).toEqual([[]]);
    });

    it('step 事件解析为 AgentStep 对象（Agent 步骤可视化）', async () => {
        vi.stubGlobal(
            'fetch',
            vi
                .fn()
                .mockResolvedValue(
                    sseResponse([
                        'event: step\ndata: {"step":1,"tool":"knowledge_search","thought":"查退款规则","observation":"命中 2 条：退款规则 / 售后政策"}\n\n',
                        'event: token\ndata: 可以\n\n',
                        'event: done\ndata: 可以\n\n',
                    ])
                )
        );

        const events: string[] = [];
        await streamChat('/ai/chat/stream', { question: '怎么退款？' }, event => {
            events.push(`${event.type}:${JSON.stringify(event.data)}`);
        });

        expect(events).toEqual([
            'step:{"step":1,"tool":"knowledge_search","thought":"查退款规则","observation":"命中 2 条：退款规则 / 售后政策"}',
            'token:"可以"',
            'done:"可以"',
        ]);
    });

    it('step 事件载荷非法时降级为最小形状', async () => {
        vi.stubGlobal(
            'fetch',
            vi.fn().mockResolvedValue(sseResponse(['event: step\ndata: [broken\n\n', 'event: done\ndata: 回答\n\n']))
        );

        const events: Array<{ type: string; data: unknown }> = [];
        await streamChat('/ai/chat/stream', {}, event => events.push(event));

        expect(events).toEqual([
            { type: 'step', data: { step: 0, tool: '[broken' } },
            { type: 'done', data: '回答' },
        ]);
    });

    it('流正常结束但没有终止事件 -> 抛错（否则页面永久卡在生成中）', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(sseResponse(['event: token\ndata: 只有半句\n\n'])));

        await expect(streamChat('/ai/chat/stream', {}, () => {})).rejects.toThrow('连接中断');
    });

    it('末帧没有空行结尾也能解析出来', async () => {
        vi.stubGlobal(
            'fetch',
            vi.fn().mockResolvedValue(sseResponse(['event: token\ndata: 前面\n\n', 'event: done\ndata: 结尾']))
        );

        const events: string[] = [];
        await streamChat('/ai/chat/stream', {}, event => {
            events.push(`${event.type}:${JSON.stringify(event.data)}`);
        });

        expect(events).toEqual(['token:"前面"', 'done:"结尾"']);
    });

    it('CRLF 分隔符同样能切帧', async () => {
        vi.stubGlobal(
            'fetch',
            vi
                .fn()
                .mockResolvedValue(
                    sseResponse(['event: token\r\ndata: 你好\r\n\r\n', 'event: done\r\ndata: 好了\r\n\r\n'])
                )
        );

        const events: string[] = [];
        await streamChat('/ai/chat/stream', {}, event => {
            events.push(`${event.type}:${JSON.stringify(event.data)}`);
        });

        expect(events).toEqual(['token:"你好"', 'done:"好了"']);
    });

    it('401 -> 抛登录错误（与网络故障区分，页面提示重新登录而不是「连接中断」）', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 401 })));

        await expect(streamChat('/ai/chat/stream', {}, () => {})).rejects.toBeInstanceOf(StreamAuthError);
    });

    it('外部取消 -> 原样抛 AbortError，调用方据此归为用户主动停止', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(sseResponse(['event: token\ndata: 慢\n\n'])));
        const controller = new AbortController();

        const pending = streamChat('/ai/chat/stream', {}, () => {}, controller.signal);
        controller.abort();

        await expect(pending).rejects.toMatchObject({ name: 'AbortError' });
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
