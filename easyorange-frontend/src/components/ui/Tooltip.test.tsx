import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { Button } from './button';
import { Tooltip } from './Tooltip';

function renderTooltip() {
    return render(
        <Tooltip content="分享给好友">
            <Button>share</Button>
        </Tooltip>
    );
}

// hidden 属性的元素不进可及性树，getByRole 查不到；显隐一律按文本节点断言
function tip() {
    return screen.getByText('分享给好友');
}

describe('Tooltip', () => {
    it('hides the bubble until hover', () => {
        renderTooltip();
        expect(tip()).toHaveAttribute('hidden');
    });

    it('shows on hover and links via aria-describedby', () => {
        renderTooltip();
        fireEvent.mouseOver(screen.getByText('share'));
        const target = tip();
        expect(target).not.toHaveAttribute('hidden');
        expect(screen.getByText('share')).toHaveAttribute('aria-describedby', target.id);
    });

    it('hides again on mouse out', () => {
        renderTooltip();
        fireEvent.mouseOver(screen.getByText('share'));
        fireEvent.mouseOut(screen.getByText('share'));
        expect(tip()).toHaveAttribute('hidden');
        expect(screen.getByText('share')).not.toHaveAttribute('aria-describedby');
    });

    it('shows on keyboard focus only after keyboard input', () => {
        renderTooltip();
        fireEvent.focus(screen.getByText('share'));
        expect(tip()).toHaveAttribute('hidden');

        fireEvent.keyDown(document, { key: 'Tab' });
        fireEvent.focus(screen.getByText('share'));
        expect(tip()).not.toHaveAttribute('hidden');
    });

    it('dismisses on Escape while shown', () => {
        renderTooltip();
        fireEvent.mouseOver(screen.getByText('share'));
        fireEvent.keyDown(screen.getByText('share'), { key: 'Escape' });
        expect(tip()).toHaveAttribute('hidden');
    });

    it('keeps the wrapped child clickable with its own handler', () => {
        const onClick = vi.fn();
        render(
            <Tooltip content="点赞">
                <Button onClick={onClick}>like</Button>
            </Tooltip>
        );
        fireEvent.click(screen.getByText('like'));
        expect(onClick).toHaveBeenCalledTimes(1);
    });

    it('applies placement class', () => {
        render(
            <Tooltip content="tip" placement="bottom">
                <Button>b</Button>
            </Tooltip>
        );
        expect(screen.getByText('tip')).toHaveClass('tt-bottom');
    });
});
