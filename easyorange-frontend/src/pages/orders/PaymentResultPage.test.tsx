import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PaymentResultPage from './PaymentResultPage';

const mockNavigate = vi.hoisted(() => vi.fn());

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...(actual as object), useNavigate: () => mockNavigate };
});

beforeEach(() => {
  vi.clearAllMocks();
});

function renderPage(initialRoute: string) {
  return renderWithProviders(<PaymentResultPage />, { initialRoute });
}

describe('PaymentResultPage - success', () => {
  it('renders success state with correct title and description', () => {
    renderPage('/payment/result?status=success&orderId=order-123');
    expect(screen.getByText('支付成功')).toBeInTheDocument();
    expect(screen.getByText('您的订单已支付成功，资产方将尽快为您发货')).toBeInTheDocument();
  });

  it('renders order hint for success', () => {
    renderPage('/payment/result?status=success&orderId=order-123');
    expect(screen.getByText('您可以到订单详情中查看物流信息')).toBeInTheDocument();
  });

  it('renders AI recommendation section for success', () => {
    renderPage('/payment/result?status=success&orderId=order-123');
    expect(screen.getByText('智能推荐')).toBeInTheDocument();
    expect(screen.getByText('为你精选相似资产')).toBeInTheDocument();
  });

  it('renders "查看订单" button for success', () => {
    renderPage('/payment/result?status=success&orderId=order-123');
    expect(screen.getByText('查看订单')).toBeInTheDocument();
  });

  it('navigates to order detail when "查看订单" is clicked', async () => {
    renderPage('/payment/result?status=success&orderId=order-123');
    const user = userEvent.setup();
    await user.click(screen.getByText('查看订单'));
    expect(mockNavigate).toHaveBeenCalledWith('/orders/order-123');
  });

  it('navigates to orders when "查看订单" is clicked without orderId', async () => {
    renderPage('/payment/result?status=success');
    const user = userEvent.setup();
    await user.click(screen.getByText('查看订单'));
    expect(mockNavigate).toHaveBeenCalledWith('/orders');
  });
});

describe('PaymentResultPage - failed', () => {
  it('renders failed state with correct title and description', () => {
    renderPage('/payment/result?status=failed&orderId=order-123');
    expect(screen.getByText('支付失败')).toBeInTheDocument();
    expect(screen.getByText('支付未成功，请重新尝试或选择其他支付方式')).toBeInTheDocument();
  });

  it('renders "重新支付" button for failed', () => {
    renderPage('/payment/result?status=failed&orderId=order-123');
    expect(screen.getByText('重新支付')).toBeInTheDocument();
  });

  it('does not render success-only sections', () => {
    renderPage('/payment/result?status=failed&orderId=order-123');
    expect(screen.queryByText('智能推荐')).not.toBeInTheDocument();
    expect(screen.queryByText('您可以到订单详情中查看物流信息')).not.toBeInTheDocument();
    expect(screen.queryByText('查看订单')).not.toBeInTheDocument();
  });

  it('navigates to payment page when "重新支付" is clicked', async () => {
    renderPage('/payment/result?status=failed&orderId=order-123');
    const user = userEvent.setup();
    await user.click(screen.getByText('重新支付'));
    expect(mockNavigate).toHaveBeenCalledWith('/payment?orderId=order-123');
  });

  it('navigates to orders when "重新支付" is clicked without orderId', async () => {
    renderPage('/payment/result?status=failed');
    const user = userEvent.setup();
    await user.click(screen.getByText('重新支付'));
    expect(mockNavigate).toHaveBeenCalledWith('/orders');
  });
});

describe('PaymentResultPage - pending', () => {
  it('renders pending state with correct title and description', () => {
    renderPage('/payment/result?orderId=order-123');
    expect(screen.getByText('支付处理中')).toBeInTheDocument();
    expect(screen.getByText('支付结果确认中，请稍后查看订单状态')).toBeInTheDocument();
  });

  it('renders "查看订单" for pending state', () => {
    renderPage('/payment/result?orderId=order-123');
    expect(screen.getByText('查看订单')).toBeInTheDocument();
  });

  it('navigates to orders when "查看订单" is clicked in pending state', async () => {
    renderPage('/payment/result?orderId=order-123');
    const user = userEvent.setup();
    await user.click(screen.getByText('查看订单'));
    expect(mockNavigate).toHaveBeenCalledWith('/orders');
  });
});

describe('PaymentResultPage - common', () => {
  it('renders "继续购物" button in all states', () => {
    renderPage('/payment/result?status=success&orderId=order-123');
    expect(screen.getByText('继续购物')).toBeInTheDocument();
  });

  it('renders "返回首页" button in all states', () => {
    renderPage('/payment/result?status=success&orderId=order-123');
    expect(screen.getByText('返回首页')).toBeInTheDocument();
  });

  it('navigates to products when "继续购物" is clicked', async () => {
    renderPage('/payment/result?status=success&orderId=order-123');
    const user = userEvent.setup();
    await user.click(screen.getByText('继续购物'));
    expect(mockNavigate).toHaveBeenCalledWith('/products');
  });

  it('navigates to home when "返回首页" is clicked', async () => {
    renderPage('/payment/result?status=success&orderId=order-123');
    const user = userEvent.setup();
    await user.click(screen.getByText('返回首页'));
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('renders page title "支付结果"', () => {
    renderPage('/payment/result?status=success&orderId=order-123');
    expect(screen.getByText('支付结果')).toBeInTheDocument();
  });
});
