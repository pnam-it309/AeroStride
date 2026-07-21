<template>
    <v-app>
        <GlobalLoader />
        <GlobalToast />
        <RouterView />
    </v-app>
</template>

<script setup>
import { RouterView } from 'vue-router';
import GlobalLoader from '@/components/common/GlobalLoader.vue';
import GlobalToast from '@/components/common/GlobalToast.vue';
</script>

<style>
/* Transition rules are defined centrally in src/scss/_animations.scss */
</style>
