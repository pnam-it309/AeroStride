<script setup>
import { computed } from 'vue';
import { CashIcon, CreditCardIcon, Receipt2Icon, TicketIcon } from 'vue-tabler-icons';

const props = defineProps(['order', 'vouchers', 'checkoutData', 'loading']);
const emit = defineEmits(['apply-voucher', 'checkout']);

const discount = computed(() => (props.order?.tongTien || 0) - (props.order?.tongTienSauGiam || 0));
const changeAmount = computed(() => Math.max(0, (props.checkoutData.receivedAmount || 0) - (props.order?.tongTienSauGiam || 0)));

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

const handleCheckout = () => {
    emit('checkout');
};
</script>

<template>
    <div class="checkout-panel h-100 d-flex flex-column">
        <div class="pricing-summary pa-5 rounded-xl border-dashed border-2 bg-grey-lighten-5 mb-6">
            <div class="d-flex justify-space-between mb-3 text-subtitle-2 text-grey-darken-1">
                <span>Tạm tính:</span>
                <span class="font-weight-bold">{{ formatCurrency(order?.tongTien) }}</span>
            </div>

            <div class="voucher-section mb-4">
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

            <div class="d-flex justify-space-between mb-3 text-error">
                <span class="text-subtitle-2">Giảm giá:</span>
                <span class="font-weight-bold text-subtitle-2">- {{ formatCurrency(discount) }}</span>
            </div>

            <v-divider class="my-4 border-dashed"></v-divider>

            <div class="d-flex justify-space-between align-center mb-2">
                <span class="text-h6 font-weight-bold">Tổng thanh toán:</span>
                <span class="text-h4 font-weight-bold text-#2E4E8E">{{ formatCurrency(order?.tongTienSauGiam) }}</span>
            </div>
        </div>

        <div class="payment-selection mb-6">
            <p class="text-subtitle-2 font-weight-bold mb-3 d-flex align-center">
                <Receipt2Icon size="18" class="mr-1" /> Hình thức thanh toán
            </p>
            <v-btn-toggle v-model="checkoutData.paymentMethod" mandatory color="#2E4E8E" class="d-flex w-100 gap-2 payment-toggle">
                <v-btn value="CASH" variant="outlined" class="flex-grow-1 h-14 rounded-lg border-2">
                    <div class="d-flex flex-column align-center">
                        <CashIcon size="20" class="mb-1" />
                        <span class="text-caption font-weight-bold">Tiền mặt</span>
                    </div>
                </v-btn>
                <v-btn value="TRANSFER" variant="outlined" class="flex-grow-1 h-14 rounded-lg border-2">
                    <div class="d-flex flex-column align-center">
                        <CreditCardIcon size="20" class="mb-1" />
                        <span class="text-caption font-weight-bold">Chuyển khoản</span>
                    </div>
                </v-btn>
            </v-btn-toggle>
        </div>

        <div v-if="checkoutData.paymentMethod === 'CASH'" class="cash-input mb-6">
            <v-text-field
                v-model.number="checkoutData.receivedAmount"
                label="Tiền khách đưa"
                variant="outlined"
                type="number"
                prefix="VND"
                class="rounded-lg"
                hide-details
            ></v-text-field>
            <div class="d-flex justify-space-between mt-3 px-1">
                <span class="text-subtitle-2 text-grey-darken-1">Tiền thừa trả khách:</span>
                <span class="font-weight-bold text-success">{{ formatCurrency(changeAmount) }}</span>
            </div>
        </div>

        <v-textarea v-model="checkoutData.note" label="Ghi chú hóa đơn" variant="outlined" rows="2" class="mb-6" hide-details></v-textarea>

        <v-btn
            block
            color="#2E4E8E"
            height="20"
            class="text-h6 font-weight-bold rounded-xl btn-checkout shadow-lg"
            :loading="loading"
            :disabled="!order?.listsHoaDonChiTiet?.length"
            @click="handleCheckout"
        >
            XÁC NHẬN THANH TOÁN
        </v-btn>
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
    background: linear-gradient(45deg, rgb(var(--v-theme-#2E4E8E)), #346ac0) !important;
}
.pricing-summary {
    border-color: #cbd5e1 !important;
}
</style>



