<script setup>
// Auth Layout
</script>

<template>
  <div class="min-h-screen grid lg:grid-cols-2">
    <!-- Left: Decorative / Brand -->
    <div class="hidden lg:flex relative bg-obsidian overflow-hidden items-center justify-center">
      <div class="absolute inset-0 bg-gradient-to-br from-aurora/20 to-transparent"></div>
      <div class="z-10 text-center p-12">
        <h1 class="text-6xl font-display font-black text-cloud mb-6 tracking-tighter italic">
          AERO<span class="text-aurora">STRIDE</span>
        </h1>
        <p class="text-xl text-cloud/60 max-w-md mx-auto font-light leading-relaxed">
          Elevate every step. Precision-engineered gear for the modern runner.
        </p>
      </div>
      <!-- Background Glows -->
      <div class="absolute -top-40 -left-40 w-96 h-96 bg-aurora/10 rounded-full blur-[100px]"></div>
      <div
        class="absolute -bottom-40 -right-40 w-96 h-96 bg-aurora/5 rounded-full blur-[100px]"
      ></div>
    </div>

    <!-- Right: Auth Content -->
    <div class="flex items-center justify-center p-8 bg-obsidian lg:bg-obsidian/50 border-white/5">
      <div class="w-full max-w-md">
        <router-view />
      </div>
    </div>
  </div>
</template>
