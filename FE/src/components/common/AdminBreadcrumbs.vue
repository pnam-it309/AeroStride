<script setup>
import { onMounted, onBeforeUnmount, watch } from 'vue';
import { useUIStore } from '@/stores/ui';

const props = defineProps({
    items: {
        type: Array,
        required: true
    }
});

const uiStore = useUIStore();

const updateBreadcrumbs = () => {
    const sanitizedItems = props.items.map(item => ({
        ...item,
        to: item.to || (item.href ? undefined : '')
    }));
    uiStore.setBreadcrumbs(sanitizedItems);
};

onMounted(updateBreadcrumbs);

watch(() => props.items, updateBreadcrumbs, { deep: true });

onBeforeUnmount(() => {
    // Optional: Clean up breadcrumbs when leaving the page
    // uiStore.setBreadcrumbs([]);
});
</script>

<template>
    <!-- Render nothing here, breadcrumbs are now in the Header -->
    <div class="d-none"></div>
</template>

<style scoped>
/* Component style moved to Header */
</style>
