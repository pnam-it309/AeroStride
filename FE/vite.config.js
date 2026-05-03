import { fileURLToPath, URL } from 'url';
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import vuetify from 'vite-plugin-vuetify';

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '');
    
    return {
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
            ],
            entries: ['./index.html', './src/**/*.{js,vue}']
        },
        build: {
            rollupOptions: {
                treeshake: true
            },
            sourcemap: false,
            chunkSizeWarningLimit: 1000
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
                    target: `http://${env.BACKEND_HOST}:${env.BACKEND_PORT}`,
                    changeOrigin: true,
                    secure: false,
                }
            }
        }
    };
});
