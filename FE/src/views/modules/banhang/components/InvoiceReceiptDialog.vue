<script setup>
/**
 * Module: Bán hàng tại quầy (Admin)
 * Component: InvoiceReceiptDialog
 * Chức năng: Hiển thị popup chứa hóa đơn bán lẻ sau khi thanh toán thành công,
 *            có hỗ trợ tính năng in hóa đơn bằng máy in nhiệt/A4.
 */
import { computed } from 'vue';
import { PrinterIcon, XIcon, CircleCheckIcon } from 'vue-tabler-icons';

const props = defineProps({
    show: Boolean,
    receipt: {
        type: Object,
        default: null
        // receipt: { order, paymentMethod, receivedAmount, note, paidAt }
    }
});
const emit = defineEmits(['close', 'print']);

// Format tiền tệ chuẩn Việt Nam
const fmt = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

// Định dạng thời gian in hóa đơn (VD: 14/05/2026 15:30:00)
const fmtDate = (ts) => {
    const d = ts ? new Date(ts) : new Date();
    return d.toLocaleString('vi-VN', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit', second: '2-digit'
    });
};

const order = computed(() => props.receipt?.order || {});
const items = computed(() => order.value?.listsHoaDonChiTiet || []);
const paymentMethod = computed(() => props.receipt?.paymentMethod || 'CASH');
const receivedAmount = computed(() => Number(props.receipt?.receivedAmount || 0));
const tongTienSauGiam = computed(() => Number(order.value?.tongTienSauGiam || 0));
const shippingFee = computed(() => Number(order.value?.phiVanChuyen || 0));
const tienThua = computed(() => Math.max(0, receivedAmount.value - tongTienSauGiam.value));
const subtotal = computed(() => {
    return items.value.reduce((sum, item) => sum + Number(item.thanhTien || (item.donGia * item.soLuong) || 0), 0);
});
// tongTienSauGiam returned from backend already includes shipping fee.
// So discount (voucher) is calculated by excluding shipping fee.
const discount = computed(() => Math.max(0, subtotal.value - Math.max(0, tongTienSauGiam.value - shippingFee.value)));
const paidAt = computed(() => fmtDate(props.receipt?.paidAt));

const paymentLabel = computed(() => {
    const pm = paymentMethod.value;
    if (pm === 'CASH') return 'Tiền mặt';
    if (pm === 'VNPAY') return 'VNPay / Chuyển khoản';
    return pm;
});

// Chuyen viec in len parent de dung mau HTML hoa don chuan cua module Quan ly hoa don.
const handlePrint = () => {
    emit('print', props.receipt);
};
</script>

<template>
    <v-dialog
        :model-value="show"
        max-width="520"
        persistent
        transition="dialog-bottom-transition"
        @update:model-value="!$event && emit('close')"
    >
        <v-card class="receipt-dialog rounded-xl overflow-hidden">
            <!-- Header action bar (không in) -->
            <div class="receipt-actions d-flex justify-space-between align-center pa-4 no-print">
                <span class="text-subtitle-1 font-weight-bold d-flex align-center ga-2">
                    <CircleCheckIcon size="22" class="text-success" />
                    Thanh toán thành công
                </span>
                <div class="d-flex ga-2">
                    <v-btn color="primary" variant="flat" size="small" prepend-icon class="rounded-lg" @click="handlePrint">
                        <template #prepend><PrinterIcon size="16" /></template>
                        In hóa đơn
                    </v-btn>
                    <v-btn icon variant="text" size="small" @click="emit('close')">
                        <XIcon size="20" />
                    </v-btn>
                </div>
            </div>

            <v-divider class="no-print" />

            <!-- Nội dung hóa đơn (được in) -->
            <div class="receipt-body pa-5 printable-receipt">
                <!-- Store Header -->
                <div class="text-center mb-4">
                    <div class="store-logo text-h5 font-weight-black text-black tracking-wide mb-1">
                        👟 AEROSTRIDE
                    </div>
                    <div class="text-caption text-grey-darken-1">Thời trang thể thao cao cấp</div>
                    <div class="text-caption text-grey-darken-1">ĐT: 1900-xxxx | aerostride.vn</div>
                    <div class="receipt-divider my-3">- - - - - - - - - - - - - - - - - - - - -</div>
                    <div class="text-subtitle-1 font-weight-bold">HÓA ĐƠN BÁN HÀNG</div>
                </div>

                <!-- Invoice Info -->
                <div class="receipt-info mb-3">
                    <div class="info-row">
                        <span class="label">Số HĐ:</span>
                        <span class="value font-weight-bold">{{ order.maHoaDon || '—' }}</span>
                    </div>
                    <div class="info-row">
                        <span class="label">Ngày:</span>
                        <span class="value">{{ paidAt }}</span>
                    </div>
                    <div class="info-row" v-if="order.tenKhachHang && order.tenKhachHang !== 'Khách lẻ'">
                        <span class="label">Khách hàng:</span>
                        <span class="value font-weight-medium">{{ order.tenKhachHang }}</span>
                    </div>
                    <div class="info-row" v-if="order.sdtKhachHang">
                        <span class="label">SĐT:</span>
                        <span class="value">{{ order.sdtKhachHang }}</span>
                    </div>
                    <div class="info-row" v-else>
                        <span class="label">Khách hàng:</span>
                        <span class="value">Khách lẻ</span>
                    </div>
                </div>

                <div class="receipt-divider mb-3">- - - - - - - - - - - - - - - - - - - - -</div>

                <!-- Items -->
                <div class="receipt-items mb-3">
                    <div
                        v-for="item in items"
                        :key="item.id"
                        class="item-row mb-2"
                    >
                        <div class="item-name font-weight-medium">{{ item.tenSanPham }}</div>
                        <div class="item-detail d-flex justify-space-between align-center">
                            <span class="text-caption text-grey">
                                {{ item.tenMauSac }} / {{ item.tenKichThuoc }}
                                <span v-if="item.maChiTietSanPham" class="ml-1">({{ item.maChiTietSanPham }})</span>
                            </span>
                            <span class="item-subtotal font-weight-bold text-body-2">{{ fmt(item.thanhTien) }}</span>
                        </div>
                        <div class="text-caption text-grey">
                            {{ fmt(item.donGia) }} × {{ item.soLuong }}
                        </div>
                    </div>
                </div>

                <div class="receipt-divider mb-3">- - - - - - - - - - - - - - - - - - - - -</div>

                <!-- Totals -->
                <div class="receipt-totals mb-3">
                    <div class="total-row">
                        <span>Cộng tiền hàng</span>
                        <span>{{ fmt(subtotal) }}</span>
                    </div>
                    <div class="total-row" v-if="shippingFee > 0">
                        <span>Phí vận chuyển</span>
                        <span>+{{ fmt(shippingFee) }}</span>
                    </div>
                    <div class="total-row text-success" v-if="discount > 0">
                        <span>Giảm giá</span>
                        <span>-{{ fmt(discount) }}</span>
                    </div>
                    <div class="total-row grand-total mt-2">
                        <span class="font-weight-bold text-subtitle-1">TỔNG THANH TOÁN</span>
                        <span class="font-weight-black text-subtitle-1 text-primary">{{ fmt(tongTienSauGiam) }}</span>
                    </div>
                </div>

                <div class="receipt-divider mb-3">- - - - - - - - - - - - - - - - - - - - -</div>

                <!-- Payment Info -->
                <div class="receipt-payment mb-3">
                    <div class="total-row">
                        <span>Phương thức</span>
                        <span class="font-weight-medium">{{ paymentLabel }}</span>
                    </div>
                    <template v-if="paymentMethod === 'CASH'">
                        <div class="total-row">
                            <span>Tiền khách đưa</span>
                            <span>{{ fmt(receivedAmount) }}</span>
                        </div>
                        <div class="total-row text-success" v-if="tienThua > 0">
                            <span>Tiền thừa trả khách</span>
                            <span class="font-weight-bold">{{ fmt(tienThua) }}</span>
                        </div>
                    </template>
                </div>

                <!-- Note -->
                <div v-if="receipt?.note" class="mb-3 text-caption text-grey-darken-1">
                    Ghi chú: {{ receipt.note }}
                </div>

                <div class="receipt-divider mb-3">- - - - - - - - - - - - - - - - - - - - -</div>

                <!-- Footer -->
                <div class="text-center text-caption text-grey-darken-1 mt-2">
                    <div class="mb-1">Cảm ơn quý khách đã mua sắm tại AeroStride.</div>
                    <div>Trân trọng và hẹn gặp lại!</div>
                </div>
            </div>
        </v-card>
    </v-dialog>
</template>

<style scoped>
.receipt-dialog {
    font-family: 'Courier New', Courier, monospace;
}

.receipt-body {
    background: #fff;
    font-size: 13px;
    line-height: 1.6;
}

.store-logo {
    letter-spacing: 2px;
}

.receipt-divider {
    color: #bbb;
    text-align: center;
    font-size: 12px;
    letter-spacing: 1px;
}

.receipt-info .info-row {
    display: flex;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 2px;
}

.receipt-info .label {
    color: #666;
    min-width: 100px;
}

.receipt-info .value {
    flex: 1;
    text-align: right;
}

.item-row {
    padding: 4px 0;
    border-bottom: 1px dashed #eee;
}

.item-row:last-child {
    border-bottom: none;
}

.item-name {
    font-size: 13px;
}

.item-subtotal {
    color: #333;
}

.receipt-totals .total-row,
.receipt-payment .total-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 4px;
}

.grand-total {
    border-top: 2px solid #333;
    padding-top: 6px;
}

/* Print styles */
@media print {
    .no-print {
        display: none !important;
    }

    .receipt-dialog,
    .receipt-body {
        box-shadow: none !important;
        border-radius: 0 !important;
        max-width: 100% !important;
        width: 80mm !important;
    }

    .v-overlay__content {
        max-width: 80mm !important;
    }
}
</style>
