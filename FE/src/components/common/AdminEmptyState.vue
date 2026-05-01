<script setup>
/**
 * AdminEmptyState - A standardized component for empty states across the admin panel.
 * Follows the "Premium" design language with icon blobs and bold typography.
 */
defineProps({
    icon: { type: [Object, String, Function], default: null },
    title: { type: String, default: 'Không có dữ liệu' },
    subtitle: { type: String, default: '' },
    iconSize: { type: [Number, String], default: 32 },
    iconColor: { type: String, default: 'primary' }
});
</script>

<template>
    <div class="admin-empty-state d-flex flex-column align-center justify-center py-12 px-6 text-center animate-fade-in">
        <div class="empty-icon-wrapper mb-6">
            <div class="icon-blob bg-blue-lighten-5 d-flex align-center justify-center" 
                 style="width: 72px; height: 72px; border-radius: 20px;">
                <slot name="icon">
                    <v-icon v-if="typeof icon === 'string'" :icon="icon" :size="iconSize" :color="iconColor" />
                    <component v-else-if="icon" :is="icon" :size="iconSize" :class="`text-${iconColor}`" />
                    <v-icon v-else icon="mdi-package-variant-closed" :size="iconSize" :color="iconColor" />
                </slot>
            </div>
        </div>
        
        <div class="text-h6 font-weight-black text-slate-800 mb-2">
            <slot name="title">{{ title }}</slot>
        </div>
        
        <div v-if="subtitle || $slots.subtitle" class="text-body-2 text-slate-500 max-w-sm mx-auto leading-relaxed">
            <slot name="subtitle">{{ subtitle }}</slot>
        </div>
        
        <div v-if="$slots.actions" class="mt-8">
            <slot name="actions"></slot>
        </div>
    </div>
</template>

<style scoped>
.max-w-sm {
    max-width: 320px;
}

.animate-fade-in {
    animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}

.icon-blob {
    transition: all 0.3s ease;
}

.admin-empty-state:hover .icon-blob {
    transform: scale(1.05) rotate(5deg);
    background-color: #e3f2fd !important;
}
</style>
