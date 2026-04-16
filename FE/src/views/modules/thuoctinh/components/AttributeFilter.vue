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
    <div class="flex-none mb-4">
        <AdminFilter 
            title="Bộ lọc thuộc tính" 
            :loading="loading" 
            @refresh="handleRefresh" 
            class="compact-filter-card"
        >
            <v-col cols="12" md="3" class="py-1">
                <div class="filter-label">Tìm kiếm nhanh</div>
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
            <v-col cols="12" md="2" class="py-1">
                <div class="filter-label">Trạng thái</div>
                <v-select 
                    :model-value="statusFilter"
                    @update:model-value="emit('update:statusFilter', $event)"
                    :items="[
                        { title: 'Tất cả', value: null },
                        { title: 'Hoạt động', value: 'DANG_HOAT_DONG' },
                        { title: 'Ngừng bán', value: 'KHONG_HOAT_DONG' }
                    ]" 
                    variant="outlined" 
                    density="compact" 
                    hide-details 
                    class="compact-input"
                ></v-select>
            </v-col>
        </AdminFilter>
    </div>
</template>

<style scoped>
.filter-label {
    font-size: 12px;
    font-weight: 700;
    color: #64748b;
    margin-bottom: 2px;
    margin-left: 2px;
}

:deep(.compact-input .v-field__input) {
    min-height: 36px !important;
    padding-top: 0 !important;
    padding-bottom: 0 !important;
    font-size: 13px;
}

:deep(.compact-input .v-field) {
    border-radius: 8px !important;
}

:deep(.compact-filter-card) {
    margin-bottom: 8px !important;
}

:deep(.compact-filter-card .v-card-text) {
    padding: 8px 16px !important;
}

:deep(.compact-filter-card .filter-header) {
    margin-bottom: 4px !important;
}

:deep(.compact-filter-card .filter-title) {
    font-size: 14px !important;
}
</style>
