<script setup>
import { computed } from 'vue';
import QrcodeVue from 'qrcode.vue';

const props = defineProps({
    vnpayDialog: {
        type: Object,
        required: true
    },
    vnpayChoiceDialog: {
        type: Object,
        required: true
    },
    vnpayMethod: {
        type: String,
        default: 'QR'
    }
});

const emit = defineEmits([
    'update:vnpayDialog',
    'update:vnpayChoiceDialog',
    'update:vnpayMethod',
    'proceedChoice',
    'confirmManual',
    'retryQr',
    'cancel',
    'openGateway'
]);

const vnpayDialogState = computed({
    get: () => props.vnpayDialog.show,
    set: (val) => emit('update:vnpayDialog', { ...props.vnpayDialog, show: val })
});

const vnpayChoiceDialogState = computed({
    get: () => props.vnpayChoiceDialog.show,
    set: (val) => emit('update:vnpayChoiceDialog', { ...props.vnpayChoiceDialog, show: val })
});

const choiceMethod = computed({
    get: () => props.vnpayChoiceDialog.method,
    set: (val) => emit('update:vnpayChoiceDialog', { ...props.vnpayChoiceDialog, method: val })
});
</script>

<template>
    <!-- VNPay QR/Gateway Payment Dialog -->
    <v-dialog v-model="vnpayDialogState" max-width="450" persistent>
        <v-card class="rounded-xl overflow-hidden pb-4">
            <v-card-text class="pt-6 text-center d-flex flex-column align-center">
                <div class="vnpay-logo-wrapper mb-4">
                    <v-img src="https://vnpay.vn/assets/images/logo-icon/logo-primary.svg" width="60" />
                </div>

                <h3 class="text-h6 font-weight-bold mb-1">Thanh toán VNPay</h3>
                <p class="text-subtitle-2 text-grey-darken-1 mb-6">Mã đơn: {{ vnpayDialog.orderId }}</p>

                <div v-if="vnpayDialog.loading" class="pa-8 d-flex flex-column align-center">
                    <v-progress-circular indeterminate color="#005BAA" size="48" class="mb-4"></v-progress-circular>
                    <div class="text-body-2 font-weight-medium text-grey-darken-2">{{ vnpayDialog.statusText }}</div>
                </div>

                <div v-else-if="vnpayDialog.verified" class="pa-8 d-flex flex-column align-center">
                    <v-icon color="success" size="64" class="mb-4">mdi-check-circle</v-icon>
                    <div class="text-h6 font-weight-bold text-success mb-2">Giao dịch thành công!</div>
                    <div class="text-body-2 text-grey-darken-1">Đơn hàng đang được hoàn tất...</div>
                </div>

                <div v-else class="w-100 d-flex flex-column align-center">
                    <template v-if="vnpayMethod === 'QR'">
                        <div class="pa-2 bg-white rounded-lg elevation-2 mb-4 d-inline-block">
                            <QrcodeVue v-if="vnpayDialog.paymentUrl" :value="vnpayDialog.paymentUrl" :size="220"
                                level="H" render-as="canvas" />
                            <div v-else class="d-flex align-center justify-center text-grey"
                                style="width: 220px; height: 220px;">
                                Chưa có mã QR
                            </div>
                        </div>
                        <div class="text-h5 font-weight-bold text-error mb-1">
                            {{ new Intl.NumberFormat('vi-VN', {
                                style: 'currency', currency: 'VND'
                            }).format(vnpayDialog.amount) }}
                        </div>
                        <div class="text-caption text-grey-darken-1 mb-6 px-4 text-center">
                            Sử dụng ứng dụng ngân hàng hoặc ví VNPay để quét mã.
                        </div>
                    </template>
                    <template v-else>
                        <div class="text-h5 font-weight-bold text-error mb-4">
                            {{ new Intl.NumberFormat('vi-VN', {
                                style: 'currency', currency: 'VND'
                            }).format(vnpayDialog.amount) }}
                        </div>
                        <div class="text-caption text-grey-darken-1 mb-6 px-4 text-center">
                            Vui lòng hoàn tất thanh toán trên VNPay.
                        </div>
                        <v-btn color="#005BAA" class="mb-6 rounded-lg text-white font-weight-bold"
                            @click="emit('openGateway')">
                            Mở lại thanh toán
                        </v-btn>
                    </template>

                    <v-btn block color="#005BAA" class="mb-3 rounded-lg text-white font-weight-bold" height="48"
                        @click="emit('confirmManual')">
                        XÁC NHẬN ĐÃ NHẬN TIỀN
                    </v-btn>

                    <v-btn v-if="vnpayMethod === 'QR'" block variant="outlined" color="grey-darken-1"
                        class="rounded-lg font-weight-bold" height="48" @click="emit('retryQr')">
                        TẠO LẠI MÃ QR
                    </v-btn>

                    <v-btn variant="text" color="error" class="mt-4" size="small" @click="emit('cancel')">
                        Hủy giao dịch
                    </v-btn>
                </div>
            </v-card-text>
        </v-card>
    </v-dialog>

    <!-- VNPay Choice Dialog -->
    <v-dialog v-model="vnpayChoiceDialogState" max-width="400" persistent>
        <v-card class="rounded-xl pa-5">
            <div class="text-center mb-5">
                <div class="text-h6 font-weight-bold">Chọn hình thức thanh toán</div>
            </div>
            <v-radio-group v-model="choiceMethod" column hide-details class="mb-5">
                <v-radio value="QR" label="Thanh toán qua quét mã QR" color="#2E4E8E"></v-radio>
                <v-radio value="GATEWAY" label="Nhập mã thẻ qua cổng VNPay" color="#2E4E8E"></v-radio>
            </v-radio-group>
            <div class="d-flex gap-3">
                <v-btn class="flex-grow-1 rounded-lg font-weight-bold" variant="outlined" color="grey-darken-1"
                    height="44" @click="vnpayChoiceDialogState = false">
                    Hủy
                </v-btn>
                <v-btn class="flex-grow-1 rounded-lg font-weight-bold text-white" color="#4285F4" height="44"
                    @click="emit('proceedChoice')">
                    Tiếp tục
                </v-btn>
            </div>
        </v-card>
    </v-dialog>
</template>
