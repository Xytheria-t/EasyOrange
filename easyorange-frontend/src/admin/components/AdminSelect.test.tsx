import { describe, it, expect, vi, beforeAll } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { AdminSelect } from './AdminSelect';

beforeAll(() => {
  // scrollIntoView is not available in jsdom
  Element.prototype.scrollIntoView = vi.fn();
});

const options = [
  { value: '1', label: 'Option A' },
  { value: '2', label: 'Option B' },
  { value: '3', label: 'Option C' },
];

describe('AdminSelect', () => {
  it('shows selected label', () => {
    render(<AdminSelect options={options} value="1" onChange={() => {}} />);
    expect(screen.getByText('Option A')).toBeInTheDocument();
  });

  it('shows placeholder when no value matches', () => {
    render(
      <AdminSelect options={options} value="" onChange={() => {}} placeholder="请选择" />,
    );
    expect(screen.getByText('请选择')).toBeInTheDocument();
  });

  it('opens dropdown on click', () => {
    render(<AdminSelect options={options} value="1" onChange={() => {}} />);
    fireEvent.click(screen.getByText('Option A'));
    expect(screen.getByText('Option B')).toBeInTheDocument();
    expect(screen.getByText('Option C')).toBeInTheDocument();
  });

  it('selects option on click', () => {
    const onChange = vi.fn();
    render(<AdminSelect options={options} value="1" onChange={onChange} />);
    fireEvent.click(screen.getByText('Option A'));
    fireEvent.click(screen.getByText('Option B'));
    expect(onChange).toHaveBeenCalledWith('2');
  });

  it('closes dropdown after selection', () => {
    const onChange = vi.fn();
    render(<AdminSelect options={options} value="1" onChange={onChange} />);
    fireEvent.click(screen.getByText('Option A'));
    fireEvent.click(screen.getByText('Option B'));
    expect(screen.queryByText('Option C')).not.toBeInTheDocument();
  });

  it('shows checkmark on selected option', () => {
    render(<AdminSelect options={options} value="1" onChange={() => {}} />);
    fireEvent.click(screen.getByText('Option A'));
    // After opening, find the Option A inside the dropdown (portal) and check for checkmark
    const optionButtons = screen.getAllByText('Option A');
    expect(optionButtons.length).toBeGreaterThanOrEqual(1);
    // The last one should be in the dropdown portal
    const dropdownOption = optionButtons[optionButtons.length - 1].closest('button');
    const checkmarkPaths = dropdownOption?.querySelector('svg');
    expect(checkmarkPaths).toBeInTheDocument();
  });

  it('toggles dropdown open/close', () => {
    render(<AdminSelect options={options} value="1" onChange={() => {}} />);
    fireEvent.click(screen.getByText('Option A'));
    expect(screen.getByText('Option B')).toBeInTheDocument();
    // Click the trigger button (the first Option A) to close
    const triggerButton = screen.getAllByText('Option A')[0].closest('button');
    fireEvent.click(triggerButton!);
    expect(screen.queryByText('Option B')).not.toBeInTheDocument();
  });
});
