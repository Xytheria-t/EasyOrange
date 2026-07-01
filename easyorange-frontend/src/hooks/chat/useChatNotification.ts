import { useCallback, useState } from 'react';

interface UseChatNotificationOptions {
    activeConversationId: string | null;
    enabled?: boolean;
}

let audioContext: AudioContext | null = null;

function getAudioContext(): AudioContext | null {
    if (typeof window === 'undefined') {
        return null;
    }
    if (!audioContext) {
        try {
            audioContext = new AudioContext();
        } catch {
            return null;
        }
    }
    return audioContext;
}

function playNotificationSound() {
    const ctx = getAudioContext();
    if (!ctx) {
        return;
    }

    try {
        const oscillator = ctx.createOscillator();
        const gainNode = ctx.createGain();

        oscillator.type = 'sine';
        oscillator.frequency.setValueAtTime(880, ctx.currentTime);
        gainNode.gain.setValueAtTime(0.1, ctx.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.15);

        oscillator.connect(gainNode);
        gainNode.connect(ctx.destination);

        oscillator.start(ctx.currentTime);
        oscillator.stop(ctx.currentTime + 0.15);
    } catch {
        // Silently ignore audio errors
    }
}

export function useChatNotification(options: UseChatNotificationOptions) {
    const { activeConversationId, enabled = true } = options;
    const [permission, setPermission] = useState<NotificationPermission>(() => {
        if (typeof window !== 'undefined' && 'Notification' in window) {
            return Notification.permission;
        }
        return 'default';
    });

    const requestPermission = useCallback(async () => {
        if (typeof window === 'undefined' || !('Notification' in window)) {
            return false;
        }
        if (Notification.permission === 'granted') {
            setPermission('granted');
            return true;
        }
        if (Notification.permission === 'denied') {
            setPermission('denied');
            return false;
        }

        try {
            const result = await Notification.requestPermission();
            setPermission(result as NotificationPermission);
            return result === 'granted';
        } catch {
            setPermission('denied');
            return false;
        }
    }, []);

    const notify = useCallback(
        (payload: { senderName: string; content: string; conversationId: string }) => {
            if (!enabled) {
                return;
            }

            const isActiveConversation = activeConversationId && payload.conversationId === activeConversationId;

            const isPageVisible = !document.hidden;

            if (isActiveConversation && isPageVisible) {
                return;
            }

            playNotificationSound();

            if (permission !== 'granted') {
                return;
            }

            try {
                new Notification(`${payload.senderName} 发来消息`, {
                    body: payload.content.slice(0, 50),
                    icon: '/favicon.ico',
                    tag: `chat-${payload.conversationId}`,
                    silent: true,
                });
            } catch {
                // Notification API may fail in some contexts
            }
        },
        [enabled, activeConversationId, permission]
    );

    return { notify, requestPermission, permission };
}
