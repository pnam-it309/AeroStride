<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { PATH } from '@/router/routePaths';
import { useRoute, useRouter } from 'vue-router';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { useNotifications } from '@/services/notificationService';
import {
  ChevronLeftIcon, DeviceFloppyIcon, PlusIcon, TrashIcon,
  PhotoIcon, InfoCircleIcon, BoxIcon, SettingsIcon, ArrowLeftIcon
} from 'vue-tabler-icons';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import AttributeQuickAddModal from './components/AttributeQuickAddModal.vue';
import { 
    dichVuThuongHieu, dichVuDanhMuc, dichVuXuatXu, 
    dichVuChatLieu, dichVuDeGiay, dichVuCoGiay, 
    dichVuMucDichChay 
} from '@/services/product/dichVuThuocTinh';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const saving = ref(false);

const confirmDialog = ref({
  show: false,
  title: '',
  message: '',
  color: 'primary',
  action: null,
  loading: false
});
const isEditMode = computed(() => !!route.params.id);

// DATA OPTIONS
const brands = ref([]);
const categories = ref([]);
const materials = ref([]);
const soles = ref([]);
const collars = ref([]);
const origins = ref([]);
const purposes = ref([]);
const colors = ref([]);
const sizes = ref([]);

// QUICK ADD STATE
const quickAdd = ref({
    show: false,
    type: '',
    title: '',
    service: null,
    targetField: ''
});

const openQuickAdd = (type, title, service, field) => {
    quickAdd.value = {
        show: true,
        type,
        title,
        service: {
            taoEntity: async (data) => {
                const res = await service[`tao${type.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join('')}`](data);
                return res;
            }
        },
        targetField: field
    };
};

const handleQuickAddSaved = async (newEntity) => {
    // Refresh options to get the new list
    const opts = await dichVuSanPham.layOptionsForm();
    brands.value = opts.thuongHieus || [];
    categories.value = opts.danhMucs || [];
    origins.value = opts.xuatXus || [];
    purposes.value = opts.mucDichChays || [];
    collars.value = opts.coGiays || [];
    materials.value = opts.chatLieus || [];
    soles.value = opts.deGiays || [];

    // Auto select the new entity
    product.value[quickAdd.value.targetField] = newEntity.id;
};

const autoCreateAttribute = async (val, field, service, type) => {
    if (typeof val !== 'string' || !val.trim()) return;
    
    // Found that it's a new string, auto create
    try {
        const payload = { ten: val, ma: '', moTa: `Tự động thêm từ sản phẩm` };
        const methodName = `tao${type.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join('')}`;
        const newEntity = await service[methodName](payload);
        
        // Refresh options
        const opts = await dichVuSanPham.layOptionsForm();
        brands.value = opts.thuongHieus || [];
        categories.value = opts.danhMucs || [];
        origins.value = opts.xuatXus || [];
        purposes.value = opts.mucDichChays || [];
        collars.value = opts.coGiays || [];
        materials.value = opts.chatLieus || [];
        soles.value = opts.deGiays || [];

        // Set the new ID
        product.value[field] = newEntity.id;
        addNotification({ title: 'Thành công', subtitle: `Đã tự động thêm ${val}`, color: 'success' });
    } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tự động thêm thuộc tính', color: 'error' });
    }
};

// FORM STATE
const product = ref({
    maSanPham: '',
    tenSanPham: '',
    moTa: '',
    idThuongHieu: null,
    idDanhMuc: null,
    idXuatXu: null,
    idChatLieu: null,
    idDeGiay: null,
    idCoGiay: null,
    idMucDichChay: null,
    gioiTinhKhachHang: 'UNISEX',
    trangThai: 'DANG_HOAT_DONG',
    moTaNgan: '',
    moTaChiTiet: ''
});

const selectedColors = ref([]);
const selectedSizes = ref([]);
// Removed variants from here to focus on product metadata

// INITIALIZE
const loadProduct = async (id) => {
    try {
        const data = await dichVuSanPham.layChiTietSanPham(id);
        product.value = { ...data };
        
        try {
            const vars = await dichVuSanPham.layBienTheSanPham(id);
            variants.value = (vars || []).map(v => ({
              ...v,
              soLuongTon: v.soLuong, 
              sku: v.maChiTietSanPham || v.sku
            }));
        } catch (variantError) {
            console.error('Error loading variants:', variantError);
        }
    } catch (error) {
        throw error;
    }
};

onMounted(async () => {
    loading.value = true;
    try {
        const opts = await dichVuSanPham.layOptionsForm();
        brands.value = opts.thuongHieus || [];
        categories.value = opts.danhMucs || [];
        origins.value = opts.xuatXus || [];
        purposes.value = opts.mucDichChays || [];
        collars.value = opts.coGiays || [];
        materials.value = opts.chatLieus || [];
        soles.value = opts.deGiays || [];
        colors.value = opts.mauSacs || [];
        sizes.value = opts.kichThuocs || [];

        if (route.params.id) {
            await loadProduct(route.params.id);
        }
    } catch (error) {
        console.error('Error initializing form:', error);
        addNotification({ title: 'Lỗi', subtitle: 'Không thể tải dữ liệu initialize', color: 'error' });
    } finally {
        loading.value = false;
    }
});

// Variant generation logic removed from this screen

const rules = {
    required: value => !!value || 'Trường này là bắt buộc',
    min0: value => value >= 0 || 'Giá trị không được nhỏ hơn 0',
    sku: value => !!value || 'SKU không được để trống'
};

const handleSave = () => {
  if (!product.value.tenSanPham || !product.value.idThuongHieu || !product.value.idDanhMuc) {
    addNotification({ title: 'Lỗi', subtitle: 'Vui lòng điền đủ thông tin bắt buộc', color: 'error' });
    return;
  }

  // Variants are managed in a separate screen

  confirmDialog.value = {
    show: true,
    title: isEditMode.value ? 'Xác nhận cập nhật' : 'Xác nhận thêm mới',
    message: isEditMode.value ? 'Bạn có chắc chắn muốn cập nhật thông tin sản phẩm này?' : 'Bạn có chắc chắn muốn thêm sản phẩm mới này?',
    color: 'success',
    action: async () => {
        confirmDialog.value.loading = true;
        try {
            const payload = {
                ...product.value,
                variants: [] // Variants handled elsewhere
            };

            if (isEditMode.value) {
                await dichVuSanPham.capNhatSanPham(route.params.id, payload);
                addNotification({ title: 'Thành công', subtitle: 'Cập nhật sản phẩm hoàn tất', color: 'success' });
            } else {
                await dichVuSanPham.taoSanPham(payload);
                addNotification({ title: 'Thành công', subtitle: 'Đã thêm sản phẩm mới', color: 'success' });
            }
            confirmDialog.value.show = false;
            router.push('/san-pham');
        } catch (error) {
            addNotification({ title: 'Lỗi', subtitle: 'Không thể lưu sản phẩm', color: 'error' });
        } finally {
            confirmDialog.value.loading = false;
        }
    }
  };
};
</script>

<template>
  <div class="pa-4 animate-fade-in min-h-screen">
    <!-- Header -->
    <div class="d-flex align-center justify-space-between mb-6">
        <div class="d-flex align-center gap-4">
            <v-btn icon variant="text" color="slate-600" @click="router.push(PATH.SAN_PHAM)" class="rounded-xl" style="background-color: transparent !important;">
                <ArrowLeftIcon size="20" />
            </v-btn>
            <div>
                <h1 class="text-h5 font-weight-bold text-slate-900 mb-1">
                    {{ isEditMode ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm mới' }}
                </h1>
                <p class="text-subtitle-2 text-slate-500 font-weight-medium">Thiết lập thông tin kho hàng và biến thể chi tiết.</p>
            </div>
        </div>
        <div class="d-flex gap-3">
             <v-btn
                v-if="isEditMode"
                variant="outlined"
                color="primary"
                class="text-none font-weight-bold px-6 rounded-xl h-11"
                @click="router.push({ name: 'BienTheSanPham', query: { productId: route.params.id } })"
            >
                <BoxIcon size="18" class="mr-2" /> Quản lý biến thể
            </v-btn>
            <v-btn
                color="primary"
                variant="flat"
                class="text-none font-weight-black px-8 rounded-xl h-11 shadow-lg"
                :loading="saving"
                @click="handleSave"
            >
                <DeviceFloppyIcon size="18" class="mr-2" /> Lưu thông tin
            </v-btn>
        </div>
    </div>

    <v-row v-if="loading">
      <v-col cols="12" class="text-center py-16">
        <v-progress-circular indeterminate color="primary" size="64" />
        <div class="mt-4 text-subtitle-1 font-weight-bold text-slate-500">Đang tải thông tin sản phẩm...</div>
      </v-col>
    </v-row>

    <v-row v-else>
      <!-- Main Info -->
      <v-col cols="12">
        <!-- General Info -->
        <v-card class="rounded-xl border shadow-none mb-6 overflow-hidden">
          <div class="px-6 py-4 bg-slate-50/50 border-b d-flex align-center">
            <InfoCircleIcon size="18" class="mr-2 text-primary" />
            <span class="font-weight-bold text-slate-700">Thông tin cơ bản</span>
          </div>
          <v-card-text class="pa-6">
            <v-row>
              <v-col cols="12" md="6">
                <div class="form-field-label">Mã sản phẩm <span class="text-caption text-primary font-italic">(Hệ thống tự tạo)</span></div>
                <v-text-field v-model="product.maSanPham" readonly placeholder="SP-XXXX" variant="outlined" density="comfortable" hide-details class="custom-input font-family-mono"></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <div class="form-field-label">Tên sản phẩm *</div>
                <v-text-field v-model="product.tenSanPham" placeholder="Nhập tên sản phẩm..." :rules="[rules.required]" variant="outlined" density="comfortable" hide-details="auto"></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <div class="form-field-label">Thương hiệu * <span class="text-caption font-weight-regular opacity-60">(Gõ mới để thêm nhanh)</span></div>
                <v-combobox 
                    v-model="product.idThuongHieu" 
                    :items="brands" 
                    item-title="ten" 
                    item-value="id" 
                    :rules="[rules.required]" 
                    placeholder="Tìm hoặc gõ tên mới..." 
                    variant="outlined" 
                    density="comfortable" 
                    hide-details="auto"
                    :return-object="false"
                    @update:model-value="(val) => autoCreateAttribute(val, 'idThuongHieu', dichVuThuongHieu, 'THUONG_HIEU')"
                ></v-combobox>
              </v-col>
              <v-col cols="12" md="6">
                <div class="form-field-label">Danh mục * <span class="text-caption font-weight-regular opacity-60">(Gõ mới để thêm nhanh)</span></div>
                <v-combobox 
                    v-model="product.idDanhMuc" 
                    :items="categories" 
                    item-title="ten" 
                    item-value="id" 
                    :rules="[rules.required]" 
                    placeholder="Tìm hoặc gõ tên mới..." 
                    variant="outlined" 
                    density="comfortable" 
                    hide-details="auto"
                    :return-object="false"
                    @update:model-value="(val) => autoCreateAttribute(val, 'idDanhMuc', dichVuDanhMuc, 'DANH_MUC')"
                ></v-combobox>
              </v-col>
              <v-col cols="12" md="6">
                <div class="form-field-label">Xuất xứ *</div>
                <v-combobox 
                    v-model="product.idXuatXu" 
                    :items="origins" 
                    item-title="ten" 
                    item-value="id" 
                    :rules="[rules.required]" 
                    placeholder="Chọn hoặc gõ mới..." 
                    variant="outlined" 
                    density="comfortable" 
                    hide-details="auto"
                    :return-object="false"
                    @update:model-value="(val) => autoCreateAttribute(val, 'idXuatXu', dichVuXuatXu, 'XUAT_XU')"
                ></v-combobox>
              </v-col>
              <v-col cols="12" md="6">
                <div class="form-field-label">Chất liệu *</div>
                <v-combobox 
                    v-model="product.idChatLieu" 
                    :items="materials" 
                    item-title="ten" 
                    item-value="id" 
                    :rules="[rules.required]" 
                    placeholder="Chọn hoặc gõ mới..." 
                    variant="outlined" 
                    density="comfortable" 
                    hide-details="auto"
                    :return-object="false"
                    @update:model-value="(val) => autoCreateAttribute(val, 'idChatLieu', dichVuChatLieu, 'CHAT_LIEU')"
                ></v-combobox>
              </v-col>
              <v-col cols="12" md="4">
                <div class="form-field-label">Loại đế *</div>
                <v-combobox 
                    v-model="product.idDeGiay" 
                    :items="soles" 
                    item-title="ten" 
                    item-value="id" 
                    :rules="[rules.required]" 
                    variant="outlined" 
                    density="comfortable" 
                    hide-details="auto"
                    :return-object="false"
                    @update:model-value="(val) => autoCreateAttribute(val, 'idDeGiay', dichVuDeGiay, 'DE_GIAY')"
                ></v-combobox>
              </v-col>
              <v-col cols="12" md="4">
                <div class="form-field-label">Loại cổ *</div>
                <v-combobox 
                    v-model="product.idCoGiay" 
                    :items="collars" 
                    item-title="ten" 
                    item-value="id" 
                    :rules="[rules.required]" 
                    variant="outlined" 
                    density="comfortable" 
                    hide-details="auto"
                    :return-object="false"
                    @update:model-value="(val) => autoCreateAttribute(val, 'idCoGiay', dichVuCoGiay, 'CO_GIAY')"
                ></v-combobox>
              </v-col>
              <v-col cols="12" md="4">
                <div class="form-field-label">Mục đích sử dụng *</div>
                <v-combobox 
                    v-model="product.idMucDichChay" 
                    :items="purposes" 
                    item-title="ten" 
                    item-value="id" 
                    :rules="[rules.required]" 
                    variant="outlined" 
                    density="comfortable" 
                    hide-details="auto"
                    :return-object="false"
                    @update:model-value="(val) => autoCreateAttribute(val, 'idMucDichChay', dichVuMucDichChay, 'MUC_DICH_CHAY')"
                ></v-combobox>
              </v-col>
              <v-col cols="12" md="4">
                <div class="form-field-label">Đối tượng sử dụng *</div>
                <v-select v-model="product.gioiTinhKhachHang" :items="[{title:'Nam',value:'NAM'},{title:'Nữ',value:'NU'},{title:'Unisex',value:'UNISEX'}]" variant="outlined" density="comfortable" hide-details></v-select>
              </v-col>
              <v-col cols="12">
                <div class="form-field-label">Mô tả ngắn</div>
                <v-textarea v-model="product.moTaNgan" variant="outlined" rows="2" placeholder="Tóm tắt đặc điểm nổi bật..." hide-details></v-textarea>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <!-- Variant Generator removed -->
      </v-col>

    </v-row>

    <!-- Confirmation Dialog -->
    <AdminConfirm
      v-model:show="confirmDialog.show"
      :title="confirmDialog.title"
      :message="confirmDialog.message"
      :color="confirmDialog.color"
      :loading="confirmDialog.loading"
      @confirm="confirmDialog.action"
    />

    <!-- Attribute Quick Add Modal -->
    <AttributeQuickAddModal
        v-model:show="quickAdd.show"
        :type="quickAdd.type"
        :title="quickAdd.title"
        :service="quickAdd.service"
        @saved="handleQuickAddSaved"
    />
  </div>
</template>

<style scoped>
.gray-bg { /* Removed background */ }

.form-field-label {
    font-size: 13px;
    font-weight: 700;
    color: #475569;
    margin-bottom: 6px;
    margin-left: 2px;
}

.image-upload-zone {
    transition: all 0.3s ease;
    background: #fdfdfe;
    border-color: #e2e8f0;
}
.image-upload-zone:hover {
    background: #f1f5f9;
    border-color: #1e3a8a;
}

:deep(.v-field) {
    border-radius: 12px !important;
}

:deep(.v-autocomplete :not(.v-field--focused).v-field__outline), 
:deep(.v-text-field :not(.v-field--focused).v-field__outline) {
    color: #e2e8f0 !important;
}

.font-family-mono { font-family: 'Fira Code', monospace; }

.custom-variant-table th {
    background: #f8fafc !important;
    font-size: 0.75rem !important;
}

.gap-1 { gap: 4px; }
.gap-3 { gap: 12px; }
.gap-4 { gap: 16px; }

:deep(.modern-chips .v-chip) {
    border-radius: 8px !important;
}
</style>



