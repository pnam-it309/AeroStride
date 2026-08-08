<script setup>
/**
 * Module: Bán hàng tại quầy (Admin)
 * Component: BetterVoucherModal
 * Chức năng: Popup thông báo khi có Voucher tốt hơn (tiết kiệm hơn) lúc bấm Thanh toán,
 *            với giao diện màu XANH DƯƠNG ĐẬM (Dark Navy Blue #1e257c) chuẩn màu hệ thống AeroStride,
 *            thiết kế bo góc vừa vừa (10px-12px) không bị tràn hay vỡ giao diện.
 */
import { computed } from 'vue';
import { InfoCircleIcon, CheckIcon, XIcon } from 'vue-tabler-icons';

const props = defineProps({
    show: {
        type: Boolean,
        default: false
    },
    currentVoucher: {
        type: Object,
        default: null
    },
    betterVoucher: {
        type: Object,
        default: null
    },
    orderTotal: {
        type: Number,
        default: 0
    },
    currentDiscount: {
        type: Number,
        default: 0
    },
    betterDiscount: {
        type: Number,
        default: 0
    }
});

const emit = defineEmits(['close', 'keep-old', 'apply-new']);

const fmt = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

const extraSavings = computed(() => {
    return Math.max(0, (props.betterDiscount || 0) - (props.currentDiscount || 0));
});

const newTotalPayment = computed(() => {
    return Math.max(0, (props.orderTotal || 0) - (props.betterDiscount || 0));
});

const currentCode = computed(() => {
    return props.currentVoucher?.ma || props.currentVoucher?.maPhieu || props.currentVoucher?.tenPhieu || 'Không có';
});

const newCode = computed(() => {
    return props.betterVoucher?.ma || props.betterVoucher?.maPhieu || props.betterVoucher?.tenPhieu || 'MÃ MỚI';
});
</script>

<template>
    <v-dialog
        :model-value="show"
        max-width="500"
        persistent
        transition="dialog-bottom-transition"
        @update:model-value="!$event && emit('close')"
    >
        <v-card class="better-voucher-dialog-card pa-5 bg-white border elevation-6">
            <!-- Modal Header -->
            <div class="d-flex align-center justify-space-between mb-4">
                <div class="d-flex align-center ga-2 dark-blue-header font-weight-bold text-h6">
                    <div class="icon-circle d-flex align-center justify-center">
                        <InfoCircleIcon size="22" class="dark-blue-icon" />
                    </div>
                    <span>Có voucher tốt hơn</span>
                </div>
                <v-btn icon variant="text" size="small" class="text-slate-500" @click="emit('close')">
                    <XIcon size="20" />
                </v-btn>
            </div>

            <!-- Banner Notice -->
            <div class="notice-banner d-flex align-center ga-2 pa-3 mb-4">
                <CheckIcon size="18" class="dark-blue-icon flex-shrink-0" />
                <span class="text-subtitle-2 font-weight-medium">Có voucher tốt hơn cho đơn hàng của bạn!</span>
            </div>

            <!-- Voucher Comparison Grid -->
            <div class="comparison-grid d-flex justify-space-between ga-3 mb-4">
                <!-- Current Voucher -->
                <div class="voucher-box flex-1 pa-3 border bg-slate-50">
                    <div class="text-caption text-slate-500 mb-1">Voucher hiện tại</div>
                    <div
                        class="voucher-badge current-badge d-inline-block px-3 py-1 text-caption font-weight-bold text-slate-700 bg-slate-200 mb-1"
                    >
                        {{ currentCode }}
                    </div>
                    <div class="text-caption font-weight-medium text-slate-600">Giảm: {{ fmt(currentDiscount) }}</div>
                </div>

                <!-- Better Voucher -->
                <div class="voucher-box flex-1 pa-3 border better-voucher-box">
                    <div class="text-caption dark-blue-text font-weight-medium mb-1">Voucher mới</div>
                    <div class="voucher-badge new-badge d-inline-block px-3 py-1 text-caption font-weight-bold mb-1">
                        {{ newCode }}
                    </div>
                    <div class="text-caption font-weight-bold dark-blue-text">Giảm: {{ fmt(betterDiscount) }}</div>
                </div>
            </div>

            <!-- Extra Savings Banner -->
            <div class="savings-banner d-flex align-center justify-space-between pa-3 mb-4">
                <span class="font-weight-bold text-body-2 dark-blue-text">Bạn tiết kiệm thêm:</span>
                <span class="font-weight-black text-subtitle-1 dark-blue-highlight">+{{ fmt(extraSavings) }}</span>
            </div>

            <!-- Summary Totals -->
            <div class="summary-section py-3 border-t border-b mb-5">
                <div class="d-flex justify-space-between text-body-2 text-slate-600 mb-2">
                    <span>Tổng tiền hàng:</span>
                    <span class="font-weight-medium text-slate-800">{{ fmt(orderTotal) }}</span>
                </div>
                <div class="d-flex justify-space-between text-body-2 dark-blue-text mb-2">
                    <span>Giảm giá:</span>
                    <span class="font-weight-bold dark-blue-text">-{{ fmt(betterDiscount) }}</span>
                </div>
                <div class="d-flex justify-space-between text-subtitle-1 font-weight-bold text-slate-900 mt-2 pt-2 border-t">
                    <span>Tổng thanh toán:</span>
                    <span class="font-weight-black dark-blue-highlight text-h6">{{ fmt(newTotalPayment) }}</span>
                </div>
            </div>

            <!-- Action Buttons -->
            <div class="d-flex ga-3">
                <button type="button" class="btn-keep-old flex-1 font-weight-medium" @click="emit('keep-old')">Giữ voucher cũ</button>

                <button type="button" class="btn-apply-new flex-1 font-weight-bold" @click="emit('apply-new')">
                    Dùng voucher mới (tiết kiệm hơn)
                </button>
            </div>
        </v-card>
    </v-dialog>
</template>

<style scoped>
.better-voucher-dialog-card {
    border-radius: 12px !important;
    font-family: inherit;
}

.voucher-badge {
    letter-spacing: 0.5px;
    border-radius: 6px !important;
}

/* Tông màu Xanh Dương Đậm (Dark Navy Blue #1e257c) chuẩn màu chủ đạo AeroStride */
.dark-blue-header {
    color: #1e257c !important;
}

.icon-circle {
    width: 32px;
    height: 32px;
    border-radius: 50% !important;
    background-color: #dbeafe !important;
}

.dark-blue-icon {
    color: #1e257c !important;
}

.dark-blue-text {
    color: #1e257c !important;
}

.dark-blue-highlight {
    color: #1e257c !important;
}

.notice-banner {
    background-color: #eff6ff !important;
    border: 1px solid #bfdbfe !important;
    color: #1e257c !important;
    border-radius: 10px !important;
}

.voucher-box {
    border-radius: 10px !important;
}

.better-voucher-box {
    background-color: #eff6ff !important;
    border: 1.5px solid #93c5fd !important;
}

.new-badge {
    background-color: #dbeafe !important;
    color: #1e257c !important;
}

.savings-banner {
    background-color: #dbeafe !important;
    border: 1px solid #93c5fd !important;
    border-radius: 10px !important;
}

/* Buttons bo góc vừa 10px, chuẩn layout không bị vỡ */
.btn-keep-old {
    padding: 11px 16px;
    border-radius: 10px !important;
    border: 1.5px solid #cbd5e1;
    background-color: #ffffff;
    color: #334155;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s ease;
    white-space: nowrap;
}

.btn-keep-old:hover {
    background-color: #f1f5f9;
    border-color: #94a3b8;
}

.btn-apply-new {
    padding: 11px 16px;
    border-radius: 10px !important;
    border: none;
    background-color: #1e257c !important;
    color: #ffffff !important;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s ease;
    white-space: nowrap;
}

.btn-apply-new:hover {
    background-color: #141a5c !important;
    box-shadow: 0 4px 12px rgba(30, 37, 124, 0.25);
}
</style>
