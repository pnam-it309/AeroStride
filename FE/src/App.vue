<script setup>
import AeroProgressBar from '@/components/base/AeroProgressBar.vue';
import AeroLoadingOverlay from '@/components/base/AeroLoadingOverlay.vue';
import AeroToastContainer from '@/components/base/AeroToastContainer.vue';
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
