<template>
  <div>
    <!-- Payment Card -->
    <v-card class="pos-card pa-4">
        <div class="d-flex justify-space-between align-center mb-3">
            <h3 class="font-weight-semibold m-0" style="font-size: 14px !important; color: #2b2a2a !important;">Thanh toán</h3>
        </div>

        <div class="d-flex align-center justify-space-between mb-4">
            <span class="text-slate-600" style="font-size: 13px !important">Hình thức thanh toán</span>
            <div class="d-flex gap-2">
                <button type="button" @click="emitMethodChange('CASH')"
                    :class="['px-4 d-flex align-center justify-center transition-all payment-btn',
                        paymentMethod === 'CASH' ? 'cash-active-btn' : 'payment-inactive-btn']">
                    <v-icon class="mr-1" size="16">mdi-cash</v-icon>
                    Tiền mặt
                </button>
                <button type="button" @click="emitMethodChange('VNPAY')"
                    :class="['px-4 d-flex align-center justify-center transition-all payment-btn',
                        paymentMethod === 'VNPAY' ? 'vnpay-active-btn' : 'payment-inactive-btn']">
                    <v-icon class="mr-1" size="16">mdi-credit-card-outline</v-icon>
                    VNPay
                </button>
            </div>
        </div>

        <!-- Money Input -->
        <div class="d-flex align-center justify-space-between mb-3">
            <span class="text-slate-600" style="font-size: 13px !important">
                {{ paymentMethod === 'CASH' ? 'Tiền khách đưa' : 'Tiền chuyển khoản' }}
            </span>
            <v-text-field :model-value="formatNumberWithDots(receivedAmount)"
                @input="onAmountInput"
                variant="outlined" density="compact" suffix="đ" hide-details
                style="width: 200px !important; max-width: 200px !important; min-width: 200px !important; flex: none !important;"
                class="text-right-input" />
        </div>

        <!-- Unpaid / Refund Message Alert -->
        <div v-if="remainingBalance > 0"
            class="d-flex align-center justify-space-between pa-3 rounded-lg bg-red-50 text-red-800 border-red">
            <div class="d-flex align-center gap-2">
                <v-icon color="error" size="18">mdi-alert-circle-outline</v-icon>
                <span class="text-slate-600" style="font-size: 13px !important">Còn thiếu</span>
            </div>
            <span class="font-weight-bold" style="font-size: 13px !important;">{{
                formatCurrency(remainingBalance)
                }}</span>
        </div>
        <div v-else-if="changeAmount > 0"
            class="d-flex align-center justify-space-between pa-3 rounded-lg bg-blue-50 text-blue-800 border-blue">
            <div class="d-flex align-center gap-2">
                <v-icon color="primary" size="18">mdi-cash-refund</v-icon>
                <span class="text-slate-600" style="font-size: 13px !important">Tiền thừa trả khách</span>
            </div>
            <span class="font-weight-bold" style="font-size: 13px !important;">{{
                formatCurrency(changeAmount) }}</span>
        </div>
    </v-card>

    <!-- Checkout / Print Action Buttons at Bottom Right -->
    <div class="d-flex gap-3 mt-4 w-100">
        <v-btn color="#107c41" height="60"
            class="font-weight-bold rounded-lg shadow-md text-white px-4 flex-grow-1"
            style="font-size: 17px !important;" :disabled="!hasItems"
            @click="$emit('print-invoice')" elevation="0">
            <v-icon class="mr-1">mdi-printer</v-icon>
            IN HÓA ĐƠN
        </v-btn>

        <v-btn color="primary" height="60"
            class="font-weight-bold rounded-lg btn-checkout shadow-md text-white px-4 flex-grow-1"
            style="font-size: 17px !important;" :loading="isProcessing"
            :disabled="!hasItems" @click="$emit('checkout')" elevation="0">
            THANH TOÁN
        </v-btn>
    </div>

  </div>
</template>

<script setup>
import { computed } from 'vue';
import QrcodeVue from 'qrcode.vue';

const props = defineProps({
    paymentMethod: { type: String, default: 'CASH' },
    receivedAmount: { type: Number, default: 0 },
    remainingBalance: { type: Number, default: 0 },
    changeAmount: { type: Number, default: 0 },
    isProcessing: { type: Boolean, default: false },
    hasItems: { type: Boolean, default: false },
    vnpayMethod: { type: String, default: 'QR' },
    vnpayDialog: {
        type: Object,
        default: () => ({ show: false, orderId: '', loading: false, verified: false, statusText: '', paymentUrl: '', amount: 0 })
    }
});

const emit = defineEmits([
    'update:paymentMethod',
    'update:receivedAmount',
    'print-invoice',
    'checkout',
    'update:vnpayDialog',
    'cancel-vnpay',
    'vnpay-redirect'
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

const onAmountInput = (e) => {
    const val = parseNumberFromDots(e.target.value);
    emit('update:receivedAmount', val);
};

const emitMethodChange = (method) => {
    emit('update:paymentMethod', method);
};

const emitDialogChange = (key, val) => {
    emit('update:vnpayDialog', { ...props.vnpayDialog, [key]: val });
};
</script>

<style scoped>
.pos-card {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05) !important;
}
.payment-btn {
    font-size: 13px !important;
    height: 34px;
    min-width: 100px;
    cursor: pointer;
    border-radius: 8px !important;
    border: 1px solid transparent;
}
.cash-active-btn {
    background-color: #fdf2f8 !important; /* Soft pastel pink */
    color: #db2777 !important; /* Premium pink text */
    border-color: #fbcfe8 !important; /* Matching soft pink border */
    font-weight: 600;
}
.cash-active-btn :deep(.v-icon) {
    color: #db2777 !important;
}
.vnpay-active-btn {
    background-color: #f0f9ff !important; /* Soft pastel blue */
    color: #005BAA !important; /* VNPay brand blue text */
    border-color: #bae6fd !important; /* Matching soft blue border */
    font-weight: 600;
}
.vnpay-active-btn :deep(.v-icon) {
    color: #005BAA !important;
}
.payment-inactive-btn {
    background-color: #ffffff !important;
    color: #64748b !important;
    border-color: #e2e8f0 !important;
}
.payment-inactive-btn :deep(.v-icon) {
    color: #94a3b8 !important;
}
.text-right-input :deep(input) {
    text-align: right;
    font-weight: bold;
    color: #0f172a;
}
.border-red {
    border: 1px solid rgba(239, 68, 68, 0.2);
}
.border-blue {
    border: 1px solid rgba(59, 130, 246, 0.2);
}
.btn-checkout {
    background: #0c3866 !important;
}
</style>
