<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import {
  dichVuThuongHieu, dichVuDanhMuc, dichVuMauSac, dichVuKichThuoc,
  dichVuChatLieu, dichVuDeGiay, dichVuCoGiay, dichVuXuatXu, dichVuMucDichChay
} from '@/services/product/dichVuThuocTinh';
import { useNotifications } from '@/services/notificationService';
import {
  ChevronLeftIcon, SaveIcon, PlusIcon, TrashIcon,
  PhotoIcon, InfoCircleIcon, BoxIcon, SettingsIcon
} from 'vue-tabler-icons';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const saving = ref(false);
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

// FORM STATE
const product = ref({
  tenSanPham: '',
  moTa: '',
  idThuongHieu: null,
  idDanhMuc: null,
  idChatLieu: null,
  idDeGiay: null,
  idCoGiay: null,
  idXuatXu: null,
  idMucDichChay: null,
  trangThai: 'DANG_HOAT_DONG',
  hinhAnhs: []
});

const selectedColors = ref([]);
const selectedSizes = ref([]);
const variants = ref([]);

// INITIALIZE
const init = async () => {
  loading.value = true;
  try {
    const [b, c, m, s, cl, o, p, col, sz] = await Promise.all([
      dichVuThuongHieu.layThuongHieu({ size: 1000 }),
      dichVuDanhMuc.layDanhMuc({ size: 1000 }),
      dichVuChatLieu.layChatLieu({ size: 1000 }),
      dichVuDeGiay.layDeGiay({ size: 1000 }),
      dichVuCoGiay.layCoGiay({ size: 1000 }),
      dichVuXuatXu.layXuatXu({ size: 1000 }),
      dichVuMucDichChay.layMucDichChay({ size: 1000 }),
      dichVuMauSac.layMauSac({ size: 1000 }),
      dichVuKichThuoc.layKichThuoc({ size: 1000 })
    ]);

    brands.value = b.content || [];
    categories.value = c.content || [];
    materials.value = m.content || [];
    soles.value = s.content || [];
    collars.value = cl.content || [];
    origins.value = o.content || [];
    purposes.value = p.content || [];
    colors.value = col.content || [];
    sizes.value = sz.content || [];

    if (isEditMode.value) {
      const data = await dichVuSanPham.layChiTietSanPham(route.params.id);
      product.value = { ...data };
      // Load variants if editing
      const vars = await dichVuSanPham.layBienTheSanPham(route.params.id);
      variants.value = vars || [];
    }
  } catch (error) {
    addNotification({ title: 'Lỗi', subtitle: 'Không thể tải dữ liệu', color: 'error' });
  } finally {
    loading.value = false;
  }
};

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
          giaBan: 0,
          soLuongTon: 0,
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
const handleSave = async () => {
  if (!product.value.tenSanPham || !product.value.idThuongHieu || !product.value.idDanhMuc) {
    addNotification({ title: 'Lỗi', subtitle: 'Vui lòng điền đủ thông tin bắt buộc', color: 'error' });
    return;
  }

  if (variants.value.length === 0) {
    addNotification({ title: 'Lỗi', subtitle: 'Vui lòng tạo ít nhất 1 biến thể', color: 'error' });
    return;
  }

  saving.value = true;
  try {
    const payload = {
      ...product.value,
      bienThes: variants.value
    };

    if (isEditMode.value) {
      await dichVuSanPham.capNhatSanPham(route.params.id, payload);
      addNotification({ title: 'Thành công', subtitle: 'Cập nhật sản phẩm hoàn tất', color: 'success' });
    } else {
      await dichVuSanPham.taoSanPham(payload);
      addNotification({ title: 'Thành công', subtitle: 'Đã thêm sản phẩm mới', color: 'success' });
    }
    router.push('/san-pham');
  } catch (error) {
    addNotification({ title: 'Lỗi', subtitle: 'Không thể lưu sản phẩm', color: 'error' });
  } finally {
    saving.value = false;
  }
};

onMounted(init);

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);

</script>

<template>
  <v-container fluid class="pa-6 bg-grey-lighten-4 min-h-screen">
    <!-- Header -->
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <v-btn icon variant="tonal" color="primary" class="mr-4 rounded-lg" @click="router.back()">
          <ChevronLeftIcon size="24" />
        </v-btn>
        <div>
          <h2 class="text-h4 font-weight-black text-slate-900">
            {{ isEditMode ? 'Chỉnh sửa sản phẩm' : 'Thêm sản phẩm mới' }}
          </h2>
          <div class="text-subtitle-1 text-slate-500">Cấu hình thông tin chi tiết và biến thể sản phẩm AeroStride</div>
        </div>
      </div>
      <div class="d-flex gap-3">
        <v-btn variant="outlined" color="grey" class="text-none font-weight-bold px-6 rounded-lg" @click="router.back()">Hủy bỏ</v-btn>
        <v-btn color="primary" class="text-none font-weight-bold px-8 rounded-lg" :loading="saving" prepend-icon="mdi-content-save" @click="handleSave">
          Lưu sản phẩm
        </v-btn>
      </div>
    </div>

    <v-row>
      <!-- Left Column: Info -->
      <v-col cols="12" lg="8">
        <!-- General Info -->
        <v-card class="rounded-xl border shadow-none mb-6">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <InfoCircleIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-black">Thông tin chung</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-row>
              <v-col cols="12">
                <v-text-field v-model="product.tenSanPham" label="Tên sản phẩm *" variant="outlined" placeholder="Ví dụ: Giày Chạy Bộ AeroStride Pro X1" hide-details class="mb-6"></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <v-select v-model="product.idThuongHieu" :items="brands" item-title="ten" item-value="id" label="Thương hiệu *" variant="outlined" hide-details class="mb-6"></v-select>
              </v-col>
              <v-col cols="12" md="6">
                <v-select v-model="product.idDanhMuc" :items="categories" item-title="ten" item-value="id" label="Danh mục *" variant="outlined" hide-details class="mb-6"></v-select>
              </v-col>
              <v-col cols="12">
                <v-textarea v-model="product.moTa" label="Mô tả sản phẩm" variant="outlined" rows="4" placeholder="Mô tả chi tiết về công nghệ, chất liệu..." hide-details></v-textarea>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <!-- Attributes -->
        <v-card class="rounded-xl border shadow-none mb-6">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <SettingsIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-black">Thuộc tính kỹ thuật</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-row>
              <v-col cols="12" md="4"><v-select v-model="product.idChatLieu" :items="materials" item-title="ten" item-value="id" label="Chất liệu" variant="outlined" density="comfortable"></v-select></v-col>
              <v-col cols="12" md="4"><v-select v-model="product.idDeGiay" :items="soles" item-title="ten" item-value="id" label="Đế giày" variant="outlined" density="comfortable"></v-select></v-col>
              <v-col cols="12" md="4"><v-select v-model="product.idCoGiay" :items="collars" item-title="ten" item-value="id" label="Cổ giày" variant="outlined" density="comfortable"></v-select></v-col>
              <v-col cols="12" md="4"><v-select v-model="product.idXuatXu" :items="origins" item-title="ten" item-value="id" label="Xuất xứ" variant="outlined" density="comfortable"></v-select></v-col>
              <v-col cols="12" md="4"><v-select v-model="product.idMucDichChay" :items="purposes" item-title="ten" item-value="id" label="Mục đích sử dụng" variant="outlined" density="comfortable"></v-select></v-col>
            </v-row>
          </v-card-text>
        </v-card>

        <!-- Variants -->
        <v-card class="rounded-xl border shadow-none">
          <v-card-title class="pa-5 border-b d-flex align-center justify-space-between">
            <div class="d-flex align-center">
              <BoxIcon size="20" class="mr-2 text-primary" />
              <span class="font-weight-black">Biến thể sản phẩm (Size & Màu sắc)</span>
            </div>
            <v-btn color="primary" variant="tonal" size="small" class="text-none font-weight-bold" @click="generateVariants">
              Làm mới biến thể
            </v-btn>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-row class="mb-6">
              <v-col cols="12" md="6">
                <v-select v-model="selectedColors" :items="colors" item-title="ten" item-value="id" label="Chọn màu sắc" multiple chips variant="outlined" class="mb-2"></v-select>
              </v-col>
              <v-col cols="12" md="6">
                <v-select v-model="selectedSizes" :items="sizes" item-title="ten" item-value="id" label="Chọn kích thước" multiple chips variant="outlined" class="mb-2"></v-select>
              </v-col>
            </v-row>

            <v-divider class="mb-6"></v-divider>

            <v-table v-if="variants.length > 0" class="variant-table">
              <thead>
                <tr>
                  <th class="text-left py-4">Sản phẩm</th>
                  <th class="text-center py-4" width="200">Giá bán</th>
                  <th class="text-center py-4" width="120">Số lượng</th>
                  <th class="text-center py-4" width="180">SKU</th>
                  <th class="text-center py-4"></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(v, idx) in variants" :key="idx">
                  <td>
                    <div class="font-weight-bold">{{ v.tenMauSac }} / {{ v.tenKichThuoc }}</div>
                  </td>
                  <td>
                    <v-text-field v-model.number="v.giaBan" type="number" suffix="₫" density="compact" hide-details variant="outlined"></v-text-field>
                  </td>
                  <td>
                    <v-text-field v-model.number="v.soLuongTon" type="number" density="compact" hide-details variant="outlined"></v-text-field>
                  </td>
                  <td>
                    <v-text-field v-model="v.sku" density="compact" hide-details variant="outlined"></v-text-field>
                  </td>
                  <td class="text-center">
                    <v-btn icon variant="text" color="error" size="small" @click="removeVariant(idx)">
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
            <span class="font-weight-black">Hình ảnh sản phẩm</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <div class="image-upload-zone d-flex flex-column align-center justify-center border-dashed border-2 rounded-lg pa-8 mb-4 cursor-pointer hover-bg-grey">
              <PhotoIcon size="48" class="text-grey mb-2" />
              <div class="text-subtitle-2 font-weight-bold">Tải hình ảnh lên</div>
              <div class="text-caption text-grey">Định dạng JPG, PNG. Tối đa 5MB.</div>
            </div>
            
            <div class="d-flex flex-wrap gap-2">
              <v-avatar v-for="(img, idx) in product.hinhAnhs" :key="idx" size="70" class="rounded-lg border position-relative">
                <v-img :src="img.url" cover></v-img>
                <v-btn icon size="x-small" color="error" class="position-absolute top-0 right-0" style="transform: translate(30%, -30%)" @click="product.hinhAnhs.splice(idx, 1)">
                  <v-icon size="10">mdi-close</v-icon>
                </v-btn>
              </v-avatar>
            </div>
          </v-card-text>
        </v-card>

        <!-- Status & Visibility -->
        <v-card class="rounded-xl border shadow-none">
          <v-card-title class="pa-5 border-b font-weight-black">Trạng thái hiển thị</v-card-title>
          <v-card-text class="pa-6">
            <v-radio-group v-model="product.trangThai" hide-details>
              <v-radio label="Đang kinh doanh" value="DANG_HOAT_DONG" color="success" class="mb-4">
                <template #label>
                  <div class="ml-2">
                    <div class="font-weight-bold text-success">Đang kinh doanh</div>
                    <div class="text-caption">Sản phẩm sẽ hiển thị trên cửa hàng và có thể bán.</div>
                  </div>
                </template>
              </v-radio>
              <v-radio label="Ngừng kinh doanh" value="KHONG_HOAT_DONG" color="error">
                <template #label>
                  <div class="ml-2">
                    <div class="font-weight-bold text-error">Ngừng kinh doanh</div>
                    <div class="text-caption">Sản phẩm sẽ bị ẩn và không thể đặt hàng.</div>
                  </div>
                </template>
              </v-radio>
            </v-radio-group>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
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
</style>
