<script setup>
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

const getColorChipStyle = (colorName) => {
    if (!colorName) return {};
    const name = colorName.toLowerCase().trim();
    let bg = '#f8fafc';
    let border = '#cbd5e1';
    let text = '#475569';
    
    if (name.includes('đen') || name.includes('black')) {
        bg = '#f1f5f9';
        border = '#94a3b8';
        text = '#0f172a';
    } else if (name.includes('trắng') || name.includes('white')) {
        bg = '#ffffff';
        border = '#cbd5e1';
        text = '#334155';
    } else if (name.includes('đỏ') || name.includes('red')) {
        bg = '#fef2f2';
        border = '#fca5a5';
        text = '#dc2626';
    } else if (name.includes('xanh dương') || name.includes('blue') || name.includes('navy')) {
        bg = '#eff6ff';
        border = '#bfdbfe';
        text = '#2563eb';
    } else if (name.includes('xanh lá') || name.includes('green') || name.includes('lục')) {
        bg = '#f0fdf4';
        border = '#bbf7d0';
        text = '#16a34a';
    } else if (name.includes('vàng') || name.includes('yellow')) {
        bg = '#fefce8';
        border = '#fef08a';
        text = '#ca8a04';
    } else if (name.includes('cam') || name.includes('orange')) {
        bg = '#fff7ed';
        border = '#ffedd5';
        text = '#ea580c';
    } else if (name.includes('hồng') || name.includes('pink')) {
        bg = '#fdf2f8';
        border = '#fbcfe8';
        text = '#db2777';
    } else if (name.includes('xám') || name.includes('ghi') || name.includes('grey') || name.includes('gray')) {
        bg = '#f1f5f9';
        border = '#cbd5e1';
        text = '#475569';
    } else if (name.includes('nâu') || name.includes('brown')) {
        bg = '#fdf8f6';
        border = '#d7ccc8';
        text = '#5d4037';
    } else if (name.includes('tím') || name.includes('purple')) {
        bg = '#faf5ff';
        border = '#e9d5ff';
        text = '#7c3aed';
    }
    
    return {
        backgroundColor: bg,
        borderColor: border,
        color: text
    };
};
</script>

<template>
    <div class="cart-items-container custom-scrollbar overflow-y-auto pr-1">
        <div v-if="!items?.length" class="text-center py-8 empty-cart-box">
            <v-icon size="40" class="text-slate-300 mb-1">mdi-cart-outline</v-icon>
            <div class="text-slate-400 font-weight-medium" style="font-size: 13px;">Giỏ hàng trống</div>
        </div>
        
        <div 
            v-else 
            v-for="item in items" 
            :key="item.id" 
            class="cart-item-card d-flex align-center justify-space-between gap-4"
        >
            <!-- Left Part: Image and Product Details -->
            <div class="d-flex align-center gap-4 flex-grow-1 min-w-0">
                <v-avatar rounded="lg" size="64" color="grey-lighten-5" class="border flex-shrink-0">
                    <v-img v-if="item.hinhAnh" :src="item.hinhAnh" cover />
                    <v-icon v-else size="24" class="text-slate-400">mdi-package-variant</v-icon>
                </v-avatar>
                
                <div class="d-flex flex-column gap-1.5 min-w-0 flex-grow-1">
                    <!-- Product Name -->
                    <div class="product-name-txt font-weight-semibold text-slate-800 text-truncate" style="font-size: 13px; line-height: 1.4;">
                        {{ item.tenSanPham }}
                    </div>
                    
                    <!-- Product Codes -->
                    <div class="d-flex align-center gap-3 mt-0.5" style="font-size: 13px;">
                        <span v-if="item.maSanPham" class="product-code-pastel-chip">{{ item.maSanPham }}</span>
                        <span v-if="item.maChiTietSanPham" class="text-slate-500 font-weight-medium">SKU: {{ item.maChiTietSanPham }}</span>
                    </div>
                    
                    <!-- Variant Selection: Color and Size -->
                    <div class="d-flex align-center gap-6 mt-1 flex-wrap" style="font-size: 13px;">
                        <div class="d-flex align-center gap-1.5">
                            <span class="text-slate-500 font-weight-medium">Màu:</span>
                            <span v-if="item.tenMauSac" class="color-pastel-chip" :style="getColorChipStyle(item.tenMauSac)">
                                {{ item.tenMauSac }}
                            </span>
                            <span v-else class="text-slate-400">--</span>
                        </div>
                        <div class="d-flex align-center gap-1.5">
                            <span class="text-slate-500 font-weight-medium">Size:</span>
                            <span class="text-slate-700 font-weight-bold">
                                {{ item.tenKichThuoc ? String(item.tenKichThuoc).replace(/^(Size|Cỡ)\s*/i, '') : '--' }}
                            </span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Right Part: Quantity, Prices and Actions -->
            <div class="d-flex align-center gap-6 flex-shrink-0">
                <!-- Unit Price -->
                <div class="text-right" style="min-width: 100px;">
                    <div class="text-slate-400 text-caption font-weight-medium mb-1">Đơn giá</div>
                    <div class="text-slate-700 font-weight-medium" style="font-size: 13px;">{{ formatCurrency(item.donGia) }}</div>
                </div>

                <!-- Quantity Control -->
                <div class="d-flex flex-column align-center">
                    <div class="text-slate-400 text-caption font-weight-medium mb-1">Số lượng</div>
                    <div class="qty-control">
                        <v-btn icon size="x-small" variant="text" @click="emit('update-qty', item, -1)">
                            <v-icon size="14" color="grey-darken-1">mdi-minus</v-icon>
                        </v-btn>
                        <input 
                            type="number" 
                            class="qty-input-table text-center" 
                            :value="item.soLuong" 
                            @change="(e) => handleDirectInput(item, e)"
                            min="0"
                        />
                        <v-btn icon size="x-small" variant="text" @click="emit('update-qty', item, 1)">
                            <v-icon size="14" color="grey-darken-1">mdi-plus</v-icon>
                        </v-btn>
                    </div>
                </div>

                <!-- Total Price -->
                <div class="text-right" style="min-width: 110px;">
                    <div class="text-slate-400 text-caption font-weight-medium mb-1">Thành tiền</div>
                    <div class="font-weight-bold text-primary" style="font-size: 13px;">{{ formatCurrency(item.thanhTien) }}</div>
                </div>

                <!-- Delete Button -->
                <v-btn icon variant="outlined" class="delete-item-btn" @click="emit('remove', item)">
                    <v-icon size="16">mdi-close</v-icon>
                </v-btn>
            </div>
        </div>
    </div>
</template>

<style scoped>
.cart-items-container {
    background-color: transparent;
    border: none;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 12px;
    max-height: 320px;
    overflow-y: auto;
}

.empty-cart-box {
    background-color: #ffffff;
    border: 1px dashed #cbd5e1;
    border-radius: 8px;
}

.cart-item-card {
    background-color: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 16px;
    box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
    transition: all 0.2s ease;
}

.cart-item-card:hover {
    border-color: #94a3b8;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
}

.qty-control {
    background: #f1f5f9;
    border-radius: 8px;
    display: inline-flex;
    align-items: center;
    padding: 2px;
}

.product-name-txt {
    font-size: 13px !important;
    font-weight: 600 !important;
    color: #334155 !important;
}

.text-primary {
    color: #1e257c !important;
}

.qty-input-table {
    min-width: 40px;
    max-width: 50px;
    text-align: center;
    border: none;
    background: transparent;
    outline: none;
    padding: 0;
    margin: 0;
    font-size: 13px !important;
    font-weight: 400 !important;
    color: #334155 !important;
    -moz-appearance: textfield;
}

.qty-input-table::-webkit-outer-spin-button,
.qty-input-table::-webkit-inner-spin-button {
    -webkit-appearance: none;
    margin: 0;
}

.delete-item-btn {
    border-radius: 6px !important;
    border: 1px solid #fca5a5 !important;
    color: #ef4444 !important;
    background-color: transparent !important;
    width: 28px !important;
    height: 28px !important;
    min-width: 28px !important;
}

.delete-item-btn:hover {
    background-color: #fef2f2 !important;
    border-color: #ef4444 !important;
}

.product-code-pastel-chip {
    display: inline-flex;
    align-items: center;
    background-color: #fcfcfc !important;
    border: 1px solid #0e18a5 !important;
    color: #323ee6 !important;
    font-size: 13px !important;
    font-weight: 500 !important;
    padding: 2px 8px !important;
    border-radius: 6px !important;
    line-height: 1.2 !important;
}

.color-pastel-chip {
    display: inline-flex;
    align-items: center;
    border-width: 1px !important;
    border-style: solid !important;
    font-size: 13px !important;
    font-weight: 500 !important;
    padding: 2px 8px !important;
    border-radius: 6px !important;
    line-height: 1.2 !important;
}

.custom-scrollbar::-webkit-scrollbar {
    width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
    background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
    background: #94a3b8;
}
</style>
