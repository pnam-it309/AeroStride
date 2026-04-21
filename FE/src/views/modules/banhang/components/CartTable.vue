<script setup>
import { ShoppingCartIcon, TrashIcon, MinusIcon, PlusIcon, BoxIcon } from 'vue-tabler-icons';

const props = defineProps(['items']);
const emit = defineEmits(['update-qty', 'remove']);

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);
</script>

<template>
    <v-card class="cart-card border shadow-none overflow-hidden h-full d-flex flex-column">
        <v-table class="pos-table flex-grow-1" fixed-header>
            <thead>
                <tr>
                    <th class="text-left py-4">Sản phẩm</th>
                    <th class="text-center py-4" style="width: 150px">Số lượng</th>
                    <th class="text-right py-4" style="width: 150px">Đơn giá</th>
                    <th class="text-right py-4" style="width: 150px">Thành tiền</th>
                    <th class="text-center py-4" style="width: 50px"></th>
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
                        <div class="d-flex align-center py-3">
                            <v-avatar color="grey-lighten-4" rounded="lg" size="44" class="mr-3 border">
                                <BoxIcon size="20" class="text-grey" />
                            </v-avatar>
                            <div>
                                <div class="font-weight-bold text-subtitle-2">{{ item.tenSanPham }}</div>
                                <div class="d-flex gap-2 mt-1">
                                    <v-chip size="x-small" label variant="outlined" color="grey">Size {{ item.tenKichThuoc }}</v-chip>
                                    <v-chip size="x-small" label variant="outlined" color="grey">{{ item.tenMauSac }}</v-chip>
                                </div>
                            </div>
                        </div>
                    </td>
                    <td class="text-center">
                        <div class="qty-control">
                            <v-btn icon size="x-small" variant="text" @click="emit('update-qty', item, -1)">
                                <MinusIcon size="14" />
                            </v-btn>
                            <span class="qty-value">{{ item.soLuong }}</span>
                            <v-btn icon size="x-small" variant="text" @click="emit('update-qty', item, 1)">
                                <PlusIcon size="14" />
                            </v-btn>
                        </div>
                    </td>
                    <td class="text-right text-body-2">{{ formatCurrency(item.donGia) }}</td>
                    <td class="text-right font-weight-bold text-primary">{{ formatCurrency(item.thanhTien) }}</td>
                    <td class="text-center">
                        <v-btn icon variant="text" color="error" size="small" @click="emit('remove', item)">
                            <TrashIcon size="18" />
                        </v-btn>
                    </td>
                </tr>
            </tbody>
        </v-table>
    </v-card>
</template>

<style scoped>
/* Scoped styles removed in favor of global _admin-common.scss */
.qty-control {
    background: #f1f5f9;
    border-radius: 8px;
    display: inline-flex;
    align-center: center !important; /* Fixed typo from original logic */
    align-items: center;
    padding: 2px;
}
.qty-value {
    min-width: 32px;
    font-weight: 700;
    text-align: center;
}
.item-row:hover {
    background-color: #f8fafc;
}
</style>



