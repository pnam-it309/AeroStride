<script setup>
/**
 * Module: Bán hàng tại quầy (Admin)
 * Component: VoucherIneligibleModal
 * Chức năng: Popup thông báo & cảnh báo khi đơn hàng không đáp ứng đủ điều kiện Đơn hàng tối thiểu (donHangToiThieu)
 *            hoặc điều kiện phiếu giảm giá bị thay đổi, cho phép nhân viên chọn Hủy phiếu hoặc Mua thêm sản phẩm.
 */
import { computed } from 'vue';
import { AlertTriangleIcon, TrashIcon, PlusIcon, XIcon, TicketIcon, InfoCircleIcon } from 'vue-tabler-icons';
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

const voucherName = computed(() => {
    return props.voucher?.ten || props.voucher?.tenPhieu || 'Phiếu ưu đãi giảm giá';
});

const calculatedShortfall = computed(() => {
    if (props.shortfall && props.shortfall > 0) return props.shortfall;
    return Math.max(0, (props.minOrder || 0) - (props.orderTotal || 0));
});
</script>

<template>
    <v-dialog
        :model-value="show"
        max-width="560"
        persistent
        transition="dialog-transition"
        @update:model-value="!$event && emit('close')"
    >
        <v-card class="voucher-ineligible-card pa-6 bg-white border elevation-10 rounded-2xl overflow-hidden">
            <!-- Modal Header -->
            <div class="d-flex align-center justify-space-between mb-4 pb-2 border-b">
                <div class="d-flex align-center ga-3">
                    <div class="warning-icon-circle d-flex align-center justify-center flex-shrink-0">
                        <AlertTriangleIcon size="22" class="warning-icon" />
                    </div>
                    <div>
                        <div class="text-h6 font-weight-bold text-slate-900 line-height-tight" style="font-size: 16.5px !important">
                            Phiếu giảm giá chưa đủ điều kiện
                        </div>
                        <div class="text-caption text-slate-500 mt-0.5">
                            Giá trị đơn hàng chưa đạt mức tối thiểu yêu cầu
                        </div>
                    </div>
                </div>
                <v-btn
                    icon
                    variant="text"
                    size="small"
                    class="text-slate-400 hover:text-slate-700 rounded-lg"
                    @click="emit('close')"
                >
                    <XIcon size="20" />
                </v-btn>
            </div>

            <!-- Warning Notice Banner -->
            <div class="warning-banner d-flex align-start ga-3 pa-3.5 mb-4 rounded-xl">
                <AlertTriangleIcon size="20" class="warning-icon flex-shrink-0 mt-0.5" />
                <div class="text-subtitle-2 font-weight-medium text-amber-950" style="line-height: 1.45">
                    {{ message || `Đơn hàng chưa đạt giá trị tối thiểu của phiếu giảm giá [${voucherCode}]. Cần thêm ${formatCurrency(calculatedShortfall)} để áp dụng.` }}
                </div>
            </div>

            <!-- Voucher & Conditions Details Box -->
            <div class="voucher-details-container rounded-xl border mb-4 bg-slate-50 p-0 overflow-hidden">
                <!-- Voucher Head Info -->
                <div class="d-flex align-center justify-space-between px-4 py-3 bg-slate-100/70 border-b">
                    <div class="d-flex align-center ga-2 text-truncate pr-2">
                        <TicketIcon size="18" class="text-primary flex-shrink-0" />
                        <span class="font-weight-bold text-slate-800 text-truncate" style="font-size: 14px">
                            {{ voucherName }}
                        </span>
                    </div>
                    <span class="voucher-code-pill px-3 py-1 rounded-full text-caption font-weight-black flex-shrink-0">
                        {{ voucherCode }}
                    </span>
                </div>

                <!-- Financial Metrics Table -->
                <div class="px-4 py-3 d-flex flex-column ga-2.5">
                    <!-- Row 1: Đơn hàng tối thiểu -->
                    <div class="d-flex justify-space-between align-center text-caption text-slate-600">
                        <span class="font-weight-medium">Đơn hàng tối thiểu yêu cầu:</span>
                        <span class="font-weight-bold text-slate-900 font-tabular" style="font-size: 13.5px">
                            {{ formatCurrency(minOrder) }}
                        </span>
                    </div>

                    <!-- Row 2: Tiền hàng hiện tại -->
                    <div class="d-flex justify-space-between align-center text-caption text-slate-600">
                        <span class="font-weight-medium">Tiền hàng hiện tại của đơn:</span>
                        <span class="font-weight-bold text-slate-900 font-tabular" style="font-size: 13.5px">
                            {{ formatCurrency(orderTotal) }}
                        </span>
                    </div>

                    <!-- Divider -->
                    <div class="border-t my-1"></div>

                    <!-- Row 3: Cần mua thêm (Highlight) -->
                    <div class="d-flex justify-space-between align-center py-1">
                        <div class="d-flex align-center ga-1.5">
                            <span class="font-weight-bold text-amber-900" style="font-size: 13.5px">
                                Cần mua thêm:
                            </span>
                        </div>
                        <div class="shortfall-badge px-3 py-1 rounded-lg font-weight-black text-amber-700 font-tabular" style="font-size: 14.5px">
                            +{{ formatCurrency(calculatedShortfall) }}
                        </div>
                    </div>
                </div>
            </div>

            <!-- Suggestion Box -->
            <div class="suggestion-box d-flex align-start ga-2.5 pa-3 rounded-xl mb-5">
                <InfoCircleIcon size="18" class="text-blue-600 flex-shrink-0 mt-0.5" />
                <div class="text-caption text-slate-600 line-height-relaxed">
                    <span class="font-weight-bold text-slate-800">Gợi ý xử lý:</span>
                    Bạn có thể bấm <strong>Hủy phiếu giảm giá</strong> để thanh toán theo giá gốc, hoặc bấm <strong>Mua thêm sản phẩm</strong> để tiếp tục thêm biến thể vào giỏ hàng đạt điều kiện.
                </div>
            </div>

            <!-- Action Buttons -->
            <div class="d-flex ga-3 align-center">
                <button
                    type="button"
                    class="btn-remove-voucher flex-1 d-flex align-center justify-center ga-2 font-weight-bold"
                    @click="emit('remove-voucher')"
                >
                    <TrashIcon size="17" />
                    <span>Hủy phiếu giảm giá</span>
                </button>

                <button
                    type="button"
                    class="btn-buy-more flex-1 d-flex align-center justify-center ga-2 font-weight-bold"
                    @click="emit('close')"
                >
                    <PlusIcon size="17" />
                    <span>Mua thêm sản phẩm</span>
                </button>
            </div>
        </v-card>
    </v-dialog>
</template>

<style scoped>
.voucher-ineligible-card {
    border-radius: 18px !important;
    box-shadow: 0 20px 45px -10px rgba(15, 23, 42, 0.18), 0 8px 18px rgba(15, 23, 42, 0.08) !important;
}

.warning-icon-circle {
    width: 38px;
    height: 38px;
    border-radius: 12px !important;
    background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%) !important;
    border: 1px solid #fcd34d;
}

.warning-icon {
    color: #d97706 !important;
}

.warning-banner {
    background-color: #fffbeb !important;
    border: 1px solid #fde68a !important;
}

.voucher-details-container {
    border-color: #e2e8f0 !important;
}

.voucher-code-pill {
    background: #fef3c7;
    color: #b45309;
    border: 1px solid #fde68a;
    letter-spacing: 0.5px;
}

.shortfall-badge {
    background-color: #fef3c7;
    border: 1px solid #fcd34d;
}

.suggestion-box {
    background-color: #f0f9ff;
    border: 1px solid #bae6fd;
}

.font-tabular {
    font-variant-numeric: tabular-nums;
}

.btn-remove-voucher {
    height: 44px;
    padding: 0 18px;
    border-radius: 12px !important;
    border: 1.5px solid #fecaca;
    background-color: #ffffff;
    color: #dc2626;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s ease;
    white-space: nowrap;
}

.btn-remove-voucher:hover {
    background-color: #fef2f2;
    border-color: #f87171;
    transform: translateY(-1px);
}

.btn-remove-voucher:active {
    transform: translateY(0);
}

.btn-buy-more {
    height: 44px;
    padding: 0 18px;
    border-radius: 12px !important;
    border: none;
    background: linear-gradient(135deg, #1e257c 0%, #171d64 100%) !important;
    color: #ffffff !important;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s ease;
    white-space: nowrap;
    box-shadow: 0 4px 14px rgba(30, 37, 124, 0.28);
}

.btn-buy-more:hover {
    background: linear-gradient(135deg, #161b5c 0%, #101449 100%) !important;
    box-shadow: 0 6px 18px rgba(30, 37, 124, 0.38);
    transform: translateY(-1px);
}

.btn-buy-more:active {
    transform: translateY(0);
}
</style>
