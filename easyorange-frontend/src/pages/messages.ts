/**
 * @fileoverview 消息中心页面
 */

import '../styles/main.css';
import '../styles/messages.css';
import { messageApi } from '../api/index.js';
import { storage, formatDate, escapeHtml, toast } from '../utils/index.js';
import { isSuccessCode, type ChatMessage, type Result } from '../types/index.js';
import BasePage from './BasePage.js';

/**
 * 对话项接口
 */
interface Conversation {
    userId: string;
    username: string;
    avatar?: string;
    lastMessage?: string;
    lastTime: string;
    unreadCount: number;
}

interface MessagesPageElements {
    messagesContainer: HTMLElement | null;
    conversationsList: HTMLElement | null;
    chatPanel: HTMLElement | null;
    chatMessages: HTMLElement | null;
    messageInput: HTMLTextAreaElement | null;
    sendBtn: HTMLElement | null;
}

class MessagesPage extends BasePage<MessagesPageElements> {
    private conversations: Conversation[];
    private currentConversation: string | null;
    private messages: ChatMessage[];

    constructor() {
        super();
        this.conversations = [];
        this.currentConversation = null;
        this.messages = [];
    }

    protected cacheElements(): void {
        this.elements.messagesContainer = this.querySelector<HTMLElement>('#messages-container');
        this.elements.conversationsList = this.querySelector<HTMLElement>('#conversations-list');
        this.elements.chatPanel = this.querySelector<HTMLElement>('#chat-panel');
        this.elements.chatMessages = null;
        this.elements.messageInput = null;
        this.elements.sendBtn = null;
    }

    protected bindEvents(): void {
        this.onEvent(this.elements.conversationsList, 'click', (e: Event) => {
            const target = e.target as HTMLElement;
            const item = target.closest('.conversation-item');
            if (item) {
                const userId = (item as HTMLElement).dataset.id;
                if (userId) {
                    this.openConversation(userId);
                }
            }
        });

        this.onEvent(this.elements.chatPanel, 'click', (e: Event) => {
            const target = e.target as HTMLElement;
            if (target.id === 'send-btn' || target.closest('#send-btn')) {
                this.sendMessage();
            }
        });

        this.onEvent(this.elements.chatPanel, 'keydown', (e: Event) => {
            const ke = e as KeyboardEvent;
            const target = ke.target as HTMLElement;
            if (target.id === 'message-input' && ke.key === 'Enter' && !ke.shiftKey) {
                ke.preventDefault();
                this.sendMessage();
            }
        });
    }

    protected async onInit(): Promise<void> {
        this.render();
        await this.loadConversations();
    }

    private render(): void {
        this.safe(this.elements.messagesContainer, (container) => {
            container.innerHTML = `
                <div class="messages-page">
                    <div class="page-header">
                        <h1>消息中心</h1>
                    </div>
                    <div class="messages-layout">
                        <div class="conversations-panel">
                            <div class="conversations-list" id="conversations-list">
                                <div class="loading">加载中...</div>
                            </div>
                        </div>
                        <div class="chat-panel" id="chat-panel">
                            <div class="chat-placeholder">
                                <p>选择一个对话开始聊天</p>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        });
    }

    private async loadConversations(): Promise<void> {
        try {
            const response = await messageApi.getConversations();
            if (isSuccessCode(response.code)) {
                this.conversations = (response.data || []) as unknown as Conversation[];
                this.renderConversations();
            } else {
                this.safe(this.elements.conversationsList, (listEl) => {
                    listEl.innerHTML = `<div class="error">${response.message}</div>`;
                });
            }
        } catch (error) {
            const err = error as Error;
            this.safe(this.elements.conversationsList, (listEl) => {
                listEl.innerHTML = `<div class="error">加载失败: ${err.message}</div>`;
            });
        }
    }

    private renderConversations(): void {
        this.safe(this.elements.conversationsList, (listEl) => {
            if (this.conversations.length === 0) {
                listEl.innerHTML = '<div class="empty">暂无对话</div>';
                return;
            }

            listEl.innerHTML = this.conversations.map((conv: Conversation) => `
                <div class="conversation-item" data-id="${conv.userId}">
                    <div class="user-avatar">
                        <img src="${conv.avatar || '/images/default-avatar.png'}" alt="${conv.username}">
                    </div>
                    <div class="conversation-info">
                        <h4>${escapeHtml(conv.username)}</h4>
                        <p class="last-message">${escapeHtml(conv.lastMessage || '')}</p>
                    </div>
                    <div class="conversation-meta">
                        <span class="time">${formatDate(conv.lastTime)}</span>
                        ${conv.unreadCount > 0 ? `<span class="unread-badge">${conv.unreadCount}</span>` : ''}
                    </div>
                </div>
            `).join('');
        });
    }

    private async openConversation(userId: string): Promise<void> {
        this.currentConversation = userId;

        const items = this.querySelectorAll<HTMLElement>('.conversation-item');
        items.forEach((item) => {
            item.classList.remove('active');
            if (item.dataset.id === userId) {
                item.classList.add('active');
            }
        });

        this.safe(this.elements.chatPanel, (chatPanel) => {
            chatPanel.innerHTML = `
                <div class="chat-header">
                    <h3 id="chat-username">聊天中...</h3>
                </div>
                <div class="chat-messages" id="chat-messages">
                    <div class="loading">加载中...</div>
                </div>
                <div class="chat-input">
                    <textarea id="message-input" placeholder="输入消息..."></textarea>
                    <button id="send-btn" class="btn-primary">发送</button>
                </div>
            `;

            this.elements.chatMessages = chatPanel.querySelector<HTMLElement>('#chat-messages');
            this.elements.messageInput = chatPanel.querySelector<HTMLTextAreaElement>('#message-input');
            this.elements.sendBtn = chatPanel.querySelector<HTMLElement>('#send-btn');
        });

        await this.loadMessages(userId);
    }

    private async loadMessages(userId: string): Promise<void> {
        try {
            const response = await messageApi.getConversation(userId, 50);
            if (isSuccessCode(response.code)) {
                this.messages = (response.data || []) as ChatMessage[];
                this.renderMessages();
                await messageApi.markAsRead(parseInt(userId, 10));
            } else {
                this.safe(this.elements.chatMessages, (messagesEl) => {
                    messagesEl.innerHTML = `<div class="error">${response.message}</div>`;
                });
            }
        } catch (error) {
            const err = error as Error;
            this.safe(this.elements.chatMessages, (messagesEl) => {
                messagesEl.innerHTML = `<div class="error">加载失败: ${err.message}</div>`;
            });
        }
    }

    private renderMessages(): void {
        const currentUserId = storage.get('userId');
        this.safe(this.elements.chatMessages, (messagesEl) => {
            messagesEl.innerHTML = this.messages.map((msg: ChatMessage) => {
                const isMine = msg.senderId === currentUserId;
                return `
                    <div class="message ${isMine ? 'mine' : 'theirs'}">
                        <div class="message-content">
                            <p>${escapeHtml(msg.content)}</p>
                            <span class="message-time">${formatDate(msg.createTime)}</span>
                        </div>
                    </div>
                `;
            }).join('');
            messagesEl.scrollTop = messagesEl.scrollHeight;
        });
    }

    private async sendMessage(): Promise<void> {
        const content = this.elements.messageInput?.value?.trim();
        if (!content || !this.currentConversation) {
            return;
        }

        try {
            const response: Result<unknown> = await messageApi.sendMessage({
                receiverId: parseInt(this.currentConversation, 10),
                content: content
            });

            if (isSuccessCode(response.code)) {
                this.safe(this.elements.messageInput, (input) => {
                    input.value = '';
                });
                await this.loadMessages(this.currentConversation);
            } else {
                toast.error(response.message || '发送失败');
            }
        } catch (error) {
            const err = error as Error;
            toast.error(`发送失败: ${err.message}`);
        }
    }

    protected override onDestroy(): void {
        this.conversations = [];
        this.currentConversation = null;
        this.messages = [];
    }
}

let messagesPageInstance: MessagesPage | null = null;
document.addEventListener('DOMContentLoaded', () => {
    messagesPageInstance = new MessagesPage();
    messagesPageInstance.init().catch(() => {
        // 初始化失败时静默处理
    });
});

export default MessagesPage;
