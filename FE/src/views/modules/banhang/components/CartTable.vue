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
                    <th class="text-left py-2">Sản phẩm</th>
                    <th class="text-center py-2" style="width: 130px">Số lượng</th>
                    <th class="text-right py-2" style="width: 130px">Đơn giá</th>
                    <th class="text-right py-2" style="width: 130px">Thành tiền</th>
                    <th class="text-center py-2" style="width: 40px"></th>
                </tr>
            </thead>
            <tbody>
                <tr v-if="!items?.length">
                    <td colspan="5" class="text-center py-16">
                        <div class="opacity-20 mb-4">
                            <ShoppingCartIcon size="64" />
                        </div>
                        <p class="text-h6 text-medium-emphasis">Giỏ hàng đang trống</p>
                        <p class="text-body-2 text-grey">Tìm mã hoặc tên sản phẩm để thêm vào đơn</p>
                    </td>
                </tr>
                <tr v-for="item in items" :key="item.id" class="item-row">
                    <td>
                        <div class="d-flex align-center py-1.5">
                            <v-avatar color="grey-lighten-4" rounded="lg" size="38" class="mr-3 border position-relative overflow-visible">
                                <v-img v-if="item.hinhAnh" :src="item.hinhAnh" cover />
                                <BoxIcon v-else size="18" class="text-grey" />
                                <div v-if="item.phanTramGiam > 0" class="cart-discount-badge-small d-flex align-center justify-center">
                                    <span class="text-white font-weight-bold" style="font-size: 9px !important;">-{{ item.phanTramGiam }}%</span>
                                </div>
                            </v-avatar>
                            <div>
                                <div class="text-slate-700 text-body-2" style="font-size: 13.5px !important; line-height: 1.3;">{{ item.tenSanPham }}</div>
                                
                                <!-- mã sp | mã sku (với nhãn vuông pastel nhé) -->
                                <div class="d-flex align-center mt-1 flex-wrap">
                                    <span class="sp-badge">Mã Sản phẩm: {{ item.maSanPham || 'SP0001' }}</span>
                                    <span style="margin-left: 15px; margin-right: 15px; font-size: 11px; color: #cbd5e1; opacity: 0.4;">|</span>
                                    <span class="sku-badge">{{ item.maChiTietSanPham || 'N/A' }}</span>
                                </div>
                                
                                <!-- màu sắc | size -->
                                <div class="d-flex align-center mt-1 text-slate-500 font-weight-medium" style="font-size: 11.5px; flex-wrap: wrap;">
                                    <span>Màu sắc: <span class="text-slate-700">{{ item.tenMauSac || 'Không màu' }}</span></span>
                                    <span style="margin-left: 15px; margin-right: 15px; font-size: 11px; color: #cbd5e1; opacity: 0.4;">|</span>
                                    <span>Size: <span class="text-slate-700">{{ item.tenKichThuoc || 'N/A' }}</span></span>
                                </div>
                            </div>
                        </div>
                    </td>
                    <td class="text-center">
                        <div class="qty-control">
                            <v-btn icon size="x-small" variant="text" @click="emit('update-qty', item, -1)">
                                <MinusIcon size="12" />
                            </v-btn>
                            <input 
                                type="number" 
                                class="qty-input-table text-center font-weight-medium" 
                                :value="item.soLuong" 
                                @change="(e) => handleDirectInput(item, e)"
                                min="0"
                            />
                            <v-btn icon size="x-small" variant="text" @click="emit('update-qty', item, 1)">
                                <PlusIcon size="12" />
                            </v-btn>
                        </div>
                    </td>
                    <td class="text-right text-caption" style="font-weight: 500 !important;">{{ formatCurrency(item.donGia) }}</td>
                    <td class="text-right text-primary text-body-2" style="font-weight: 500 !important;">{{ formatCurrency(item.thanhTien) }}</td>
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
    border-spacing: 0 10px !important;
    background: transparent !important;
    padding: 0 6px;
}

.pos-table :deep(thead th) {
    background-color: #f8fafc !important;
    color: #475569 !important;
    font-size: 12px !important;
    font-weight: 600 !important;
    border-bottom: none !important;
    height: 38px !important;
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
    padding-top: 8px !important;
    padding-bottom: 8px !important;
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
