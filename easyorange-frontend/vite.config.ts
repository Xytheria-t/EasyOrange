import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'url';
import { dirname, resolve } from 'path';
import { visualizer } from 'rollup-plugin-visualizer';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export default defineConfig({
    plugins: [
        react(),
        ...(process.env.NODE_ENV === 'production' ? [
            visualizer({
                open: true,
                gzipSize: true,
                brotliSize: true,
                filename: 'dist/stats.html'
            })
        ] : [])
    ],
    base: './',
    resolve: {
        alias: {
            '@': resolve(__dirname, 'src'),
            '@api': resolve(__dirname, 'src/api'),
            '@utils': resolve(__dirname, 'src/utils'),
            '@components': resolve(__dirname, 'src/components'),
            '@pages': resolve(__dirname, 'src/pages'),
            '@types': resolve(__dirname, 'src/types'),
            '@constants': resolve(__dirname, 'src/constants'),
            '@assets': resolve(__dirname, 'src/assets'),
            '@hooks': resolve(__dirname, 'src/hooks'),
            '@store': resolve(__dirname, 'src/store'),
            '@lib': resolve(__dirname, 'src/lib')
        }
    },
    server: {
        port: 5173,
        host: '0.0.0.0',
        open: true,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                secure: false
            }
        }
    },
    build: {
        target: 'es2020',
        outDir: 'dist',
        sourcemap: true,
        rollupOptions: {
            output: {
                manualChunks: {
                    vendor: ['react', 'react-dom', 'react-router-dom'],
                    query: ['@tanstack/react-query'],
                    ui: ['zustand'],
                },
                chunkFileNames: 'assets/js/[name]-[hash].js',
                entryFileNames: 'assets/js/[name]-[hash].js',
                assetFileNames: (assetInfo) => {
                    if (/\.(png|jpe?g|gif|svg|webp|ico)$/i.test(assetInfo.name)) {
                        return 'assets/images/[name]-[hash].[ext]';
                    }
                    if (/\.(css|scss|less)$/i.test(assetInfo.name)) {
                        return 'assets/css/[name]-[hash].[ext]';
                    }
                    return 'assets/[name]-[hash].[ext]';
                }
            }
        },
        chunkSizeWarningLimit: 1000,
    },
});
