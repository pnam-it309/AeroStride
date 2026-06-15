<script setup>
/**
 * Module: Giao diện Thanh toán (Checkout Panel)
 * Chức năng: Quản lý giao diện và xử lý logic thanh toán tại quầy (chọn voucher, tính tổng tiền,
 * chọn phương thức thanh toán tiền mặt/VNPAY, tiền thừa).
 */
import { computed } from 'vue';

const props = defineProps({
    order: Object,
    vouchers: Array,
    checkoutData: Object,
    loading: Boolean,
    showLeftOnly: {
        type: Boolean,
        default: false
    },
    showRightOnly: {
        type: Boolean,
        default: false
    }
});
const emit = defineEmits(['apply-voucher', 'checkout']);

const discount = computed(() => (props.order?.tongTien || 0) - (props.order?.tongTienSauGiam || 0));
const totalPayable = computed(() => Number(props.order?.tongTienSauGiam || 0) + Number(props.order?.phiVanChuyen || 0));
const changeAmount = computed(() => Math.max(0, (props.checkoutData.receivedAmount || 0) - totalPayable.value));
const itemCount = computed(() => (props.order?.listsHoaDonChiTiet || []).reduce((sum, item) => sum + (Number(item.soLuong) || 0), 0));

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

const canCheckout = computed(() => {
    if (props.loading) return false;
    if (!props.order) return false;
    if (props.checkoutData.paymentMethod === 'CASH') {
        return (props.checkoutData.receivedAmount || 0) >= totalPayable.value;
    }
    return true;
});

const handleCheckout = () => {
    emit('checkout');
};
</script>

<template>
    <div class="checkout-panel">
        <v-row>
            <!-- Column 1: Order Pricing Summary & Notes -->
            <v-col v-if="!showRightOnly" cols="12" :md="showLeftOnly ? 6 : 4" class="d-flex flex-column">
                <div class="panel-section pricing-summary pa-4 h-100 d-flex flex-column justify-space-between">
                    <div>
                        <div class="d-flex justify-space-between align-center mb-3">
                            <span class="font-weight-bold text-slate-800" style="font-size: 14px;">Tổng đơn</span>
                            <v-chip color="primary" variant="tonal" size="small">{{ order?.maHoaDon }}</v-chip>
                        </div>
                        
                        <!-- Temporary amount -->
                        <div class="d-flex justify-space-between mb-2 text-slate-600" style="font-size: 13px;">
                            <span>Tạm tính ({{ itemCount }} sp):</span>
                            <span class="font-weight-medium">{{ formatCurrency(order?.tongTien) }}</span>
                        </div>

                        <!-- Voucher select -->
                        <div class="voucher-section my-3">
                            <v-select
                                :model-value="order?.idPhieuGiamGia"
                                :items="vouchers"
                                item-title="ten"
                                item-value="id"
                                label="Mã giảm giá"
                                variant="outlined"
                                density="compact"
                                hide-details
                                prepend-inner-icon="mdi-ticket-percent"
                                @update:model-value="(val) => emit('apply-voucher', val)"
                            ></v-select>
                        </div>

                        <!-- Discount -->
                        <div class="d-flex justify-space-between mb-2 align-center">
                            <span style="font-size: 13px; color: #ef4444;">Giảm giá:</span>
                            <span class="font-weight-bold" style="font-size: 13px; color: #ef4444;">- {{ formatCurrency(discount) }}</span>
                        </div>
                    </div>

                    <div>
                        <v-divider class="my-2 border-dashed"></v-divider>
                        <!-- Order Note -->
                        <v-textarea v-model="checkoutData.note" label="Ghi chú hóa đơn" variant="outlined" rows="1" class="mt-2" hide-details></v-textarea>
                    </div>
                </div>
            </v-col>

            <!-- Column 2: Payment Method Selection -->
            <v-col v-if="!showRightOnly" cols="12" :md="showLeftOnly ? 6 : 4" class="d-flex flex-column">
                <div class="panel-section payment-selection pa-4 h-100 d-flex flex-column justify-space-between">
                    <div>
                        <p class="font-weight-bold mb-3 d-flex align-center text-slate-800" style="font-size: 13px;">
                            <v-icon size="small" class="mr-2" color="primary">mdi-cash-register</v-icon> Hình thức thanh toán
                        </p>
                        <v-btn-toggle v-model="checkoutData.paymentMethod" mandatory color="#2E4E8E" class="d-flex w-100 gap-2 payment-toggle mb-3">
                            <v-btn value="CASH" variant="outlined" class="flex-grow-1 h-14 rounded-lg border-2">
                                <div class="d-flex flex-column align-center">
                                    <v-icon size="20" class="mb-1">mdi-cash</v-icon>
                                    <span class="text-caption font-weight-bold">Tiền mặt</span>
                                </div>
                            </v-btn>
                            <v-btn value="VNPAY" variant="outlined" class="flex-grow-1 h-14 rounded-lg border-2">
                                <div class="d-flex flex-column align-center">
                                    <v-icon size="20" class="mb-1">mdi-credit-card-outline</v-icon>
                                    <span class="text-caption font-weight-bold">VNPay</span>
                                </div>
                            </v-btn>
                        </v-btn-toggle>
                    </div>

                    <!-- Cash Input / VNPay Instructions -->
                    <div v-if="checkoutData.paymentMethod === 'CASH'" class="cash-input mt-2">
                        <v-text-field
                            v-model.number="checkoutData.receivedAmount"
                            label="Tiền khách đưa"
                            variant="outlined"
                            type="number"
                            prefix="VND"
                            class="rounded-lg"
                            density="compact"
                            hide-details
                        ></v-text-field>
                    </div>
                    
                    <div v-else class="vnpay-status d-flex align-center justify-center pa-4 bg-blue-lighten-5 rounded-lg border border-blue-lighten-4 mt-2">
                        <v-icon color="info" class="mr-2">mdi-information-outline</v-icon>
                        <span class="text-caption text-blue-darken-3 font-weight-medium">Thanh toán qua cổng VNPay (F10)</span>
                    </div>
                </div>
            </v-col>

            <!-- Column 3: Final Checkout & Pay Button -->
            <v-col v-if="!showLeftOnly" cols="12" :md="showRightOnly ? 12 : 4" class="d-flex flex-column">
                <div class="panel-section checkout-summary-box pa-4 h-100 d-flex flex-column justify-space-between bg-slate-50">
                    <div>
                        <div class="d-flex justify-space-between align-center pt-2 mb-3">
                            <span style="font-size: 13px; color: #64748b;">Tổng thanh toán</span>
                            <span class="font-weight-bold text-primary-darken-2" style="font-size: 20px; color: #2E4E8E;">{{ formatCurrency(totalPayable) }}</span>
                        </div>

                        <div v-if="checkoutData.paymentMethod === 'CASH'" class="d-flex justify-space-between align-center my-3 bg-white pa-2 rounded-lg border">
                            <span class="text-slate-600" style="font-size: 13px;">Tiền thừa trả khách:</span>
                            <span class="font-weight-bold" :class="changeAmount >= 0 ? 'text-success' : 'text-error'" style="font-size: 14px;">
                                {{ formatCurrency(Math.max(0, changeAmount)) }}
                            </span>
                        </div>
                    </div>

                    <v-btn
                        block
                        size="x-large"
                        :color="canCheckout ? 'primary' : 'grey-lighten-1'"
                        class="font-weight-bold rounded-lg btn-checkout shadow-md text-white mt-4"
                        style="font-size: 14px; letter-spacing: 0.5px; height: 52px !important;"
                        :loading="loading"
                        :disabled="!canCheckout"
                        @click="handleCheckout"
                        elevation="0"
                    >
                        THANH TOÁN (F10)
                    </v-btn>
                </div>
            </v-col>
        </v-row>
    </div>
</template>

<style scoped>
.h-14 {
    height: 56px !important;
}
.payment-toggle {
    background: transparent !important;
}
.btn-checkout {
    background: #4285F4 !important;
    text-transform: none;
    letter-spacing: 0;
}
.pricing-summary {
    border-color: #cbd5e1 !important;
}
.panel-section {
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    background: #ffffff;
}
.total-box {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 12px;
    border-radius: 8px;
    background: #eef4ff;
}

.checkout-panel :deep(.v-field) {
    border-radius: 20px !important;
    background-color: white !important;
    box-shadow: none !important;
}

.checkout-panel :deep(.v-textarea .v-field) {
    border-radius: 16px !important;
}

.checkout-panel :deep(.v-field__outline) {
    color: #cbd5e1 !important;
    --v-field-border-width: 1px !important;
    --v-field-border-opacity: 1 !important;
    transition: color 0.2s ease, opacity 0.2s ease !important;
}

.checkout-panel :deep(.v-field:hover .v-field__outline) {
    color: #1e257c !important;
    --v-field-border-opacity: 0.6 !important;
}

.checkout-panel :deep(.v-field--focused .v-field__outline) {
    color: #1e257c !important;
    --v-field-border-width: 1px !important;
    --v-field-border-opacity: 1 !important;
}

.checkout-panel :deep(.v-field__input),
.checkout-panel :deep(.v-field__input input),
.checkout-panel :deep(.v-select__selection-text),
.checkout-panel :deep(.v-select__selection),
.checkout-panel :deep(.v-textarea textarea) {
    font-size: 13px !important;
    color: #334155 !important; /* Slate 700 */
    font-weight: 400 !important;
}

.checkout-panel :deep(input::placeholder),
.checkout-panel :deep(.v-field__input::placeholder),
.checkout-panel :deep(.v-label) {
    font-size: 13px !important;
    color: #94a3b8 !important;
    opacity: 0.8 !important;
    font-weight: 400 !important;
}
</style>
