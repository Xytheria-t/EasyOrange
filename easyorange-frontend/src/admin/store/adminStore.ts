import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

interface AdminState {
    sidebarCollapsed: boolean;
    toggleSidebar: () => void;
    setSidebarCollapsed: (collapsed: boolean) => void;
    currentMenu: string;
    setCurrentMenu: (menu: string) => void;
}

export const useAdminStore = create<AdminState>()(
    persist(
        set => ({
            sidebarCollapsed: false,
            currentMenu: 'dashboard',

            toggleSidebar: () => set(state => ({ sidebarCollapsed: !state.sidebarCollapsed })),

            setSidebarCollapsed: collapsed => set({ sidebarCollapsed: collapsed }),

            setCurrentMenu: menu => set({ currentMenu: menu }),
        }),
        {
            name: 'admin-storage',
            storage: createJSONStorage(() => localStorage),
            partialize: state => ({
                sidebarCollapsed: state.sidebarCollapsed,
                currentMenu: state.currentMenu,
            }),
        }
    )
);
