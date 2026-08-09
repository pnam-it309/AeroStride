<script setup>
import { RouterLink } from 'vue-router';
import logo from '@/assets/images/logos/logoclient.jpg';
</script>
<template>
    <div class="logo">
        <RouterLink to="/" class="d-flex justify-center align-center">
            <img :src="logo" alt="AeroStride Logo" class="logo-img" />
        </RouterLink>
    </div>
</template>

<style scoped>
.logo {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
}
.logo-img {
    max-width: 150px;
    height: auto;
    object-fit: contain;
    display: block;
    mix-blend-mode: multiply;
    transition: opacity 0.2s ease;
}
.logo-img:hover {
    opacity: 0.85;
}
</style>
