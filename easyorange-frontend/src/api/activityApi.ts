/**
 * @fileoverview 活动 API 模块
 */

import type { PageResult } from '../types/index.js';
import { request } from './core/request.js';

export const ACTIVITY_STATUS = { ONGOING: 1, ENDED: 0, CANCELLED: -1 } as const;
export const ACTIVITY_TYPE_NAMES: Record<string, string> = {};
export const ACTIVITY_STATUS_NAMES: Record<string, string> = {};

export type ActivityType = string;
export type ActivityStatus = number;

export interface Activity {
    id: number;
    title: string;
    description: string;
    startTime: string;
    endTime: string;
    location: string;
    maxParticipants: number;
    currentParticipants: number;
    organizerId: number;
    organizerName: string;
    status: ActivityStatus;
    type: ActivityType;
    activityType?: string;
    coverImage?: string;
    isJoined?: boolean;
    createTime: string;
}

export interface CreateActivityRequest {
    title: string;
    description: string;
    startTime: string;
    endTime: string;
    location: string;
    maxParticipants: number | null;
    type: ActivityType;
    activityType?: string;
    coverImage?: string;
    rules?: string;
}

export const activityApi = {
    getActivities(_params?: { status?: number }) {
        return request<PageResult<Activity>>('/activities', {
            method: 'GET'
        });
    },

    getMyActivities() {
        return request<PageResult<Activity>>('/activities/my', {
            method: 'GET'
        });
    },

    getJoinedActivities() {
        return request<PageResult<Activity>>('/activities/joined', {
            method: 'GET'
        });
    },

    join(activityId: number) {
        return request(`/activities/${activityId}/join`, {
            method: 'POST'
        });
    },

    leave(activityId: number) {
        return request(`/activities/${activityId}/leave`, {
            method: 'POST'
        });
    },

    create(data: CreateActivityRequest) {
        return request('/activities', {
            method: 'POST',
            body: data
        });
    }
};
