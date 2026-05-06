<script setup>
import { computed, ref, watch } from 'vue';
import { getDisplayImageUrl } from '../utils/imageDisplay';

const props = defineProps({
    src: {
        type: String,
        default: ''
    },
    fallbackSrc: {
        type: String,
        default: ''
    },
    alt: {
        type: String,
        default: 'product-image'
    },
    fit: {
        type: String,
        default: 'cover'
    }
});

const resolvedSrc = computed(() => getDisplayImageUrl(props.src) || props.fallbackSrc || '');
const currentSrc = ref(resolvedSrc.value);

watch(resolvedSrc, (value) => {
    currentSrc.value = value;
}, { immediate: true });

const handleError = () => {
    if (props.fallbackSrc && currentSrc.value !== props.fallbackSrc) {
        currentSrc.value = props.fallbackSrc;
    }
};
</script>

<template>
    <img
        :src="currentSrc"
        :alt="alt"
        :class="['safe-product-image', `safe-product-image--${fit}`]"
        referrerpolicy="no-referrer"
        loading="lazy"
        decoding="async"
        @error="handleError"
    />
</template>

<style scoped>
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
</style>
