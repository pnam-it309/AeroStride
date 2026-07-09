<template>
  <div>
    <!-- Payment Card -->
    <v-card class="pos-card pa-4 rounded-lg border mt-4">
        <div class="d-flex justify-space-between align-center mb-3">
            <h3 class="text-slate-800 m-0" style="font-size: 13px !important">Thanh toán</h3>
        </div>

        <div class="d-flex align-center justify-space-between mb-4">
            <span class="text-slate-600" style="font-size: 13px !important">Hình thức thanh toán</span>
            <div class="d-flex gap-2">
                <button type="button" @click="emitMethodChange('CASH')"
                    :class="['px-3 d-flex align-center justify-center transition-all',
                        paymentMethod === 'CASH' ? 'cash-active-btn' : 'payment-inactive-btn']"
                    style="font-size: 13px !important; border: 1px solid; border-radius: 0px !important; height: 32px; min-width: 90px; cursor: pointer;">
                    <v-icon class="mr-1" size="16">mdi-cash</v-icon>
                    Tiền mặt
                </button>
                <button type="button" @click="emitMethodChange('VNPAY')"
                    :class="['px-3 d-flex align-center justify-center transition-all',
                        paymentMethod === 'VNPAY' ? 'vnpay-active-btn' : 'payment-inactive-btn']"
                    style="font-size: 13px !important; border: 1px solid; border-radius: 0px !important; height: 32px; min-width: 90px; cursor: pointer;">
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
    <div class="d-flex gap-3 mt-2 w-100">
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
.cash-active-btn {
    background-color: #107c41;
    color: white;
    border-color: #107c41 !important;
    font-weight: 600;
}
.vnpay-active-btn {
    background-color: #005BAA;
    color: white;
    border-color: #005BAA !important;
    font-weight: 600;
}
.payment-inactive-btn {
    background-color: white;
    color: #64748b;
    border-color: #e2e8f0 !important;
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
    background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%) !important;
}
</style>
