<script setup>
import { ref } from 'vue';
import { UserIcon, UserPlusIcon, PhoneIcon, SearchIcon, XIcon } from 'vue-tabler-icons';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';

const props = defineProps(['selectedCustomerName', 'activeOrderId']);
const emit = defineEmits(['select', 'remove']);

const search = ref('');
const results = ref([]);
const loading = ref(false);

const onSearch = async () => {
    if (!search.value || search.value.length < 2) {
        results.value = [];
        return;
    }
    loading.value = true;
    try {
        const data = await dichVuDonHang.searchKhachHang(search.value);
        results.value = data || [];
    } catch (e) {
        console.error(e);
    } finally {
        loading.value = false;
    }
};

const selectCustomer = (c) => {
    emit('select', c);
    search.value = '';
    results.value = [];
};
</script>

<template>
    <div class="customer-selector">
        <div class="d-flex justify-space-between align-center mb-3">
            <h3 class="text-subtitle-1 font-weight-bold">
                <UserIcon size="18" class="mr-1 text-primary" style="vertical-align: middle"/>
                Khách hàng
            </h3>
            <v-btn
                v-if="!selectedCustomerName"
                size="small"
                variant="text"
                color="primary"
                prepend-icon="mdi-plus"
                class="text-none"
            >
                Thêm mới
            </v-btn>
        </div>

        <div v-if="selectedCustomerName" class="selected-customer-card pa-3 border rounded-lg bg-primary-lighten-5 d-flex align-center">
            <v-avatar color="primary" size="36" class="mr-3">
                <span class="text-white text-caption">{{ selectedCustomerName.charAt(0) }}</span>
            </v-avatar>
            <div class="flex-grow-1">
                <div class="font-weight-bold text-body-2">{{ selectedCustomerName }}</div>
                <div class="text-caption text-grey-darken-1">Khách hàng thân thiết</div>
            </div>
            <v-btn icon size="x-small" variant="text" color="grey-darken-1" @click="emit('remove')">
                <XIcon size="16" />
            </v-btn>
        </div>

        <div v-else class="position-relative">
            <v-text-field
                v-model="search"
                placeholder="Tìm tên hoặc số điện thoại..."
                variant="outlined"
                density="comfortable"
                hide-details
                prepend-inner-icon="mdi-account-search"
                @input="onSearch"
            ></v-text-field>

            <v-card v-if="results.length > 0" class="search-overlay mt-1 shadow-lg border overflow-hidden">
                <v-list class="pa-0">
                    <v-list-item 
                        v-for="c in results" 
                        :key="c.id" 
                        class="pa-3 border-b"
                        @click="selectCustomer(c)"
                    >
                        <v-list-item-title class="font-weight-bold">{{ c.ten }}</v-list-item-title>
                        <v-list-item-subtitle class="d-flex align-center mt-1">
                            <PhoneIcon size="14" class="mr-1 opacity-60" /> {{ c.sdt }}
                        </v-list-item-subtitle>
                    </v-list-item>
                </v-list>
            </v-card>
        </div>
    </div>
</template>

<style scoped>
.selected-customer-card {
    border-color: rgba(var(--v-theme-primary), 0.2) !important;
    background-color: rgba(var(--v-theme-primary), 0.05);
}

.search-overlay {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    z-index: 999;
}
</style>
