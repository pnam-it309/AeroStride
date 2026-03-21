<script setup>
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { formatCurrency } from '@/utils/formatters';

const route = useRoute();
const router = useRouter();

const isSuccess = ref(false);
const transactionId = ref('');
const amount = ref(0);
const message = ref('');

onMounted(() => {
  // Parsing parameters from VNPAY or Backend redirect
  // Example URL: /admin/payment-result?status=success&vnp_TransactionNo=123...
  const status = route.query.status || route.query.vnp_ResponseCode;
  
  // VNPAY 00 code means success
  isSuccess.value = status === 'success' || status === '00';
  transactionId.value = route.query.transactionId || route.query.vnp_TransactionNo || 'N/A';
  amount.value = parseInt(route.query.amount || route.query.vnp_Amount || 0) / 100; // VNPAY amount is multiplied by 100
  message.value = route.query.message || (isSuccess.value ? 'Thanh toán đơn hàng thành công!' : 'Thanh toán không thành công hoặc đã bị hủy.');
});

const goDashboard = () => router.push('/admin/dashboard');
const goOrders = () => router.push('/admin/orders');
</script>

<template>
  <div class="min-h-[80vh] flex items-center justify-center p-4">
    <div class="max-w-md w-full bg-white rounded-2xl shadow-xl overflow-hidden border border-gray-100">
      <!-- Header Image/Icon Section -->
      <div :class="[isSuccess ? 'bg-green-500' : 'bg-red-500', 'py-10 flex flex-col items-center']">
        <div class="bg-white/20 p-4 rounded-full backdrop-blur-sm animate-bounce">
          <svg v-if="isSuccess" xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
          </svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </div>
        <h1 class="text-white text-2xl font-bold mt-4">
          {{ isSuccess ? 'Thành công!' : 'Thất bại!' }}
        </h1>
      </div>

      <!-- Details Section -->
      <div class="p-8">
        <div class="text-center mb-8">
          <p class="text-gray-600 text-lg">{{ message }}</p>
        </div>

        <div class="space-y-4 border-t border-b border-gray-100 py-6 mb-8">
          <div class="flex justify-between">
            <span class="text-gray-400">Mã giao dịch</span>
            <span class="font-mono font-medium text-gray-800">{{ transactionId }}</span>
          </div>
          <div v-if="amount > 0" class="flex justify-between">
            <span class="text-gray-400">Số tiền</span>
            <span class="font-bold text-gray-900">{{ formatCurrency(amount) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-400">Thời gian</span>
            <span class="text-gray-800">{{ new Date().toLocaleString('vi-VN') }}</span>
          </div>
        </div>

        <!-- Actions -->
        <div class="grid grid-cols-1 gap-3">
          <button 
            @click="isSuccess ? goOrders() : goDashboard()"
            class="w-full py-3 px-4 bg-gray-900 text-white rounded-xl font-bold hover:bg-gray-800 transition-colors shadow-lg shadow-gray-200"
          >
            {{ isSuccess ? 'Xem đơn hàng' : 'Quay lại trang chủ' }}
          </button>
          <button 
            v-if="isSuccess"
            @click="goDashboard"
            class="w-full py-3 px-4 bg-white text-gray-600 border border-gray-200 rounded-xl font-bold hover:bg-gray-50 transition-colors"
          >
            Về trang chủ
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
