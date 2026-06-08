<script setup>
/**
 * Module: Bán hàng tại quầy (Admin)
 * Component: OrderTabs
 * Chức năng: Hiển thị thanh tabs các hóa đơn đang chờ thanh toán (tối đa 5 hóa đơn),
 *            cho phép tạo thêm hoặc chuyển đổi giữa các hóa đơn.
 */
import { computed } from 'vue';
import { PlusIcon, ReceiptIcon, XIcon } from 'vue-tabler-icons';

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
    <div class="order-tabs d-flex align-center gap-2 overflow-x-auto pb-2">
        <v-btn
            v-for="(order, idx) in orders"
            :key="order.id"
            :variant="activeIndex === idx ? 'flat' : 'tonal'"
            :color="activeIndex === idx ? '#2E4E8E' : 'grey-lighten-3'"
            height="56"
            class="order-tab rounded-lg text-none px-4 transition-all"
            @click="emit('select', idx)"
        >
            <ReceiptIcon size="18" class="mr-2 opacity-70 flex-shrink-0" />
            <span class="tab-copy">
                <span class="font-weight-bold">Đơn {{ idx + 1 }}</span>
                <span class="tab-summary">
                    {{ orderSummaries[idx]?.quantity || 0 }} SP - {{ formatCurrency(orderSummaries[idx]?.total) }}
                </span>
            </span>
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

        <v-btn v-if="orders.length < MAX_WAITING_ORDERS" icon color="primary" variant="tonal" size="48" class="rounded-lg flex-shrink-0" @click="emit('create')">
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
.order-tab {
    min-width: 164px;
    justify-content: flex-start;
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
