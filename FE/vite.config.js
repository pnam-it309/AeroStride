import { fileURLToPath, URL } from 'url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import vuetify from 'vite-plugin-vuetify';

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [
        vue(),
        vuetify({
            autoImport: true,
            styles: { configFile: 'src/scss/_variables.scss' }
        })
    ],
    base: './',
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        }
    },
    esbuild: {
        target: 'esnext',
        supported: {
            'object-rest-spread': true
        }
    },
    build: {
        target: 'esnext',
        rollupOptions: {
            treeshake: true,
            output: {
                manualChunks(id) {
                    if (id.includes('node_modules')) {
                        const parts = id.split('node_modules/')[1].split('/');
                        const scopeOrName = parts[0].startsWith('@')
                            ? `${parts[0]}-${parts[1]}`
                            : parts[0];

                        if (scopeOrName === 'vuetify' || scopeOrName.startsWith('vuetify-') || scopeOrName.startsWith('@vuetify')) {
                            return 'vendor-vuetify';
                        }

                        if (scopeOrName === 'vue-demi') {
                            return 'vendor-vue';
                        }

                        if (scopeOrName === 'vue' || scopeOrName.startsWith('@vue-')) {
                            return 'vendor-vue';
                        }

                        if (scopeOrName === 'vue-router' || scopeOrName === 'pinia' || scopeOrName === 'axios') {
                            return 'vendor-core';
                        }

                        if (['vue-router', 'pinia', 'axios'].includes(scopeOrName)) {
                            return 'vendor-core';
                        }

                        if (scopeOrName === 'vue-tabler-icons') {
                            return 'vendor-icons';
                        }

                        if (scopeOrName === 'vue3-apexcharts') {
                            return 'vendor-vue3-apexcharts';
                        }

                        if (scopeOrName === 'vue3-perfect-scrollbar') {
                            return 'vendor-vue3-perfect-scrollbar';
                        }

                        if (scopeOrName === 'qrcode.vue') {
                            return 'vendor-qrcode-vue';
                        }

                        if (scopeOrName === 'html5-qrcode' || scopeOrName === 'apexcharts' || scopeOrName === 'date-fns' || scopeOrName === 'maska') {
                            return `vendor-${scopeOrName.replace('@', '').replace('/', '-')}`;
                        }

                        return `vendor-${scopeOrName.replace('@', '').replace('/', '-')}`;
                    }
                }
            }
        },
        sourcemap: false,
        chunkSizeWarningLimit: 2000
    },
    css: {
        preprocessorOptions: {
            scss: {
                silenceDeprecations: ['global-builtin', 'if-function'],
                quietDeps: true
            },
            sass: {
                silenceDeprecations: ['global-builtin', 'if-function'],
                quietDeps: true
            }
        }
    },
    optimizeDeps: {
        esbuildOptions: {
            target: 'esnext'
        },
        exclude: ['vuetify', '@stomp/stompjs', '@vuelidate/core', '@vuelidate/validators'],
        include: [
            'axios',
            'pinia',
            'vue',
            'vue-router',
            'vue-tabler-icons'
        ],
        entries: ['./index.html', './src/**/*.{js,vue}']
    },
    server: {
        watch: {
            usePolling: true,
            interval: 1000, // Kiểm tra file mỗi 1s để tránh quá tải CPU Windows
        },
        host: true,
        strictPort: true,
        port: 5173,
        proxy: {
            '/api': {
                target: `http://${process.env.BACKEND_HOST || 'localhost'}:${process.env.BACKEND_PORT || 8080}`,
                changeOrigin: true,
                secure: false,
            }
        }
    }
});
