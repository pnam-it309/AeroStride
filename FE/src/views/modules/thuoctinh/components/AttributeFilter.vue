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
        >
            <v-col cols="12" md="3" class="py-1">
                <div class="filter-field-label">Tìm kiếm nhanh</div>
                <v-text-field 
                    :model-value="searchQuery"
                    @update:model-value="emit('update:searchQuery', $event)"
                    placeholder="Tên, mã..." 
                    variant="outlined" 
                    density="compact"
                    hide-details 
                    prepend-inner-icon="mdi-magnify" 
                    @keyup.enter="handleSearch"
                ></v-text-field>
            </v-col>
            <v-col cols="12" md="2" class="py-1">
                <div class="filter-field-label">Trạng thái</div>
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
                ></v-select>
            </v-col>
        </AdminFilter>
    </div>
</template>

<style scoped>
/* Scoped styles removed in favor of global _admin-common.scss */
</style>
