import { describe, it, expect } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { server } from '@/testUtils/mocks/server';
import {
  usePayment,
  usePaymentByOrder,
  usePaymentStatus,
  useCreatePayment,
  useRefundPayment,
  useClosePayment,
} from './usePayment';
import type { ReactNode } from 'react';

const testQc = new QueryClient({
  defaultOptions: {
    queries: { retry: false, gcTime: 0 },
    mutations: { retry: false },
  },
});

function Wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={testQc}>{children}</QueryClientProvider>;
}


describe('usePayment', () => {
  it('returns payment info', async () => {
    server.use(
      http.get('/api/payments/pay-1', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: { id: 'pay-1', paymentNo: 'PAY001', orderId: 'ord-1', amount: 100, status: 'PENDING' },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => usePayment('pay-1'), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.id).toBe('pay-1');
    expect(result.current.data?.paymentNo).toBe('PAY001');
    expect(result.current.data?.amount).toBe(100);
  });

  it('is not enabled when id is empty', () => {
    const { result } = renderHook(() => usePayment(''), {
      wrapper: Wrapper,
    });

    expect(result.current.fetchStatus).toBe('idle');
  });
});

describe('usePaymentByOrder', () => {
  it('returns payment by order id', async () => {
    server.use(
      http.get('/api/payments/orders/ord-1', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: { id: 'pay-1', orderId: 'ord-1', amount: 100, status: 'PENDING' },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => usePaymentByOrder('ord-1'), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.orderId).toBe('ord-1');
  });
});

describe('usePaymentStatus', () => {
  it('returns payment status', async () => {
    server.use(
      http.get('/api/payments/pay-1/status', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: { status: 'PENDING', paymentMethod: null, payTime: null },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => usePaymentStatus('pay-1'), {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.status).toBe('PENDING');
  });
});

describe('useCreatePayment', () => {
  it('creates payment successfully', async () => {
    server.use(
      http.post('/api/payments', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: { paymentId: 'pay-1', paymentNo: 'PAY001', status: 'PENDING' },
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useCreatePayment(), {
      wrapper: Wrapper,
    });

    result.current.mutate({ orderId: 'ord-1', paymentMethod: 'ALIPAY' });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.paymentId).toBe('pay-1');
  });
});

describe('useRefundPayment', () => {
  it('refunds payment successfully', async () => {
    server.use(
      http.post('/api/payments/pay-1/refund', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: null,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useRefundPayment(), {
      wrapper: Wrapper,
    });

    result.current.mutate('pay-1');

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});

describe('useClosePayment', () => {
  it('closes payment successfully', async () => {
    server.use(
      http.post('/api/payments/pay-1/close', () => {
        return HttpResponse.json({
          code: 'A0000',
          message: 'success',
          data: null,
          timestamp: Date.now(),
        });
      }),
    );

    const { result } = renderHook(() => useClosePayment(), {
      wrapper: Wrapper,
    });

    result.current.mutate('pay-1');

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});
