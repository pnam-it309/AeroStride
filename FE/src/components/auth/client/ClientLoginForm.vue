<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUIStore } from '@/stores/ui';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { PATH } from '@/router/routePaths';

const router = useRouter();
const uiStore = useUIStore();
const checkbox = ref(false);
const loading = ref(false);
const errorMessage = ref('');

const loginForm = ref({
  username: '',
  password: ''
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
    // We reuse the same dichVuXacThuc for now, assuming user and admin share the auth endpoint.
    const response = await dichVuXacThuc.dangNhap({
      ...loginForm.value,
      loginType: 'CLIENT'
    });
    
    uiStore.hideLoading();
    // Redirect to home/client dashboard instead of main, but since client dashboard isn't built yet, we can push to a client route or just root
    router.push('/');
  } catch (error) {
    errorMessage.value = error.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.';
  } finally {
    loading.value = false;
  }
};
</script>

<template>
    <v-form @submit.prevent="handleLogin" class="mt-4 w-100">
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
                        <v-label class="font-weight-bold mb-2 text-subtitle-2 text-grey-darken-3">Tên đăng nhập hoặc Email</v-label>
                        <v-text-field 
                            v-model="loginForm.username"
                            variant="outlined" 
                            class="modern-input" 
                            hide-details 
                            color="blue-darken-3"
                            placeholder="Nhập email hoặc tên đăng nhập"
                            prepend-inner-icon="mdi-email-outline"
                            :disabled="loading"
                        ></v-text-field>
                    </div>
                </v-col>
                <v-col cols="12" class="pt-2">
                    <div class="input-wrapper">
                        <div class="d-flex justify-space-between align-center mb-2">
                            <v-label class="font-weight-bold mb-0 text-subtitle-2 text-grey-darken-3">Mật khẩu</v-label>
                            <RouterLink :to="PATH.FORGOT_PASSWORD" class="text-blue-darken-3 text-decoration-none text-caption font-weight-bold hover-underline transition-fast">
                                Quên mật khẩu?
                            </RouterLink>
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
                    <v-checkbox 
                        v-model="checkbox"
                        hide-details 
                        color="blue-darken-3"
                        class="modern-checkbox"
                        :disabled="loading"
                    >
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
                            Tiếp tục với tư cách khách (Không cần đăng nhập)
                        </v-btn>
                    </div>
                </v-col>

                <v-col cols="12" class="text-center mt-6">
                    <div class="divider-text d-flex align-center">
                        <v-divider></v-divider>
                        <span class="px-4 text-caption text-grey">Hoặc đăng nhập với</span>
                        <v-divider></v-divider>
                    </div>
                </v-col>

                <v-col cols="12" class="d-flex justify-center gap-4 mt-2">
                    <v-btn icon variant="outlined" color="grey-lighten-1" class="social-btn" :disabled="loading">
                        <v-icon color="red">mdi-google</v-icon>
                    </v-btn>
                    <v-btn icon variant="outlined" color="grey-lighten-1" class="social-btn" :disabled="loading">
                        <v-icon color="blue-darken-2">mdi-facebook</v-icon>
                    </v-btn>
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
    border-color: #1565C0 !important;
}

.modern-login-btn {
    height: 54px !important;
    border-radius: 12px !important;
    background-color: #0D47A1 !important; /* Ép màu xanh dương đậm */
    color: #ffffff !important; /* Ép chữ trắng */
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    letter-spacing: 0.5px !important;
    box-shadow: 0 4px 15px rgba(13, 71, 161, 0.2) !important;
}

.modern-login-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(13, 71, 161, 0.35) !important;
    background-color: #1565C0 !important;
}

.hover-underline:hover {
    text-decoration: underline !important;
}

.animate-fade-in {
    animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(-8px); }
    to { opacity: 1; transform: translateY(0); }
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
