<script setup>
/**
 * Module: Bán hàng tại quầy (Admin)
 * Component: OrderTabs
 * Chức năng: Hiển thị thanh tabs các hóa đơn đang chờ thanh toán (tối đa 5 hóa đơn),
 *            cho phép tạo thêm hoặc chuyển đổi giữa các hóa đơn.
 */
import { computed } from 'vue';

const MAX_WAITING_ORDERS = 5;

const props = defineProps(['orders', 'activeIndex']);
const emit = defineEmits(['select', 'create', 'close']);

// Format tổng tiền hiển thị trên mỗi tab (không lấy số thập phân)
const formatCurrency = (value) =>
    new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
        maximumFractionDigits: 0
    }).format(value || 0);

const orderSummaries = computed(() =>
    (props.orders || []).map((order) => {
        const items = order?.listsHoaDonChiTiet || [];
        const totalQuantity = items.reduce((sum, item) => sum + (Number(item.soLuong) || 0), 0);

        return {
            quantity: totalQuantity,
            total: order?.tongTienSauGiam ?? order?.tongTien ?? 0
        };
    })
);
</script>

<template>
    <div class="order-tabs d-flex align-center gap-1 overflow-x-auto">
        <v-btn
            v-for="(order, idx) in orders"
            :key="order.id"
            variant="flat"
            :color="activeIndex === idx ? 'white' : 'grey-lighten-4'"
            :class="activeIndex === idx ? 'active-tab' : 'inactive-tab'"
            height="36"
            class="order-tab rounded-lg text-none px-4 transition-all"
            @click="emit('select', idx)"
        >
            <span style="font-size: 13px;">
                {{ orders.length === 1 ? 'Đơn mới' : 'Đơn ' + (idx + 1) }}
            </span>
            <v-btn
                v-if="orders.length > 1"
                icon
                size="20"
                variant="text"
                class="ml-2 hover-close mr-n2"
                @click.stop="emit('close', order.id, idx)"
            >
                <v-icon size="14">mdi-close</v-icon>
            </v-btn>
        </v-btn>

        <v-btn v-if="orders.length < MAX_WAITING_ORDERS" icon color="white" variant="flat" size="36" class="rounded-lg flex-shrink-0" style="border: 1px solid #cbd5e1;" @click="emit('create')">
            <v-icon size="18" color="grey-darken-1">mdi-plus</v-icon>
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
.order-tab {
    justify-content: center;
}
.active-tab {
    color: #1e257c !important;
    border: 1px solid #1e257c !important;
    font-weight: 600 !important;
}
.inactive-tab {
    color: #475569 !important;
    border: 1px solid #cbd5e1 !important;
    font-weight: 400 !important;
}
.tab-copy {
    display: inline-flex;
    flex-direction: column;
    align-items: flex-start;
    line-height: 1.15;
    min-width: 0;
}
.tab-summary {
    display: block;
    max-width: 112px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 11px;
    font-weight: 600;
    opacity: 0.82;
    margin-top: 3px;
}
.hover-close:hover {
    color: rgb(var(--v-theme-error)) !important;
    background: rgba(var(--v-theme-error), 0.1);
}
</style>
