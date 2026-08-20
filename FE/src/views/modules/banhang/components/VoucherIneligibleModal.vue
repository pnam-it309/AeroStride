<script setup>
/**
 * Module: Bán hàng tại quầy (Admin)
 * Component: VoucherIneligibleModal
 * Chức năng: Popup thông báo & cảnh báo khi đơn hàng không đáp ứng đủ điều kiện Đơn hàng tối thiểu (donHangToiThieu)
 *            hoặc điều kiện phiếu giảm giá bị thay đổi, cho phép nhân viên chọn Hủy phiếu hoặc Mua thêm sản phẩm.
 */
import { computed } from 'vue';
import { AlertTriangleIcon, TrashIcon, PlusIcon, XIcon, ShoppingCartIcon } from 'vue-tabler-icons';
import { formatCurrency } from '@/utils/formatters';

const props = defineProps({
    show: {
        type: Boolean,
        default: false
    },
    voucher: {
        type: Object,
        default: null
    },
    orderTotal: {
        type: Number,
        default: 0
    },
    minOrder: {
        type: Number,
        default: 0
    },
    shortfall: {
        type: Number,
        default: 0
    },
    message: {
        type: String,
        default: ''
    }
});

const emit = defineEmits(['close', 'remove-voucher']);

const voucherCode = computed(() => {
    return props.voucher?.ma || props.voucher?.maPhieu || props.voucher?.tenPhieu || props.voucher?.ten || 'PHIẾU GIẢM GIÁ';
});

const calculatedShortfall = computed(() => {
    if (props.shortfall && props.shortfall > 0) return props.shortfall;
    return Math.max(0, (props.minOrder || 0) - (props.orderTotal || 0));
});
</script>

<template>
    <v-dialog
        :model-value="show"
        max-width="520"
        persistent
        transition="dialog-bottom-transition"
        @update:model-value="!$event && emit('close')"
    >
        <v-card class="voucher-ineligible-card pa-5 bg-white border elevation-6">
            <!-- Modal Header -->
            <div class="d-flex align-center justify-space-between mb-4">
                <div class="d-flex align-center ga-2 text-warning-header font-weight-bold text-h6">
                    <div class="warning-icon-circle d-flex align-center justify-center">
                        <AlertTriangleIcon size="22" class="warning-icon" />
                    </div>
                    <span>Phiếu giảm giá chưa đủ điều kiện</span>
                </div>
                <v-btn icon variant="text" size="small" class="text-slate-500" @click="emit('close')">
                    <XIcon size="20" />
                </v-btn>
            </div>

            <!-- Warning Notice Banner -->
            <div class="warning-banner d-flex align-center ga-2.5 pa-3 mb-4">
                <AlertTriangleIcon size="18" class="warning-icon flex-shrink-0" />
                <span class="text-subtitle-2 font-weight-medium">
                    {{ message || `Đơn hàng chưa đạt giá trị tối thiểu của phiếu giảm giá [${voucherCode}].` }}
                </span>
            </div>

            <!-- Voucher Information Box -->
            <div class="voucher-info-box pa-3.5 border rounded-lg bg-slate-50 mb-4">
                <div class="d-flex align-center justify-space-between mb-2">
                    <span class="text-caption text-slate-500 font-weight-medium">Phiếu đang áp dụng:</span>
                    <span class="voucher-code-badge px-2.5 py-0.5 rounded text-caption font-weight-bold">
                        {{ voucherCode }}
                    </span>
                </div>
                <div class="text-body-2 font-weight-semibold text-slate-800 mb-2">
                    {{ props.voucher?.ten || props.voucher?.tenPhieu || 'Phiếu ưu đãi' }}
                </div>

                <!-- Condition Metric Grid -->
                <div class="metric-grid d-grid ga-2 pt-2 border-t mt-2">
                    <div class="d-flex justify-space-between align-center text-caption text-slate-600">
                        <span>Đơn hàng tối thiểu yêu cầu:</span>
                        <span class="font-weight-bold text-slate-900">{{ formatCurrency(minOrder) }}</span>
                    </div>
                    <div class="d-flex justify-space-between align-center text-caption text-slate-600">
                        <span>Tiền hàng hiện tại của đơn:</span>
                        <span class="font-weight-bold text-slate-900">{{ formatCurrency(orderTotal) }}</span>
                    </div>
                    <div class="d-flex justify-space-between align-center text-caption pt-1 border-t">
                        <span class="font-weight-semibold text-amber-900">Cần mua thêm:</span>
                        <span class="font-weight-black text-amber-700 text-subtitle-2">
                            +{{ formatCurrency(calculatedShortfall) }}
                        </span>
                    </div>
                </div>
            </div>

            <!-- Suggestion / Explanation -->
            <div class="text-caption text-slate-500 mb-5 line-height-normal">
                💡 <span class="font-weight-medium">Gợi ý xử lý:</span> Bạn có thể <strong>Hủy phiếu giảm giá</strong> để tiếp tục thanh toán đơn hàng với giá thông thường, hoặc bấm <strong>Mua thêm sản phẩm</strong> để thêm biến thể vào giỏ đạt giá trị tối thiểu.
            </div>

            <!-- Action Buttons -->
            <div class="d-flex ga-3">
                <button
                    type="button"
                    class="btn-remove-voucher flex-1 d-flex align-center justify-center ga-1.5 font-weight-semibold"
                    @click="emit('remove-voucher')"
                >
                    <TrashIcon size="16" />
                    <span>Hủy phiếu giảm giá</span>
                </button>

                <button
                    type="button"
                    class="btn-buy-more flex-1 d-flex align-center justify-center ga-1.5 font-weight-bold"
                    @click="emit('close')"
                >
                    <PlusIcon size="16" />
                    <span>Mua thêm sản phẩm</span>
                </button>
            </div>
        </v-card>
    </v-dialog>
</template>

<style scoped>
.voucher-ineligible-card {
    border-radius: 12px !important;
    font-family: inherit;
}

.text-warning-header {
    color: #b45309 !important;
}

.warning-icon-circle {
    width: 32px;
    height: 32px;
    border-radius: 50% !important;
    background-color: #fef3c7 !important;
}

.warning-icon {
    color: #d97706 !important;
}

.warning-banner {
    background-color: #fffbeb !important;
    border: 1px solid #fde68a !important;
    color: #92400e !important;
    border-radius: 10px !important;
}

.voucher-info-box {
    border-color: #e2e8f0 !important;
}

.voucher-code-badge {
    background-color: #fef3c7;
    color: #b45309;
    border: 1px solid #fde68a;
}

.metric-grid {
    grid-template-columns: 1fr;
}

.btn-remove-voucher {
    padding: 11px 16px;
    border-radius: 10px !important;
    border: 1.5px solid #fca5a5;
    background-color: #fef2f2;
    color: #dc2626;
    font-size: 13.5px;
    cursor: pointer;
    transition: all 0.2s ease;
    white-space: nowrap;
}

.btn-remove-voucher:hover {
    background-color: #fee2e2;
    border-color: #f87171;
}

.btn-buy-more {
    padding: 11px 16px;
    border-radius: 10px !important;
    border: none;
    background-color: #1e257c !important;
    color: #ffffff !important;
    font-size: 13.5px;
    cursor: pointer;
    transition: all 0.2s ease;
    white-space: nowrap;
}

.btn-buy-more:hover {
    background-color: #141a5c !important;
    box-shadow: 0 4px 12px rgba(30, 37, 124, 0.25);
}
</style>
