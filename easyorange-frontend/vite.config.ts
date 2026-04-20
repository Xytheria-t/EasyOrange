import { defineConfig } from 'vite';
import { fileURLToPath } from 'url';
import { dirname, resolve } from 'path';
import { visualizer } from 'rollup-plugin-visualizer';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

export default defineConfig({
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
            '@assets': resolve(__dirname, 'src/assets')
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
        assetsDir: 'assets',
        minify: 'terser',
        sourcemap: 'hidden',
        assetsInlineLimit: 4096,
        rollupOptions: {
            input: {
                main: resolve(__dirname, 'index.html'),
                products: resolve(__dirname, 'products.html'),
                profile: resolve(__dirname, 'profile.html'),
                publish: resolve(__dirname, 'publish.html'),
                favorites: resolve(__dirname, 'favorites.html'),
                messages: resolve(__dirname, 'messages.html')
            },
            output: {
                manualChunks: (id) => {
                    if (id.includes('node_modules')) {
                        return 'vendor';
                    }
                },
                chunkFileNames: (chunkInfo) => {
                    const facadeModuleId = chunkInfo.facadeModuleId 
                        ? chunkInfo.facadeModuleId.split('/').pop() 
                        : 'chunk';
                    
                    if (facadeModuleId.includes('api')) {
                        return 'assets/js/api/[name]-[hash].js';
                    }
                    if (facadeModuleId.includes('components')) {
                        return 'assets/js/components/[name]-[hash].js';
                    }
                    if (facadeModuleId.includes('pages')) {
                        return 'assets/js/pages/[name]-[hash].js';
                    }
                    if (facadeModuleId.includes('utils')) {
                        return 'assets/js/utils/[name]-[hash].js';
                    }
                    return 'assets/js/[name]-[hash].js';
                },
                assetFileNames: (assetInfo) => {
                    const info = assetInfo.name.split('.');
                    const ext = info[info.length - 1];
                    
                    if (/\.(png|jpe?g|gif|svg|webp|ico)$/i.test(assetInfo.name)) {
                        return 'assets/images/[name]-[hash].[ext]';
                    }
                    if (/\.(css|scss|less)$/i.test(assetInfo.name)) {
                        return 'assets/css/[name]-[hash].[ext]';
                    }
                    if (/\.(woff2?|eot|ttf|otf)$/i.test(assetInfo.name)) {
                        return 'assets/fonts/[name]-[hash].[ext]';
                    }
                    return 'assets/[name]-[hash].[ext]';
                }
            }
        },
        terserOptions: {
            compress: {
                drop_console: true,
                drop_debugger: true,
                dead_code: true,
                evaluate: true,
                unused: true
            },
            format: {
                comments: false
            }
        },
        chunkSizeWarningLimit: 1000,
        cssCodeSplit: true,
        reportCompressedSize: true
    },
    plugins: process.env.NODE_ENV === 'production' ? [
        visualizer({
            open: true,
            gzipSize: true,
            brotliSize: true,
            filename: 'dist/stats.html'
        })
    ] : []
});
