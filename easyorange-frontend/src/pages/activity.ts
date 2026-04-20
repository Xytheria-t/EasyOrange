/**
 * @fileoverview 活动页面逻辑
 * @version 1.0.0
 */

import api, { ACTIVITY_TYPE_NAMES, ACTIVITY_STATUS, ACTIVITY_STATUS_NAMES, type ActivityType, type ActivityStatus, type Activity, type CreateActivityRequest } from '../api/index.js';
import { toast, dom, formatDate } from '../utils/index.js';

export type { ActivityType, ActivityStatus, Activity, CreateActivityRequest };

type ActivityTab = 'ongoing' | 'upcoming' | 'my' | 'joined';

/** 活动页面元素接口 */
export interface ActivityPageElements {
    activityContainer: HTMLElement | null;
    activityTabs: HTMLElement | null;
    activityList: HTMLElement | null;
    createActivityBtn: HTMLElement | null;
    activityModal: HTMLElement | null;
    activityForm: HTMLFormElement | null;
}

/** 活动页面对象接口 */
export interface ActivityPageInterface {
    elements: ActivityPageElements;
    activities: Activity[];
    currentTab: ActivityTab;
    init(): void;
    initElements(): void;
    bindEvents(): void;
    switchTab(tab: ActivityTab): void;
    loadActivities(): Promise<void>;
    renderActivities(): void;
    renderActivityCard(activity: Activity): string;
    renderActivityActions(activity: Activity, isFull: boolean): string;
    getTypeIcon(type: ActivityType): string;
    renderEmptyState(): void;
    joinActivity(activityId: number): Promise<void>;
    leaveActivity(activityId: number): Promise<void>;
    showCreateModal(): void;
    hideCreateModal(): void;
    handleSubmit(e: Event): Promise<void>;
}

// ============================================
// 活动页面逻辑
// ============================================

const ActivityPage: ActivityPageInterface = {
    elements: {
        activityContainer: null,
        activityTabs: null,
        activityList: null,
        createActivityBtn: null,
        activityModal: null,
        activityForm: null
    },
    activities: [],
    currentTab: 'ongoing',

    init(): void {
        this.initElements();
        this.bindEvents();
        this.loadActivities();
    },

    initElements(): void {
        this.elements = {
            activityContainer: dom.get('#activityContainer') as HTMLElement | null,
            activityTabs: dom.get('#activityTabs') as HTMLElement | null,
            activityList: dom.get('#activityList') as HTMLElement | null,
            createActivityBtn: dom.get('#createActivityBtn') as HTMLElement | null,
            activityModal: dom.get('#activityModal') as HTMLElement | null,
            activityForm: dom.get('#activityForm') as HTMLFormElement | null
        };
    },

    bindEvents(): void {
        if (this.elements.activityTabs) {
            dom.on(this.elements.activityTabs, 'click', (e: Event) => {
                const target = e.target as HTMLElement;
                const tab = target.closest('.activity-tab') as HTMLElement;
                if (tab && tab.dataset.tab) {
                    this.switchTab(tab.dataset.tab as ActivityTab);
                }
            });
        }

        if (this.elements.createActivityBtn) {
            dom.on(this.elements.createActivityBtn, 'click', () => this.showCreateModal());
        }

        if (this.elements.activityForm) {
            dom.on(this.elements.activityForm, 'submit', (e: Event) => this.handleSubmit(e));
        }
    },

    switchTab(tab: ActivityTab): void {
        this.currentTab = tab;
        
        const tabs = dom.getAll('.activity-tab', this.elements.activityTabs ?? undefined);
        tabs.forEach(t => t.classList.toggle('active', t.dataset.tab === tab));
        
        this.loadActivities();
    },

    async loadActivities(): Promise<void> {
        try {
            let response: unknown;
            switch (this.currentTab) {
                case 'ongoing':
                case 'upcoming':
                    response = await api.activity.getActivities({ status: this.currentTab === 'ongoing' ? 1 : 0 });
                    break;
                case 'my':
                    response = await api.activity.getMyActivities();
                    break;
                case 'joined':
                    response = await api.activity.getJoinedActivities();
                    break;
                default:
                    response = await api.activity.getActivities({ status: 1 });
            }

            const data = (response as { data?: Activity[] }).data ?? (response as Activity[]);
            this.activities = Array.isArray(data) ? data : [];
            this.renderActivities();
        } catch (error) {
            toast.error('加载活动失败');
            this.renderEmptyState();
        }
    },

    renderActivities(): void {
        if (!this.elements.activityList) {return;}

        if (!this.activities || this.activities.length === 0) {
            this.renderEmptyState();
            return;
        }

        this.elements.activityList.innerHTML = this.activities.map(activity => this.renderActivityCard(activity)).join('');
    },

    renderActivityCard(activity: Activity): string {
        const statusClass = `status-${activity.status}`;
        const activityType = activity.activityType || activity.type || 'default';
        const typeIcon = this.getTypeIcon(activityType);
        const isFull = activity.maxParticipants !== undefined && 
                       activity.maxParticipants !== null && 
                       (activity.currentParticipants ?? 0) >= activity.maxParticipants;

        return `
            <div class="activity-card" data-id="${activity.id}">
                <div class="activity-header">
                    <div class="activity-type-badge ${activityType}">
                        ${typeIcon} ${ACTIVITY_TYPE_NAMES[activityType] || activityType}
                    </div>
                    <div class="activity-status ${statusClass}">
                        ${ACTIVITY_STATUS_NAMES[activity.status] || '未知'}
                    </div>
                </div>
                <div class="activity-cover">
                    ${activity.coverImage 
                        ? `<img src="${activity.coverImage}" alt="${activity.title}">`
                        : `<div class="cover-placeholder">${typeIcon}</div>`
                    }
                </div>
                <div class="activity-content">
                    <h3 class="activity-title">${activity.title}</h3>
                    <p class="activity-description">${activity.description || '暂无描述'}</p>
                    <div class="activity-meta">
                        <div class="meta-item">
                            <span class="meta-icon">📍</span>
                            <span>${activity.location || '线上'}</span>
                        </div>
                        <div class="meta-item">
                            <span class="meta-icon">🕐</span>
                            <span>${formatDate(activity.startTime, 'datetime')}</span>
                        </div>
                        <div class="meta-item">
                            <span class="meta-icon">👥</span>
                            <span>${activity.currentParticipants || 0}/${activity.maxParticipants || '不限'}</span>
                        </div>
                    </div>
                </div>
                <div class="activity-footer">
                    ${this.renderActivityActions(activity, isFull)}
                </div>
            </div>
        `;
    },

    renderActivityActions(activity: Activity, isFull: boolean): string {
        if (activity.status === ACTIVITY_STATUS.CANCELLED) {
            return '<span class="activity-cancelled">活动已取消</span>';
        }

        if (activity.status === ACTIVITY_STATUS.ENDED) {
            return '<span class="activity-ended">活动已结束</span>';
        }

        if (activity.isJoined) {
            return `
                <button class="btn btn-outline" onclick="ActivityPage.leaveActivity(${activity.id})">
                    退出活动
                </button>
            `;
        }

        if (isFull) {
            return '<span class="activity-full">名额已满</span>';
        }

        return `
            <button class="btn btn-primary" onclick="ActivityPage.joinActivity(${activity.id})">
                立即参与
            </button>
        `;
    },

    getTypeIcon(type: ActivityType): string {
        const icons: Record<ActivityType, string> = {
            sale: '🏷️',
            auction: '🔨',
            exchange: '🔄',
            donation: '💝'
        };
        return icons[type] || '📅';
    },

    renderEmptyState(): void {
        if (!this.elements.activityList) {return;}

        const tabNames: Record<ActivityTab, string> = {
            ongoing: '进行中',
            upcoming: '即将开始',
            my: '相关',
            joined: '相关'
        };

        this.elements.activityList.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">📅</div>
                <h3>暂无活动</h3>
                <p>当前没有${tabNames[this.currentTab]}的活动</p>
            </div>
        `;
    },

    async joinActivity(activityId: number): Promise<void> {
        try {
            await api.activity.join(activityId);
            toast.success('参与成功！');
            this.loadActivities();
        } catch (error) {
            const err = error as Error;
            toast.error(`参与失败：${  err.message || '请稍后重试'}`);
        }
    },

    async leaveActivity(activityId: number): Promise<void> {
        try {
            await api.activity.leave(activityId);
            toast.success('已退出活动');
            this.loadActivities();
        } catch (error) {
            const err = error as Error;
            toast.error(`退出失败：${  err.message || '请稍后重试'}`);
        }
    },

    showCreateModal(): void {
        if (this.elements.activityModal) {
            dom.addClass(this.elements.activityModal, 'active');
        }
    },

    hideCreateModal(): void {
        if (this.elements.activityModal) {
            dom.removeClass(this.elements.activityModal, 'active');
        }
    },

    async handleSubmit(e: Event): Promise<void> {
        e.preventDefault();

        const form = e.target as HTMLFormElement;
        const formData = new FormData(form);

        const data: CreateActivityRequest = {
            title: formData.get('title') as string,
            description: formData.get('description') as string,
            activityType: formData.get('activityType') as ActivityType,
            type: formData.get('activityType') as ActivityType,
            coverImage: formData.get('coverImage') as string,
            startTime: formData.get('startTime') as string,
            endTime: formData.get('endTime') as string,
            location: formData.get('location') as string,
            maxParticipants: parseInt(formData.get('maxParticipants') as string) || null,
            rules: formData.get('rules') as string
        };

        if (!data.title || !data.activityType || !data.startTime || !data.endTime) {
            toast.error('请填写完整信息');
            return;
        }

        if (new Date(data.startTime) >= new Date(data.endTime)) {
            toast.error('结束时间必须晚于开始时间');
            return;
        }

        try {
            await api.activity.create(data);
            toast.success('活动创建成功！');
            this.hideCreateModal();
            form.reset();
            this.loadActivities();
        } catch (error) {
            const err = error as Error;
            toast.error(`创建失败：${  err.message || '请稍后重试'}`);
        }
    }
};

// 挂载到全局对象以供内联事件使用
(window as unknown as { ActivityPage: ActivityPageInterface }).ActivityPage = ActivityPage;

export default ActivityPage;
