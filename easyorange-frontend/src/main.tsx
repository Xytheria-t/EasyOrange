import { QueryClientProvider } from '@tanstack/react-query';
import React from 'react';
import ReactDOM from 'react-dom/client';
import { HelmetProvider } from 'react-helmet-async';
import App from './App';
import { restoreSession } from './features/auth/session';
import { queryClient } from './lib/queryClient';
import './styles/tailwind.css';
import './styles/main.css';
import './components/sections/floating-nav.css';

restoreSession();

// Devtools 仅开发态加载，生产 bundle 不含 react-query-devtools
const ReactQueryDevtools = import.meta.env.DEV
    ? React.lazy(() => import('@tanstack/react-query-devtools').then(m => ({ default: m.ReactQueryDevtools })))
    : () => null;

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
    <React.StrictMode>
        <HelmetProvider>
            <QueryClientProvider client={queryClient}>
                <App />
                <ReactQueryDevtools initialIsOpen={false} />
            </QueryClientProvider>
        </HelmetProvider>
    </React.StrictMode>
);
