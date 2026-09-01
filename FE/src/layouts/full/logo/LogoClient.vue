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
            <div v-if="!hideText" class="logo-text-block d-flex flex-column justify-center">
                <span class="brand-text-italic">AEROSTRIDE</span>
                <span class="brand-subtext" :class="{ 'text-grey-lighten-2': dark }">SPORT & SNEAKERS</span>
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
        transform: translateY(-1px) scale(1.02);

        .brand-text-italic {
            filter: drop-shadow(0 2px 8px rgba(37, 99, 235, 0.4));
        }

        .logo-img {
            transform: scale(1.05);
        }
    }
}

.logo-img-box {
    width: 50px;
    height: 50px;
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

/* Chữ AEROSTRIDE in nghiêng hiệu ứng chữ xanh rực rỡ */
.brand-text-italic {
    font-size: 1.65rem;
    font-weight: 900;
    font-style: italic;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    font-family: 'Outfit', 'Inter', sans-serif;
    background: linear-gradient(135deg, #1e257c 0%, #2563eb 50%, #38bdf8 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    display: block;
    line-height: 1.1;
    text-shadow: 0 2px 10px rgba(37, 99, 235, 0.15);
    transition: filter 0.3s ease;
}

.brand-subtext {
    font-size: 0.65rem;
    font-weight: 800;
    letter-spacing: 0.2em;
    text-transform: uppercase;
    color: #64748b;
    margin-top: 2px;
    display: block;
}

@media (max-width: 600px) {
    .logo-img-box {
        width: 42px;
        height: 42px;
    }
    .brand-text-italic {
        font-size: 1.35rem;
    }
    .brand-subtext {
        font-size: 0.55rem;
    }
}
</style>
