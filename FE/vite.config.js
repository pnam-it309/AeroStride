import { fileURLToPath, URL } from 'url';
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import vuetify from 'vite-plugin-vuetify';
import viteCompression from 'vite-plugin-compression';
import path from 'path';

function optimizeTablerIcons() {
    return {
        name: 'optimize-tabler-icons',
        enforce: 'pre',
        apply: 'build', // Only run in build to prevent dev server reloads
        transform(code, id) {
            if (id.includes('node_modules')) return null;
            if (code.includes('vue-tabler-icons')) {
                const regex = /import\s+\{([^}]+)\}\s+from\s+['"]vue-tabler-icons['"]/g;
                return {
                    code: code.replace(regex, (match, imports) => {
                        const iconNames = imports.split(',').map(i => i.trim()).filter(Boolean);
                        // Redirect to specific files so Vite doesn't load the entire library index
                        return iconNames.map(icon => `import ${icon} from 'vue-tabler-icons/icons/${icon}.js';`).join('\n');
                    }),
                    map: null
                };
            }
            return null;
        }
    };
}

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '');
    return {
        plugins: [
            optimizeTablerIcons(),
            vue({
                template: {
                    compilerOptions: {
                        isCustomElement: (tag) => tag === 'model-viewer'
                    }
                }
            }),
            vuetify({
                autoImport: true,
                styles: { configFile: 'src/scss/_variables.scss' }
            }),
            viteCompression({
                verbose: false,
                disable: mode === 'development',
                threshold: 10240,
                algorithm: 'gzip',
                ext: '.gz',
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
                scss: {
                    // Tắt sourcemap cho CSS ở bản build để nhẹ hơn, nhưng bật ở dev để debug
                    sourceMap: mode === 'development',
                }
            }
        },
        optimizeDeps: {
            exclude: ['vuetify'],
            // Ép Vite bundle sẵn các thư viện này ngay khi khởi động (Cold Start)
            // Tránh việc đang dùng thì web bị khựng để "Re-bundling dependencies"
            include: [
                'vue',
                'pinia',
                'vue-router',
                'axios',
                'apexcharts',
                'vue3-apexcharts',
                'lodash/debounce',
                'date-fns',
                '@google/model-viewer',
                'vue3-perfect-scrollbar',
                'vue-tabler-icons'
            ]
        },
        build: {
            rollupOptions: {
                treeshake: true,
                output: {
                    manualChunks(id) {
                        if (id.includes('node_modules')) {
                            // Core libraries
                            if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router')) {
                                return 'vendor-core';
                            }
                            // Heavy 3D libraries
                            if (id.includes('three') || id.includes('@google/model-viewer')) {
                                return 'vendor-3d';
                            }
                            // Charting libraries
                            if (id.includes('apexcharts')) {
                                return 'vendor-charts';
                            }
                            // Tiptap/Editor libraries
                            if (id.includes('@tiptap')) {
                                return 'vendor-editor';
                            }
                            // Icons
                            if (id.includes('vue-tabler-icons')) {
                                return 'vendor-icons';
                            }
                            // Vuetify
                            if (id.includes('vuetify')) {
                                return 'vendor-vuetify';
                            }
                            
                            return 'vendor-utils';
                        }
                    }
                }
            },
            sourcemap: false,
            chunkSizeWarningLimit: 2000
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