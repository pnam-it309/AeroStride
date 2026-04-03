<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import * as LucideIcons from 'lucide-vue-next'
import { ChevronDown } from 'lucide-vue-next'
import logoUrl from '@/assets/logo.jpg'

const props = defineProps({
  isSidebarOpen: Boolean,
  navItems: Array,
})

const route = useRoute()
const expandedItems = ref([])
const hoveredItem = ref(null)
let hideTimeout = null

const startHideTimeout = () => {
  hideTimeout = setTimeout(() => {
    hoveredItem.value = null
  }, 200)
}

const clearHideTimeout = () => {
  if (hideTimeout) clearTimeout(hideTimeout)
}

const toggleExpand = (itemName) => {
  const index = expandedItems.value.indexOf(itemName)
  if (index > -1) {
    expandedItems.value.splice(index, 1)
  } else {
    expandedItems.value.push(itemName)
  }
}

const isExpanded = (itemName) => expandedItems.value.includes(itemName)

const matchesPath = (targetPath) => {
  if (!targetPath) return false
  return route.path === targetPath || route.path.startsWith(`${targetPath}/`)
}

const hasActiveChildren = (children = []) => {
  return children.some((child) => {
    if (matchesPath(child.path)) return true
    return hasActiveChildren(child.children || [])
  })
}

const isActive = (item) => {
  if (matchesPath(item.path)) return true
  if (item.children) {
    return hasActiveChildren(item.children)
  }
  return false
}

// Initial state: Expand parents that have active children
onMounted(() => {
  props.navItems.forEach(item => {
    if (item.children && hasActiveChildren(item.children)) {
      if (!isExpanded(item.name)) expandedItems.value.push(item.name)
    }
  })
})
</script>

<template>
  <aside
    class="h-screen bg-white border-r border-gray-200 transition-all duration-500 z-50 flex flex-col shadow-[10px_0_30px_rgba(0,0,0,0.02)] flex-shrink-0"
    :class="isSidebarOpen ? 'w-80' : 'w-24'"
  >
    <!-- Logo Section -->
    <div class="px-2 py-3 flex items-center justify-center overflow-hidden bg-white">
      <img
        :src="logoUrl"
        alt="AeroStride Logo"
        class="object-contain flex-shrink-0 transform hover:scale-110 transition-all duration-700 cursor-pointer"
        :class="isSidebarOpen ? 'h-32' : 'h-16'"
      />
    </div>

    <!-- Navigation -->
    <nav class="flex-1 px-4 py-2 space-y-3 overflow-x-hidden overflow-y-auto custom-scrollbar">
      <div v-for="item in navItems" :key="item.name" class="flex flex-col">

        <!-- Items without children → router-link -->
        <router-link
          v-if="!item.children"
          :to="item.path"
          class="flex items-center gap-4 px-4 py-4 rounded-2xl transition-all duration-500 group flex-nowrap relative overflow-hidden"
          :class="[
            isActive(item) ? 'bg-aurora/5 text-aurora ring-1 ring-aurora/10 shadow-sm' : 'text-gray-500 hover:text-gray-900',
          ]"
        >
          <!-- Hover Background (Direction: Left to Right) -->
          <div class="absolute inset-0 bg-gradient-to-r from-aurora/10 via-aurora/5 to-transparent -translate-x-full group-hover:translate-x-0 transition-transform duration-500 ease-out -z-10"></div>

          <div
            class="w-12 h-12 rounded-2xl border border-gray-100 flex items-center justify-center transition-all duration-500 flex-shrink-0 shadow-sm"
            :class="isActive(item) ? 'bg-aurora text-white shadow-lg shadow-aurora/30 scale-105' : 'bg-gray-50 group-hover:bg-aurora group-hover:text-white group-hover:shadow-lg group-hover:shadow-aurora/30'"
          >
            <!-- Category Icon: Subtle Move on Hover, No Rotate -->
            <component :is="LucideIcons[item.icon]" class="w-6 h-6 transition-all duration-300 group-hover:-translate-y-0.5 group-hover:scale-110" />
          </div>
          
          <div v-if="isSidebarOpen" class="flex flex-col min-w-0 transition-all duration-300 group-hover:translate-x-1">
            <span
              class="font-bold text-[11px] uppercase tracking-[0.2em] whitespace-nowrap"
              :class="isActive(item) ? 'text-gray-900' : 'text-gray-400 group-hover:text-gray-900 font-bold'"
            >{{ item.name }}</span>
            <span class="text-[10px] text-gray-400 group-hover:text-aurora font-medium truncate w-44 transition-colors">{{ item.description }}</span>
          </div>

          <!-- Active indicator -->
          <div v-if="isActive(item)" class="absolute left-0 w-1.5 h-8 bg-aurora rounded-r-full shadow-glow"></div>
        </router-link>
        <div v-else class="relative rounded-2xl group/parent" @mouseenter="clearHideTimeout(); hoveredItem = item.name" @mouseleave="startHideTimeout()">
           <!-- Hover Background for Parent (Left to Right) -->
          <div class="absolute inset-0 bg-gradient-to-r from-aurora/10 via-aurora/5 to-transparent -translate-x-full group-hover/parent:translate-x-0 transition-transform duration-500 ease-out -z-10"></div>

          <button
            @click="toggleExpand(item.name)"
            class="w-full flex items-center gap-4 px-4 py-4 rounded-2xl transition-all duration-500 flex-nowrap relative"
            :class="isActive(item) ? 'bg-aurora/5 text-aurora ring-1 ring-aurora/10 shadow-sm' : 'text-gray-500 hover:text-gray-900'"
          >
            <div
              class="w-12 h-12 rounded-2xl border border-gray-100 flex items-center justify-center transition-all duration-500 flex-shrink-0 shadow-sm"
              :class="isActive(item) ? 'bg-aurora text-white shadow-lg shadow-aurora/30 scale-105' : 'bg-gray-50 group-hover/parent:bg-aurora group-hover/parent:text-white group-hover/parent:shadow-lg group-hover/parent:shadow-aurora/30'"
            >
              <component :is="LucideIcons[item.icon]" class="w-6 h-6 transition-all duration-500 group-hover/parent:-translate-y-0.5 group-hover/parent:scale-110" />
            </div>
            <template v-if="isSidebarOpen">
              <div class="flex-1 flex flex-col min-w-0 text-left transition-all duration-300 group-hover/parent:translate-x-1">
                <span
                  class="font-bold text-[11px] uppercase tracking-[0.2em] whitespace-nowrap"
                  :class="isActive(item) ? 'text-gray-900' : 'text-gray-400 group-hover/parent:text-gray-900 font-bold'"
                >{{ item.name }}</span>
                <span class="text-[10px] text-gray-400 group-hover/parent:text-aurora font-medium truncate w-44 transition-colors">{{ item.description }}</span>
              </div>
              <!-- CHEVRON: Always rotate on hover, even if expanded -->
              <div class="transition-transform duration-500 ease-spring" 
                   :class="isExpanded(item.name) ? 'rotate-180 group-hover/parent:rotate-[540deg]' : 'rotate-0 group-hover/parent:rotate-[360deg]'">
                <ChevronDown
                  class="w-5 h-5 text-gray-400 transition-colors"
                  :class="isExpanded(item.name) ? 'text-aurora' : 'group-hover/parent:text-gray-900 group-hover/parent:scale-125'"
                />
              </div>
            </template>
            <!-- Active indicator -->
            <div v-if="isActive(item)" class="absolute left-0 w-1.5 h-8 bg-aurora rounded-r-full shadow-glow"></div>
          </button>

          <!-- Floating Submenu (When Sidebar Closed) -->
          <div
            v-if="!isSidebarOpen && hoveredItem === item.name"
            @mouseenter="clearHideTimeout()"
            @mouseleave="startHideTimeout()"
            class="fixed left-24 top-0 translate-y-1/4 ml-2 w-64 bg-white border border-gray-100 rounded-3xl shadow-2xl p-4 py-8 z-[100] animate-in fade-in slide-in-from-left-4 duration-300 ring-1 ring-black/5"
          >
             <div class="mb-6 px-4">
                <div class="flex items-center gap-3 mb-2">
                   <div class="w-8 h-8 rounded-xl bg-aurora/10 flex items-center justify-center text-aurora">
                      <component :is="LucideIcons[item.icon]" class="w-4 h-4" />
                   </div>
                   <span class="text-[10px] font-black text-gray-900 uppercase tracking-widest">{{ item.name }}</span>
                </div>
                <div class="h-0.5 w-full bg-slate-50 flex">
                   <div class="h-full w-8 bg-aurora shadow-glow"></div>
                </div>
             </div>
             <div class="flex flex-col space-y-1.5">
                <template v-for="child in item.children" :key="child.name">
                  <router-link
                    v-if="!child.children"
                    :to="child.path"
                    class="flex items-center gap-4 px-4 py-4 rounded-2xl text-[11px] font-bold uppercase tracking-wider transition-all duration-300 group/child relative overflow-hidden"
                    :class="matchesPath(child.path) ? 'bg-aurora/5 text-aurora ring-1 ring-aurora/10' : 'text-gray-400 hover:text-gray-900 hover:bg-gray-50 shadow-sm hover:shadow-md'"
                  >
                     <div class="w-1.5 h-1.5 rounded-full transition-all" :class="matchesPath(child.path) ? 'bg-aurora scale-125 shadow-glow' : 'bg-gray-300 shadow-sm group-hover/child:bg-aurora'"></div>
                     {{ child.name }}
                  </router-link>
                  <div v-else class="rounded-2xl border border-slate-100 bg-slate-50/70 px-3 py-3">
                    <router-link
                      :to="child.path"
                      class="flex items-center gap-3 px-2 py-2 text-[11px] font-black uppercase tracking-[0.18em] transition"
                      :class="isActive(child) ? 'text-aurora' : 'text-slate-500 hover:text-slate-900'"
                    >
                      <div class="h-1.5 w-1.5 rounded-full" :class="isActive(child) ? 'bg-aurora shadow-glow' : 'bg-slate-300'" />
                      {{ child.name }}
                    </router-link>
                    <div class="mt-2 ml-4 flex flex-col space-y-1.5 border-l border-slate-200 pl-4">
                      <router-link
                        v-for="grandChild in child.children"
                        :key="grandChild.name"
                        :to="grandChild.path"
                        class="flex items-center gap-3 rounded-xl px-3 py-2 text-[10px] font-bold uppercase tracking-[0.16em] transition"
                        :class="matchesPath(grandChild.path) ? 'bg-aurora/5 text-aurora' : 'text-slate-400 hover:bg-white hover:text-slate-900'"
                      >
                        <div class="h-1.5 w-1.5 rounded-full" :class="matchesPath(grandChild.path) ? 'bg-aurora shadow-glow' : 'bg-slate-300'" />
                        {{ grandChild.name }}
                      </router-link>
                    </div>
                  </div>
                </template>
             </div>
          </div>

          <!-- Standard Inline Submenu (When Sidebar Open) -->
          <transition
            enter-active-class="transition-all duration-300 ease-out"
            leave-active-class="transition-all duration-200 ease-in"
            enter-from-class="opacity-0 -translate-y-2 max-h-0"
            leave-to-class="opacity-0 -translate-y-2 max-h-0"
          >
            <div
              v-if="isSidebarOpen && isExpanded(item.name)"
              class="ml-14 mt-1 mb-3 flex flex-col space-y-1 pl-4 border-l-2 border-gray-100/50"
            >
              <template v-for="child in item.children" :key="child.name">
                <router-link
                  v-if="!child.children"
                  :to="child.path"
                  class="flex items-center gap-3 px-4 py-3 rounded-xl text-[11px] font-bold uppercase tracking-[0.15em] transition-all duration-200 relative group/child"
                  :class="matchesPath(child.path)
                    ? 'text-aurora bg-aurora/5'
                    : 'text-gray-400 hover:text-gray-900 hover:bg-gray-50'"
                >
                  <div
                    class="w-1.5 h-1.5 rounded-full transition-all flex-shrink-0"
                    :class="matchesPath(child.path)
                      ? 'bg-aurora scale-125 shadow-[0_0_8px_rgba(6,182,212,0.6)]'
                      : 'bg-gray-300 group-hover/child:bg-aurora'"
                  />
                  {{ child.name }}
                </router-link>
                <div v-else class="space-y-1 rounded-2xl border border-slate-100 bg-slate-50/80 px-3 py-3">
                  <router-link
                    :to="child.path"
                    class="flex items-center gap-3 rounded-xl px-3 py-2 text-[11px] font-black uppercase tracking-[0.16em] transition"
                    :class="isActive(child) ? 'bg-white text-aurora shadow-sm' : 'text-slate-500 hover:bg-white hover:text-slate-900'"
                  >
                    <div
                      class="w-1.5 h-1.5 rounded-full transition-all flex-shrink-0"
                      :class="isActive(child)
                        ? 'bg-aurora scale-125 shadow-[0_0_8px_rgba(6,182,212,0.6)]'
                        : 'bg-slate-300'"
                    />
                    {{ child.name }}
                  </router-link>
                  <div class="ml-4 flex flex-col space-y-1 border-l border-slate-200 pl-4">
                    <router-link
                      v-for="grandChild in child.children"
                      :key="grandChild.name"
                      :to="grandChild.path"
                      class="flex items-center gap-3 rounded-xl px-3 py-2 text-[10px] font-bold uppercase tracking-[0.16em] transition"
                      :class="matchesPath(grandChild.path)
                        ? 'bg-white text-aurora shadow-sm'
                        : 'text-slate-400 hover:bg-white hover:text-slate-900'"
                    >
                      <div
                        class="w-1.5 h-1.5 rounded-full transition-all flex-shrink-0"
                        :class="matchesPath(grandChild.path)
                          ? 'bg-aurora scale-125 shadow-[0_0_8px_rgba(6,182,212,0.6)]'
                          : 'bg-slate-300'"
                      />
                      {{ grandChild.name }}
                    </router-link>
                  </div>
                </div>
              </template>
            </div>
          </transition>
        </div>
      </div>
    </nav>

  </aside>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 5px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #f1f5f9;
  border-radius: 10px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #e2e8f0;
}

.shadow-glow {
  box-shadow: 0 0 20px rgba(6, 182, 212, 0.5);
}

.ease-spring {
  transition-timing-function: cubic-bezier(0.68, -0.6, 0.32, 1.6);
}

.scrollbar-none {
  scrollbar-width: none;
}
</style>
