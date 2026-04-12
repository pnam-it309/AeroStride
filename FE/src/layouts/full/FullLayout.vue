<script setup>
import { RouterView } from 'vue-router';
import MainView from './Main.vue';
</script>

<template>
    <v-locale-provider >
        <v-app style="overflow: hidden !important;">
            <MainView />
            <v-main style="height: 100vh; overflow: hidden !important;">
                <v-container fluid class="page-wrapper bg-background px-4 pt-4" style="height: 100%; overflow-y: auto !important; overflow-x: hidden;">
                    <div class="maxWidth" style="margin: 0 auto;">
                        <RouterView />
                    </div>
                </v-container>
            </v-main>
        </v-app>
    </v-locale-provider>
</template>

<style>
html, body {
  overflow: hidden !important;
  height: 100%;
  margin: 0;
}
</style>
