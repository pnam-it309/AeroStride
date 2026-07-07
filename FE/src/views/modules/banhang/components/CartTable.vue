<script setup>
/**
 * Component: CartTable
 * Y nghia: bang gio hang POS, hien bien the da chon, don gia sau giam,
 * so luong va phat emit cap nhat/xoa dong ve BanHang.vue.
 */
import { ShoppingCartIcon, TrashIcon, MinusIcon, PlusIcon, BoxIcon } from 'vue-tabler-icons';

const props = defineProps(['items']);
const emit = defineEmits(['update-qty', 'remove']);

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

const handleDirectInput = (item, event) => {
    let val = event.target.value;
    if (!/^\d+$/.test(val)) {
        event.target.value = item.soLuong;
        return;
    }
    let newQty = parseInt(val, 10);
    if (isNaN(newQty) || newQty < 0) newQty = 0;

    const delta = newQty - item.soLuong;
    if (delta !== 0) {
        // Pass event.target so BanHang can force the input back if max is exceeded
        emit('update-qty', item, delta, event.target);
    } else {
        event.target.value = item.soLuong;
    }
};
</script>

<template>
    <v-card class="cart-card shadow-none overflow-hidden h-full d-flex flex-column">
        <v-table class="pos-table flex-grow-1" fixed-header height="100%">
            <thead>
                <tr>
                    <th class="text-center py-2" style="width: 60px; white-space: nowrap !important;">STT</th>
                    <th class="text-left py-2" style="width: 130px; white-space: nowrap !important;">Mã sản phẩm</th>
                    <th class="text-left py-2">Tên sản phẩm</th>
                    <th class="text-left py-2" style="width: 100px; white-space: nowrap !important;">Màu sắc</th>
                    <th class="text-left py-2" style="width: 90px; white-space: nowrap !important;">Kích cỡ</th>
                    <th class="text-center py-2" style="width: 130px; white-space: nowrap !important;">Số lượng</th>
                    <th class="text-right py-2" style="width: 140px; white-space: nowrap !important;">Đơn giá</th>
                    <th class="text-center py-2" style="width: 90px; white-space: nowrap !important;">Thao tác</th>
                </tr>
            </thead>
            <tbody>
                <tr v-if="!items?.length">
                    <td colspan="8" class="text-center py-16">
                        <div class="opacity-20 mb-4">
                            <ShoppingCartIcon size="64" />
                        </div>
                        <p class="text-h6 text-medium-emphasis">Giỏ hàng đang trống</p>
                        <p class="text-body-2 text-grey">Tìm mã hoặc tên sản phẩm để thêm vào đơn</p>
                    </td>
                </tr>
                <tr v-for="(item, idx) in items" :key="item.id" class="item-row">
                    <td class="text-center font-weight-medium text-slate-500"
                        style="font-size: 13px; white-space: nowrap !important;">
                        {{ idx + 1 }}
                    </td>
                    <td class="text-left font-weight-medium text-slate-700"
                        style="font-size: 13px; white-space: nowrap !important;">
                        {{ item.maChiTietSanPham || item.maSanPham || 'N/A' }}
                    </td>
                    <td>
                        <div class="d-flex align-center py-1.5">
                            <v-avatar color="grey-lighten-4" rounded="lg" size="38"
                                class="mr-3 border position-relative overflow-visible">
                                <v-img v-if="item.hinhAnh" :src="item.hinhAnh" cover />
                                <BoxIcon v-else size="18" class="text-grey" />
                                <div v-if="item.phanTramGiam > 0"
                                    class="cart-discount-badge-small d-flex align-center justify-center">
                                    <span class="text-white font-weight-bold" style="font-size: 9px !important;">-{{
                                        item.phanTramGiam }}%</span>
                                </div>
                            </v-avatar>
                            <span class="text-slate-700 text-body-2 font-weight-medium"
                                style="font-size: 13px !important; line-height: 1.3;">
                                {{ item.tenSanPham }}
                            </span>
                        </div>
                    </td>
                    <td class="text-left text-slate-600" style="font-size: 13px; white-space: nowrap !important;">
                        {{ item.tenMauSac || 'Không màu' }}
                    </td>
                    <td class="text-left text-slate-600" style="font-size: 13px; white-space: nowrap !important;">
                        {{ item.tenKichThuoc || 'N/A' }}
                    </td>
                    <td class="text-center" style="white-space: nowrap !important;">
                        <div class="qty-control">
                            <v-btn icon size="x-small" variant="text" @click="emit('update-qty', item, -1)">
                                <MinusIcon size="12" />
                            </v-btn>
                            <input type="number" class="qty-input-table text-center font-weight-medium"
                                :value="item.soLuong" @change="(e) => handleDirectInput(item, e)" min="0" />
                            <v-btn icon size="x-small" variant="text" @click="emit('update-qty', item, 1)">
                                <PlusIcon size="12" />
                            </v-btn>
                        </div>
                    </td>
                    <td class="text-right font-weight-bold"
                        style="font-size: 13px !important; white-space: nowrap !important;">
                        <template v-if="item.phanTramGiam > 0">
                            <span class="d-block font-weight-bold" style="color: #0c3866 !important;">
                                {{ formatCurrency(item.donGia) }}
                            </span>
                            <span class="font-weight-bold"
                                style="text-decoration: line-through; text-decoration-color: #94a3b8; -webkit-text-decoration-color: #94a3b8; color: #c92c04 !important; font-size: 11px !important; font-weight: normal; display: block; margin-top: 2px;">
                                {{ formatCurrency(item.donGia / (1 - item.phanTramGiam / 100)) }}
                            </span>
                        </template>
                        <template v-else>
                            <span style="color: #0c3866 !important;">
                                {{ formatCurrency(item.donGia) }}
                            </span>
                        </template>
                    </td>
                    <td class="text-right font-weight-bold text-primary text-body-2"
                        style="white-space: nowrap !important;">
                        {{ formatCurrency(item.thanhTien) }}
                    </td>
                    <td class="text-center">
                        <v-btn icon variant="text" color="error" size="small" @click="emit('remove', item)">
                            <TrashIcon size="16" />
                        </v-btn>
                    </td>
                </tr>
            </tbody>
        </v-table>
    </v-card>
</template>

<style scoped>
.qty-control {
    background: #f1f5f9;
    border-radius: 6px;
    display: inline-flex;
    align-items: center;
    padding: 1px;
    z-index: 2;
}

.cart-card {
    height: 100% !important;
    flex: 1;
    min-height: 0;
    border-radius: 8px !important;
    background: transparent !important;
}

.pos-table {
    height: 100%;
    background: transparent !important;
}

.pos-table :deep(.v-table__wrapper) {
    background: transparent !important;
    overflow-y: auto !important;
}

.pos-table :deep(.v-table__wrapper)::-webkit-scrollbar {
    width: 6px;
    height: 6px;
}

.pos-table :deep(.v-table__wrapper)::-webkit-scrollbar-track {
    background: transparent;
}

.pos-table :deep(.v-table__wrapper)::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 3px;
}

.pos-table :deep(.v-table__wrapper)::-webkit-scrollbar-thumb:hover {
    background: #94a3b8;
}

.pos-table :deep(.v-btn--icon) {
    border-radius: 6px !important;
}

.cart-discount-badge-small {
    position: absolute;
    top: -5px;
    left: -5px;
    background: #ef4444;
    color: #ffffff;
    height: 16px;
    width: 16px;
    border-radius: 50%;
    z-index: 2;
}

.pos-table :deep(table) {
    border-collapse: separate !important;
    border-spacing: 0 12px !important;
    background: transparent !important;
    padding: 0 6px;
}

.pos-table :deep(thead th) {
    background-color: #f1f5f9 !important;
    color: #334155 !important;
    font-size: 13px !important;
    font-weight: 600 !important;
    border-bottom: none !important;
    height: 40px !important;
}

.pos-table :deep(thead th:first-child) {
    border-top-left-radius: 8px !important;
    border-bottom-left-radius: 8px !important;
}

.pos-table :deep(thead th:last-child) {
    border-top-right-radius: 8px !important;
    border-bottom-right-radius: 8px !important;
}

.pos-table :deep(tbody tr.item-row) {
    background-color: #ffffff !important;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.02) !important;
    transition: all 0.2s ease;
}

.pos-table :deep(tbody tr.item-row:hover) {
    transform: translateY(-1px);
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.08), 0 2px 4px -1px rgba(0, 0, 0, 0.04) !important;
}

.pos-table :deep(tbody tr.item-row:hover td) {
    background-color: #ffffff !important;
}

.pos-table :deep(tbody td) {
    border-top: 1px solid #e2e8f0 !important;
    border-bottom: 1px solid #e2e8f0 !important;
    padding-top: 20px !important;
    padding-bottom: 20px !important;
    background-color: #ffffff !important;
    transition: background-color 0.2s ease;
}

.pos-table :deep(tbody td:first-child) {
    border-left: 1px solid #e2e8f0 !important;
    border-top-left-radius: 8px !important;
    border-bottom-left-radius: 8px !important;
}

.pos-table :deep(tbody td:last-child) {
    border-right: 1px solid #e2e8f0 !important;
    border-top-right-radius: 8px !important;
    border-bottom-right-radius: 8px !important;
}

.qty-input-table {
    min-width: 32px;
    max-width: 44px;
    text-align: center;
    border: none;
    background: transparent;
    outline: none;
    padding: 0;
    margin: 0;
    font-size: 13px;
    -moz-appearance: textfield;
}

.qty-input-table::-webkit-outer-spin-button,
.qty-input-table::-webkit-inner-spin-button {
    -webkit-appearance: none;
    margin: 0;
}

.sku-badge {
    background-color: #ffe4e6 !important;
    color: #9f1239 !important;
    font-size: 11px !important;
    font-weight: 500;
    padding: 1px 6px;
    border-radius: 4px;
    display: inline-flex;
    align-items: center;
}

.sp-badge {
    background-color: #e0f2fe !important;
    color: #0369a1 !important;
    font-size: 11px !important;
    font-weight: 500;
    padding: 1px 6px;
    border-radius: 4px;
    display: inline-flex;
    align-items: center;
}
</style>
