<script setup>
defineProps({
  isSidebarOpen: Boolean,
  navItems: Array,
})
</script>

<template>
  <aside
    class="h-screen bg-white border-r border-gray-200 transition-all duration-500 z-50 flex flex-col shadow-xl flex-shrink-0"
    :class="isSidebarOpen ? 'w-64' : 'w-20'"
  >
    <div class="p-6 flex items-center gap-3 overflow-hidden">
      <div class="w-8 h-8 bg-aurora rounded-lg flex-shrink-0 shadow-lg shadow-aurora/30"></div>
      <transition
        enter-active-class="transition-all duration-500 ease-out"
        leave-active-class="transition-all duration-300 ease-in"
        enter-from-class="opacity-0 -translate-x-4"
        leave-to-class="opacity-0 -translate-x-4"
      >
        <span
          v-if="isSidebarOpen"
          class="font-display font-black text-xl tracking-tight italic text-gray-900 whitespace-nowrap"
        >
          AERO<span class="text-aurora">STRIDE</span>
        </span>
      </transition>
    </div>

    <nav class="flex-1 px-4 py-8 space-y-3 overflow-x-hidden">
      <router-link
        v-for="item in navItems"
        :key="item.name"
        :to="item.path"
        class="flex items-center gap-4 px-4 py-3.5 rounded-xl transition-all duration-300 group hover:bg-gray-50 flex-nowrap"
        active-class="bg-aurora/5 text-aurora shadow-sm"
      >
        <div
          class="w-10 h-10 rounded-xl bg-gray-50 border border-gray-100 group-hover:bg-aurora text-gray-400 group-hover:text-white flex items-center justify-center transition-all duration-300 flex-shrink-0 group-hover:shadow-lg group-hover:shadow-aurora/20"
        >
          <i :class="[`ph-${item.icon}-bold`, 'text-xl']"></i>
        </div>
        <transition
          enter-active-class="transition-all duration-500 delay-150"
          leave-active-class="transition-all duration-200"
          enter-from-class="opacity-0 translate-x-2"
          leave-to-class="opacity-0 translate-x-2"
        >
          <span
            v-if="isSidebarOpen"
            class="font-black text-xs uppercase tracking-widest text-gray-500 group-hover:text-gray-900 whitespace-nowrap"
          >
            {{ item.name }}
          </span>
        </transition>
      </router-link>
    </nav>
  </aside>
</template>
