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

onMounted(() => {
  socialAuthService.initGoogle(null, handleGoogleCredentialResponse);
  socialAuthService.initFacebook();
});

const handleGoogleCredentialResponse = async (response) => {
  if (response.credential) {
    loadingProvider.value = 'GOOGLE';
    try {
      uiStore.showLoading?.('Đang đăng nhập bằng Google...');
      await authStore.socialLogin({
        provider: 'GOOGLE',
        token: response.credential
      });
      handleSuccess('Google');
    } catch (err) {
      handleError(err, 'Google');
    } finally {
      loadingProvider.value = null;
      uiStore.hideLoading?.();
    }
  }
};

const handleSocialClick = async (provider) => {
  loadingProvider.value = provider;

  try {
    if (provider === 'GOOGLE') {
      uiStore.showLoading?.('Đang kết nối tới Google...');
      await socialAuthService.loginWithGoogle();
      handleSuccess('Google');
      return;
    }

    if (provider === 'FACEBOOK') {
      uiStore.showLoading?.('Đang kết nối tới Facebook...');
      await socialAuthService.loginWithFacebook();
      handleSuccess('Facebook');
      return;
    }
  } catch (err) {
    handleError(err, provider === 'GOOGLE' ? 'Google' : 'Facebook');
  } finally {
    loadingProvider.value = null;
    uiStore.hideLoading?.();
  }
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
