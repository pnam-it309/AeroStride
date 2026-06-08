<script setup>
import { ref, computed, watch } from 'vue';
import { XIcon, ShoppingCartIcon, MinusIcon, PlusIcon, BoxIcon } from 'vue-tabler-icons';

const props = defineProps({
    show: Boolean,
    productGroup: Object // { maSanPham, tenSanPham, hinhAnh, variants: [...] }
});

const emit = defineEmits(['update:show', 'add-to-cart']);

const selectedColor = ref('');
const selectedSize = ref('');
const quantity = ref(1);

const closeDialog = () => {
    emit('update:show', false);
};

// Reset data khi mở modal
watch(() => props.show, (val) => {
    if (val && props.productGroup) {
        quantity.value = 1;
        const variants = props.productGroup.variants || [];
        const colorsList = Array.from(new Set(variants.map(v => v.tenMauSac || 'Mặc định')));
        selectedColor.value = colorsList[0] || '';
        
        const colorVariants = variants.filter(v => (v.tenMauSac || 'Mặc định') === selectedColor.value);
        const sizesList = Array.from(new Set(colorVariants.map(v => v.tenKichThuoc || 'Mặc định')));
        selectedSize.value = sizesList[0] || '';
    }
});

watch(selectedColor, (newColor) => {
    const variants = props.productGroup?.variants || [];
    const colorVariants = variants.filter(v => (v.tenMauSac || 'Mặc định') === newColor);
    const sizesList = Array.from(new Set(colorVariants.map(v => v.tenKichThuoc || 'Mặc định')));
    if (!sizesList.includes(selectedSize.value)) {
        selectedSize.value = sizesList[0] || '';
    }
});

const colors = computed(() => {
    const variants = props.productGroup?.variants || [];
    return Array.from(new Set(variants.map(v => v.tenMauSac || 'Mặc định')));
});

const sizes = computed(() => {
    const variants = props.productGroup?.variants || [];
    const colorVariants = variants.filter(v => (v.tenMauSac || 'Mặc định') === selectedColor.value);
    return Array.from(new Set(colorVariants.map(v => v.tenKichThuoc || 'Mặc định')));
});

const activeVariant = computed(() => {
    const variants = props.productGroup?.variants || [];
    return variants.find(v => (v.tenMauSac || 'Mặc định') === selectedColor.value && (v.tenKichThuoc || 'Mặc định') === selectedSize.value);
});

const totalPrice = computed(() => {
    const price = activeVariant.value?.giaBan || 0;
    return price * quantity.value;
});

const activeStock = computed(() => Number(activeVariant.value?.soLuongTon ?? activeVariant.value?.soLuong ?? 0));

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

const handleAddCart = () => {
    if (!activeVariant.value) return;
    emit('add-to-cart', {
        variant: activeVariant.value,
        quantity: quantity.value
    });
    closeDialog();
};

const increaseQty = () => {
    const max = activeStock.value;
    if (quantity.value < max) {
        quantity.value++;
    }
};

const decreaseQty = () => {
    if (quantity.value > 1) {
        quantity.value--;
    }
};

const handleInputQty = (e) => {
    let val = parseInt(e.target.value);
    if (isNaN(val) || val < 1) val = 1;
    const max = activeStock.value;
    if (val > max) val = max;
    quantity.value = val;
};
</script>

<template>
    <v-dialog :model-value="show" @update:model-value="closeDialog" max-width="480" persistent>
        <v-card class="variant-modal rounded-xl overflow-hidden shadow-lg border">
            <!-- Header -->
            <div class="d-flex align-center justify-space-between pa-4 pb-0">
                <div class="d-flex align-center gap-3" style="max-width: 80%">
                    <v-avatar rounded="lg" size="64" class="border flex-shrink-0">
                        <v-img v-if="activeVariant?.hinhAnh || productGroup?.hinhAnh" 
                               :src="activeVariant?.hinhAnh || productGroup?.hinhAnh" cover />
                        <BoxIcon v-else size="28" class="text-grey" />
                    </v-avatar>
                    <div>
                        <div class="text-h6 font-weight-bold text-slate-800 line-clamp-1" :title="productGroup?.tenSanPham">{{ productGroup?.tenSanPham }}</div>
                        <div class="text-caption text-grey-darken-1">Chọn biến thể & số lượng</div>
                        <div class="text-caption mt-1">Tồn kho: <b class="text-primary">{{ activeStock }}</b></div>
                    </div>
                </div>
                <v-btn icon variant="text" size="small" @click="closeDialog" class="mb-auto flex-shrink-0">
                    <XIcon size="20" />
                </v-btn>
            </div>

            <!-- Body -->
            <div class="pa-4 pt-4">
                <!-- Màu sắc -->
                <div class="mb-4">
                    <div class="text-caption font-weight-bold text-slate-500 text-uppercase mb-2">Màu sắc</div>
                    <div class="d-flex flex-wrap gap-2">
                        <div 
                            v-for="color in colors" :key="color"
                            class="selection-btn"
                            :class="{ 'active': selectedColor === color }"
                            @click="selectedColor = color"
                        >
                            {{ color }}
                        </div>
                    </div>
                </div>

                <!-- Kích thước -->
                <div class="mb-6">
                    <div class="text-caption font-weight-bold text-slate-500 text-uppercase mb-2">Kích thước</div>
                    <div class="d-flex flex-wrap gap-2">
                        <div 
                            v-for="size in sizes" :key="size"
                            class="selection-btn"
                            :class="{ 'active': selectedSize === size }"
                            @click="selectedSize = size"
                        >
                            {{ size }}
                        </div>
                    </div>
                </div>

                <!-- Số lượng và Thành tiền -->
                <div class="d-flex align-center justify-space-between mb-6 mt-2">
                    <div class="qty-control">
                        <v-btn icon size="small" variant="text" @click="decreaseQty">
                            <MinusIcon size="16" />
                        </v-btn>
                        <input type="number" class="qty-input" :value="quantity" @change="handleInputQty" />
                        <v-btn icon size="small" variant="text" @click="increaseQty">
                            <PlusIcon size="16" />
                        </v-btn>
                    </div>
                    
                    <div class="text-right">
                        <div class="text-caption text-slate-500 mb-1">Thành tiền</div>
                        <div class="text-h5 font-weight-bold text-primary">{{ formatCurrency(totalPrice) }}</div>
                    </div>
                </div>

                <!-- Button -->
                <v-btn 
                    color="#4285F4" 
                    size="large" 
                    block 
                    class="rounded-lg text-white font-weight-bold shadow-md add-btn"
                    height="48"
                    elevation="0"
                    :disabled="!activeVariant || activeStock === 0"
                    @click="handleAddCart"
                >
                    <ShoppingCartIcon v-if="activeVariant && activeStock > 0" size="20" class="mr-2" />
                    {{ (!activeVariant || activeStock === 0) ? 'Hết hàng' : 'Thêm vào giỏ hàng' }}
                </v-btn>
            </div>
        </v-card>
    </v-dialog>
</template>

<style scoped>
.variant-modal {
    background-color: #ffffff;
}
.gap-2 { gap: 8px; }
.gap-3 { gap: 12px; }
.line-clamp-1 {
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.selection-btn {
    padding: 8px 16px;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 600;
    color: #475569;
    background: #f8fafc;
    cursor: pointer;
    transition: all 0.2s;
    user-select: none;
}
.selection-btn:hover {
    border-color: #cbd5e1;
    background: #f1f5f9;
}
.selection-btn.active {
    border-color: #4285F4;
    color: #4285F4;
    background: #eff6ff;
}

.qty-control {
    display: flex;
    align-items: center;
    background: #f1f5f9;
    border-radius: 12px;
    padding: 4px;
}
.qty-input {
    width: 44px;
    text-align: center;
    font-weight: 700;
    font-size: 16px;
    outline: none;
    background: transparent;
    border: none;
    -moz-appearance: textfield;
}
.qty-input::-webkit-outer-spin-button,
.qty-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.text-primary {
    color: #4285F4 !important;
}
.shadow-md {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06) !important;
}
.add-btn {
    text-transform: none;
    font-size: 15px;
    letter-spacing: 0;
}
</style>
