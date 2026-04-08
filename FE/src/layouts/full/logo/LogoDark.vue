<script setup>
import { RouterLink } from 'vue-router';
import logo from '@/assets/images/logos/logo.jpg';
</script>
<template>
    <div class="logo">
        <RouterLink to="/" class="d-flex justify-center">
            <img :src="logo" alt="home" class="logo-img" />
        </RouterLink>
    </div>
</template>

<style scoped>
.logo {
    display: flex;
    justify-content: center;
    width: 100%;
}
.logo-img {
    max-width: 150px;
    max-height: 120px;
    height: auto;
    object-fit: contain;
    display: block;
}
</style>
