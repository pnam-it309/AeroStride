<script setup>
import { useRouter } from 'vue-router'
import { ROUTES } from '@/constants'
import AeroButton from '@/components/base/AeroButton.vue'

const router = useRouter()

const reloadPage = () => {
  window.location.reload()
}
</script>

<template>
  <div class="min-h-[80vh] flex items-center justify-center p-6">
    <div
      class="max-w-md w-full text-center space-y-8 animate-in fade-in slide-in-from-bottom-8 duration-700"
    >
      <!-- Glitchy Icon -->
      <div class="relative flex justify-center">
        <div
          class="w-32 h-32 rounded-3xl bg-gray-900 flex items-center justify-center shadow-2xl rotate-3 hover:rotate-0 transition-transform duration-500 overflow-hidden group"
        >
          <i
            class="ph-warning-octagon-bold text-6xl text-yellow-400 animate-bounce group-hover:scale-110 transition-transform"
          ></i>
          <!-- Subtle scanline effect overlay -->
          <div
            class="absolute inset-0 bg-gradient-to-b from-transparent via-white/5 to-transparent h-1 w-full animate-scanline"
          ></div>
        </div>
      </div>

      <div class="space-y-4">
        <div
          class="inline-flex items-center px-3 py-1 bg-gray-100 text-gray-600 rounded-full text-[10px] font-black uppercase tracking-widest border border-gray-200 mb-2"
        >
          Error 500
        </div>
        <h2 class="text-3xl font-black text-gray-900 uppercase tracking-tight">
          Máy chủ gặp sự cố
        </h2>
        <p class="text-gray-500 font-medium px-4">
          Có gì đó không ổn đang xảy ra phía máy chủ của chúng tôi. Kỹ thuật viên đã được thông báo
          và đang tiến hành khắc phục.
        </p>
      </div>

      <div class="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
        <AeroButton @click="reloadPage" class="min-w-[160px]" glow>
          <i class="ph-arrows-clockwise-bold mr-2"></i> Thử lại
        </AeroButton>
        <AeroButton
          @click="router.push(ROUTES.ADMIN.DASHBOARD)"
          class="min-w-[160px]"
          variant="outline"
        >
          Về Dashboard
        </AeroButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes scanline {
  0% {
    transform: translateY(-100%);
  }
  100% {
    transform: translateY(300%);
  }
}
.animate-scanline {
  animation: scanline 3s linear infinite;
}
</style>
