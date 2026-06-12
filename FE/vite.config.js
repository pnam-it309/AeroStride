import { fileURLToPath, URL } from 'url';
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import vuetify from 'vite-plugin-vuetify';
import viteCompression from 'vite-plugin-compression';
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '');
    return {
        plugins: [
            vue(),
            vuetify({
                autoImport: true,
                styles: { configFile: 'src/scss/_variables.scss' }
            }),
            viteCompression({
                verbose: true,
                disable: mode === 'development', // Bắt buộc phải tắt lúc dev, nếu không mỗi lần load nó lại ngồi nén file rất lâu
                threshold: 10240, // Compress files larger than 10KB
                algorithm: 'gzip',
                ext: '.gz',
            }),
            viteCompression({
                verbose: true,
                disable: mode === 'development', // Bắt buộc phải tắt lúc dev
                threshold: 10240,
                algorithm: 'brotliCompress',
                ext: '.br',
            }),
        ],
        base: './',
        resolve: {
            alias: {
                '@': fileURLToPath(new URL('./src', import.meta.url))
            }
        },
        css: {
            preprocessorOptions: {
                scss: {}
            }
        },
        optimizeDeps: {
            exclude: ['vuetify'],
            include: [
                'axios',
                'pinia',
                'vue',
                'vue-router',
                'vue-tabler-icons',
                '@vuelidate/core',
                '@vuelidate/validators',
                'apexcharts',
                'vue3-apexcharts'
            ]
        },
        build: {
            rollupOptions: {
                treeshake: true,
                output: {
                    manualChunks(id) {
                        if (id.includes('node_modules')) {
                            if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router')) {
                                return 'vendor-core';
                            }
                            if (id.includes('vuetify')) {
                                return 'vendor-vuetify';
                            }
                            if (id.includes('apexcharts') || id.includes('vue-tabler-icons')) {
                                return 'vendor-charts-icons';
                            }
                            return 'vendor-utils';
                        }
                    }
                }
            },
            sourcemap: false,
            chunkSizeWarningLimit: 1500
        },
        server: {
            allowedHosts: true,
            watch: {
                // Chỉ bật usePolling khi chạy trong Docker (WSL2), trên Windows chạy local thì tắt đi để tránh mất 20s+ khởi động
                usePolling: env.IS_DOCKER === 'true' || process.env.CHOKIDAR_USEPOLLING === 'true', 
                interval: 1000,
                // Bổ sung thêm các thư mục không cần thiết để giảm tải quá trình quét của Polling (khi bật)
                ignored: ['**/node_modules/**', '**/dist/**', '**/.git/**', '**/.vscode/**', '**/.idea/**'],
            },
            host: true,
            strictPort: true,
            port: Number(env.FE_DEV_PORT),
            hmr: {
                clientPort: Number(env.FE_DEV_PORT),
            },
            proxy: {
                '/api': {
                    target: `http://${env.BACKEND_HOST}:${env.BACKEND_PORT}`,
                    changeOrigin: true,
                    secure: false,
                },
                '/uploads': {
                    target: `http://${env.BACKEND_HOST}:${env.BACKEND_PORT}`,
                    changeOrigin: true,
                    secure: false,
                },
                '/ws': {
                    target: `http://${env.BACKEND_HOST}:${env.BACKEND_PORT}`,
                    changeOrigin: true,
                    secure: false,
                    ws: true,
                },
                '/ws-chat': {
                    target: `http://${env.BACKEND_HOST}:${env.BACKEND_PORT}`,
                    changeOrigin: true,
                    secure: false,
                    ws: true,
                },
            }
        }
    };
});