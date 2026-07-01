import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { type RenderOptions, render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter } from 'react-router-dom';

export function createTestQueryClient() {
    return new QueryClient({
        defaultOptions: {
            queries: { retry: false, gcTime: 0 },
            mutations: { retry: false },
        },
    });
}

interface RenderWithProvidersOptions extends Omit<RenderOptions, 'wrapper'> {
    initialRoute?: string;
    queryClient?: QueryClient;
}

export function renderWithProviders(ui: ReactElement, options: RenderWithProvidersOptions = {}) {
    const { initialRoute = '/', queryClient = createTestQueryClient(), ...renderOptions } = options;

    function Wrapper({ children }: { children: React.ReactNode }) {
        return (
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={[initialRoute]}>{children}</MemoryRouter>
            </QueryClientProvider>
        );
    }

    return { ...render(ui, { wrapper: Wrapper, ...renderOptions }), queryClient };
}
