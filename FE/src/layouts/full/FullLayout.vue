<script setup>
import { RouterView } from 'vue-router';
import MainView from './Main.vue';
</script>

<template>
    <v-locale-provider>
        <MainView />
        <v-main class="main-content-area">
            <v-container fluid class="page-wrapper px-0 pt-0 pb-0"
                style="height: 100%; overflow: hidden !important; display: flex; flex-direction: column;">
                <div class="content-shell">
                    <RouterView v-slot="{ Component, route }">
                        <transition name="page-fade" mode="out-in">
                            <component :is="Component" :key="route.path" />
                        </transition>
                    </RouterView>
                </div>
            </v-container>
        </v-main>
    </v-locale-provider>
</template>

<style>
html,
body {
    overflow: hidden !important;
    height: 100%;
    margin: 0;
}

/* Global compact mode for page content spacing */
.page-wrapper .pa-6 {
    padding-top: 20px !important;
    padding-right: 24px !important;
    padding-bottom: 20px !important;
    padding-left: 24px !important;
}

.page-wrapper .pa-4 {
    padding-top: 16px !important;
    padding-right: 20px !important;
    padding-bottom: 16px !important;
    padding-left: 20px !important;
}

.page-wrapper .mb-6 {
    margin-bottom: 10px !important;
}

.page-wrapper .mt-6 {
    margin-top: 10px !important;
}

.page-wrapper .py-6 {
    padding-top: 10px !important;
    padding-bottom: 10px !important;
}

.main-content-area {
    height: 100vh;
    overflow: hidden !important;
    background-color: #ffffff;
}

.content-shell {
    width: 100%;
    height: 100%;
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden !important;
    margin: 0;
}

/* Optimized Smooth Page Transition */
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
