import { useEffect, useState } from 'react';

const UPDATE_INTERVAL_MS = 30_000;

function formatCurrentTime(now: Date): string {
    const dateStr = `${now.getMonth() + 1}月${now.getDate()}日`;
    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    return `${dateStr} ${weekDays[now.getDay()]}`;
}

export function useDashboardTime(): string {
    const [currentTime, setCurrentTime] = useState(() => formatCurrentTime(new Date()));

    useEffect(() => {
        const timer = setInterval(() => {
            setCurrentTime(formatCurrentTime(new Date()));
        }, UPDATE_INTERVAL_MS);
        return () => clearInterval(timer);
    }, []);

    return currentTime;
}
