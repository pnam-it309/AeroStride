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
    <div class="max-w-md w-full text-center space-y-8 animate-in fade-in zoom-in duration-700">
      <!-- 404 Number with Glow -->
      <div class="relative">
        <h1
          class="text-[12rem] font-black leading-none text-transparent bg-clip-text bg-gradient-to-b from-aurora/50 to-aurora/10 select-none"
        >
          404
        </h1>
        <div class="absolute inset-0 flex items-center justify-center">
          <i
            class="ph-file-search-bold text-6xl text-aurora animate-pulse shadow-aurora/50 drop-shadow-[0_0_15px_rgba(var(--aurora-rgb),0.5)]"
          ></i>
        </div>
      </div>

      <div class="space-y-4">
        <h2 class="text-3xl font-black text-gray-900 uppercase tracking-tight">
          Trang không tồn tại
        </h2>
        <p class="text-gray-500 font-medium">
          Dường như bạn đã đi lạc vào một vùng không gian không có thực. Đường dẫn này có thể đã bị
          xóa hoặc chưa từng tồn tại.
        </p>
      </div>

      <div class="flex flex-col sm:flex-row items-center justify-center gap-4">
        <AeroButton @click="goBack" class="min-w-[160px]" glow> Quay lại Dashboard </AeroButton>
        <button
          @click="$router.go(-1)"
          class="px-6 py-2.5 text-xs font-black uppercase tracking-widest text-gray-400 hover:text-gray-900 transition-colors"
        >
          Trở lại trang trước
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.font-black {
  -webkit-text-stroke: 1px rgba(var(--aurora-rgb), 0.1);
}
</style>
