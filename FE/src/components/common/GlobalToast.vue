<template>
    <v-snackbar
        v-model="toast.show"
        :color="toast.color"
        :timeout="toast.timeout"
        location="top right"
        class="premium-toast"
        elevation="12"
    >
        <div class="d-flex align-center cursor-pointer" style="cursor: pointer;" @click="toast.show = false">
            <v-icon :icon="toast.icon" class="mr-3 text-white" size="22"></v-icon>
            <span class="text-white font-weight-medium text-body-2">{{ toast.message }}</span>
        </div>

        <template v-slot:actions>
            <v-btn variant="text" icon="mdi-close" size="small" color="white" @click="toast.show = false"></v-btn>
        </template>
    </v-snackbar>
</template>

<script setup>
import { useToastStore } from '@/stores/toastStore';
import { watch } from 'vue';

const toast = useToastStore();

// Reset show state if a new message arrives while one is already showing
watch(
    () => toast.message,
    () => {
        if (toast.show) {
            toast.show = false;
            setTimeout(() => {
                toast.show = true;
            }, 100);
        }
    }
);
</script>

<style scoped>
.premium-toast :deep(.v-snackbar__wrapper) {
    border-radius: 12px !important;
    padding: 8px 16px !important;
}

/* Ensure text is always white even if color changes */
.text-white {
    color: white !important;
}
</style>
