import { fileURLToPath, URL } from 'url';
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import vuetify from 'vite-plugin-vuetify';
import viteCompression from 'vite-plugin-compression';

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
                disable: false,
                threshold: 10240, // Compress files larger than 10KB
                algorithm: 'gzip',
                ext: '.gz',
            }),
            viteCompression({
                verbose: true,
                disable: false,
                threshold: 10240,
                algorithm: 'brotliCompress',
                ext: '.br',
            })
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
                '@vuelidate/validators'
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
            chunkSizeWarningLimit: 1000
        },
        server: {
            allowedHosts: true,
            watch: {
                usePolling: true,
                interval: 1000,
                ignored: ['**/node_modules/**', '**/dist/**'],
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