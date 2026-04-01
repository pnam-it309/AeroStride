<script setup>
import logo from '@/assets/logo.jpg'
</script>

<template>
  <div class="min-h-screen grid lg:grid-cols-2">
    <!-- Left: Decorative / Brand -->
    <div class="hidden lg:flex relative bg-obsidian overflow-hidden items-center justify-center">
      <div class="absolute inset-0 bg-gradient-to-br from-aurora/20 to-transparent"></div>
      <div class="z-10 text-center p-12">
        <div class="mb-12 relative flex justify-center">
          <!-- Multi-layered glowing backgrounds -->
          <div class="absolute inset-0 bg-aurora/40 blur-[120px] rounded-full scale-110 animate-pulse-slow"></div>
          <div class="absolute inset-0 bg-aurora/10 blur-[60px] rounded-full scale-90"></div>
          
          <!-- The Badge Container -->
          <div class="relative z-20 bg-white p-2 rounded-full shadow-2xl shadow-aurora/40 ring-12 ring-white/10 overflow-hidden transform hover:scale-105 transition-transform duration-700 cursor-pointer">
            <div class="w-56 h-56 flex items-center justify-center bg-white">
              <img :src="logo" alt="AeroStride Logo" class="w-full h-auto object-center" />
            </div>
          </div>
        </div>
        <p class="text-xl text-cloud/60 max-w-md mx-auto font-light leading-relaxed">
          Nâng tầm từng bước chạy. Trang thiết bị được chế tạo chính xác dành cho người chạy bộ hiện đại.
        </p>
      </div>
      <!-- Background Glows -->
      <div class="absolute -top-40 -left-40 w-96 h-96 bg-aurora/10 rounded-full blur-[100px]"></div>
      <div class="absolute -bottom-40 -right-40 w-96 h-96 bg-aurora/5 rounded-full blur-[100px]"></div>
    </div>

    <!-- Right: Auth Content -->
    <div class="flex items-center justify-center p-8 bg-obsidian lg:bg-obsidian/50 border-white/5">
      <div class="w-full max-w-md">
        <router-view />
      </div>
    </div>
  </div>
</template>
