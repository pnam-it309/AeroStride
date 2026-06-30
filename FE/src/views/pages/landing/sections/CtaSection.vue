<script setup>
const props = defineProps({ active: Boolean, warm: Boolean });
</script>

<template>
    <section class="snap-section cta-section d-flex align-center justify-center text-center bg-white">
        <template v-if="props.active || props.warm">
            <div class="cta-bg"></div>
            <v-container class="z-index-10" :class="{ 'animate-in-up': props.active }">
                <h2 class="text-h1 font-weight-black text-blue-darken-4 mb-8">Ready to Stride?</h2>
                <v-btn size="x-large" color="#2962ff" rounded="xl" variant="flat" height="72" class="px-16 text-h5 text-white font-weight-bold" to="/shoes">
                    KHÁM PHÁ CÁC MẪU GIÀY
                </v-btn>
                <p class="mt-8 text-h6 opacity-60">Ghé thăm showroom hoặc săn voucher giảm giá ngay hôm nay!</p>
            </v-container>
        </template>
    </section>
</template>

<style scoped lang="scss">
.snap-section {
    height: 100vh;
    width: 100%;
    display: flex;
    align-items: center;
    position: relative;
    overflow: hidden;
    background: #ffffff !important;
}
.cta-bg {
    position: absolute;
    width: 100%;
    height: 100%;
    background: transparent;
    z-index: 0;
}
.z-index-10 {
    z-index: 10;
}
.animate-in-up {
    animation: slideInUp 0.8s ease-out both;
}
@keyframes slideInUp {
    from {
        opacity: 0;
        transform: translateY(100px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}
</style>
