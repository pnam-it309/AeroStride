<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useUIStore } from '@/stores/ui';
import { useAuthStore } from '@/stores/authStore';
import { PATH } from '@/router/routePaths';
import api from '@/services/apiService';
import { API_AUTH } from '@/constants/apiPaths';

const router = useRouter();
const authStore = useAuthStore();
const uiStore = useUIStore();

const loading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const showPassword = ref(false);
const showConfirmPassword = ref(false);

const form = ref({
    ten: '',
    tenTaiKhoan: '',
    email: '',
    sdt: '',
    matKhau: '',
    xacNhanMatKhau: ''
});

const passwordMismatch = computed(() => {
    return (
        form.value.xacNhanMatKhau.length > 0 &&
        form.value.matKhau !== form.value.xacNhanMatKhau
    );
});

const handleRegister = async () => {
    // Basic validation
    if (
        !form.value.ten ||
        !form.value.tenTaiKhoan ||
        !form.value.email ||
        !form.value.sdt ||
        !form.value.matKhau ||
        !form.value.xacNhanMatKhau
    ) {
        errorMessage.value = 'Vui lòng điền đầy đủ thông tin';
        return;
    }

    if (passwordMismatch.value) {
        errorMessage.value = 'Mật khẩu xác nhận không khớp';
        return;
    }

    if (form.value.matKhau.length < 6) {
        errorMessage.value = 'Mật khẩu phải có ít nhất 6 ký tự';
        return;
    }

    loading.value = true;
    errorMessage.value = '';

    try {
        uiStore.showLoading('Đang tạo tài khoản...');

        const response = await api.post(API_AUTH.REGISTER, {
            ten: form.value.ten,
            tenTaiKhoan: form.value.tenTaiKhoan,
            email: form.value.email,
            sdt: form.value.sdt,
            matKhau: form.value.matKhau
        });

        // Auto-login after register: store tokens from response
        if (response.data?.data) {
            const { accessToken, refreshToken, username, role } = response.data.data;
            sessionStorage.setItem('accessToken', accessToken);
            sessionStorage.setItem('refreshToken', refreshToken);
            sessionStorage.setItem('user', JSON.stringify({ username, role }));
            await authStore.fetchCurrentUser?.();
        }

        uiStore.hideLoading();
        router.push('/');
    } catch (error) {
        errorMessage.value =
            error?.response?.data?.message ||
            error.message ||
            'Đăng ký thất bại. Vui lòng thử lại.';
    } finally {
        loading.value = false;
        uiStore.hideLoading();
    }
};
</script>

<template>
    <v-form @submit.prevent="handleRegister" class="mt-4 w-100">
        <!-- Error alert -->
        <v-alert
            v-if="errorMessage"
            type="error"
            variant="tonal"
            class="mb-5 rounded-lg animate-fade-in"
            closable
            @click:close="errorMessage = ''"
        >
            {{ errorMessage }}
        </v-alert>

        <div class="form-container">
            <v-row>
                <!-- Họ và tên -->
                <v-col cols="12" class="pb-2">
                    <div class="input-wrapper">
                        <v-label class="font-weight-bold mb-2 text-subtitle-2 text-grey-darken-3">
                            Họ và tên
                        </v-label>
                        <v-text-field
                            v-model="form.ten"
                            variant="outlined"
                            class="modern-input"
                            hide-details
                            color="blue-darken-3"
                            placeholder="Nguyễn Văn A"
                            prepend-inner-icon="mdi-account-outline"
                            :disabled="loading"
                        />
                    </div>
                </v-col>

                <!-- Tên tài khoản -->
                <v-col cols="12" class="py-2">
                    <div class="input-wrapper">
                        <v-label class="font-weight-bold mb-2 text-subtitle-2 text-grey-darken-3">
                            Tên tài khoản
                        </v-label>
                        <v-text-field
                            v-model="form.tenTaiKhoan"
                            variant="outlined"
                            class="modern-input"
                            hide-details
                            color="blue-darken-3"
                            placeholder="username123"
                            prepend-inner-icon="mdi-at"
                            :disabled="loading"
                        />
                    </div>
                </v-col>

                <!-- Email -->
                <v-col cols="12" md="6" class="py-2 pr-md-2">
                    <div class="input-wrapper">
                        <v-label class="font-weight-bold mb-2 text-subtitle-2 text-grey-darken-3">
                            Email
                        </v-label>
                        <v-text-field
                            v-model="form.email"
                            variant="outlined"
                            class="modern-input"
                            hide-details
                            color="blue-darken-3"
                            placeholder="example@email.com"
                            prepend-inner-icon="mdi-email-outline"
                            type="email"
                            :disabled="loading"
                        />
                    </div>
                </v-col>

                <!-- Số điện thoại -->
                <v-col cols="12" md="6" class="py-2 pl-md-2">
                    <div class="input-wrapper">
                        <v-label class="font-weight-bold mb-2 text-subtitle-2 text-grey-darken-3">
                            Số điện thoại
                        </v-label>
                        <v-text-field
                            v-model="form.sdt"
                            variant="outlined"
                            class="modern-input"
                            hide-details
                            color="blue-darken-3"
                            placeholder="0912 345 678"
                            prepend-inner-icon="mdi-phone-outline"
                            :disabled="loading"
                        />
                    </div>
                </v-col>

                <!-- Mật khẩu -->
                <v-col cols="12" class="py-2">
                    <div class="input-wrapper">
                        <v-label class="font-weight-bold mb-2 text-subtitle-2 text-grey-darken-3">
                            Mật khẩu
                        </v-label>
                        <v-text-field
                            v-model="form.matKhau"
                            variant="outlined"
                            class="modern-input"
                            hide-details
                            color="blue-darken-3"
                            placeholder="••••••••"
                            prepend-inner-icon="mdi-lock-outline"
                            :type="showPassword ? 'text' : 'password'"
                            :append-inner-icon="showPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
                            @click:append-inner="showPassword = !showPassword"
                            :disabled="loading"
                        />
                    </div>
                </v-col>

                <!-- Xác nhận mật khẩu -->
                <v-col cols="12" class="py-2">
                    <div class="input-wrapper">
                        <v-label class="font-weight-bold mb-2 text-subtitle-2 text-grey-darken-3">
                            Xác nhận mật khẩu
                        </v-label>
                        <v-text-field
                            v-model="form.xacNhanMatKhau"
                            variant="outlined"
                            class="modern-input"
                            :class="{ 'input-error': passwordMismatch }"
                            :hint="passwordMismatch ? 'Mật khẩu không khớp' : ''"
                            :persistent-hint="passwordMismatch"
                            color="blue-darken-3"
                            placeholder="••••••••"
                            prepend-inner-icon="mdi-lock-check-outline"
                            :type="showConfirmPassword ? 'text' : 'password'"
                            :append-inner-icon="showConfirmPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
                            @click:append-inner="showConfirmPassword = !showConfirmPassword"
                            :disabled="loading"
                        />
                    </div>
                </v-col>

                <!-- Submit button -->
                <v-col cols="12" class="mt-3">
                    <v-btn
                        id="btn-register-submit"
                        size="x-large"
                        color="blue-darken-3"
                        class="modern-register-btn text-white font-weight-bold text-button"
                        block
                        type="submit"
                        :loading="loading"
                        :disabled="loading || passwordMismatch"
                        elevation="2"
                    >
                        Tạo tài khoản
                        <v-icon end icon="mdi-account-plus" class="ml-2" />
                    </v-btn>

                    <div class="text-center mt-3">
                        <v-btn
                            variant="text"
                            color="blue-darken-3"
                            class="text-caption font-weight-bold premium-link"
                            to="/"
                        >
                            Tiếp tục với tư cách khách
                        </v-btn>
                    </div>
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

.input-error :deep(.v-field) {
    border-color: #ef4444 !important;
}

.input-error :deep(.v-field--focused) {
    box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.08) !important;
}

.modern-register-btn {
    height: 54px !important;
    border-radius: 12px !important;
    background-color: #0d47a1 !important;
    color: #ffffff !important;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    letter-spacing: 0.5px !important;
    box-shadow: 0 4px 15px rgba(13, 71, 161, 0.2) !important;
}

.modern-register-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(13, 71, 161, 0.35) !important;
    background-color: #1565c0 !important;
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
</style>
