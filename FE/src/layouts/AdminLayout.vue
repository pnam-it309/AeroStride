<script setup>
import { ref } from 'vue'
import AdminSidebar from '../components/admin/layout/AdminSidebar.vue'
import AdminHeader from '../components/admin/layout/AdminHeader.vue'

import { ADMIN_NAV_ITEMS } from '@/constants'

const isSidebarOpen = ref(true)

const navItems = ADMIN_NAV_ITEMS
</script>

<template>
  <div class="flex h-screen bg-white">

    <AdminSidebar 
      :is-sidebar-open="isSidebarOpen" 
      :nav-items="navItems" 
    />

    <!-- Main -->
    <div class="flex flex-col flex-1 overflow-hidden">

      <AdminHeader @toggle-sidebar="isSidebarOpen = !isSidebarOpen" :is-sidebar-open="isSidebarOpen" />

      <!-- Content (One Point View: Standardized for all pages) -->
      <main class="flex-1 overflow-hidden px-12 py-8 flex flex-col">
        <div class="w-full max-w-[1600px] mx-auto flex-1 flex flex-col min-h-0">
          <router-view v-slot="{ Component }">
            <transition name="page-slide" mode="out-in">
              <div :key="$route.path" class="h-full flex flex-col min-h-0">
                <component :is="Component" />
              </div>
            </transition>
          </router-view>
        </div>
      </main>

    </div>

  </div>
</template>
<style scoped>
.router-link-active {
  box-shadow: 0 0 15px rgba(6, 182, 212, 0.3);
  border: 1px solid rgba(6, 182, 212, 0.2);
}
</style>