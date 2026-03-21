<script setup>
import { useRoute, useRouter } from 'vue-router'
import useAuthStore from '@/stores/authStore'
import { ROUTES } from '@/constants'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

defineProps({
  isSidebarOpen: Boolean
})
defineEmits(['toggle-sidebar'])

const handleLogout = () => {
  authStore.logout()
  router.push(ROUTES.AUTH.LOGIN)
}
</script>

<template>
  <header class="h-24 border-b border-gray-100 flex items-center justify-between px-10 z-40 bg-white/80 backdrop-blur-md sticky top-0">
    <div class="flex items-center gap-6">
      <!-- Toggle Button with Rotation Animation -->
      <button 
        @click="$emit('toggle-sidebar')"
        class="group relative w-10 h-10 flex items-center justify-center rounded-xl bg-gray-50 border border-gray-100 hover:border-aurora/50 transition-all active:scale-90"
      >
        <div 
          class="flex flex-col gap-1 transition-transform duration-500 group-hover:rotate-[360deg]"
          :class="isSidebarOpen ? 'rotate-0' : 'rotate-180'"
        >
          <div class="w-4 h-0.5 bg-gray-400 group-hover:bg-aurora transition-colors"></div>
          <div class="w-2 h-0.5 bg-gray-400 group-hover:bg-aurora transition-colors"></div>
          <div class="w-4 h-0.5 bg-gray-400 group-hover:bg-aurora transition-colors"></div>
        </div>
      </button>

      <div class="flex flex-col">
        <h2 class="text-2xl font-black font-display text-gray-900 uppercase tracking-tighter italic leading-none">
          {{ route.meta.title || 'Overview' }}
        </h2>
        <span class="text-[10px] font-bold text-gray-400 uppercase tracking-[0.3em] mt-1 ml-0.5">Control Center</span>
      </div>
    </div>
    
    <div class="flex items-center gap-8">
      <div class="hidden md:flex items-center bg-gray-50 rounded-xl px-4 py-2.5 border border-gray-100 focus-within:border-aurora/50 transition-colors">
        <input type="text" placeholder="Global Search..." class="bg-transparent border-none outline-none text-sm w-64 text-gray-900 font-bold placeholder:text-gray-300" />
        <span class="text-gray-300">⌘K</span>
      </div>

      <div class="flex items-center gap-4 pl-8 border-l border-gray-100">
        <button 
          @click="handleLogout"
          class="text-[10px] font-black text-gray-400 hover:text-red-500 uppercase tracking-widest transition-colors"
        >
          Sign Out
        </button>
        <div class="w-12 h-12 rounded-2xl bg-aurora/10 border border-aurora/20 flex items-center justify-center overflow-hidden shadow-sm">
          <span class="text-xs font-black text-aurora italic">AS</span>
        </div>
      </div>
    </div>
  </header>
</template>
