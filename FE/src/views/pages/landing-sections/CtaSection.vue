<script setup>
import { defineProps } from 'vue';
const props = defineProps({ active: Boolean });
</script>

<template>
    <section class="snap-section cta-section d-flex align-center justify-center text-center bg-white">
        <div class="cta-bg"></div>
        <v-container class="z-index-10" :class="{ 'animate-in-up': props.active }">
            <h2 class="text-h1 font-weight-black text-blue-darken-4 mb-8">Ready to Stride?</h2>
            <v-btn size="x-large" color="blue-accent-4" rounded="xl" class="px-16 py-8 text-h4 elevation-20 font-weight-bold">
                MUA NGAY BÂY GIỜ
            </v-btn>
            <p class="mt-8 text-h6 opacity-60">Ưu đãi 20% cho khách hàng mới trong tuần này!</p>
        </v-container>
    </section>
</template>

<style scoped lang="scss">
.snap-section { height: 100vh; width: 100%; scroll-snap-align: start; scroll-snap-stop: always; display: flex; align-items: center; position: relative; overflow: hidden; }
.cta-bg { position: absolute; width: 200%; height: 200%; background: radial-gradient(circle, rgba(41, 98, 255, 0.1) 0%, transparent 60%); z-index: 0; }
.z-index-10 { z-index: 10; }
.animate-in-up { animation: slideInUp 0.8s ease-out both; }
@keyframes slideInUp { from { opacity: 0; transform: translateY(100px); } to { opacity: 1; transform: translateY(0); } }
</style>
