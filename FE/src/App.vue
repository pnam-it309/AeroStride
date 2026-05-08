<template>
  <v-app>
    <GlobalLoader />
    <GlobalToast />
    <RouterView v-slot="{ Component }">
      <transition name="layout-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </RouterView>
  </v-app>
</template>

<script setup>
import { RouterView } from "vue-router";
import GlobalLoader from "@/components/common/GlobalLoader.vue";
import GlobalToast from "@/components/common/GlobalToast.vue";
</script>

<style>
/* Optimized Layout Transition */
.layout-fade-enter-active,
.layout-fade-leave-active {
  transition: opacity 0.3s ease;
}

.layout-fade-enter-from,
.layout-fade-leave-to {
  opacity: 0;
}
</style>
