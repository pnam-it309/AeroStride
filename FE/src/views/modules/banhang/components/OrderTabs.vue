<script setup>
/**
 * Component: OrderTabs
 * Y nghia: thanh tab hoa don cho cua POS, cho phep chuyen don, tao don moi,
 * dong don va gioi han so hoa don cho.
 */
/**
 * Module: Bán hàng tại quầy (Admin)
 * Component: OrderTabs
 * Chức năng: Hiển thị thanh tabs các hóa đơn đang chờ thanh toán (tối đa 5 hóa đơn),
 *            cho phép tạo thêm hoặc chuyển đổi giữa các hóa đơn.
 */
import { PlusIcon, XIcon } from 'vue-tabler-icons';

const MAX_WAITING_ORDERS = 5;

defineProps(['orders', 'activeIndex']);
const emit = defineEmits(['select', 'create', 'close']);

const getOrderItemCount = (order) =>
    (order?.listsHoaDonChiTiet || []).reduce((sum, item) => sum + (Number(item.soLuong) || 0), 0);
</script>

<template>
    <div class="order-tabs d-flex align-center gap-2 pb-1">
        <div
            v-for="(order, idx) in orders"
            :key="order.id"
            class="order-tab d-flex align-center cursor-pointer px-4 transition-all"
            :class="activeIndex === idx ? 'order-tab-active' : 'order-tab-inactive'"
            @click="emit('select', idx)"
        >
            <span class="order-tab-label font-weight-bold text-body-2">
                #{{ order.maHoaDon || idx + 1 }}
            </span>
            <span class="order-item-count" :title="`Tổng số lượng sản phẩm trong giỏ: ${getOrderItemCount(order)}`">
                {{ getOrderItemCount(order) }} SP
            </span>
            <v-btn
                icon
                size="x-small"
                variant="text"
                class="ml-2 hover-close-btn"
                @click.stop="emit('close', order.id, idx)"
            >
                <XIcon size="14" />
            </v-btn>
        </div>

        <v-btn 
            v-if="orders.length < MAX_WAITING_ORDERS" 
            icon 
            color="#2E4E8E" 
            variant="flat" 
            size="40" 
            class="rounded-lg flex-shrink-0 plus-tab-btn" 
            @click="emit('create')"
        >
            <PlusIcon size="20" />
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
    min-width: 206px;
    height: 40px;
    border-radius: 8px;
    user-select: none;
    box-sizing: border-box;
    display: inline-flex;
    align-items: center;
}

.order-tab-label {
    flex: 1 1 auto;
    white-space: nowrap;
}

.order-item-count {
    flex: 0 0 auto;
    margin-left: 8px;
    padding: 2px 7px;
    border-radius: 999px;
    background: #eff6ff;
    color: #1e40af;
    border: 1px solid #bfdbfe;
    font-size: 11px;
    line-height: 16px;
    font-weight: 700;
    white-space: nowrap;
}

.order-tab-inactive .order-item-count {
    background: #ffffff;
    color: #475569;
    border-color: #e2e8f0;
}

.order-tab-active {
    background: #ffffff !important;
    color: #2e4e8e !important;
    border-left: 4px solid #2e4e8e !important;
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05) !important;
    border-top: 1px solid #dfe5ee;
    border-right: 1px solid #dfe5ee;
    border-bottom: 1px solid #dfe5ee;
}

.order-tab-inactive {
    background: #f1f5f9 !important;
    color: #64748b !important;
    border: 1px solid #e2e8f0 !important;
}

.order-tab-inactive:hover {
    background: #e2e8f0 !important;
}

.hover-close-btn {
    width: 20px !important;
    height: 20px !important;
    color: #94a3b8;
}

.hover-close-btn:hover {
    color: #ef4444 !important;
    background: rgba(239, 68, 68, 0.1) !important;
}

.plus-tab-btn {
    background-color: #e0f2fe !important;
    color: #0284c7 !important;
    border: 1px solid #bae6fd !important;
    min-width: 40px !important;
    height: 40px !important;
}
</style>
