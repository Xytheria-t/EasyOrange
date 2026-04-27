const activeLayers = new Set<string>();
const bodyClassOwners = new Map<string, Set<string>>();

function syncBodyState(): void {
    const shouldLockScroll = activeLayers.size > 0 || document.body.classList.contains('modal-open');
    document.body.classList.toggle('ui-layer-open', activeLayers.size > 0);
    document.body.style.overflow = shouldLockScroll ? 'hidden' : '';
}

function addBodyClassOwner(layerId: string, bodyClass: string): void {
    const owners = bodyClassOwners.get(bodyClass) ?? new Set<string>();
    owners.add(layerId);
    bodyClassOwners.set(bodyClass, owners);
    document.body.classList.add(bodyClass);
}

function removeBodyClassOwner(layerId: string, bodyClass: string): void {
    const owners = bodyClassOwners.get(bodyClass);
    if (!owners) {
        return;
    }

    owners.delete(layerId);
    if (owners.size > 0) {
        return;
    }

    bodyClassOwners.delete(bodyClass);
    document.body.classList.remove(bodyClass);
}

export function openOverlayLayer(layerId: string, bodyClass?: string): void {
    activeLayers.add(layerId);
    if (bodyClass) {
        addBodyClassOwner(layerId, bodyClass);
    }
    syncBodyState();
}

export function closeOverlayLayer(layerId: string, bodyClass?: string): void {
    activeLayers.delete(layerId);
    if (bodyClass) {
        removeBodyClassOwner(layerId, bodyClass);
    }
    syncBodyState();
}
