import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { StatusBadge } from './StatusBadge';

describe('StatusBadge', () => {
    describe('user status', () => {
        it('renders 正常 for status NORMAL', () => {
            renderWithProviders(<StatusBadge status="NORMAL" type="user" />);
            expect(screen.getByText('正常')).toBeInTheDocument();
        });

        it('renders 禁用 for status DISABLED', () => {
            renderWithProviders(<StatusBadge status="DISABLED" type="user" />);
            expect(screen.getByText('禁用')).toBeInTheDocument();
        });

        it('renders 锁定 for status LOCKED', () => {
            renderWithProviders(<StatusBadge status="LOCKED" type="user" />);
            expect(screen.getByText('锁定')).toBeInTheDocument();
        });
    });

    describe('product status', () => {
        it('renders 草稿 for status DRAFT', () => {
            renderWithProviders(<StatusBadge status="DRAFT" type="product" />);
            expect(screen.getByText('草稿')).toBeInTheDocument();
        });

        it('renders 待审核 for status PENDING_REVIEW', () => {
            renderWithProviders(<StatusBadge status="PENDING_REVIEW" type="product" />);
            expect(screen.getByText('待审核')).toBeInTheDocument();
        });

        it('renders 已驳回 for status REJECTED', () => {
            renderWithProviders(<StatusBadge status="REJECTED" type="product" />);
            expect(screen.getByText('已驳回')).toBeInTheDocument();
        });

        it('renders 上架 for status ONLINE', () => {
            renderWithProviders(<StatusBadge status="ONLINE" type="product" />);
            expect(screen.getByText('上架')).toBeInTheDocument();
        });

        it('renders 已售 for status SOLD', () => {
            renderWithProviders(<StatusBadge status="SOLD" type="product" />);
            expect(screen.getByText('已售')).toBeInTheDocument();
        });

        it('renders 下架 for status OFFLINE', () => {
            renderWithProviders(<StatusBadge status="OFFLINE" type="product" />);
            expect(screen.getByText('下架')).toBeInTheDocument();
        });

        it('renders raw string for unknown product status', () => {
            renderWithProviders(<StatusBadge status="CUSTOM" type="product" />);
            expect(screen.getByText('CUSTOM')).toBeInTheDocument();
        });
    });

    describe('order status', () => {
        it('renders 待付款 for status PENDING_PAYMENT', () => {
            renderWithProviders(<StatusBadge status="PENDING_PAYMENT" type="order" />);
            expect(screen.getByText('待付款')).toBeInTheDocument();
        });

        it('renders 已完成 for status COMPLETED', () => {
            renderWithProviders(<StatusBadge status="COMPLETED" type="order" />);
            expect(screen.getByText('已完成')).toBeInTheDocument();
        });
    });

    describe('report status', () => {
        it('renders 待处理 for status 0', () => {
            renderWithProviders(<StatusBadge status={0} type="report" />);
            expect(screen.getByText('待处理')).toBeInTheDocument();
        });

        it('renders 已处理 for status 2', () => {
            renderWithProviders(<StatusBadge status={2} type="report" />);
            expect(screen.getByText('已处理')).toBeInTheDocument();
        });
    });

    describe('fallback', () => {
        it('renders 未知 for unknown numeric status', () => {
            renderWithProviders(<StatusBadge status={999} type="user" />);
            expect(screen.getByText('未知')).toBeInTheDocument();
        });

        it('renders status string for string status', () => {
            renderWithProviders(<StatusBadge status="CUSTOM" type="user" />);
            expect(screen.getByText('CUSTOM')).toBeInTheDocument();
        });
    });
});
