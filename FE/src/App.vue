<template>
  <v-app>
    <GlobalLoader />
    <RouterView></RouterView>
  </v-app>
</template>

<script setup>
import { RouterView } from "vue-router";
import GlobalLoader from "@/components/common/GlobalLoader.vue";
</script>
