<template>
  <v-app>
    <RouterView v-slot="{ Component }">
      <transition name="page-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </RouterView>
  </v-app>
</template>
<script setup>
import { RouterView } from "vue-router";
</script>

<style scoped>
.page-fade-enter-active,
.page-fade-leave-active {
    transition: 
        opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1),
        transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.page-fade-enter-from {
    opacity: 0;
    transform: translateY(8px);
}

.page-fade-leave-to {
    opacity: 0;
    transform: translateY(-8px);
}
</style>
