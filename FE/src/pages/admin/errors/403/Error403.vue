<script setup>
import { useRouter } from 'vue-router'
import { ROUTES } from '@/constants'
import AeroButton from '@/components/base/AeroButton.vue'

const router = useRouter()

const goBack = () => {
  router.push(ROUTES.ADMIN.DASHBOARD)
}
</script>

<template>
  <div class="min-h-[80vh] flex items-center justify-center p-6">
    <div class="max-w-md w-full text-center space-y-8 animate-in fade-in slide-in-from-top-8 duration-700">
      <!-- Shield Icon with Warning Glow -->
      <div class="relative flex justify-center">
        <div class="w-32 h-32 rounded-full bg-red-50 border border-red-100 flex items-center justify-center relative">
          <i class="ph-shield-warning-bold text-6xl text-red-500"></i>
          <!-- Pulsing ring -->
          <div class="absolute inset-0 rounded-full border-2 border-red-400 animate-ping opacity-20"></div>
        </div>
      </div>

      <div class="space-y-4">
        <div class="inline-flex items-center px-3 py-1 bg-red-50 text-red-600 rounded-full text-[10px] font-black uppercase tracking-widest border border-red-100 mb-2">
          Error 403
        </div>
        <h2 class="text-3xl font-black text-gray-900 uppercase tracking-tight">Truy cập bị từ chối</h2>
        <p class="text-gray-500 font-medium px-4">
          Bạn không có đủ quyền hạn để truy cập vào khu vực này. Vui lòng liên hệ quản trị viên cấp cao nếu bạn tin rằng đây là một lỗi.
        </p>
      </div>

      <div class="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
        <AeroButton @click="goBack" class="min-w-[160px]" variant="primary" glow>
          Về trang chủ
        </AeroButton>
        <AeroButton @click="router.push(ROUTES.AUTH.LOGIN)" class="min-w-[160px]" variant="outline">
          Đăng nhập lại
        </AeroButton>
      </div>
    </div>
  </div>
</template>
