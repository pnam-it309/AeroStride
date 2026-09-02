<template>
    <v-card class="pos-card pa-4">
        <!-- Header: Title & Delivery Switch -->
        <div class="d-flex justify-space-between align-center border-b pb-2 mb-3">
            <div class="d-flex align-center ga-2">
                <div class="rounded-circle d-flex align-center justify-center bg-orange-lighten-5 pa-2" style="width: 28px; height: 28px">
                    <v-icon color="deep-orange" size="16">mdi-wallet-outline</v-icon>
                </div>
                <h3 class="font-weight-semibold ma-0" style="font-size: 14px !important; color: #2b2a2a !important">
                    Thông tin thanh toán
                </h3>
            </div>
            <div
                class="d-flex align-center ga-2 cursor-pointer select-none"
                style="user-select: none"
                @click.stop.prevent="$emit('update:isGiaoHang', !isGiaoHang)"
            >
                <span class="text-slate-600 font-weight-medium" style="font-size: 12px !important">Giao hàng</span>
                <v-switch
                    :model-value="Boolean(isGiaoHang)"
                    color="primary"
                    hide-details
                    density="compact"
                    inset
                    readonly
                    class="custom-switch-giao-hang"
                    :class="{ 'is-active': isGiaoHang }"
                    style="pointer-events: none"
                />
            </div>
        </div>

        <!-- Voucher Selection & Ticket Section (Chỉ hiển thị khi đã chọn sản phẩm) -->
        <div v-if="hasProducts" class="d-flex flex-column mb-3 ga-2">
            <div class="d-flex align-center">
                <span class="font-weight-semibold" style="font-size: 11px !important; letter-spacing: 0.5px; color: #2b2a2a !important"
                    >MÃ ƯU ĐÃI / GIẢM GIÁ</span
                >
            </div>

            <!-- Beautiful Ticket representation of Selected Voucher -->
            <div v-if="activeVoucher" class="voucher-ticket d-flex flex-column rounded-lg overflow-hidden border mt-1">
                <!-- Ticket Info Header -->
                <div class="d-flex">
                    <!-- Ticket Left Badge (Orange-red gradient) -->
                    <div
                        class="ticket-left d-flex flex-column align-center justify-center pa-3 text-white text-center font-weight-bold flex-shrink-0"
                        style="width: 85px; min-height: 75px; background: linear-gradient(135deg, #ff6b4a, #fa3e19); position: relative"
                    >
                        <span style="font-size: 18px; color: #ffffff !important">
                            {{
                                activeVoucher.loaiPhieu === 'PHAN_TRAM' ||
                                activeVoucher.loaiPhieu === 'PERCENT' ||
                                activeVoucher.loaiPhieu === 1
                                    ? `${activeVoucher.phanTramGiamGia || activeVoucher.giamGia}%`
                                    : formatShortAmount(activeVoucher.soTienGiam || activeVoucher.giamGia || totalDiscountAmount)
                            }}
                        </span>
                        <div class="ticket-dot ticket-dot-top"></div>
                        <div class="ticket-dot ticket-dot-bottom"></div>
                    </div>

                    <!-- Ticket Right Info -->
                    <div
                        class="ticket-right flex-grow-1 pa-3 d-flex flex-column justify-center text-slate-800"
                        style="position: relative; background-color: #ffffff !important"
                    >
                        <div class="font-weight-bold text-slate-900 text-truncate" style="font-size: 13px">
                            {{ formattedVoucherTitle }}
                        </div>
                        <div class="text-slate-500 text-caption mt-1">
                            Đơn tối thiểu:
                            <span class="font-weight-semibold">{{ formatCurrency(activeVoucher.donHangToiThieu || 0) }}</span>
                        </div>
                        <div class="font-weight-bold mt-1" style="font-size: 13px; color: #d32f2f !important">
                            - {{ formatCurrency(voucherDiscountAmount || activeVoucher.soTienGiam || 0) }}
                        </div>
                    </div>
                </div>

                <!-- Cảnh báo khi đơn hàng chưa đạt điều kiện tối thiểu của voucher đang áp dụng -->
                <div
                    v-if="activeVoucher && Number(activeVoucher.donHangToiThieu || 0) > Number(voucherBaseAmount || totalRawAmount || 0)"
                    class="d-flex align-center justify-space-between ga-2 rounded-lg border mt-1 px-3 py-2"
                    style="background-color: #fffbeb !important; border-color: #fde68a !important; color: #b45309 !important; font-size: 12px !important;"
                >
                    <div class="d-flex align-center ga-1.5 overflow-hidden">
                        <v-icon size="16" color="#b45309" class="flex-shrink-0">mdi-alert-outline</v-icon>
                        <span class="text-truncate font-weight-medium">
                            Chưa đủ đơn tối thiểu (thiếu {{ formatCurrency(Number(activeVoucher.donHangToiThieu || 0) - Number(voucherBaseAmount || totalRawAmount || 0)) }})
                        </span>
                    </div>
                    <button
                        type="button"
                        class="font-weight-bold text-caption flex-shrink-0 text-decoration-underline"
                        style="color: #dc2626; cursor: pointer; background: none; border: none;"
                        @click="emit('open-voucher-ineligible-modal')"
                    >
                        Hủy / Xem
                    </button>
                </div>
            </div>

            <!-- Hai trạng thái loại trừ: có voucher thì chỉ hiện ticket, không có mới hiện thông báo. -->
            <div
                v-else
                class="d-flex align-center ga-2 rounded-lg border mt-1"
                style="
                    background-color: #f8fafc !important;
                    border-color: #e2e8f0 !important;
                    color: #64748b !important;
                    font-size: 12.5px !important;
                    padding: 12px 14px !important;
                    line-height: 1.4 !important;
                    min-height: 44px !important;
                "
            >
                <v-icon size="16" color="blue-grey">mdi-ticket-percent-outline</v-icon>
                <span class="flex-grow-1 font-weight-medium">
                    {{ noVoucherMessage }}
                </span>
            </div>

            <!-- Computed trực tiếp từ vouchers + tổng tiền, không gọi thêm API khi giỏ thay đổi. -->
            <div
                v-if="upsellSuggestion || betterVoucherSuggestionText"
                class="d-flex align-start ga-3 rounded-lg border mt-1"
                style="background-color: #f8faf0 !important; border-color: #e2e8f0 !important; padding: 10px 14px !important"
            >
                <v-icon size="18" style="color: #d97706 !important; margin-top: 2px" class="animate-pulse flex-shrink-0"
                    >mdi-sparkles</v-icon
                >
                <div class="d-flex flex-column" style="line-height: 1.5 !important">
                    <div v-if="upsellSuggestion" class="font-weight-medium text-slate-700" style="font-size: 12.5px !important">
                        Mua thêm
                        <span class="font-weight-bold" style="color: #d97706 !important">
                            {{ formatCurrency(upsellSuggestion.remainingAmount) }}
                        </span>
                        để được áp dụng mã
                        <span class="font-weight-bold" style="color: #0c5c9e !important">
                            {{ upsellVoucherLabel }}
                        </span>
                        (giảm {{ formatCurrency(upsellSuggestion.discountAmount) }})
                        <span
                            v-if="activeVoucher && upsellSuggestion.extraDiscountAmount > 0"
                            class="font-weight-semibold"
                            style="color: #15803d !important"
                        >
                            — lợi hơn {{ formatCurrency(upsellSuggestion.extraDiscountAmount) }}
                        </span>
                    </div>
                    <div v-else class="font-weight-medium text-slate-700" style="font-size: 12.5px !important">
                        {{ betterVoucherSuggestionText }}
                    </div>
                </div>
            </div>
        </div>

        <!-- Summary Details Box -->
        <div
            class="summary-details-box pa-3 rounded-lg border d-flex flex-column"
            style="background-color: #ffffff !important; border-color: #e2e8f0 !important"
        >
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
                <span class="font-weight-semibold" style="font-size: 13px !important; color: #d32f2f !important">
                    - {{ formatCurrency(productDiscountAmount) }}
                </span>
            </div>

            <!-- Voucher Discount -->
            <div v-if="totalDiscountAmount > 0" class="d-flex align-center justify-space-between">
                <span class="text-slate-600" style="font-size: 13px !important">Giảm giá:</span>
                <span class="font-weight-bold" style="font-size: 13px !important; color: #d32f2f !important">
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
                                <svg
                                    width="45"
                                    height="15"
                                    viewBox="0 0 45 15"
                                    fill="none"
                                    xmlns="http://www.w3.org/2000/svg"
                                    style="display: inline-block; vertical-align: middle; margin-left: 6px"
                                >
                                    <path d="M1 2.5 L7 2.5 L4.5 6.5 L7 6.5 L3.5 10.5 L1 10.5 L3.5 6.5 L1 6.5 Z" fill="#0C2A46" />
                                    <path d="M5.5 2.5 L11.5 2.5 L9 6.5 L11.5 6.5 L8 10.5 L5.5 10.5 L8 6.5 L5.5 6.5 Z" fill="#FA6400" />
                                    <text
                                        x="13.5"
                                        y="11"
                                        fill="#FA6400"
                                        font-family="'Inter', sans-serif"
                                        font-weight="900"
                                        font-style="italic"
                                        font-size="10.5"
                                        letter-spacing="-0.5px"
                                    >
                                        GHN
                                    </text>
                                </svg>
                            </span>
                            <v-text-field
                                :model-value="formatNumberWithDots(shippingFee)"
                                @input="onShippingInput"
                                variant="outlined"
                                density="compact"
                                suffix="đ"
                                hide-details
                                style="
                                    width: 140px !important;
                                    max-width: 140px !important;
                                    min-width: 140px !important;
                                    flex: none !important;
                                "
                                class="text-right-input custom-value-input"
                                :loading="shippingFeeLoading"
                                :disabled="isFreeShip"
                            />
                        </div>
                        <!-- Shipping fee status -->
                        <div
                            v-if="shippingFeeSource === 'GHN'"
                            class="text-caption text-success text-right"
                            style="font-size: 11px !important"
                        >
                            Đã tính phí từ GHN
                        </div>
                        <div
                            v-else-if="shippingFeeSource === 'FALLBACK'"
                            class="text-caption text-amber-darken-3 text-right font-weight-medium"
                            style="font-size: 11px !important"
                        >
                            Phí tiêu chuẩn toàn quốc (30.000 ₫)
                        </div>
                        <div v-else-if="shippingFeeError" class="text-caption text-error text-right" style="font-size: 11px !important">
                            {{ shippingFeeError }}
                        </div>
                    </div>
                </div>
            </transition>

            <!-- Solid divider line -->
            <div style="border-top: 1px dashed #d1d2ca !important; margin: 2px 0 !important"></div>

            <!-- Total Payable -->
            <div class="d-flex align-center justify-space-between py-1">
                <span class="text-slate-700 font-weight-semibold" style="font-size: 15px !important; letter-spacing: 0.5px"
                    >Số tiền cần thanh toán:</span
                >
                <span class="font-weight-bold animate-fade-in" style="font-size: 15px !important; color: #0c3866 !important">
                    {{ formatCurrency(finalCollectAmount) }}
                </span>
            </div>
        </div>
    </v-card>
</template>

<script setup>
import { computed } from 'vue';
import { findBestVoucherUpsell } from '../voucherUpsell.js';
import { formatCurrency, formatNumberWithDots, parseNumberFromDots, formatShortAmount } from '@/utils/formatters';

const props = defineProps({
    isGiaoHang: { type: Boolean, default: false },
    vouchers: { type: Array, default: () => [] },
    selectedVoucherId: { type: String, default: null },
    appliedVoucher: { type: Object, default: null },
    voucherSuggestionText: { type: String, default: '' },
    betterVoucherSuggestionText: { type: String, default: '' },

    totalRawAmount: { type: Number, default: 0 },
    voucherBaseAmount: { type: Number, default: null },
    productDiscountAmount: { type: Number, default: 0 },
    voucherDiscountAmount: { type: Number, default: 0 },

    totalDiscountAmount: { type: Number, default: 0 },
    finalCollectAmount: { type: Number, default: 0 },

    shippingFee: { type: Number, default: 0 },
    shippingFeeLoading: { type: Boolean, default: false },
    shippingFeeSource: { type: String, default: '' },
    shippingFeeError: { type: String, default: '' },
    isFreeShip: { type: Boolean, default: false }
});

const emit = defineEmits(['update:isGiaoHang', 'apply-voucher', 'update:shippingFee', 'open-voucher-ineligible-modal']);

const isUuidString = (str) =>
    typeof str === 'string' && /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(str);

const activeVoucher = computed(() => {
    const baseVoucher = props.appliedVoucher || null;
    const voucherIdOrCode = props.selectedVoucherId;

    if (baseVoucher) {
        const matched = props.vouchers?.find((v) => String(v.id) === String(baseVoucher.id) || String(v.ma) === String(baseVoucher.ma));
        if (matched) return matched;
        return baseVoucher;
    }

    if (props.vouchers?.length && voucherIdOrCode) {
        const found = props.vouchers.find(
            (v) =>
                String(v.id) === String(voucherIdOrCode) ||
                String(v.ma) === String(voucherIdOrCode) ||
                String(v.maPhieu) === String(voucherIdOrCode)
        );
        if (found) return found;
    }

    if (voucherIdOrCode) {
        return {
            id: voucherIdOrCode,
            ma: isUuidString(voucherIdOrCode) ? '' : voucherIdOrCode,
            ten: 'Phiếu giảm giá ưu đãi',
            soTienGiam: props.voucherDiscountAmount || props.totalDiscountAmount,
            donHangToiThieu: 0,
            loaiPhieu: 'SO_TIEN'
        };
    }

    return null;
});

const formattedVoucherTitle = computed(() => {
    if (!activeVoucher.value) return '';

    const v = activeVoucher.value;
    let code = v.ma || v.maPhieu || v.code || '';
    if (isUuidString(code)) {
        code = '';
    }

    const name = v.tenPhieu || v.ten || 'Phiếu giảm giá';

    if (code) {
        return `[${code}] ${name}`;
    }
    return name;
});

const noVoucherMessage = computed(() => {
    const message = String(props.voucherSuggestionText || '').trim();
    return message.toLowerCase().includes('chưa có') ? message : 'Chưa có phiếu giảm giá phù hợp với đơn hàng hiện tại.';
});

const hasProducts = computed(() => (props.voucherBaseAmount ?? props.totalRawAmount) > 0);

const upsellSuggestion = computed(() => {
    if (!hasProducts.value) return null;
    return findBestVoucherUpsell(props.vouchers, props.voucherBaseAmount ?? props.totalRawAmount, Date.now(), activeVoucher.value);
});

const upsellVoucherLabel = computed(() => {
    const voucher = upsellSuggestion.value?.voucher;
    return voucher?.ma || voucher?.maPhieu || voucher?.tenPhieu || voucher?.ten || 'ưu đãi tốt hơn';
});

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
    transition:
        background-color 0.3s ease,
        opacity 0.3s ease;
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
    transition:
        background-color 0.3s ease,
        transform 0.3s ease;
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
    transition:
        grid-template-rows 0.3s cubic-bezier(0.25, 0.8, 0.25, 1),
        opacity 0.25s ease-out,
        margin-bottom 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
    opacity: 1;
    margin-bottom: 16px !important;
    overflow: hidden;
}
.shipping-fee-wrapper > div {
    min-height: 0;
}
.expand-shipping-fee-enter-active,
.expand-shipping-fee-leave-active {
    transition:
        grid-template-rows 0.3s cubic-bezier(0.25, 0.8, 0.25, 1),
        opacity 0.25s ease-out,
        margin-bottom 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.expand-shipping-fee-enter-from,
.expand-shipping-fee-leave-to {
    grid-template-rows: 0fr !important;
    opacity: 0 !important;
    margin-bottom: 0 !important;
}
.animate-pulse {
    animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
@keyframes pulse {
    0%,
    100% {
        opacity: 1;
    }
    50% {
        opacity: 0.5;
    }
}
.hover-bg-slate-50:hover {
    background-color: #f8fafc;
}
</style>
