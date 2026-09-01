<script setup>
import { RouterLink } from 'vue-router';
import logo from '@/assets/images/logos/logoclient.jpg';

defineProps({
    dark: {
        type: Boolean,
        default: false
    },
    hideText: {
        type: Boolean,
        default: false
    }
});
</script>

<template>
    <div class="logo-client-container">
        <RouterLink to="/" class="logo-link d-flex align-center ga-3 text-decoration-none">
            <div class="logo-img-box">
                <img :src="logo" alt="AeroStride Logo" class="logo-img" />
            </div>
            <div v-if="!hideText" class="logo-text-block d-flex align-center">
                <span class="brand-text-italic">AEROSTRIDE</span>
            </div>
        </RouterLink>
    </div>
</template>

<style scoped lang="scss">
.logo-client-container {
    display: inline-flex;
    align-items: center;
}

.logo-link {
    cursor: pointer;
    transition: transform 0.25s ease;

    &:hover {
        transform: translateY(-1px) scale(1.03);

        .brand-text-italic {
            filter: drop-shadow(0 2px 10px rgba(37, 99, 235, 0.5));
        }

        .logo-img {
            transform: scale(1.06);
        }
    }
}

.logo-img-box {
    width: 62px;
    height: 62px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 12px;
    background: transparent;
    overflow: hidden;
    flex-shrink: 0;
}

.logo-img {
    width: 100%;
    height: 100%;
    object-fit: contain;
    mix-blend-mode: multiply;
    transition: transform 0.3s ease;
}

.logo-text-block {
    line-height: 1;
}

/* Chữ AEROSTRIDE in nghiêng hiệu ứng chữ xanh rực rỡ phóng to */
.brand-text-italic {
    font-size: 2.15rem;
    font-weight: 900;
    font-style: italic;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    font-family: 'Outfit', 'Inter', sans-serif;
    background: linear-gradient(135deg, #1e257c 0%, #2563eb 50%, #38bdf8 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    display: block;
    line-height: 1;
    text-shadow: 0 2px 12px rgba(37, 99, 235, 0.2);
    transition: filter 0.3s ease;
}

@media (max-width: 600px) {
    .logo-img-box {
        width: 48px;
        height: 48px;
    }
    .brand-text-italic {
        font-size: 1.6rem;
    }
}
</style>
