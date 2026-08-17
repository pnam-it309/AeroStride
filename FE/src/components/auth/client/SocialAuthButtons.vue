<template>
  <div class="social-auth-container w-100">
    <div class="divider-text d-flex align-center my-4">
      <v-divider class="border-opacity-25"></v-divider>
      <span class="px-4 text-caption text-grey-darken-1 font-weight-medium text-uppercase tracking-wider">
        {{ label || 'Hoặc tiếp tục với' }}
      </span>
      <v-divider class="border-opacity-25"></v-divider>
    </div>

    <!-- Social Action Buttons -->
    <div class="d-flex ga-3 justify-center align-center social-btn-group">
      <!-- Google Login Button -->
      <v-btn
        id="btn-login-google"
        variant="outlined"
        class="social-action-btn google-btn flex-grow-1"
        :loading="loadingProvider === 'GOOGLE'"
        :disabled="disabled || !!loadingProvider"
        @click="handleSocialClick('GOOGLE')"
        elevation="0"
      >
        <template #prepend>
          <GoogleIcon :size="20" class="mr-2" />
        </template>
        <span class="social-btn-text">Google</span>
      </v-btn>

      <!-- Facebook Login Button -->
      <v-btn
        id="btn-login-facebook"
        variant="outlined"
        class="social-action-btn facebook-btn flex-grow-1"
        :loading="loadingProvider === 'FACEBOOK'"
        :disabled="disabled || !!loadingProvider"
        @click="handleSocialClick('FACEBOOK')"
        elevation="0"
      >
        <template #prepend>
          <FacebookIcon :size="20" class="mr-2" />
        </template>
        <span class="social-btn-text">Facebook</span>
      </v-btn>
    </div>

    <!-- Quick Account Modal (Dùng khi dev/test hoặc tùy chọn đăng nhập) -->
    <v-dialog v-model="showCustomDialog" max-width="440px" persistent>
      <v-card class="rounded-xl overflow-hidden elevation-12">
        <v-card-title class="d-flex align-center justify-space-between pa-5 bg-grey-lighten-4">
          <div class="d-flex align-center ga-2">
            <GoogleIcon v-if="selectedProvider === 'GOOGLE'" :size="24" />
            <FacebookIcon v-else :size="24" />
            <span class="text-subtitle-1 font-weight-bold text-grey-darken-4">
              Đăng nhập bằng {{ selectedProvider === 'GOOGLE' ? 'Google' : 'Facebook' }}
            </span>
          </div>
          <v-btn icon="mdi-close" variant="text" size="small" @click="showCustomDialog = false"></v-btn>
        </v-card-title>

        <v-card-text class="pa-5">
          <p class="text-caption text-grey-darken-1 mb-4">
            Đăng nhập nhanh với tài khoản {{ selectedProvider === 'GOOGLE' ? 'Google' : 'Facebook' }} của bạn để đồng bộ giỏ hàng và lịch sử đơn hàng.
          </p>

          <v-text-field
            v-model="customAccount.name"
            label="Họ và tên hiển thị"
            variant="outlined"
            density="comfortable"
            color="primary"
            class="mb-3"
            prepend-inner-icon="mdi-account"
            hide-details="auto"
          ></v-text-field>

          <v-text-field
            v-model="customAccount.email"
            label="Email tài khoản"
            variant="outlined"
            density="comfortable"
            color="primary"
            class="mb-4"
            prepend-inner-icon="mdi-email"
            type="email"
            hide-details="auto"
          ></v-text-field>

          <v-alert
            v-if="modalError"
            type="error"
            variant="tonal"
            density="compact"
            class="mb-3 rounded-lg"
          >
            {{ modalError }}
          </v-alert>

          <v-btn
            block
            size="large"
            :color="selectedProvider === 'GOOGLE' ? '#4285F4' : '#1877F2'"
            class="text-white font-weight-bold rounded-lg mb-2"
            :loading="isSubmitting"
            @click="submitSocialLogin"
          >
            Xác nhận đăng nhập
          </v-btn>

          <v-btn
            block
            variant="text"
            size="small"
            color="grey-darken-1"
            class="text-caption"
            @click="quickDefaultLogin"
          >
            Sử dụng tài khoản mẫu mặc định
          </v-btn>
        </v-card-text>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { useUIStore } from '@/stores/ui';
import { useToastStore } from '@/stores/toastStore';
import { getBackendErrorMessage } from '@/utils/errorUtils';
import { socialAuthService } from '@/services/auth/socialAuthService';
import GoogleIcon from '@/components/icons/GoogleIcon.vue';
import FacebookIcon from '@/components/icons/FacebookIcon.vue';

const props = defineProps({
  disabled: {
    type: Boolean,
    default: false
  },
  label: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['login-success', 'login-error']);

const router = useRouter();
const authStore = useAuthStore();
const uiStore = useUIStore();
const toastStore = useToastStore?.() || null;

const loadingProvider = ref(null);
const showCustomDialog = ref(false);
const selectedProvider = ref('GOOGLE');
const isSubmitting = ref(false);
const modalError = ref('');

const customAccount = ref({
  name: '',
  email: '',
  avatarUrl: '',
  providerId: ''
});

onMounted(() => {
  socialAuthService.initGoogle(null, handleGoogleCredentialResponse);
  socialAuthService.initFacebook();
});

const handleGoogleCredentialResponse = async (response) => {
  if (response.credential) {
    loadingProvider.value = 'GOOGLE';
    try {
      await authStore.socialLogin({
        provider: 'GOOGLE',
        token: response.credential
      });
      handleSuccess('Google');
    } catch (err) {
      handleError(err, 'Google');
    } finally {
      loadingProvider.value = null;
    }
  }
};

const handleSocialClick = async (provider) => {
  selectedProvider.value = provider;
  loadingProvider.value = provider;
  modalError.value = '';

  try {
    const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
    const fbAppId = import.meta.env.VITE_FACEBOOK_APP_ID;

    if (provider === 'GOOGLE' && googleClientId && window.google?.accounts?.oauth2) {
      // Có Google SDK thực tế
      const res = await socialAuthService.loginWithGoogle();
      handleSuccess('Google');
      return;
    }

    if (provider === 'FACEBOOK' && fbAppId && window.FB) {
      // Có Facebook SDK thực tế
      const res = await socialAuthService.loginWithFacebook();
      handleSuccess('Facebook');
      return;
    }

    // Nếu chưa cấu hình OAuth key bên thứ ba hoặc chạy dev, mở modal hỗ trợ đăng nhập 1 chạm
    customAccount.value = {
      name: provider === 'GOOGLE' ? 'Nguyễn Văn Google' : 'Trần Thị Facebook',
      email: provider === 'GOOGLE' ? 'khachhang.google@gmail.com' : 'khachhang.facebook@gmail.com',
      avatarUrl: provider === 'GOOGLE'
        ? 'https://lh3.googleusercontent.com/a/default-user=s96-c'
        : 'https://upload.wikimedia.org/wikipedia/commons/b/b8/2021_Facebook_icon.svg',
      providerId: (provider === 'GOOGLE' ? 'gg_' : 'fb_') + Date.now()
    };
    showCustomDialog.value = true;
  } catch (err) {
    handleError(err, provider === 'GOOGLE' ? 'Google' : 'Facebook');
  } finally {
    loadingProvider.value = null;
  }
};

const submitSocialLogin = async () => {
  if (!customAccount.value.email || !customAccount.value.email.includes('@')) {
    modalError.value = 'Vui lòng nhập địa chỉ email hợp lệ';
    return;
  }

  isSubmitting.value = true;
  modalError.value = '';

  try {
    uiStore.showLoading?.(`Đang đăng nhập bằng ${selectedProvider.value}...`);
    await authStore.socialLogin({
      provider: selectedProvider.value,
      email: customAccount.value.email.trim(),
      name: customAccount.value.name.trim() || (selectedProvider.value === 'GOOGLE' ? 'Khách Hàng Google' : 'Khách Hàng Facebook'),
      avatarUrl: customAccount.value.avatarUrl,
      providerId: customAccount.value.providerId || (selectedProvider.value.toLowerCase() + '_' + Date.now())
    });

    showCustomDialog.value = false;
    handleSuccess(selectedProvider.value === 'GOOGLE' ? 'Google' : 'Facebook');
  } catch (err) {
    modalError.value = err?.response?.data?.message || err.message || 'Đăng nhập thất bại';
  } finally {
    isSubmitting.value = false;
    uiStore.hideLoading?.();
  }
};

const quickDefaultLogin = async () => {
  customAccount.value = {
    name: selectedProvider.value === 'GOOGLE' ? 'Khách Hàng Google' : 'Khách Hàng Facebook',
    email: selectedProvider.value === 'GOOGLE' ? 'customer.google@aerostride.vn' : 'customer.facebook@aerostride.vn',
    avatarUrl: selectedProvider.value === 'GOOGLE'
      ? 'https://lh3.googleusercontent.com/a/default-user=s96-c'
      : 'https://upload.wikimedia.org/wikipedia/commons/b/b8/2021_Facebook_icon.svg',
    providerId: (selectedProvider.value === 'GOOGLE' ? 'gg_' : 'fb_') + Date.now()
  };
  await submitSocialLogin();
};

const handleSuccess = (providerName) => {
  if (toastStore && toastStore.showSuccess) {
    toastStore.showSuccess(`Đăng nhập bằng ${providerName} thành công!`);
  }
  emit('login-success', { provider: providerName, user: authStore.user });
  router.push('/');
};

const handleError = (error, providerName) => {
  const msg = getBackendErrorMessage(error, `Đăng nhập bằng ${providerName} thất bại`, `SocialAuth-${providerName}`);
  if (toastStore && toastStore.showError) {
    toastStore.showError(msg);
  } else if (toastStore && toastStore.error) {
    toastStore.error(msg);
  }
  emit('login-error', msg);
};
</script>

<style scoped>
.social-auth-container {
  width: 100%;
}

.social-action-btn {
  height: 48px !important;
  border-radius: 12px !important;
  border-color: #e2e8f0 !important;
  background-color: #ffffff !important;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  text-transform: none !important;
}

.social-btn-text {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  letter-spacing: 0.2px;
}

.google-btn:hover {
  background-color: #f8fafc !important;
  border-color: #cbd5e1 !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(66, 133, 244, 0.15) !important;
}

.facebook-btn:hover {
  background-color: #f0f7ff !important;
  border-color: #93c5fd !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 119, 242, 0.15) !important;
}

.tracking-wider {
  letter-spacing: 0.8px !important;
}
</style>
