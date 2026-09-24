import { getStoredToken } from '@/features/auth/session';
import type { AgentStep, ChatSource, ChatStreamEvent } from '@/types/ai';

/**
 * 无进展超时：多久没收到任何新数据就判定流已死。
 *
 * 用「无进展」而非「总时长」：后端 SSE 的总预算是 120s，而 token 持续到达时
 * 单次回答本来就可能跑满一分钟以上 —— 按总时长掐会把正常的长回答腰斩。
 * 真正该超时的是「连接还在、但再也没有字节过来」：代理静默掐线、网关挂起、
 * 服务端线程卡死，这几种情况下不设上限界面会永远停在「生成中」。
 */
const STREAM_IDLE_TIMEOUT_MS = 45_000;

/** 流正常结束却没有终止事件时的兜底文案。 */
const STREAM_TRUNCATED_MESSAGE = '连接中断，请重试';

/** 登录态失效：需要重新登录，与网络故障区分开。 */
export class StreamAuthError extends Error {
    constructor() {
        super('登录已过期，请重新登录');
        this.name = 'StreamAuthError';
    }
}

/**
 * 服务端返回非 2xx（限流 / 校验失败 / 5xx）。服务端 Result 信封里的 message 是给用户看的
 * 文案（如「AI 服务繁忙，请稍后重试」），必须透传到界面 —— 笼统报「连接中断」会把
 * 可自愈的问题（稍后重试即可）伪装成网络故障。
 */
export class StreamRequestError extends Error {
    constructor(
        message: string,
        readonly status: number
    ) {
        super(message);
        this.name = 'StreamRequestError';
    }
}

/** 无进展超时：与网络故障区分开，前端可提示「响应超时」。 */
export class StreamIdleTimeoutError extends Error {
    constructor(ms: number) {
        super(`超过 ${Math.round(ms / 1000)} 秒无响应，请重试`);
        this.name = 'StreamIdleTimeoutError';
    }
}

/** 打断挂起 read 用的 AbortError —— 与 fetch 自身抛出的同构，调用方统一按 name 判定 */
function abortError(): Error {
    return new DOMException('The operation was aborted.', 'AbortError');
}

/**
 * SSE 流式消费（fetch + ReadableStream）— POST 请求可携带 Authorization 头，
 * 这是原生 EventSource（仅 GET、不能带头）无法满足的；逐帧解析 text/event-stream。
 * 事件协议与后端 SseEmitter 对齐：step / token / sources / done / error。
 *
 * 终止事件（done / error）缺失时抛错而不是静默 resolve：调用方靠 Promise 结束来
 * 解锁输入框，静默返回会让「流被中途掐断」和「回答正常结束」在界面上无法区分，
 * 前者会把输入框永久锁在禁用态。
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

    if (response.status === 401 || response.status === 403) {
        throw new StreamAuthError();
    }
    if (!response.ok || !response.body) {
        throw new StreamRequestError(await responseErrorMessage(response), response.status);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';
    let terminated = false;

    // 无进展超时用独立的 reject 句柄打断正在挂起的 reader.read()：
    // fetch 已经带着调用方的 signal 发出去，流的存亡由那个 signal 决定；
    // 这里只解决「连接还开着、但永远不再有字节」这一种情况（代理静默掐线、
    // 网关挂起、服务端线程卡死）。超时与外部取消分流：前者报超时，后者原样
    // 上抛 AbortError，调用方据此把「用户主动停止」和「故障」分开处理。
    let rejectRead: ((reason: Error) => void) | undefined;
    let idleTimer: ReturnType<typeof setTimeout> | undefined;
    let timedOut = false;
    const abortRead = (reason: Error) => rejectRead?.(reason);
    const onExternalAbort = () => abortRead(abortError());
    signal?.addEventListener('abort', onExternalAbort);
    const armIdleTimer = () => {
        if (idleTimer !== undefined) {
            clearTimeout(idleTimer);
        }
        idleTimer = setTimeout(() => {
            timedOut = true;
            abortRead(abortError());
        }, STREAM_IDLE_TIMEOUT_MS);
    };
    armIdleTimer();
    // signal 可能在本函数跑到这之前就已 aborted（调用方拿到 signal 立刻取消）：
    // abort 事件只发一次，addEventListener 不会再补，这里补一次即时检查
    if (signal?.aborted) {
        abortRead(abortError());
    }

    try {
        while (true) {
            const read = reader.read();
            const { done, value } = await new Promise<ReadableStreamReadResult<Uint8Array>>((resolve, reject) => {
                // 先挂上 reject 句柄再检查已取消：abort 早于这次 read 到达时，
                // 事件早已发过，句柄挂上后必须主动补一次拒绝，否则 read 会一直挂着
                rejectRead = reject;
                if (signal?.aborted) {
                    reject(abortError());
                    return;
                }
                read.then(resolve, reject);
            });
            rejectRead = undefined;
            if (done) {
                break;
            }
            armIdleTimer();
            // SSE 允许 \r\n 分隔；统一归一化后再切帧，避免 \r\n\r\n 永远匹配不上
            buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n');
            let separator = buffer.indexOf('\n\n');
            while (separator >= 0) {
                const frame = buffer.slice(0, separator);
                buffer = buffer.slice(separator + 2);
                if (handleFrame(frame, onEvent)) {
                    terminated = true;
                }
                separator = buffer.indexOf('\n\n');
            }
        }
        // 末帧可能没有空行结尾：残留 buffer 也要解析，否则丢掉最后一个事件
        if (buffer.trim() && handleFrame(buffer, onEvent)) {
            terminated = true;
        }
    } catch (e) {
        if (timedOut) {
            throw new StreamIdleTimeoutError(STREAM_IDLE_TIMEOUT_MS);
        }
        throw e;
    } finally {
        if (idleTimer !== undefined) {
            clearTimeout(idleTimer);
        }
        signal?.removeEventListener('abort', onExternalAbort);
        // 挂起的 read 被 reject 后底层流仍在跑，超时路径要主动断开，否则连接泄漏
        await reader.cancel().catch(() => undefined);
    }

    if (!terminated) {
        throw new Error(STREAM_TRUNCATED_MESSAGE);
    }
}

/** 非 2xx 时提取用户可读文案：优先 Result 信封的 message，解析不出再退回状态码文案 */
async function responseErrorMessage(response: Response): Promise<string> {
    try {
        const body = (await response.json()) as { message?: unknown };
        if (body && typeof body.message === 'string' && body.message) {
            return body.message;
        }
    } catch {
        // 响应体不是 JSON（网关页/空体），走默认文案
    }
    return `请求失败（HTTP ${response.status}）`;
}

/**
 * 解析一帧并派发事件。
 *
 * @returns 该帧是否为终止事件（done / error）—— 流末尾据此判断是否被中途掐断
 */ function handleFrame(frame: string, onEvent: (event: ChatStreamEvent) => void): boolean {
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
        // 注释帧（`: keep-alive`）与空帧不是终止事件
        return false;
    }
    const raw = dataLines.join('\n');
    switch (eventName) {
        case 'step':
            onEvent({ type: 'step', data: parseAgentStep(raw) });
            return false;
        case 'token':
            onEvent({ type: 'token', data: parseRaw(raw) });
            return false;
        case 'sources':
            onEvent({ type: 'sources', data: parseSources(raw) });
            return false;
        case 'done':
            onEvent({ type: 'done', data: parseRaw(raw) });
            return true;
        case 'error':
            onEvent({ type: 'error', data: parseRaw(raw) });
            return true;
        default:
            return false;
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

/** sources 载荷是 ChatSource[]；解析不出对象数组时降级为空数组，不把原始文本当来源渲染 */
function parseSources(raw: string): ChatSource[] {
    try {
        const parsed = JSON.parse(raw) as unknown;
        if (Array.isArray(parsed)) {
            return parsed.flatMap(item => {
                if (item && typeof item === 'object' && typeof (item as ChatSource).id === 'string') {
                    const source = item as ChatSource;
                    return [
                        {
                            type: source.type === 'asset' ? 'asset' : 'knowledge',
                            id: source.id,
                            title: typeof source.title === 'string' ? source.title : source.id,
                        },
                    ];
                }
                return [];
            });
        }
    } catch {
        // fall through
    }
    return [];
}

/** step 事件载荷为 JSON 对象（AgentStepView），解析失败时降级为最小可用形状 */
function parseAgentStep(raw: string): AgentStep {
    try {
        const parsed = JSON.parse(raw) as Partial<AgentStep>;
        if (parsed && typeof parsed === 'object' && typeof parsed.tool === 'string') {
            return {
                step: typeof parsed.step === 'number' ? parsed.step : 0,
                tool: parsed.tool,
                thought: parsed.thought,
                observation: parsed.observation,
            };
        }
    } catch {
        // fall through
    }
    return { step: 0, tool: raw };
}
