<script setup>
defineProps({
    colspan: { type: [Number, String], default: 20 },
    icon: { type: String, default: 'mdi-package-variant' },
    text: { type: String, default: 'Không có dữ liệu phù hợp để hiển thị' }
});
</script>

<template>
    <tr>
        <td :colspan="colspan" class="empty-state py-16 text-center">
            <div class="d-flex flex-column align-center py-12 bg-slate-50-30 rounded-lg mx-4 my-2">
                <v-icon :icon="icon" size="48" style="color: #94a3b8 !important; opacity: 0.6;" class="mb-3" />
                <span class="text-slate-500" style="font-size: 14px !important; font-weight: 400 !important;">{{ text }}</span>
            </div>
        </td>
    </tr>
</template>

<style scoped>
.empty-state {
    border-bottom: none !important;
}
</style>
