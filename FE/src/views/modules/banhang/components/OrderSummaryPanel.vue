<template>
  <v-card class="pos-card pa-4">
    <!-- Header: Title & Delivery Switch -->
    <div class="d-flex justify-space-between align-center border-b pb-2 mb-3">
        <div class="d-flex align-center ga-2">
            <div class="rounded-circle d-flex align-center justify-center bg-orange-lighten-5 pa-2" style="width: 28px; height: 28px;">
                <v-icon color="deep-orange" size="16">mdi-wallet-outline</v-icon>
            </div>
            <h3 class="font-weight-semibold ma-0" style="font-size: 14px !important; color: #2b2a2a !important;">Thông tin thanh toán</h3>
        </div>
        <div class="d-flex align-center ga-2">
            <span class="text-slate-600 font-weight-medium" style="font-size: 12px !important">Giao hàng</span>
            <v-switch :model-value="isGiaoHang" @update:model-value="val => $emit('update:isGiaoHang', val)" color="primary" hide-details
                density="compact" inset class="custom-switch-giao-hang"
                :class="{ 'is-active': isGiaoHang }" />
        </div>
    </div>

    <!-- Voucher Selection & Ticket Section -->
    <div class="d-flex flex-column mb-3 ga-2">
        <div class="d-flex align-center">
            <span class="font-weight-semibold" style="font-size: 11px !important; letter-spacing: 0.5px; color: #2b2a2a !important;">MÃ ƯU ĐÃI / GIẢM GIÁ</span>
        </div>

        <!-- Beautiful Ticket representation of Selected Voucher -->
        <div v-if="activeVoucher" class="voucher-ticket d-flex flex-column rounded-lg overflow-hidden border mt-1">
            <!-- Ticket Info Header -->
            <div class="d-flex">
                <!-- Ticket Left Badge (Orange-red gradient) -->
                <div class="ticket-left d-flex flex-column align-center justify-center pa-3 text-white text-center font-weight-bold flex-shrink-0"
                    style="width: 80px; background: linear-gradient(135deg, #FF6B4A, #FA3E19); position: relative;">
                    <span style="font-size: 18px; color: #ffffff !important;">
                        {{ activeVoucher.loaiPhieu === 'PHAN_TRAM' || activeVoucher.loaiPhieu === 'PERCENT' ? `${activeVoucher.phanTramGiamGia}%` : formatShortAmount(activeVoucher.soTienGiam) }}
                    </span>
                    <div class="ticket-dot ticket-dot-top"></div>
                    <div class="ticket-dot ticket-dot-bottom"></div>
                </div>
                
                <!-- Ticket Right Info -->
                <div class="ticket-right flex-grow-1 pa-3 d-flex flex-column justify-center text-slate-800" style="position: relative; background-color: #ffffff !important;">
                    <div class="font-weight-bold text-slate-900 text-truncate" style="font-size: 13px;">
                        [{{ activeVoucher.ma || activeVoucher.maPhieu }}] {{ activeVoucher.tenPhieu || activeVoucher.ten }}
                    </div>
                    <div class="text-slate-500 text-caption mt-1">
                        Đơn tối thiểu: <span class="font-weight-semibold">{{ formatCurrency(activeVoucher.donHangToiThieu) }}</span>
                    </div>
                    <div class="font-weight-bold mt-1" style="font-size: 13px; color: #d32f2f !important;">
                        - {{ formatCurrency(totalDiscountAmount) }}
                    </div>
                </div>
            </div>

            <!-- Suggestion Banner (Green/Blue Alert) - Merged inside Ticket -->
            <div v-if="voucherSuggestionText" 
                class="d-flex align-center ga-2 border-t"
                style="background-color: #f0fdf4 !important; border-top-color: #dcfce7 !important; color: #1b7d3e !important; font-size: 12.5px !important; padding: 12px 14px !important; line-height: 1.4 !important; min-height: 44px !important;"
                :class="{ 'cursor-pointer': canApplySuggestedVoucher }"
                @click="() => { if (canApplySuggestedVoucher) $emit('apply-suggested-voucher') }">
                <v-icon size="16" style="color: #1b7d3e !important;">
                    {{ canApplySuggestedVoucher ? 'mdi-gift' : 'mdi-check-circle' }}
                </v-icon>
                <span class="flex-grow-1 font-weight-medium">
                    {{ voucherSuggestionText }}
                </span>
                <v-icon v-if="canApplySuggestedVoucher" size="14" style="color: #1b7d3e !important;">mdi-chevron-right</v-icon>
            </div>
        </div>

        <!-- Standalone Suggestion Banner if NO activeVoucher is selected -->
        <div v-else-if="voucherSuggestionText" 
            class="d-flex align-center ga-2 rounded-lg border mt-1"
            style="background-color: #f0fdf4 !important; border-color: #dcfce7 !important; color: #1b7d3e !important; font-size: 12.5px !important; padding: 12px 14px !important; line-height: 1.4 !important; min-height: 44px !important;"
            :class="{ 'cursor-pointer': canApplySuggestedVoucher }"
            @click="() => { if (canApplySuggestedVoucher) $emit('apply-suggested-voucher') }">
            <v-icon size="16" style="color: #1b7d3e !important;">
                {{ canApplySuggestedVoucher ? 'mdi-gift' : 'mdi-check-circle' }}
            </v-icon>
            <span class="flex-grow-1 font-weight-medium">
                {{ voucherSuggestionText }}
            </span>
            <v-icon v-if="canApplySuggestedVoucher" size="14" style="color: #1b7d3e !important;">mdi-chevron-right</v-icon>
        </div>

        <!-- Standalone Upsell Banner (Amber Alert) - Always separated from ticket -->
        <div v-if="betterVoucherSuggestionText" 
            class="d-flex align-start ga-3 rounded-lg border mt-1"
            style="background-color: #f8faf0 !important; border-color: #e2e8f0 !important; padding: 10px 14px !important;">
            <v-icon size="18" style="color: #d97706 !important; margin-top: 2px;" class="animate-pulse flex-shrink-0">mdi-sparkles</v-icon>
            <div class="d-flex flex-column" style="line-height: 1.5 !important;">
                <template v-if="parsedBetterVoucher">
                    <div style="font-size: 12.5px !important;">
                        <span class="text-slate-800 font-weight-medium">Mua thêm </span>
                        <span class="font-weight-bold" style="color: #d32f2f !important;">{{ parsedBetterVoucher.remainingAmount }}</span>
                    </div>
                    <div class="mt-1" style="font-size: 11.5px !important; color: #64748b !important;">
                        Để nhận mã ưu đãi hơn: <span class="font-weight-bold" style="color: #d32f2f !important;">{{ parsedBetterVoucher.voucherCode }} ({{ parsedBetterVoucher.discountAmount }})</span>
                    </div>
                </template>
                <template v-else>
                    <span class="font-weight-medium text-slate-700" style="font-size: 12.5px !important;">
                        {{ betterVoucherSuggestionText }}
                    </span>
                </template>
            </div>
        </div>
    </div>

    <!-- Summary Details Box -->
    <div class="summary-details-box pa-3 rounded-lg border d-flex flex-column" style="background-color: #ffffff !important; border-color: #e2e8f0 !important;">
        <!-- Total Raw Amount -->
        <div class="d-flex align-center justify-space-between">
            <span class="text-slate-600" style="font-size: 13px !important">Tổng tiền hàng:</span>
            <span class="font-weight-semibold text-slate-800" style="font-size: 13px !important">
                {{ formatCurrency(totalRawAmount) }}
            </span>
        </div>

        <!-- Product Discount -->
        <div v-if="productDiscountAmount > 0" class="d-flex align-center justify-space-between">
            <span class="text-slate-600" style="font-size: 13px !important">Đợt giảm giá:</span>
            <span class="font-weight-semibold" style="font-size: 13px !important; color: #d32f2f !important;">
                - {{ formatCurrency(productDiscountAmount) }}
            </span>
        </div>

        <!-- Voucher Discount -->
        <div v-if="totalDiscountAmount > 0" class="d-flex align-center justify-space-between">
            <span class="text-slate-600" style="font-size: 13px !important">Giảm giá:</span>
            <span class="font-weight-bold" style="font-size: 13px !important; color: #d32f2f !important;">
                - {{ formatCurrency(totalDiscountAmount) }}
            </span>
        </div>

        <!-- Shipping Fee -->
        <transition name="expand-shipping-fee">
            <div v-if="isGiaoHang" class="shipping-fee-wrapper">
                <div class="d-flex flex-column ga-1">
                    <div class="d-flex align-center justify-space-between">
                        <span class="text-slate-600 d-flex align-center" style="font-size: 13px !important">
                            <span>Phí vận chuyển:</span>
                            <svg width="45" height="15" viewBox="0 0 45 15" fill="none"
                                xmlns="http://www.w3.org/2000/svg"
                                style="display: inline-block; vertical-align: middle; margin-left: 6px;">
                                <path
                                    d="M1 2.5 L7 2.5 L4.5 6.5 L7 6.5 L3.5 10.5 L1 10.5 L3.5 6.5 L1 6.5 Z"
                                    fill="#0C2A46" />
                                <path
                                    d="M5.5 2.5 L11.5 2.5 L9 6.5 L11.5 6.5 L8 10.5 L5.5 10.5 L8 6.5 L5.5 6.5 Z"
                                    fill="#FA6400" />
                                <text x="13.5" y="11" fill="#FA6400"
                                    font-family="'Inter', sans-serif" font-weight="900"
                                    font-style="italic" font-size="10.5"
                                    letter-spacing="-0.5px">GHN</text>
                            </svg>
                        </span>
                        <v-menu offset="4">
                            <template v-slot:activator="{ props }">
                                <v-text-field :model-value="formatNumberWithDots(shippingFee)"
                                    @input="onShippingInput"
                                    v-bind="props" variant="outlined" density="compact"
                                    suffix="đ" hide-details
                                    style="width: 140px !important; max-width: 140px !important; min-width: 140px !important; flex: none !important;"
                                    class="text-right-input custom-value-input"
                                    :loading="shippingFeeLoading"
                                    :disabled="isFreeShip" />
                            </template>
                            <v-list class="pa-0 border rounded-lg elevation-2 bg-white"
                                style="min-width: 140px;">
                                <div>
                                    <div class="bg-slate-100 text-slate-700 px-3 py-1 font-weight-bold"
                                        style="font-size: 11px;">Nội thành:</div>
                                    <v-list-item @click="$emit('update:shippingFee', 30000)"
                                        class="px-3 py-2 cursor-pointer hover-bg-slate-50 text-caption text-slate-800 font-weight-medium">
                                        30.000 <span class="text-decoration-underline">đ</span>
                                    </v-list-item>
                                </div>
                                <div>
                                    <div class="bg-slate-100 text-slate-700 px-3 py-1 font-weight-bold"
                                        style="font-size: 11px;">Ngoại thành:</div>
                                    <v-list-item @click="$emit('update:shippingFee', 30000)"
                                        class="px-3 py-2 cursor-pointer hover-bg-slate-50 text-caption text-slate-800 font-weight-medium">
                                        30.000 <span class="text-decoration-underline">đ</span>
                                    </v-list-item>
                                </div>
                                <div>
                                    <div class="bg-slate-100 text-slate-700 px-3 py-1 font-weight-bold"
                                        style="font-size: 11px;">Ngoại tỉnh:</div>
                                    <v-list-item @click="$emit('update:shippingFee', 30000)"
                                        class="px-3 py-2 cursor-pointer hover-bg-slate-50 text-caption text-slate-800 font-weight-medium">
                                        30.000 <span class="text-decoration-underline">đ</span>
                                    </v-list-item>
                                </div>
                            </v-list>
                        </v-menu>
                    </div>
                    <!-- Shipping fee status -->
                    <div v-if="shippingFeeSource === 'GHN'" class="text-caption text-success text-right" style="font-size: 11px !important;">
                        Đã tính phí từ GHN
                    </div>
                    <div v-else-if="shippingFeeError" class="text-caption text-error text-right" style="font-size: 11px !important;">
                        {{ shippingFeeError }}
                    </div>
                </div>
            </div>
        </transition>

        <!-- Solid divider line -->
        <div style="border-top: 1px dashed #d1d2ca !important; margin: 2px 0 !important;"></div>

        <!-- Total Payable -->
        <div class="d-flex align-center justify-space-between py-1">
            <span class="text-slate-700 font-weight-semibold" style="font-size: 15px !important; letter-spacing: 0.5px;">Số tiền cần thanh toán:</span>
            <span class="font-weight-bold animate-fade-in" style="font-size: 15px !important; color: #0c3866 !important;">
                {{ formatCurrency(finalCollectAmount) }}
            </span>
        </div>
    </div>
  </v-card>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
    isGiaoHang: { type: Boolean, default: false },
    vouchers: { type: Array, default: () => [] },
    selectedVoucherId: { type: String, default: null },
    voucherSuggestionText: { type: String, default: '' },
    voucherSuggestionClass: { type: String, default: '' },
    canApplySuggestedVoucher: { type: Boolean, default: false },
    betterVoucherSuggestionText: { type: String, default: '' },
    
    totalRawAmount: { type: Number, default: 0 },
    productDiscountAmount: { type: Number, default: 0 },
    appliedDiscountSummary: { type: String, default: '' },
    
    totalDiscountAmount: { type: Number, default: 0 },
    finalCollectAmount: { type: Number, default: 0 },
    
    shippingFee: { type: Number, default: 0 },
    shippingFeeLoading: { type: Boolean, default: false },
    shippingFeeSource: { type: String, default: '' },
    shippingFeeError: { type: String, default: '' },
    isFreeShip: { type: Boolean, default: false }
});

const emit = defineEmits([
    'update:isGiaoHang',
    'apply-voucher',
    'apply-suggested-voucher',
    'update:shippingFee'
]);

const activeVoucher = computed(() => {
    if (!props.selectedVoucherId || !props.vouchers?.length) return null;
    return props.vouchers.find(v => String(v.id) === String(props.selectedVoucherId)) || null;
});

const parsedBetterVoucher = computed(() => {
    if (!props.betterVoucherSuggestionText) return null;
    const regex = /Mua thêm (.*?) để nhận phiếu tốt hơn:\s*(.*?)\s*\((.*?)\)/i;
    const match = props.betterVoucherSuggestionText.match(regex);
    if (match) {
        return {
            remainingAmount: match[1].trim(),
            voucherCode: match[2].trim(),
            discountAmount: match[3].trim(),
        };
    }
    const fallbackRegex = /Mua thêm (.*?)(?:\s+|$)/i;
    const fallbackMatch = props.betterVoucherSuggestionText.match(fallbackRegex);
    if (fallbackMatch) {
        return {
            remainingAmount: fallbackMatch[1].trim(),
            voucherCode: '',
            discountAmount: '',
        };
    }
    return null;
});

const formatShortAmount = (num) => {
    if (!num) return '0';
    if (num >= 1000000) return (num / 1000000).toFixed(0) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(0) + 'K';
    return num.toString();
};

const formatNumberWithDots = (num) => {
    if (num === null || num === undefined) return '';
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ".");
};

const parseNumberFromDots = (str) => {
    if (!str) return 0;
    const parsed = parseInt(str.replace(/\./g, ''), 10);
    return isNaN(parsed) ? 0 : parsed;
};

const formatCurrency = (value) => {
    if (value == null) return '';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
};

const onShippingInput = (e) => {
    const val = parseNumberFromDots(e.target.value);
    emit('update:shippingFee', val);
};
</script>

<style scoped>
.pos-card {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05) !important;
}
.custom-switch-giao-hang :deep(.v-switch__track) {
    height: 14px !important;
    width: 34px !important;
    min-width: 34px !important;
    border-radius: 7px !important;
    background-color: #cbd5e1 !important;
    opacity: 0.6 !important;
    transition: background-color 0.3s ease, opacity 0.3s ease;
}
.custom-switch-giao-hang.is-active :deep(.v-switch__track) {
    background-color: #0c3866 !important;
    opacity: 0.5 !important;
}
.custom-switch-giao-hang :deep(.v-switch__thumb) {
    height: 20px !important;
    width: 20px !important;
    background-color: #ffffff !important;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2) !important;
    transition: background-color 0.3s ease, transform 0.3s ease;
}
.custom-switch-giao-hang.is-active :deep(.v-switch__thumb) {
    background-color: #0c3866 !important;
}
.custom-switch-giao-hang :deep(.v-selection-control) {
    min-height: 28px !important;
}
.compact-select :deep(.v-field__input) {
    font-size: 13px !important;
    min-height: 28px !important;
    padding-top: 0 !important;
    padding-bottom: 0 !important;
}
.custom-value-input :deep(input) {
    font-weight: bold;
    color: #0f172a;
}
.text-right-input :deep(input) {
    text-align: right;
    font-weight: bold;
    color: #0f172a;
}
.voucher-ticket {
    border-color: #ffe8e3 !important;
}
.ticket-left {
    position: relative;
    border-right: 1px dashed rgba(255, 255, 255, 0.4);
}
.ticket-dot {
    position: absolute;
    width: 10px;
    height: 10px;
    background-color: #fff;
    border-radius: 50%;
    right: -5px;
}
.ticket-dot-top {
    top: -5px;
}
.ticket-dot-bottom {
    bottom: -5px;
}
.summary-details-box {
    border-color: #e2e8f0 !important;
}
.summary-details-box > div {
    margin-bottom: 16px;
}
.summary-details-box > div:last-child {
    margin-bottom: 0;
}
.shipping-fee-wrapper {
    display: grid;
    grid-template-rows: 1fr;
    transition: grid-template-rows 0.3s cubic-bezier(0.25, 0.8, 0.25, 1),
                opacity 0.25s ease-out,
                margin-bottom 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
    opacity: 1;
    margin-bottom: 16px !important;
    overflow: hidden;
}
.shipping-fee-wrapper > div {
    min-height: 0;
}
.expand-shipping-fee-enter-active, .expand-shipping-fee-leave-active {
    transition: grid-template-rows 0.3s cubic-bezier(0.25, 0.8, 0.25, 1),
                opacity 0.25s ease-out,
                margin-bottom 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.expand-shipping-fee-enter-from, .expand-shipping-fee-leave-to {
    grid-template-rows: 0fr !important;
    opacity: 0 !important;
    margin-bottom: 0 !important;
}
.animate-pulse {
    animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
@keyframes pulse {
    0%, 100% {
        opacity: 1;
    }
    50% {
        opacity: .5;
    }
}
.hover-bg-slate-50:hover {
    background-color: #f8fafc;
}
</style>
