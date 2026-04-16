<script setup>
import { PlusIcon, ReceiptIcon, XIcon } from 'vue-tabler-icons';

const props = defineProps(['orders', 'activeIndex']);
const emit = defineEmits(['select', 'create', 'close']);
</script>

<template>
    <div class="order-tabs d-flex align-center gap-2 mb-6 overflow-x-auto pb-2">
        <v-btn
            v-for="(order, idx) in orders"
            :key="order.id"
            :variant="activeIndex === idx ? 'flat' : 'tonal'"
            :color="activeIndex === idx ? '#2E4E8E' : 'grey-lighten-3'"
            height="48"
            class="rounded-lg text-none px-5 transition-all"
            @click="emit('select', idx)"
        >
            <ReceiptIcon size="18" class="mr-2 opacity-70" />
            <span class="font-weight-bold">Đơn {{ idx + 1 }}</span>
            <v-btn
                v-if="orders.length > 1"
                icon
                size="x-small"
                variant="text"
                class="ml-2 hover-close"
                @click.stop="emit('close', order.id, idx)"
            >
                <XIcon size="14" />
            </v-btn>
        </v-btn>

        <v-btn v-if="orders.length < 5" icon color="primary" variant="tonal" size="48" class="rounded-lg" @click="emit('create')">
            <PlusIcon size="24" />
        </v-btn>
    </div>
</template>

<style scoped>
.gap-2 {
    gap: 8px;
}
.transition-all {
    transition: all 0.2s ease;
}
.hover-close:hover {
    color: rgb(var(--v-theme-error)) !important;
    background: rgba(var(--v-theme-error), 0.1);
}
</style>



