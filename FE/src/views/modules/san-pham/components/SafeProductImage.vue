<script setup>
import { computed, ref, watch } from 'vue';
import { getDisplayImageUrl } from '@/utils/imageDisplay';
import defaultShoeImg from '@/assets/images/products/cat_running.jpg';

const props = defineProps({
    src: {
        type: String,
        default: ''
    },
    fallbackSrc: {
        type: String,
        default: defaultShoeImg
    },
    alt: {
        type: String,
        default: 'product-image'
    },
    fit: {
        type: String,
        default: 'cover'
    },
    iconSize: {
        type: [Number, String],
        default: 24
    }
});

const imageError = ref(false);
const resolvedSrc = computed(() => {
    const raw = props.src || '';
    if (!raw || typeof raw !== 'string' || !raw.trim()) {
        return props.fallbackSrc || defaultShoeImg;
    }
    return getDisplayImageUrl(raw) || props.fallbackSrc || defaultShoeImg;
});
const currentSrc = ref(resolvedSrc.value);

watch(
    resolvedSrc,
    (value) => {
        currentSrc.value = value;
        imageError.value = false;
    },
    { immediate: true }
);

const handleError = () => {
    if (props.fallbackSrc && currentSrc.value !== props.fallbackSrc) {
        currentSrc.value = props.fallbackSrc;
    } else {
        imageError.value = true;
    }
};
</script>

<template>
    <div class="safe-product-image-wrap">
        <img
            v-if="currentSrc && !imageError"
            :src="currentSrc"
            :alt="alt"
            :class="['safe-product-image', `safe-product-image--${fit}`]"
            referrerpolicy="no-referrer"
            loading="lazy"
            decoding="async"
            @error="handleError"
        />
        <div v-else class="safe-product-image-fallback">
            <v-icon :size="iconSize" color="grey-lighten-1">mdi-package-variant-closed</v-icon>
        </div>
    </div>
</template>

<style scoped>
.safe-product-image-wrap {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    background-color: #f8fafc;
}

.safe-product-image {
    width: 100%;
    height: 100%;
    display: block;
}

.safe-product-image--cover {
    object-fit: cover;
}

.safe-product-image--contain {
    object-fit: contain;
}

.safe-product-image-fallback {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
}
</style>
