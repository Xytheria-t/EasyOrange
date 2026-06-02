import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'url';
import { dirname, resolve } from 'path';
import { visualizer } from 'rollup-plugin-visualizer';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const isAnalyze = process.env.ANALYZE === 'true' || process.env.NODE_ENV === 'analyze';
const shouldVisualize = process.env.NODE_ENV === 'production' || isAnalyze;

export default defineConfig({
    plugins: [
        react(),
        ...(shouldVisualize ? [
            visualizer({
                open: false,
                gzipSize: true,
                brotliSize: true,
                filename: 'dist/stats.html',
                template: 'treemap'
            })
        ] : [])
    ],
    base: './',
    resolve: {
        tsconfigPaths: true
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
        sourcemap: false,
        minify: 'terser',
        terserOptions: {
            compress: {
                drop_console: true,
                drop_debugger: true,
                pure_funcs: ['console.log', 'console.info', 'console.debug']
            }
        },
        rolldownOptions: {
            output: {
                manualChunks(id) {
                    if (id.includes('node_modules')) {
                        if (id.includes('recharts') || id.includes('d3-') || id.includes('victory-vendor')) {
                            return 'vendor-recharts';
                        }
                        if (id.includes('react') || id.includes('react-dom') || id.includes('react-router-dom')) {
                            return 'vendor-react';
                        }
                        if (id.includes('@tanstack/react-query')) {
                            return 'vendor-query';
                        }
                        if (id.includes('zustand')) {
                            return 'vendor-state';
                        }
                        if (id.includes('lucide-react')) {
                            return 'vendor-icons';
                        }
                        return 'vendor';
                    }
                    if (id.includes('/components/sections/')) {
                        return 'sections';
                    }
                    if (id.includes('/components/ui/')) {
                        return 'ui-components';
                    }
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
        chunkSizeWarningLimit: 500,
    },
});
