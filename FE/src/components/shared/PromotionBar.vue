<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const messages = [
    { text: 'Free Standard Delivery & 30-Day Free Returns', linkText: 'Join Now', link: '#' },
    { text: 'Extra 20% Off Select Sale Styles', linkText: 'Shop All', link: '#' },
    { text: 'New AeroStride X1 "Sensor" Is Here', linkText: 'Explore', link: '#' }
];

const currentIndex = ref(0);
let interval = null;

onMounted(() => {
    interval = setInterval(() => {
        currentIndex.value = (currentIndex.value + 1) % messages.length;
    }, 5000);
});

onUnmounted(() => {
    if (interval) clearInterval(interval);
});
</script>

<template>
    <div class="promotion-bar-wrapper bg-grey-lighten-4">
        <transition name="fade-slide" mode="out-in">
            <div :key="currentIndex" class="promo-content py-2 text-center">
                <span class="promo-text">{{ messages[currentIndex].text }}</span>
                <a :href="messages[currentIndex].link" class="promo-link ml-2">{{ messages[currentIndex].linkText }}</a>
                <a href="#" class="promo-details ml-4">View Details</a>
            </div>
        </transition>
    </div>
</template>

<style scoped lang="scss">
.promotion-bar-wrapper {
    width: 100%; border-bottom: 1px solid #e5e5e5;
    position: relative; overflow: hidden;
    min-height: 44px; display: flex; align-items: center; justify-content: center;
}

.promo-content {
    display: flex; align-items: center; justify-content: center; width: 100%;
}

.promo-text {
    font-size: 0.85rem; font-weight: 700; color: #111;
}

.promo-link, .promo-details {
    font-size: 0.85rem; font-weight: 800; color: #111; text-decoration: underline;
    &:hover { opacity: 0.7; }
}

.promo-details {
    text-decoration: none; border-bottom: 1px solid #111; padding-bottom: 1px;
}

/* Slide Transition */
.fade-slide-enter-active, .fade-slide-leave-active {
    transition: all 0.5s ease;
}
.fade-slide-enter-from { opacity: 0; transform: translateY(10px); }
.fade-slide-leave-to { opacity: 0; transform: translateY(-10px); }
</style>
