<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUIStore } from '@/stores/ui';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';

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
    uiStore.showLoading('Đang xác thực thông tin quản trị...');
    const response = await dichVuXacThuc.dangNhap(loginForm.value);
    
    // Explicitly hide after successful auth to trigger transition
    uiStore.hideLoading();
    router.push('/main');
  } catch (error) {
    errorMessage.value = error.message || 'Đăng nhập thất bại. Vui lòng thử lại.';
  } finally {
    loading.value = false;
  }
};
</script>

<template>
    <v-form @submit.prevent="handleLogin" class="mt-4">
        <v-alert
            v-if="errorMessage"
            type="error"
            variant="tonal"
            class="mb-6 rounded-xl animate-fade-in"
            closable
            @click:close="errorMessage = ''"
        >
            {{ errorMessage }}
        </v-alert>
        
        <div class="form-container">
            <v-row class="mb-3">
                <v-col cols="12">
                    <div class="input-group">
                        <v-label class="font-weight-black mb-2 text-uppercase text-caption tracking-wider">Tên đăng nhập</v-label>
                        <v-text-field 
                            v-model="loginForm.username"
                            variant="outlined" 
                            class="premium-input" 
                            hide-details 
                            color="primary"
                            placeholder="Nhập username"
                            prepend-inner-icon="mdi-account-circle-outline"
                            :disabled="loading"
                        ></v-text-field>
                    </div>
                </v-col>
                <v-col cols="12">
                    <div class="input-group">
                        <v-label class="font-weight-black mb-2 text-uppercase text-caption tracking-wider">Mật khẩu</v-label>
                        <v-text-field 
                            v-model="loginForm.password"
                            variant="outlined" 
                            class="premium-input" 
                            type="password" 
                            hide-details
                            color="primary"
                            placeholder="••••••••"
                            prepend-inner-icon="mdi-lock-outline"
                            :disabled="loading"
                        ></v-text-field>
                    </div>
                </v-col>
                <v-col cols="12" class="py-0 mt-2">
                    <div class="d-flex flex-wrap align-center w-100">
                        <v-checkbox 
                            v-model="checkbox"
                            hide-details 
                            color="primary"
                            class="tiny-checkbox"
                            :disabled="loading"
                        >
                            <template v-slot:label>
                                <span class="text-caption font-weight-bold">Duy trì đăng nhập</span>
                            </template>
                        </v-checkbox>
                        <div class="ml-auto">
                            <RouterLink to="/auth/forgot-password"
                                class="text-primary text-decoration-none text-caption font-weight-black hover-underline">
                                Quên mật khẩu?
                            </RouterLink>
                        </div>
                    </div>
                </v-col>
                <v-col cols="12" class="mt-6">
                    <v-btn 
                        size="x-large" 
                        rounded="xl" 
                        color="primary" 
                        class="login-btn emerald-gradient-btn" 
                        block 
                        type="submit"
                        :loading="loading"
                        :disabled="loading"
                    >
                        Đăng nhập hệ thống
                        <v-icon end icon="mdi-arrow-right" class="ml-2"></v-icon>
                    </v-btn>
                </v-col>
            </v-row>
        </div>
    </v-form>
</template>

<style scoped>
.form-container {
    perspective: 1000px;
}

.premium-input :deep(.v-field) {
    border-radius: 16px !important;
    background: rgba(248, 250, 252, 0.8) !important;
    transition: all 0.3s ease;
    border-color: rgba(0,0,0,0.05) !important;
}

.premium-input :deep(.v-field:hover) {
    background: #ffffff !important;
    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

.premium-input :deep(.v-field--focused) {
    background: #ffffff !important;
    box-shadow: 0 0 20px rgba(var(--v-theme-primary), 0.15);
}

.login-btn {
    height: 56px !important;
    font-weight: 900 !important;
    color: white !important;
    letter-spacing: 1px !important;
}

.login-btn:hover {
    transform: translateY(-5px) scale(1.02);
    box-shadow: 0 15px 30px rgba(var(--v-theme-primary), 0.45) !important;
}

.emerald-gradient-btn {
    background: #1e3a8a !important;
    color: white !important;
}

.hover-underline:hover {
    text-decoration: underline !important;
}

.animate-fade-in {
    animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(-10px); }
    to { opacity: 1; transform: translateY(0); }
}

.tiny-checkbox :deep(.v-selection-control) {
    min-height: 30px;
}
</style>
