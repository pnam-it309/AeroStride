<template>
  <v-card class="pos-card pa-4 rounded-lg border h-100 d-flex flex-column">
    <v-row no-gutters class="flex-grow-1">
        <!-- Left Column inside the card -->
        <v-col cols="12" md="6" class="pr-md-6 d-flex flex-column justify-space-between"
            style="gap: 15px;">
            <div class="d-flex justify-space-between align-center mb-3">
                <h3 class="font-weight-bold text-slate-800 m-0"
                    style="font-size: 14px !important">Tổng đơn hàng</h3>
                <div class="d-flex align-center gap-2">
                    <span class="text-slate-600" style="font-size: 13px !important">Giao hàng</span>
                    <v-switch :model-value="isGiaoHang" @update:model-value="val => $emit('update:isGiaoHang', val)" color="primary" hide-details
                        density="compact" inset class="custom-switch-giao-hang"
                        :class="{ 'is-active': isGiaoHang }" />
                </div>
            </div>

            <div class="flex-grow-1 d-flex flex-column" style="gap: 20px;">
                <!-- Voucher Selection -->
                <div class="d-flex flex-column" style="gap: 6px;">
                    <div class="d-flex align-center justify-space-between">
                        <span class="text-slate-600" style="font-size: 13px !important">Phiếu giảm giá</span>
                        <v-select :model-value="selectedVoucherId"
                            :items="vouchers" item-title="customTitle" item-value="id"
                            variant="outlined" density="compact" hide-details
                            @update:model-value="val => $emit('apply-voucher', val)"
                            class="compact-select custom-value-input"
                            style="width: 200px !important; max-width: 200px !important; min-width: 200px !important; flex: none !important;"
                            clearable placeholder="Nhập phiếu giảm giá"
                            persistent-placeholder no-data-text="Chưa có phiếu giảm giá" />
                    </div>
                    <!-- Suggestion Text -->
                    <div v-if="voucherSuggestionText"
                        class="text-caption text-right font-italic"
                        :class="[voucherSuggestionClass, { 'text-decoration-underline': canApplySuggestedVoucher }]"
                        style="font-size: 11px !important;"
                        :style="{ cursor: canApplySuggestedVoucher ? 'pointer' : 'default' }"
                        @click="() => { if (canApplySuggestedVoucher) $emit('apply-suggested-voucher') }">
                        {{ voucherSuggestionText }}
                    </div>
                    <div v-if="betterVoucherSuggestionText"
                        class="text-caption text-right font-italic text-deep-orange-darken-3"
                        style="font-size: 11px !important;">
                        {{ betterVoucherSuggestionText }}
                    </div>
                </div>

                <!-- Total Amount Raw -->
                <div class="d-flex align-center justify-space-between">
                    <span class="text-slate-600" style="font-size: 13px !important">Tổng tiền hàng</span>
                    <span class="font-weight-bold"
                        style="font-size: 13px !important; color: #0c3866;">{{
                            formatCurrency(totalRawAmount)
                        }}</span>
                </div>

                <!-- Discount amount applied -->
                <div class="d-flex align-center justify-space-between">
                    <span class="text-slate-600" style="font-size: 13px !important">Đợt giảm giá</span>
                    <span class="font-weight-bold"
                        style="font-size: 13px !important; color: #0f172a;">
                        <template v-if="productDiscountAmount > 0">
                            {{ appliedDiscountSummary }} (-{{ formatCurrency(productDiscountAmount) }})
                        </template>
                        <template v-else>Không có</template>
                    </span>
                </div>

            </div>
        </v-col>

        <!-- Right Column: Shipping Fee + Final Collected Amount -->
        <v-col cols="12" md="6"
            class="d-flex flex-column justify-start pl-md-6 border-s-dashed-md"
            style="gap: 15px;">
            <!-- Total Discount Amount -->
            <div class="d-flex align-center justify-space-between">
                <span class="text-slate-600" style="font-size: 13px !important">Tổng tiền giảm</span>
                <span class="font-weight-semibold"
                    style="font-size: 14px !important; color: #991b1b !important;">
                    {{ totalDiscountAmount > 0 ? '-' : '' }}{{ formatCurrency(totalDiscountAmount) }}
                </span>
            </div>

            <!-- Shipping Fee -->
            <div class="d-flex flex-column"
                style="min-height: 58px; justify-content: flex-start;">
                <div class="d-flex align-center justify-space-between">
                    <span class="text-slate-600 d-flex align-center" style="font-size: 13px !important">
                        <span>Phí vận chuyển</span>
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
                                style="width: 240px !important; max-width: 240px !important; min-width: 240px !important; flex: none !important;"
                                class="text-right-input custom-value-input"
                                :loading="shippingFeeLoading"
                                :disabled="isFreeShip || !isGiaoHang" />
                        </template>
                        <v-list class="pa-0 border rounded-lg elevation-2 bg-white"
                            style="min-width: 170px;">
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
                <!-- Shipping fee status: GHN success or fallback/error note for staff. -->
                <div v-if="shippingFeeSource === 'GHN'" class="text-caption text-success text-right">
                    Đã tính phí từ GHN
                </div>
                <div v-else-if="shippingFeeError" class="text-caption text-error text-right">
                    {{ shippingFeeError }}
                </div>
            </div>

            <div class="d-flex align-center justify-space-between"
                style="border-top: 1px dashed #cbd5e1; margin-top: 8px; padding-top: 12px;">
                <span class="text-slate-700 font-weight-bold" style="font-size: 14px !important">Tổng tiền cần thanh toán</span>
                <span class="font-weight-semibold" style="font-size: 16px !important; color: #0c3866;">
                    {{ formatCurrency(finalCollectAmount) }}
                </span>
            </div>
        </v-col>
    </v-row>
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
.custom-switch-giao-hang :deep(.v-selection-control) {
    min-height: 24px;
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
.border-s-dashed-md {
    border-left: 1px dashed #e2e8f0;
}
@media (max-width: 959px) {
    .border-s-dashed-md {
        border-left: none;
        border-top: 1px dashed #e2e8f0;
        padding-top: 15px;
        margin-top: 15px;
    }
}
.hover-bg-slate-50:hover {
    background-color: #f8fafc;
}
</style>
