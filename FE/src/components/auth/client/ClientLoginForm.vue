<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useUIStore } from '@/stores/ui';
import { useAuthStore } from '@/stores/authStore';
import { PATH } from '@/router/routePaths';
import SocialAuthButtons from './SocialAuthButtons.vue';
import { getBackendErrorMessage } from '@/utils/errorUtils';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const uiStore = useUIStore();
const checkbox = ref(false);
const loading = ref(false);
const errorMessage = ref('');
const registeredNotice = ref(route.query.registered === 'true');

const loginForm = ref({
    username: route.query.username ? String(route.query.username) : '',
    password: ''
});

onMounted(() => {
    if (route.query.username) {
        loginForm.value.username = String(route.query.username);
    }
});

const handleLogin = async () => {
    if (!loginForm.value.username || !loginForm.value.password) {
        errorMessage.value = 'Vui lòng nhập tên đăng nhập và mật khẩu';
        return;
    }

    loading.value = true;
    errorMessage.value = '';

    try {
        uiStore.showLoading('Đang đăng nhập...');
        const response = await authStore.login({
            ...loginForm.value,
            loginType: 'CLIENT'
        });

        uiStore.hideLoading();
        router.push('/');
    } catch (error) {
        errorMessage.value = getBackendErrorMessage(error, 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.', 'ClientLoginForm');
    } finally {
        loading.value = false;
        uiStore.hideLoading();
    }
};
</script>

<template>
    <v-form @submit.prevent="handleLogin" class="mt-4 w-100">
        <v-alert
            v-if="registeredNotice"
            type="success"
            variant="tonal"
            class="mb-6 rounded-lg animate-fade-in"
            closable
            @click:close="registeredNotice = false"
        >
            Đăng ký tài khoản thành công! Vui lòng nhập mật khẩu để đăng nhập.
        </v-alert>

        <v-alert
            v-if="errorMessage"
            type="error"
            variant="tonal"
            class="mb-6 rounded-lg animate-fade-in"
            closable
            @click:close="errorMessage = ''"
        >
            {{ errorMessage }}
        </v-alert>

        <div class="form-container">
            <v-row class="mb-3">
                <v-col cols="12" class="pb-2">
                    <div class="input-wrapper">
                        <v-label class="font-weight-bold mb-2 text-subtitle-2 text-grey-darken-3">Tài khoản</v-label>
                        <v-text-field
                            v-model="loginForm.username"
                            variant="outlined"
                            class="modern-input"
                            hide-details
                            color="blue-darken-3"
                            placeholder="Nhập số điện thoại hoặc email"
                            prepend-inner-icon="mdi-account-outline"
                            :disabled="loading"
                        ></v-text-field>
                    </div>
                </v-col>
                <v-col cols="12" class="pt-2">
                    <div class="input-wrapper">
                        <div class="d-flex justify-space-between align-center mb-2">
                            <v-label class="font-weight-bold mb-0 text-subtitle-2 text-grey-darken-3">Mật khẩu</v-label>
                        </div>
                        <v-text-field
                            v-model="loginForm.password"
                            variant="outlined"
                            class="modern-input"
                            type="password"
                            hide-details
                            color="blue-darken-3"
                            placeholder="••••••••"
                            prepend-inner-icon="mdi-lock-outline"
                            :disabled="loading"
                        ></v-text-field>
                    </div>
                </v-col>
                <v-col cols="12" class="py-0 mt-2">
                    <v-checkbox v-model="checkbox" hide-details color="blue-darken-3" class="modern-checkbox" :disabled="loading">
                        <template v-slot:label>
                            <span class="text-body-2 font-weight-medium text-grey-darken-2">Ghi nhớ đăng nhập</span>
                        </template>
                    </v-checkbox>
                </v-col>
                <v-col cols="12" class="mt-4">
                    <v-btn
                        size="x-large"
                        color="blue-darken-3"
                        class="modern-login-btn text-white font-weight-bold text-button tracking-wide"
                        block
                        type="submit"
                        :loading="loading"
                        :disabled="loading"
                        elevation="2"
                    >
                        Đăng Nhập
                        <v-icon end icon="mdi-arrow-right" class="ml-2"></v-icon>
                    </v-btn>

                    <!-- Guest Access Link -->
                    <div class="text-center mt-3">
                        <v-btn variant="text" color="blue-darken-3" class="text-caption font-weight-bold premium-link" to="/">
                            Tiếp tục với tư cách khách
                        </v-btn>
                    </div>
                </v-col>

                <!-- Social Login Buttons with Official Brand Icons -->
                <v-col cols="12" class="mt-2">
                    <SocialAuthButtons :disabled="loading" label="Hoặc đăng nhập với" />
                </v-col>
            </v-row>
        </div>
    </v-form>
</template>

<style scoped>
.modern-input :deep(.v-field) {
    border-radius: 8px !important;
    background: #f9fafb !important;
    transition: all 0.3s ease;
    border-color: #e5e7eb !important;
}

.modern-input :deep(.v-field:hover) {
    background: #ffffff !important;
    border-color: #d1d5db !important;
}

.modern-input :deep(.v-field--focused) {
    background: #ffffff !important;
    box-shadow: 0 0 0 4px rgba(25, 118, 210, 0.05);
    border-color: #1565c0 !important;
}

.modern-login-btn {
    height: 54px !important;
    border-radius: 12px !important;
    background-color: #0d47a1 !important;
    /* Ép màu xanh dương đậm */
    color: #ffffff !important;
    /* Ép chữ trắng */
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    letter-spacing: 0.5px !important;
    box-shadow: 0 4px 15px rgba(13, 71, 161, 0.2) !important;
}

.modern-login-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(13, 71, 161, 0.35) !important;
    background-color: #1565c0 !important;
}

.hover-underline:hover {
    text-decoration: underline !important;
}

.animate-fade-in {
    animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(-8px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.modern-checkbox :deep(.v-selection-control) {
    min-height: 32px;
}

.social-btn {
    transition: all 0.3s ease;
    border-width: 1px !important;
    border-radius: 12px;
}

.social-btn:hover {
    background-color: #f3f4f6;
    transform: translateY(-2px);
    border-color: #d1d5db !important;
}

.gap-4 {
    gap: 16px;
}
</style>
