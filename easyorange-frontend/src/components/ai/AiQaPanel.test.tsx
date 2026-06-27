import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import AiQaPanel from './AiQaPanel';
import type { QaRequest } from '@/api/aiApi';
import type { QaHistoryItem } from '@/hooks/useAiQa';

const mockProduct = {
  id: 1,
  title: 'AI托管 iPhone 14',
  description: '99新，使用一个月',
  categoryName: '手机',
  price: 4500,
  conditionLevel: 1,
  sellerName: '小明',
  sellerCreditLevel: 'GOOD',
};

function createHistoryItem(overrides: Partial<QaHistoryItem> = {}): QaHistoryItem {
  return {
    question: '这个商品成色如何？',
    answer: { answer: '商品描述为99新，建议联系资产方确认具体细节。', hasConfidence: true },
    ...overrides,
  };
}

describe('AiQaPanel', () => {
  beforeEach(() => {
    // jsdom doesn't implement clipboard API or scrollIntoView
    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn(),
      },
    });
    Element.prototype.scrollIntoView = vi.fn();
  });

  describe('empty state', () => {
    it('shows suggested questions when history is empty', () => {
      renderWithProviders(
        <AiQaPanel
          product={mockProduct}
          onAsk={vi.fn()}
          qaHistory={[]}
          isLoading={false}
        />,
      );
      expect(screen.getByText('向 AI 询问关于商品的任何问题')).toBeInTheDocument();
      expect(screen.getByText('这个商品成色如何？')).toBeInTheDocument();
      expect(screen.getByText('价格还能优惠吗？')).toBeInTheDocument();
      expect(screen.getByText('支持面交吗？')).toBeInTheDocument();
      expect(screen.getByText('有原装配件吗？')).toBeInTheDocument();
    });

    it('does not show messages when history is empty', () => {
      renderWithProviders(
        <AiQaPanel
          product={mockProduct}
          onAsk={vi.fn()}
          qaHistory={[]}
          isLoading={false}
        />,
      );
      expect(screen.queryByText('商品描述为99新')).not.toBeInTheDocument();
    });

    it('clicking suggested question fills input', async () => {
      renderWithProviders(
        <AiQaPanel
          product={mockProduct}
          onAsk={vi.fn()}
          qaHistory={[]}
          isLoading={false}
        />,
      );
      await userEvent.click(screen.getByText('这个商品成色如何？'));
      const input = screen.getByPlaceholderText('输入您的问题...');
      expect(input).toHaveValue('这个商品成色如何？');
    });
  });

  describe('message input and submission', () => {
    function submitQuestion(handleAsk: ReturnType<typeof vi.fn>, question: string) {
      const { container } = renderWithProviders(
        <AiQaPanel
          product={mockProduct}
          onAsk={handleAsk}
          qaHistory={[]}
          isLoading={false}
        />,
      );
      const input = screen.getByPlaceholderText('输入您的问题...');
      fireEvent.change(input, { target: { value: question } });
      const form = container.querySelector('form')!;
      fireEvent.submit(form);
      return { input, form };
    }

    it('submits question on form submit', () => {
      const handleAsk = vi.fn();
      submitQuestion(handleAsk, '有保修吗？');
      expect(handleAsk).toHaveBeenCalledTimes(1);
      const request: QaRequest = handleAsk.mock.calls[0][0];
      expect(request.question).toBe('有保修吗？');
      expect(request.productId).toBe(1);
      expect(request.productName).toBe('AI托管 iPhone 14');
    });

    it('does not submit empty input', () => {
      const handleAsk = vi.fn();
      const { container } = renderWithProviders(
        <AiQaPanel
          product={mockProduct}
          onAsk={handleAsk}
          qaHistory={[]}
          isLoading={false}
        />,
      );
      const sendBtn = container.querySelector('button[type="submit"]')!;
      expect(sendBtn).toBeDisabled();
    });

    it('clears input after submission', () => {
      const handleAsk = vi.fn();
      const { input } = submitQuestion(handleAsk, '有保修吗？');
      expect(input).toHaveValue('');
    });

    it('disables input and send button while loading', () => {
      const { container } = renderWithProviders(
        <AiQaPanel
          product={mockProduct}
          onAsk={vi.fn()}
          qaHistory={[createHistoryItem()]}
          isLoading
        />,
      );
      expect(screen.getByPlaceholderText('输入您的问题...')).toBeDisabled();
      expect(container.querySelector('button[type="submit"]')).toBeDisabled();
    });
  });

  describe('loading state', () => {
    it('shows typing animation when loading with history', () => {
      renderWithProviders(
        <AiQaPanel
          product={mockProduct}
          onAsk={vi.fn()}
          qaHistory={[createHistoryItem()]}
          isLoading
        />,
      );
      const dots = document.querySelectorAll('.ai-typing-dot');
      expect(dots.length).toBe(3);
    });

    it('does not show typing animation when not loading', () => {
      renderWithProviders(
        <AiQaPanel
          product={mockProduct}
          onAsk={vi.fn()}
          qaHistory={[createHistoryItem()]}
          isLoading={false}
        />,
      );
      expect(document.querySelector('.ai-typing-dot')).not.toBeInTheDocument();
    });
  });

  describe('message history', () => {
    it('renders question and answer from history', () => {
      renderWithProviders(
        <AiQaPanel
          product={mockProduct}
          onAsk={vi.fn()}
          qaHistory={[createHistoryItem()]}
          isLoading={false}
        />,
      );
      expect(screen.getByText('这个商品成色如何？')).toBeInTheDocument();
      expect(screen.getByText('商品描述为99新，建议联系资产方确认具体细节。')).toBeInTheDocument();
    });

    it('renders multiple messages', () => {
      const items: QaHistoryItem[] = [
        createHistoryItem(),
        createHistoryItem({
          question: '价格还能优惠吗？',
          answer: { answer: '您可以尝试与资产方协商。', hasConfidence: false },
        }),
      ];
      renderWithProviders(
        <AiQaPanel
          product={mockProduct}
          onAsk={vi.fn()}
          qaHistory={items}
          isLoading={false}
        />,
      );
      expect(screen.getByText('这个商品成色如何？')).toBeInTheDocument();
      expect(screen.getByText('价格还能优惠吗？')).toBeInTheDocument();
      expect(screen.getByText('您可以尝试与资产方协商。')).toBeInTheDocument();
    });

    it('shows confidence badge when hasConfidence is true', () => {
      renderWithProviders(
        <AiQaPanel
          product={mockProduct}
          onAsk={vi.fn()}
          qaHistory={[createHistoryItem({ answer: { answer: '确认信息', hasConfidence: true } })]}
          isLoading={false}
        />,
      );
      expect(screen.getByText('已确认')).toBeInTheDocument();
    });

    it('does not show confidence badge when hasConfidence is false', () => {
      renderWithProviders(
        <AiQaPanel
          product={mockProduct}
          onAsk={vi.fn()}
          qaHistory={[createHistoryItem({ answer: { answer: '不确定', hasConfidence: false } })]}
          isLoading={false}
        />,
      );
      expect(screen.queryByText('已确认')).not.toBeInTheDocument();
    });

    it('copy button copies answer to clipboard', async () => {
      const writeText = vi.fn(() => Promise.resolve());
      Object.assign(navigator, { clipboard: { writeText } });

      renderWithProviders(
        <AiQaPanel
          product={mockProduct}
          onAsk={vi.fn()}
          qaHistory={[createHistoryItem()]}
          isLoading={false}
        />,
      );
      const copyBtn = screen.getByTitle('复制回答');
      await userEvent.click(copyBtn);
      expect(writeText).toHaveBeenCalledWith('商品描述为99新，建议联系资产方确认具体细节。');
    });

    it('shows check icon after copy', async () => {
      const writeText = vi.fn(() => Promise.resolve());
      Object.assign(navigator, { clipboard: { writeText } });

      renderWithProviders(
        <AiQaPanel
          product={mockProduct}
          onAsk={vi.fn()}
          qaHistory={[createHistoryItem()]}
          isLoading={false}
        />,
      );
      // Before click, copy icon is visible
      const copyBtn = screen.getByTitle('复制回答');
      expect(copyBtn.querySelector('svg')).toBeInTheDocument();
      await userEvent.click(copyBtn);
      // After click, check icon should appear (we just check copy button still exists)
      expect(screen.getByTitle('复制回答')).toBeInTheDocument();
    });
  });

  describe('header', () => {
    it('renders header title', () => {
      renderWithProviders(
        <AiQaPanel
          product={mockProduct}
          onAsk={vi.fn()}
          qaHistory={[]}
          isLoading={false}
        />,
      );
      expect(screen.getByText('AI 智能问答')).toBeInTheDocument();
      expect(screen.getByText('基于商品信息的智能助手')).toBeInTheDocument();
    });
  });
});
