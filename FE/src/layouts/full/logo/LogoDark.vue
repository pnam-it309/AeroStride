<script setup>
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import logo from '@/assets/images/logos/logo.jpg';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';
import { PATH } from '@/router/routePaths';
import { isRoleStaff } from '@/constants/appConstants';

const adminHomePath = computed(() => {
    const userRole = dichVuXacThuc.layUserHienTai()?.role;
    if (isRoleStaff(userRole)) {
        return PATH.BAN_HANG;
    }
    return PATH.DASHBOARD;
});
</script>
<template>
    <div class="logo">
        <RouterLink :to="adminHomePath" class="d-flex justify-center">
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
    max-width: 182px;
    max-height: 138px;
    height: auto;
    object-fit: contain;
    display: block;
}
</style>
