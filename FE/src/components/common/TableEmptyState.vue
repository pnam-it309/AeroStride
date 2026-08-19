<script setup>
defineProps({
    colspan: { type: [Number, String], default: 20 },
    icon: { type: String, default: 'mdi-database-search-outline' },
    text: { type: String, default: 'Không có dữ liệu phù hợp để hiển thị' }
});
</script>

<template>
    <tr>
        <td :colspan="colspan" class="empty-state py-8 text-center">
            <div class="d-flex flex-column align-center justify-center py-4 px-4 w-100 animate-fade-in">
                <div
                    class="empty-state-icon-box d-flex align-center justify-center rounded-circle mb-3 mx-auto"
                    style="width: 64px; height: 64px; background: rgba(241, 245, 249, 0.8); border: 1.5px dashed #cbd5e1"
                >
                    <v-icon :icon="icon" size="30" style="color: #94a3b8 !important" />
                </div>
                <span class="text-slate-600 text-center font-weight-medium" style="font-size: 14px !important; line-height: 1.5; max-width: 420px">{{ text }}</span>
            </div>
        </td>
    </tr>
</template>

<style scoped>
.empty-state {
    border-bottom: none !important;
}
</style>
