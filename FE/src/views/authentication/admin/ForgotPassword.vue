<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/services/apiService';
import { useNotifications } from '@/services/notificationService';
import { PATH } from '@/router/routePaths';

const email = ref('');
const loading = ref(false);
const { addNotification } = useNotifications();
const router = useRouter();

const handleSubmit = async () => {
  if (!email.value) {
    addNotification({ title: 'Lỗi', subtitle: 'Vui lòng nhập email', color: 'error' });
    return;
  }
  loading.value = true;
  try {
    await api.post(`/reset-password-requests/request`, null, { params: { email: email.value } });
    addNotification({ title: 'Thành công', subtitle: 'Yêu cầu đã được gửi tới admin. Vui lòng chờ xác nhận!', color: 'success' });
    router.push(PATH.ADMIN_LOGIN);
  } catch (e) {
    addNotification({ title: 'Lỗi', subtitle: e.response?.data?.message || 'Không gửi được yêu cầu', color: 'error' });
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <v-container class="d-flex flex-column align-center justify-center min-h-screen">
    <v-card class="pa-8" max-width="400">
      <h2 class="text-h5 font-weight-bold mb-6 text-center">Quên mật khẩu nhân viên</h2>
      <v-form @submit.prevent="handleSubmit">
        <v-text-field
          v-model="email"
          label="Email nhân viên"
          type="email"
          required
          :disabled="loading"
          prepend-inner-icon="mdi-email"
        ></v-text-field>
        <v-btn
          color="primary"
          class="mt-4"
          block
          type="submit"
          :loading="loading"
        >
          Gửi yêu cầu
        </v-btn>
        <v-btn variant="text" class="mt-2" block @click="router.push(PATH.ADMIN_LOGIN)">Quay lại đăng nhập</v-btn>
      </v-form>
    </v-card>
  </v-container>
</template>
