<script setup>
import { RouterView } from 'vue-router';
import MainView from './Main.vue';
</script>

<template>
    <v-locale-provider >
        <v-app style="overflow: hidden !important;">
            <MainView />
            <v-main style="height: 100vh; overflow: hidden !important;">
                <v-container fluid class="page-wrapper bg-background px-sm-5 px-4 pt-12 rounded-xl" style="height: 100%; overflow: hidden !important;">
                    <div class="maxWidth" style="height: 100%; overflow: hidden !important;">
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
