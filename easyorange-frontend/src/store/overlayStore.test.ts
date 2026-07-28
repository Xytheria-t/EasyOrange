import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

describe('overlayStore', () => {
    let openOverlayLayer: (layerId: string, bodyClass?: string) => void;
    let closeOverlayLayer: (layerId: string, bodyClass?: string) => void;

    beforeEach(async () => {
        vi.resetModules();
        document.body.className = '';
        document.body.style.overflow = '';

        const module = await import('./overlayStore');
        openOverlayLayer = module.openOverlayLayer;
        closeOverlayLayer = module.closeOverlayLayer;
    });

    afterEach(() => {
        document.body.className = '';
        document.body.style.overflow = '';
    });

    it('opens overlay layer and locks body scroll', () => {
        openOverlayLayer('modal-1');

        expect(document.body.classList.contains('ui-layer-open')).toBe(true);
        expect(document.body.style.overflow).toBe('hidden');
    });

    it('adds custom body class when provided', () => {
        openOverlayLayer('drawer-1', 'drawer-open');

        expect(document.body.classList.contains('drawer-open')).toBe(true);
        expect(document.body.classList.contains('ui-layer-open')).toBe(true);
    });

    it('closes overlay layer and restores body scroll when no layers remain', () => {
        openOverlayLayer('modal-1');
        closeOverlayLayer('modal-1');

        expect(document.body.classList.contains('ui-layer-open')).toBe(false);
        expect(document.body.style.overflow).toBe('');
    });

    it('removes custom body class when layer closes', () => {
        openOverlayLayer('drawer-1', 'drawer-open');
        closeOverlayLayer('drawer-1', 'drawer-open');

        expect(document.body.classList.contains('drawer-open')).toBe(false);
        expect(document.body.classList.contains('ui-layer-open')).toBe(false);
    });

    it('keeps body class when other layers still own it', () => {
        openOverlayLayer('drawer-a', 'drawer-open');
        openOverlayLayer('drawer-b', 'drawer-open');

        closeOverlayLayer('drawer-a', 'drawer-open');

        expect(document.body.classList.contains('drawer-open')).toBe(true);
        expect(document.body.classList.contains('ui-layer-open')).toBe(true);

        closeOverlayLayer('drawer-b', 'drawer-open');

        expect(document.body.classList.contains('drawer-open')).toBe(false);
    });

    it('keeps scroll locked when body already has modal-open class', () => {
        document.body.classList.add('modal-open');
        openOverlayLayer('modal-1');
        closeOverlayLayer('modal-1');

        expect(document.body.style.overflow).toBe('hidden');
    });

    it('handles closing a layer that was never opened', () => {
        expect(() => closeOverlayLayer('unknown')).not.toThrow();
        expect(document.body.classList.contains('ui-layer-open')).toBe(false);
    });

    it('handles removing a body class that was never added', () => {
        expect(() => closeOverlayLayer('unknown', 'missing-class')).not.toThrow();
        expect(document.body.classList.contains('missing-class')).toBe(false);
    });

    it('supports multiple independent layers', () => {
        openOverlayLayer('layer-1', 'class-a');
        openOverlayLayer('layer-2', 'class-b');

        expect(document.body.classList.contains('class-a')).toBe(true);
        expect(document.body.classList.contains('class-b')).toBe(true);
        expect(document.body.classList.contains('ui-layer-open')).toBe(true);

        closeOverlayLayer('layer-1', 'class-a');

        expect(document.body.classList.contains('class-a')).toBe(false);
        expect(document.body.classList.contains('class-b')).toBe(true);
        expect(document.body.classList.contains('ui-layer-open')).toBe(true);
    });
});
