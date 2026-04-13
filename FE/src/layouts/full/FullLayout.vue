<script setup>
import { RouterView } from 'vue-router';
import MainView from './Main.vue';
</script>

<template>
    <v-locale-provider>
        <v-app style="overflow: hidden !important">
            <MainView />
            <v-main style="height: 100vh; overflow: hidden !important">
                <v-container
                    fluid
                    class="page-wrapper bg-background px-0 pt-2"
                    style="height: 100%; overflow-y: auto !important; overflow-x: hidden"
                >
                    <div class="content-shell">
                        <RouterView />
                    </div>
                </v-container>
            </v-main>
        </v-app>
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
    padding-top: 12px !important;
    padding-right: 10px !important;
    padding-bottom: 12px !important;
    padding-left: 4px !important;
}

.page-wrapper .pa-4 {
    padding-top: 10px !important;
    padding-right: 10px !important;
    padding-bottom: 10px !important;
    padding-left: 4px !important;
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

.content-shell {
    width: 100%;
    margin: 0;
}
</style>
