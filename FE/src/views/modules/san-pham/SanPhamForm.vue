<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import { useNotifications } from '@/services/notificationService';
import {
  ChevronLeftIcon, DeviceFloppyIcon, PlusIcon, TrashIcon,
  PhotoIcon, InfoCircleIcon, BoxIcon, SettingsIcon, ArrowLeftIcon
} from 'vue-tabler-icons';
import AdminConfirm from '@/components/common/AdminConfirm.vue';

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
const isEditMode = computed(() => !!route.params.id && !route.path.includes('/detail'));
const isDetailView = computed(() => route.path.includes('/detail'));

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
  moTaChiTiet: '',
  hinhAnhs: []
});

const selectedColors = ref([]);
const selectedSizes = ref([]);
const variants = ref([]);

// INITIALIZE
const loadProduct = async (id) => {
  try {
    const data = await dichVuSanPham.layChiTietSanPham(id);
    product.value = { 
      ...data, 
      hinhAnhs: data.hinhAnhs || (data.hinhAnh ? [{ url: data.hinhAnh, isMain: true }] : []) 
    };
    
    // Load variants (combined from detail or separate fetch)
    let rawVariants = data.variants || [];
    if (rawVariants.length === 0) {
      try {
        const fallbackVars = await dichVuSanPham.layBienTheSanPham(id);
        if (fallbackVars) rawVariants = fallbackVars;
      } catch (e) {
        console.warn('Could not fetch variants from separate endpoint', e);
      }
    }

    variants.value = rawVariants.map(v => ({
      ...v,
      soLuongTon: v.soLuong, 
      sku: v.maChiTietSanPham 
    }));
  } catch (error) {
    throw error;
  }
};

onMounted(async () => {
  loading.value = true;
  try {
    const opts = await dichVuSanPham.layOptionsForm();
    
    // Map options to local refs
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

// VARIANT GENERATION
const generateVariants = () => {
  if (selectedColors.value.length === 0 || selectedSizes.value.length === 0) {
    addNotification({ title: 'Cảnh báo', subtitle: 'Vui lòng chọn ít nhất 1 màu và 1 size', color: 'warning' });
    return;
  }

  const newVariants = [];
  selectedColors.value.forEach(colorId => {
    const color = colors.value.find(c => c.id === colorId);
    selectedSizes.value.forEach(sizeId => {
      const size = sizes.value.find(s => s.id === sizeId);
      
      // Check if variant already exists (by name/id to avoid duplicates)
      const existing = variants.value.find(v => v.idMauSac === colorId && v.idKichThuoc === sizeId);
      
      if (!existing) {
          newVariants.push({
            idMauSac: colorId,
            tenMauSac: color.ten,
            idKichThuoc: sizeId,
            tenKichThuoc: size.ten,
            giaNhap: 0,
            giaBan: 0,
            soLuong: 0,
            soLuongTon: 0,
            maChiTietSanPham: `${(product.value.tenSanPham || 'PRO').substring(0, 3).toUpperCase()}-${color.ten.substring(0, 2).toUpperCase()}-${size.ten}`,
            sku: `${(product.value.tenSanPham || 'PRO').substring(0, 3).toUpperCase()}-${color.ten.substring(0, 2).toUpperCase()}-${size.ten}`
          });
      } else {
        newVariants.push(existing);
      }
    });
  });
  variants.value = newVariants;
};

const removeVariant = (index) => {
  variants.value.splice(index, 1);
};

// SAVE LOGIC
const handleSave = () => {
  if (!product.value.tenSanPham || !product.value.idThuongHieu || !product.value.idDanhMuc) {
    addNotification({ title: 'Lỗi', subtitle: 'Vui lòng điền đủ thông tin bắt buộc', color: 'error' });
    return;
  }

  if (variants.value.length === 0) {
    addNotification({ title: 'Lỗi', subtitle: 'Vui lòng tạo ít nhất 1 biến thể', color: 'error' });
    return;
  }

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
                hinhAnh: product.value.hinhAnhs.find(i => i.isMain)?.url || product.value.hinhAnhs[0]?.url || '',
                variants: variants.value.map(v => ({
                    ...v,
                    soLuong: v.soLuongTon,
                    maChiTietSanPham: v.sku
                }))
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


const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

</script>

<template>
  <v-container fluid class="pa-6 bg-grey-lighten-4 min-h-screen">
    <!-- Header -->
    <div class="d-flex align-center mb-6">
      <v-btn icon variant="tonal" color="secondary" class="mr-4 rounded-lg" @click="router.push('/san-pham')">
        <ArrowLeftIcon size="20" />
      </v-btn>
      <div>
        <h1 class="text-h4 font-weight-bold">
            {{ isDetailView ? 'Thông tin sản phẩm' : (isEditMode ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm mới') }}
        </h1>
        <p class="text-subtitle-1 text-medium-emphasis">
          {{ isDetailView ? 'Xem chi tiết thông số và biến thể hiện có' : 'Thiết lập thông tin kho hàng và biến thể chi tiết' }}
        </p>
      </div>
      <v-spacer></v-spacer>
      <div class="d-flex gap-2">
        <v-btn
          v-if="!isDetailView"
          color="primary"
          prepend-icon="mdi-content-save"
          class="text-none font-weight-bold px-8 rounded-lg"
          height="44"
          :loading="saving"
          @click="handleSave"
        >
          Lưu sản phẩm
        </v-btn>
        <v-btn
          v-if="isEditMode && !isDetailView"
          variant="tonal"
          color="secondary"
          class="text-none font-weight-bold px-6 rounded-lg"
          height="44"
          @click="router.push(`/san-pham/detail/${route.params.id}`)"
        >
          Xem chi tiết
        </v-btn>
        <v-btn
          v-if="isDetailView"
          color="primary"
          variant="elevated"
          prepend-icon="mdi-pencil"
          class="text-none font-weight-bold px-10 rounded-lg"
          height="44"
          @click="router.push(`/san-pham/form/${route.params.id}`)"
        >
          Chỉnh sửa ngay
        </v-btn>
      </div>
    </div>

    <v-row v-if="loading">
      <v-col cols="12" class="text-center py-16">
        <v-progress-circular indeterminate color="primary" size="64" />
        <div class="mt-4 text-h6 text-medium-emphasis">Đang tải thông tin sản phẩm...</div>
      </v-col>
    </v-row>

    <!-- DASHBOARD VIEW (DETAIL) -->
    <v-row v-else-if="isDetailView">
      <v-col cols="12" lg="4">
        <!-- Main Info Card -->
        <v-card class="rounded-xl border shadow-none mb-6">
          <div class="pa-6 text-center">
            <v-img 
              :src="(product.hinhAnhs || []).find(i => i.isMain)?.url || product.hinhAnhs?.[0]?.url || 'https://via.placeholder.com/300'" 
              height="280" 
              cover 
              class="rounded-xl mb-4 border"
            ></v-img>
            <h2 class="text-h5 font-weight-black mb-2">{{ product.tenSanPham }}</h2>
            <v-chip 
              :color="product.trangThai === 'DANG_HOAT_DONG' ? 'success' : 'error'" 
              variant="flat" 
              class="mb-4 px-6 font-weight-bold"
            >
              {{ product.trangThai === 'DANG_HOAT_DONG' ? 'Đang kinh doanh' : 'Ngừng kinh doanh' }}
            </v-chip>
            
            <v-divider class="mb-4"></v-divider>
            
            <div class="d-flex justify-space-around text-center">
              <div>
                <div class="text-h6 font-weight-black text-primary">{{ variants.length }}</div>
                <div class="text-caption text-uppercase font-weight-bold text-medium-emphasis">Biến thể</div>
              </div>
              <v-divider vertical inset></v-divider>
              <div>
                <div class="text-h6 font-weight-black text-info">{{ variants.reduce((acc, v) => acc + (v.soLuongTon || 0), 0) }}</div>
                <div class="text-caption text-uppercase font-weight-bold text-medium-emphasis">Tổng kho</div>
              </div>
            </div>
          </div>
        </v-card>

        <!-- Technical Attributes Card -->
        <v-card class="rounded-xl border shadow-none">
          <v-card-title class="pa-5 border-b font-weight-bold text-subtitle-1">Cấu hình kỹ thuật</v-card-title>
          <v-card-text class="pa-6">
            <div class="attribute-list">
              <div v-for="(val, label) in {
                'Thương hiệu': product.tenThuongHieu,
                'Danh mục': product.tenDanhMuc,
                'Xuất xứ': product.tenXuatXu,
                'Chất liệu': product.tenChatLieu,
                'Đế giày': product.tenDeGiay,
                'Cổ giày': product.tenCoGiay,
                'Mục đích': product.tenMucDichChay,
                'Đối tượng': product.gioiTinhKhachHang
              }" :key="label" class="d-flex justify-space-between align-center mb-4 pb-2 border-b-dashed">
                <span class="text-subtitle-2 text-medium-emphasis font-weight-medium">{{ label }}</span>
                <span class="text-subtitle-2 font-weight-black text-dark">{{ val || 'Chưa cập nhật' }}</span>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" lg="8">
        <!-- Description & Details -->
        <v-card class="rounded-xl border shadow-none mb-6">
          <v-card-title class="pa-5 border-b font-weight-bold text-subtitle-1">Mô tả sản phẩm</v-card-title>
          <v-card-text class="pa-6">
            <div class="mb-6">
              <h4 class="text-subtitle-1 font-weight-black mb-2 text-primary">Tóm tắt sản phẩm</h4>
              <p class="text-body-1 line-height-relaxed">{{ product.moTaNgan || 'Không có mô tả ngắn.' }}</p>
            </div>
            <div>
              <h4 class="text-subtitle-1 font-weight-black mb-2 text-primary">Mô tả chi tiết</h4>
              <div class="text-body-1 text-justify whitespace-pre-line">{{ product.moTaChiTiet || 'Chưa có thông tin mô tả chi tiết cho sản phẩm này.' }}</div>
            </div>
          </v-card-text>
        </v-card>

        <!-- Variants Gallery Table -->
        <v-card class="rounded-xl border shadow-none">
          <v-card-title class="pa-5 border-b d-flex align-center justify-space-between">
             <span class="font-weight-bold text-subtitle-1">Danh sách biến thể hiện có</span>
             <v-chip color="info" size="small" variant="tonal" class="font-weight-bold">Tổng {{ variants.length }} phân loại</v-chip>
          </v-card-title>
          <v-card-text class="pa-0">
            <v-table class="variant-detail-table">
               <thead>
                 <tr>
                    <th class="text-left">Phân loại</th>
                    <th class="text-right">Giá nhập</th>
                    <th class="text-right">Giá bán</th>
                    <th class="text-center">Số lượng</th>
                    <th class="text-center">Mã SKU</th>
                 </tr>
               </thead>
               <tbody>
                 <tr v-for="v in variants" :key="v.id">
                    <td class="py-4">
                       <div class="d-flex align-center">
                          <v-avatar color="grey-lighten-4" size="40" class="mr-3 rounded-lg border">
                             <v-img :src="product.hinhAnhs.find(i => i.isMain)?.url || product.hinhAnhs[0]?.url"></v-img>
                          </v-avatar>
                          <div>
                             <div class="font-weight-black text-dark">{{ v.tenMauSac }}</div>
                             <div class="text-caption text-primary font-weight-black">Size: {{ v.tenKichThuoc }}</div>
                          </div>
                       </div>
                    </td>
                    <td class="text-right font-weight-bold text-grey">{{ formatCurrency(v.giaNhap) }}</td>
                    <td class="text-right font-weight-black text-error">{{ formatCurrency(v.giaBan) }}</td>
                     <td class="text-center">
                        <v-chip :color="(v.soLuong || v.soLuongTon) > 10 ? 'success' : 'orange'" size="x-small" variant="flat" class="font-weight-bold">
                           {{ v.soLuong || v.soLuongTon || 0 }} SP
                        </v-chip>
                     </td>
                     <td class="text-center text-caption font-family-mono">{{ v.maChiTietSanPham || v.sku }}</td>
                 </tr>
               </tbody>
            </v-table>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-row v-else>
      <!-- Left Column: Info -->
      <v-col cols="12" lg="8">
        <!-- General Info -->
        <v-card class="rounded-xl border shadow-none mb-6">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <InfoCircleIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-medium">Thông tin chung</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-row>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Mã sản phẩm <span class="text-caption text-primary">(Hệ thống tự tạo)</span></div>
                <v-text-field v-model="product.maSanPham" readonly placeholder="Hệ thống tự tạo..." variant="outlined" density="comfortable" hide-details class="mb-4 font-weight-bold"></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Tên sản phẩm *</div>
                <v-text-field v-model="product.tenSanPham" :readonly="isDetailView" placeholder="Nhập tên sản phẩm..." variant="outlined" density="comfortable" hide-details class="mb-4"></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Thương hiệu *</div>
                <v-autocomplete v-model="product.idThuongHieu" :readonly="isDetailView" :items="brands" item-title="ten" item-value="id" placeholder="Tìm kiếm thương hiệu..." variant="outlined" density="comfortable" hide-details class="mb-4"></v-autocomplete>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Danh mục *</div>
                <v-autocomplete v-model="product.idDanhMuc" :readonly="isDetailView" :items="categories" item-title="ten" item-value="id" placeholder="Tìm kiếm danh mục..." variant="outlined" density="comfortable" hide-details class="mb-4"></v-autocomplete>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Xuất xứ *</div>
                <v-autocomplete v-model="product.idXuatXu" :readonly="isDetailView" :items="origins" item-title="ten" item-value="id" placeholder="Chọn quốc gia..." variant="outlined" density="comfortable" hide-details class="mb-4"></v-autocomplete>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Chất liệu *</div>
                <v-autocomplete v-model="product.idChatLieu" :readonly="isDetailView" :items="materials" item-title="ten" item-value="id" placeholder="Nhập chất liệu..." variant="outlined" density="comfortable" hide-details class="mb-4"></v-autocomplete>
              </v-col>
              <v-col cols="12" md="4">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Loại đế *</div>
                <v-autocomplete v-model="product.idDeGiay" :readonly="isDetailView" :items="soles" item-title="ten" item-value="id" variant="outlined" density="comfortable" hide-details></v-autocomplete>
              </v-col>
              <v-col cols="12" md="4">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Loại cổ *</div>
                <v-autocomplete v-model="product.idCoGiay" :readonly="isDetailView" :items="collars" item-title="ten" item-value="id" variant="outlined" density="comfortable" hide-details></v-autocomplete>
              </v-col>
              <v-col cols="12" md="4">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Mục đích sử dụng *</div>
                <v-autocomplete v-model="product.idMucDichChay" :readonly="isDetailView" :items="purposes" item-title="ten" item-value="id" placeholder="Chọn mục đích..." variant="outlined" density="comfortable" hide-details></v-autocomplete>
              </v-col>
              <v-col cols="12" md="4">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Đối tượng sử dụng *</div>
                <v-select v-model="product.gioiTinhKhachHang" :readonly="isDetailView" :items="[{title:'Nam',value:'NAM'},{title:'Nữ',value:'NU'},{title:'Unisex',value:'UNISEX'}]" variant="outlined" density="comfortable" hide-details></v-select>
              </v-col>
              <v-col cols="12">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Mô tả ngắn</div>
                <v-textarea v-model="product.moTaNgan" :readonly="isDetailView" variant="outlined" rows="2" placeholder="Tóm tắt đặc điểm nổi bật..." hide-details></v-textarea>
              </v-col>
              <v-col cols="12">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Mô tả chi tiết</div>
                <v-textarea v-model="product.moTaChiTiet" :readonly="isDetailView" variant="outlined" rows="5" placeholder="Thông tin kỹ thuật, chất liệu, công nghệ..." hide-details></v-textarea>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <!-- Variant Generator -->
        <v-card v-if="!isEditMode && !isDetailView" class="rounded-xl border shadow-none mb-6">
          <v-card-title class="pa-5 border-b d-flex align-center justify-space-between">
            <div class="d-flex align-center">
              <BoxIcon size="20" class="mr-2 text-primary" />
              <span class="font-weight-medium">Biến thể sản phẩm (Size & Màu sắc)</span>
            </div>
            <v-btn color="primary" variant="tonal"  class="text-none font-weight-bold" @click="generateVariants">
              Làm mới biến thể
            </v-btn>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-row class="mb-6">
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Màu sắc</div>
                <v-select v-model="selectedColors" :items="colors" item-title="ten" item-value="id" multiple chips variant="outlined" density="comfortable" hide-details></v-select>
              </v-col>
              <v-col cols="12" md="6">
                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Kích cỡ</div>
                <v-select v-model="selectedSizes" :items="sizes" item-title="ten" item-value="id" multiple chips variant="outlined" density="comfortable" hide-details></v-select>
              </v-col>
            </v-row>

            <v-divider class="mb-6"></v-divider>

            <v-table v-if="variants.length > 0" class="variant-table">
              <thead>
                <tr>
                  <th class="text-left py-4">Sản phẩm</th>
                  <th class="text-center py-4" width="150">Giá nhập</th>
                  <th class="text-center py-4" width="150">Giá bán</th>
                  <th class="text-center py-4" width="120">Số lượng</th>
                  <th class="text-center py-4" width="180">SKU</th>
                  <th class="text-center py-4" width="80"></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(variant, idx) in variants" :key="idx">
                  <td class="pa-4">
                    <div class="d-flex align-center">
                      <v-chip size="small" variant="flat" class="mr-2" style="border: 1px solid #ddd">{{ variant.tenMauSac }}</v-chip>
                      <v-chip size="small" variant="outlined">{{ variant.tenKichThuoc }}</v-chip>
                    </div>
                  </td>
                  <td class="pa-4">
                    <v-text-field v-model="variant.giaNhap" :readonly="isDetailView" type="number" prefix="₫" variant="underlined" density="compact" hide-details></v-text-field>
                  </td>
                  <td class="pa-4">
                    <v-text-field v-model="variant.giaBan" :readonly="isDetailView" type="number" prefix="₫" variant="underlined" density="compact" hide-details></v-text-field>
                  </td>
                  <td class="pa-4">
                    <v-text-field v-model="variant.soLuongTon" :readonly="isDetailView" type="number" variant="underlined" density="compact" hide-details></v-text-field>
                  </td>
                  <td class="pa-4">
                    <v-text-field v-model="variant.sku" :readonly="isDetailView" variant="underlined" density="compact" hide-details></v-text-field>
                  </td>
                  <td v-if="!isDetailView" class="pa-4 text-center">
                    <v-btn icon variant="text" color="error" size="small" @click="variants.splice(idx, 1)">
                      <TrashIcon size="18" />
                    </v-btn>
                  </td>
                </tr>
              </tbody>
            </v-table>
            <div v-else class="text-center py-10 text-grey">
              <BoxIcon size="48" class="mb-3 opacity-20" />
              <p>Chưa có biến thể nào được tạo. Chọn màu sắc và kích thước để bắt đầu.</p>
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Right Column: Images & Status -->
      <v-col cols="12" lg="4">
        <!-- Media -->
        <v-card class="rounded-xl border shadow-none mb-6">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <PhotoIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-medium">Hình ảnh sản phẩm</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <div class="image-upload-zone d-flex flex-column align-center justify-center border-dashed border-2 rounded-lg pa-8 mb-4 cursor-pointer hover-bg-grey">
              <PhotoIcon size="48" class="text-grey mb-2" />
              <div class="text-subtitle-2 font-weight-bold">Tải hình ảnh lên</div>
              <div class="text-caption text-grey">Định dạng JPG, PNG. Tối đa 5MB.</div>
            </div>
            
            <div class="d-flex flex-wrap gap-2">
              <v-avatar v-for="(img, idx) in product.hinhAnhs" :key="idx" size="70" class="rounded-lg border position-relative overflow-visible">
                <v-img :src="img.url" cover></v-img>
                <v-chip v-if="img.isMain" size="x-small" color="primary" variant="flat" class="position-absolute" style="top: -8px; left: -8px; font-size: 8px; height: 16px; padding: 0 4px; z-index: 10">Chính</v-chip>
                <div class="position-absolute d-flex align-center justify-center rounded-lg" style="top:0; left:0; width:100%; height:100%; background: rgba(0,0,0,0.3); opacity: 0; transition: 0.2s" @mouseover="$el.style.opacity=1" @mouseleave="$el.style.opacity=0">
                   <v-btn icon size="x-small" color="white" variant="text" @click="img.isMain = true; product.hinhAnhs.forEach((it, i) => { if(i !== idx) it.isMain = false })">
                    <v-icon size="14">mdi-star</v-icon>
                  </v-btn>
                </div>
                <v-btn icon color="error" size="x-small" class="position-absolute" style="top: -10px; right: -10px; width: 20px; height: 20px;" @click="product.hinhAnhs.splice(idx, 1)">
                  <v-icon size="12">mdi-close</v-icon>
                </v-btn>
              </v-avatar>
            </div>
          </v-card-text>
        </v-card>


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
  </v-container>
</template>

<style scoped>
.image-upload-zone {
  transition: all 0.2s ease;
  background: #f8fafc;
}
.image-upload-zone:hover {
  background: #f1f5f9;
  border-color: rgb(var(--v-theme-primary));
}
.gap-3 { gap: 12px; }
.gap-2 { gap: 8px; }
.variant-table :deep(th) {
  background: #f8fafc !important;
  font-weight: 700 !important;
  text-transform: uppercase;
  font-size: 0.75rem;
  letter-spacing: 0.05em;
  color: #64748b;
}
.variant-detail-table :deep(th) {
  background: #f8fafc !important;
  font-weight: 700 !important;
  font-size: 0.75rem;
  letter-spacing: 0.05em;
  color: #64748b;
  padding: 16px !important;
}
.border-b-dashed { border-bottom: 1px dashed #e2e8f0; }
.line-height-relaxed { line-height: 1.6; }
.font-family-mono { font-family: monospace; }
</style>
