import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ChatInputBar from './ChatInputBar';

describe('ChatInputBar', () => {
  it('has placeholder when not disabled', () => {
    render(<ChatInputBar onSend={() => {}} onTyping={() => {}} />);
    expect(screen.getByPlaceholderText('输入消息...')).toBeInTheDocument();
  });

  it('send button is disabled when textarea is empty', () => {
    render(<ChatInputBar onSend={() => {}} onTyping={() => {}} />);
    expect(screen.getByLabelText('发送')).toBeDisabled();
  });

  it('send button is enabled when textarea has text', async () => {
    render(<ChatInputBar onSend={() => {}} onTyping={() => {}} />);
    const textarea = screen.getByPlaceholderText('输入消息...');
    await userEvent.type(textarea, 'hello');
    expect(screen.getByLabelText('发送')).toBeEnabled();
  });

  it('calls onSend with trimmed content on button click', async () => {
    const onSend = vi.fn();
    render(<ChatInputBar onSend={onSend} onTyping={() => {}} />);
    const textarea = screen.getByPlaceholderText('输入消息...');
    await userEvent.type(textarea, 'hello');
    await userEvent.click(screen.getByLabelText('发送'));
    expect(onSend).toHaveBeenCalledWith('hello');
  });

  it('clears textarea after sending', async () => {
    const onSend = vi.fn();
    render(<ChatInputBar onSend={onSend} onTyping={() => {}} />);
    const textarea = screen.getByPlaceholderText('输入消息...') as HTMLTextAreaElement;
    await userEvent.type(textarea, 'hello');
    await userEvent.click(screen.getByLabelText('发送'));
    expect(textarea.value).toBe('');
  });

  it('calls onSend on Enter key without Shift', async () => {
    const onSend = vi.fn();
    render(<ChatInputBar onSend={onSend} onTyping={() => {}} />);
    const textarea = screen.getByPlaceholderText('输入消息...');
    await userEvent.type(textarea, 'hello{Enter}');
    expect(onSend).toHaveBeenCalledWith('hello');
  });

  it('does NOT call onSend on Shift+Enter', async () => {
    const onSend = vi.fn();
    render(<ChatInputBar onSend={onSend} onTyping={() => {}} />);
    const textarea = screen.getByPlaceholderText('输入消息...');
    await userEvent.type(textarea, 'hello');
    await userEvent.keyboard('{Shift>}{Enter}{/Shift}');
    expect(onSend).not.toHaveBeenCalled();
  });

  it('calls onTyping when user types', async () => {
    const onTyping = vi.fn();
    render(<ChatInputBar onSend={() => {}} onTyping={onTyping} />);
    const textarea = screen.getByPlaceholderText('输入消息...');
    await userEvent.type(textarea, 'h');
    expect(onTyping).toHaveBeenCalled();
  });

  it('textarea is disabled when disabled prop is true', () => {
    render(<ChatInputBar onSend={() => {}} onTyping={() => {}} disabled={true} />);
    expect(screen.getByPlaceholderText('')).toBeDisabled();
  });

  it('send button is disabled when disabled prop is true', () => {
    render(<ChatInputBar onSend={() => {}} onTyping={() => {}} disabled={true} />);
    expect(screen.getByLabelText('发送')).toBeDisabled();
  });

  it('does not call onSend when disabled', async () => {
    const onSend = vi.fn();
    render(<ChatInputBar onSend={onSend} onTyping={() => {}} disabled={true} />);
    await userEvent.click(screen.getByLabelText('发送'));
    expect(onSend).not.toHaveBeenCalled();
  });
});
