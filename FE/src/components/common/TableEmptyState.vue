<script setup>
defineProps({
    colspan: { type: [Number, String], default: 20 },
    icon: { type: String, default: 'mdi-database-search-outline' },
    text: { type: String, default: 'Không có dữ liệu phù hợp để hiển thị' },
    subtext: { type: String, default: '' },
    showAction: { type: Boolean, default: false },
    actionText: { type: String, default: '' }
});

defineEmits(['action']);
</script>

<template>
    <tr>
        <td :colspan="colspan" class="empty-state py-8 text-center">
            <div class="d-flex flex-column align-center justify-center py-4 px-4 w-100 animate-fade-in">
                <div
                    class="empty-state-icon-box d-flex align-center justify-center rounded-circle mb-3 mx-auto"
                    style="width: 68px; height: 68px; background: rgba(241, 245, 249, 0.8); border: 1.5px dashed #cbd5e1"
                >
                    <v-icon :icon="icon || 'mdi-database-search-outline'" size="32" style="color: #94a3b8 !important" />
                </div>
                <span class="text-slate-600 text-center font-weight-medium" style="font-size: 14px !important; line-height: 1.5; max-width: 420px">{{ text }}</span>
                <span v-if="subtext" class="text-slate-400 text-center text-caption mt-1">{{ subtext }}</span>
                <slot>
                    <v-btn
                        v-if="showAction && actionText"
                        variant="outlined"
                        color="primary"
                        size="small"
                        class="rounded-pill mt-3 text-none"
                        @click="$emit('action')"
                    >
                        {{ actionText }}
                    </v-btn>
                </slot>
            </div>
        </td>
    </tr>
</template>

<style scoped>
.empty-state {
    border-bottom: none !important;
}
</style>
