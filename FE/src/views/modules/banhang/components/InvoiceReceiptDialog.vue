<script setup>
/**
 * Module: Bán hàng tại quầy (Admin)
 * Component: InvoiceReceiptDialog
 * Chức năng: Hiển thị popup hóa đơn chính thức từ Backend (BE) sau khi thanh toán thành công,
 *            sử dụng mẫu HTML chuẩn từ BE và hỗ trợ in hóa đơn.
 */
import { ref, watch, computed } from 'vue';
import { PrinterIcon, XIcon, CircleCheckIcon } from 'vue-tabler-icons';
import { dichVuHoaDon } from '@/services/admin/dichVuHoaDon';

const props = defineProps({
    show: Boolean,
    receipt: {
        type: Object,
        default: null
        // receipt: { order, paymentMethod, receivedAmount, note, paidAt }
    }
});

const emit = defineEmits(['close', 'print']);

const loadingHtml = ref(false);
const invoiceHtml = ref('');

const orderId = computed(() => {
    return props.receipt?.order?.id || props.receipt?.order?.idHoaDon || null;
});

const fetchInvoiceHtml = async () => {
    if (!orderId.value) {
        invoiceHtml.value = '';
        return;
    }
    loadingHtml.value = true;
    try {
        const html = await dichVuHoaDon.inHoaDon(orderId.value);
        invoiceHtml.value = html || '';
    } catch (err) {
        console.error('Error fetching invoice HTML from BE:', err);
        invoiceHtml.value =
            '<div style="padding: 24px; text-align: center; color: #dc2626; font-family: sans-serif;">Không thể tải hóa đơn từ máy chủ (Backend). Vui lòng thử lại.</div>';
    } finally {
        loadingHtml.value = false;
    }
};

watch(
    () => [props.show, orderId.value],
    ([isShowing, id]) => {
        if (isShowing && id) {
            fetchInvoiceHtml();
        }
    },
    { immediate: true }
);

const handlePrint = () => {
    emit('print', props.receipt);
};
</script>

<template>
    <v-dialog
        :model-value="show"
        max-width="850"
        persistent
        transition="dialog-bottom-transition"
        @update:model-value="!$event && emit('close')"
    >
        <v-card class="receipt-dialog rounded-xl overflow-hidden">
            <!-- Header action bar -->
            <div class="receipt-actions d-flex justify-space-between align-center pa-4 bg-white border-b">
                <span class="text-subtitle-1 font-weight-bold d-flex align-center ga-2 text-slate-800">
                    <CircleCheckIcon size="22" class="text-success" />
                    Thanh toán thành công
                </span>
                <div class="d-flex ga-2">
                    <v-btn color="primary" variant="flat" size="small" class="rounded-lg" @click="handlePrint">
                        <template #prepend><PrinterIcon size="16" /></template>
                        In hóa đơn
                    </v-btn>
                    <v-btn icon variant="text" size="small" @click="emit('close')">
                        <XIcon size="20" />
                    </v-btn>
                </div>
            </div>

            <!-- Preloader -->
            <v-progress-linear v-if="loadingHtml" indeterminate color="primary" />

            <!-- Nội dung hóa đơn chính thức nạp từ Backend (BE) -->
            <div class="receipt-body bg-slate-50 d-flex justify-center align-center position-relative" style="min-height: 500px">
                <iframe v-if="invoiceHtml" :srcdoc="invoiceHtml" class="invoice-iframe" title="Hóa đơn bán hàng"></iframe>
                <div v-else-if="!loadingHtml" class="text-slate-400 text-body-2 pa-8 text-center">Không có thông tin hóa đơn.</div>
            </div>
        </v-card>
    </v-dialog>
</template>

<style scoped>
.invoice-iframe {
    width: 100%;
    height: 720px;
    border: none;
    background: #ffffff;
}

@media print {
    .receipt-actions {
        display: none !important;
    }
}
</style>
