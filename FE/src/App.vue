<script setup>
import { onMounted, watch } from 'vue';
import { useNotificationStore } from '@/stores/notificationStore';
import { useToast } from '@/composable/useToast';
import AeroProgressBar from '@/components/base/AeroProgressBar.vue';
import AeroLoadingOverlay from '@/components/base/AeroLoadingOverlay.vue';
import AeroToastContainer from '@/components/base/AeroToastContainer.vue';

const notificationStore = useNotificationStore();
const { addToast } = useToast();

onMounted(() => {
  notificationStore.connect();
});

// Khi có thông báo mới từ WebSocket (Redis broadcast), hiển thị Toast
watch(() => notificationStore.notifications.length, (newVal, oldVal) => {
  if (newVal > oldVal) {
    const lastNoti = notificationStore.notifications[newVal - 1];
    addToast(lastNoti.content, 'info');
  }
});
</script>

<template>
  <AeroProgressBar />
  <AeroLoadingOverlay />
  <AeroToastContainer />
  
  <router-view />
</template>

<style>
/* Global styles are handled in src/assets/index.css */
</style>
