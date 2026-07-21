<script setup>
import AdminFilter from '@/components/common/AdminFilter.vue';

const props = defineProps({
    loading: Boolean,
    isRefreshing: Boolean,
    searchQuery: String,
    statusFilter: [String, Number, null]
});

const emit = defineEmits(['refresh', 'search', 'update:searchQuery', 'update:statusFilter']);

const handleRefresh = () => emit('refresh');
const handleSearch = () => emit('search');
</script>

<template>
    <div class="filter-shell">
        <AdminFilter title="Bộ lọc thuộc tính" :loading="loading" @refresh="handleRefresh">
            <v-col cols="12" md="5" class="py-1">
                <div class="filter-field-label">Tìm kiếm nhanh</div>
                <v-text-field
                    :model-value="searchQuery"
                    @update:model-value="emit('update:searchQuery', $event)"
                    placeholder="Tên, mã..."
                    variant="outlined"
                    density="compact"
                    hide-details
                    prepend-inner-icon="mdi-magnify"
                    class="compact-input"
                    @keyup.enter="handleSearch"
                ></v-text-field>
            </v-col>
            <v-col cols="12" md="5" class="py-1">
                <div class="filter-field-label">Trạng thái</div>
                <v-select
                    :model-value="statusFilter"
                    @update:model-value="emit('update:statusFilter', $event)"
                    :items="[
                        { title: 'Tất cả', value: null },
                        { title: 'Đang hoạt động', value: 'DANG_HOAT_DONG' },
                        { title: 'Ngừng hoạt động', value: 'NGUNG_HOAT_DONG' }
                    ]"
                    variant="outlined"
                    density="compact"
                    hide-details
                    class="compact-input"
                    :menu-props="{ contentClass: 'attribute-select-menu' }"
                ></v-select>
            </v-col>
        </AdminFilter>
    </div>
</template>

<style scoped>
:deep(.compact-input) .v-field__input,
:deep(.compact-input) input,
:deep(.compact-input) input::placeholder,
:deep(.compact-input) .v-select__selection-text {
    font-size: 13px !important;
}

.filter-field-label {
    font-size: 13px !important;
}

:global(.attribute-select-menu .v-list-item-title),
:global(.attribute-select-menu .v-list-item) {
    font-size: 13px !important;
}
</style>
