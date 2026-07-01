import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeAll, describe, expect, it, vi } from 'vitest';
import { AdminSelect } from './AdminSelect';

const options = [
    { value: '1', label: 'Option A' },
    { value: '2', label: 'Option B' },
    { value: '3', label: 'Option C' },
];

beforeAll(() => {
    // jsdom does not implement these PointerEvent APIs that Radix Select uses
    Element.prototype.scrollIntoView = vi.fn();
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
});

describe('AdminSelect', () => {
    it('shows selected label', () => {
        render(<AdminSelect options={options} value="1" onChange={() => {}} />);
        expect(screen.getByText('Option A')).toBeInTheDocument();
    });

    it('shows placeholder when no value matches', () => {
        render(<AdminSelect options={options} value="" onChange={() => {}} placeholder="请选择" />);
        expect(screen.getByText('请选择')).toBeInTheDocument();
    });

    it('opens dropdown on click', async () => {
        render(<AdminSelect options={options} value="1" onChange={() => {}} />);
        await userEvent.click(screen.getByRole('combobox'));
        expect(screen.getByText('Option B')).toBeInTheDocument();
        expect(screen.getByText('Option C')).toBeInTheDocument();
    });

    it('selects option on click', async () => {
        const onChange = vi.fn();
        render(<AdminSelect options={options} value="1" onChange={onChange} />);
        await userEvent.click(screen.getByRole('combobox'));
        await userEvent.click(screen.getByText('Option B'));
        expect(onChange).toHaveBeenCalledWith('2');
    });

    it('closes dropdown after selection', async () => {
        const onChange = vi.fn();
        render(<AdminSelect options={options} value="1" onChange={onChange} />);
        await userEvent.click(screen.getByRole('combobox'));
        await userEvent.click(screen.getByText('Option B'));
        expect(screen.queryByText('Option C')).not.toBeInTheDocument();
    });

    it('shows checkmark on selected option', async () => {
        render(<AdminSelect options={options} value="1" onChange={() => {}} />);
        await userEvent.click(screen.getByRole('combobox'));
        const selectedItem = screen.getByRole('option', { selected: true });
        expect(selectedItem).toHaveTextContent('Option A');
        expect(selectedItem.querySelector('svg')).toBeInTheDocument();
    });

    it('closes dropdown on escape', async () => {
        render(<AdminSelect options={options} value="1" onChange={() => {}} />);
        await userEvent.click(screen.getByRole('combobox'));
        expect(screen.getByText('Option B')).toBeInTheDocument();
        await userEvent.keyboard('{Escape}');
        expect(screen.queryByText('Option B')).not.toBeInTheDocument();
    });
});
