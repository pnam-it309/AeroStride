<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeftIcon,
  PhotoIcon,
  PencilIcon,
  PlusIcon,
  RefreshIcon,
  SearchIcon,
  PackageIcon,
  QrcodeIcon
} from 'vue-tabler-icons'
import AdminFilter from '@/components/common/AdminFilter.vue'
import AdminTable from '@/components/common/AdminTable.vue'
import AdminPagination from '@/components/common/AdminPagination.vue'
import AdminConfirm from '@/components/common/AdminConfirm.vue'
import { useNotifications } from '@/services/notificationService'
import { dichVuSanPham } from '@/services/product/dichVuSanPham'
import { dichVuBienThe } from '@/services/product/dichVuBienThe'
import VariantFormModal from './components/VariantFormModal.vue'
import VariantImageModal from './components/VariantImageModal.vue'
import VariantManagementDrawer from './components/VariantManagementDrawer.vue'
import QrScanner from './components/QrScanner.vue'
import QrcodeVue from 'qrcode.vue'

const router = useRouter()
const route = useRoute()
const { addNotification } = useNotifications()

const productOptions = ref([])
const formOptions = ref({
  mauSacs: [],
  kichThuocs: [],
  trangThais: [],
})
const selectedProductId = ref('')
const selectedProduct = ref(null)
const loading = ref(false)

const filters = reactive({
  keyword: '',
  mauSacId: '',
  kichThuocId: '',
  trangThai: '',
})

const pagination = reactive({
  page: 1,
  size: 10,
})

const variantModal = reactive({ open: false, mode: 'create', submitting: false, variant: null })
const imageModal = reactive({ open: false, mode: 'create', submitting: false, image: null, variantId: '' })
const variantDrawer = reactive({ open: false, variant: null })
const qrDialog = reactive({ open: false, value: '' })
const showQrScanner = ref(false)

// State cho xác nhận
const confirmDialog = ref({
  show: false,
  title: '',
  message: '',
  color: 'primary',
  action: null,
  loading: false
})

const numberFormatter = new Intl.NumberFormat('vi-VN')

const resetFilters = () => {
  Object.assign(filters, {
    keyword: '',
    mauSacId: '',
    kichThuocId: '',
    trangThai: '',
  })
  pagination.page = 1
}

const formatCurrency = (value) => {
  if (value === null || value === undefined) return '--'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(Number(value))
}

const formatNumber = (value) => {
  if (value === null || value === undefined) return '--'
  return numberFormatter.format(Number(value))
}

const getStatusLabel = (status) => {
  if (status === 'DANG_HOAT_DONG') return 'Đang hoạt động'
  if (status === 'KHONG_HOAT_DONG') return 'Ngừng bán'
  return status || 'Không xác định'
}

const isActiveStatus = (status) => status === 'DANG_HOAT_DONG';

const selectedProductSummary = computed(() => {
  if (!selectedProduct.value) {
    return { title: 'Chưa chọn sản phẩm', subtitle: 'Chọn sản phẩm để quản lý biến thể', maSanPham: '' }
  }
  return {
    title: selectedProduct.value.tenSanPham,
    subtitle: `${selectedProduct.value.maSanPham} • ${formatNumber(selectedProduct.value.variants?.length || 0)} biến thể`,
    maSanPham: selectedProduct.value.maSanPham || '',
  }
})

const filteredVariants = computed(() => {
  const variants = selectedProduct.value?.variants || []
  return variants.filter((variant) => {
    const keyword = filters.keyword.trim().toLowerCase()
    const matchesKeyword = !keyword || variant.maChiTietSanPham?.toLowerCase().includes(keyword)
    return matchesKeyword && (!filters.mauSacId || variant.idMauSac === filters.mauSacId) &&
      (!filters.kichThuocId || variant.idKichThuoc === filters.kichThuocId) &&
      (!filters.trangThai || variant.trangThai === filters.trangThai)
  })
})

const totalPages = computed(() => Math.max(Math.ceil(filteredVariants.value.length / pagination.size), 1))
const paginatedVariants = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return filteredVariants.value.slice(start, start + pagination.size)
})

const fetchFormOptions = async () => {
  try {
    const response = await dichVuSanPham.layOptionsForm()
    formOptions.value = {
      mauSacs: response.mauSacs || [],
      kichThuocs: response.kichThuocs || [],
      trangThais: response.trangThais || [],
    }
  } catch (error) { }
}

const fetchProductOptions = async () => {
  try {
    const response = await dichVuSanPham.layDanhSachSanPham({ page: 0, size: 100 })
    productOptions.value = response.content || []
  } catch (error) { }
}

const fetchSelectedProduct = async (productId) => {
  if (!productId) { selectedProduct.value = null; return; }
  loading.value = true
  try {
    selectedProduct.value = await dichVuSanPham.layChiTietSanPham(productId)
  } catch (error) {
    selectedProduct.value = null
  } finally {
    loading.value = false
  }
}

const handleVariantSubmit = (payload) => {
  confirmDialog.value = {
    show: true,
    title: variantModal.mode === 'create' ? 'Xác nhận thêm' : 'Xác nhận cập nhật',
    message: `Bạn có chắc chắn muốn ${variantModal.mode === 'create' ? 'thêm mới' : 'cập nhật'} biến thể này không?`,
    color: 'primary',
    action: async () => {
      confirmDialog.value.loading = true;
      variantModal.submitting = true
      try {
        if (variantModal.mode === 'create') {
          await dichVuBienThe.taoBienThe(selectedProductId.value, payload)
          addNotification({ title: 'Thành công', subtitle: 'Đã thêm biến thể mới', color: 'success' });
        } else {
          await dichVuBienThe.capNhatBienThe(variantModal.variant.id, payload)
          addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật biến thể', color: 'success' });
        }
        variantModal.open = false
        confirmDialog.value.show = false;
        await fetchSelectedProduct(selectedProductId.value)
      } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Thao tác thất bại', color: 'error' });
      } finally {
        variantModal.submitting = false
        confirmDialog.value.loading = false;
      }
    }
  };
}

const handleStatusChange = (item) => {
  const newStatus = item.trangThai === 'DANG_HOAT_DONG' ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
  confirmDialog.value = {
    show: true,
    title: 'Xác nhận đổi trạng thái',
    message: `Bạn có chắc chắn muốn ${newStatus === 'DANG_HOAT_DONG' ? 'kích hoạt' : 'ngừng kinh doanh'} biến thể này không?`,
    color: 'warning',
    action: async () => {
      confirmDialog.value.loading = true;
      try {
        await dichVuBienThe.capNhatBienThe(item.id, { ...item, trangThai: newStatus });
        addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật trạng thái', color: 'success' });
        await fetchSelectedProduct(selectedProductId.value);
        confirmDialog.value.show = false;
      } catch (error) {
        addNotification({ title: 'Lỗi', subtitle: 'Không thể cập nhật trạng thái', color: 'error' });
      } finally {
        confirmDialog.value.loading = false;
      }
    }
  };
}

const openCreateVariantModal = () => {
  variantModal.variant = null
  variantModal.mode = 'create'
  variantModal.open = true
}

const handleQrScan = (code) => {
  if (!code) return
  filters.keyword = code
  addNotification({ title: 'Quét mã thành công', subtitle: `Mã: ${code}`, color: 'success' })
}

const getVariantThumbnail = (item) => {
  if (item?.urlAnh) return item.urlAnh
  if (item?.hinhAnh && item.hinhAnh.length > 0) return item.hinhAnh[0].url
  return '/placeholder-product.png'
}

onMounted(async () => {
  await Promise.all([fetchFormOptions(), fetchProductOptions()])
  const routeProductId = route.query.productId?.toString()
  selectedProductId.value = routeProductId || (productOptions.value.length > 0 ? productOptions.value[0].id : '')
})

watch(() => selectedProductId.value, (val) => fetchSelectedProduct(val))
</script>

<template>
  <div class="pa-4 animate-fade-in d-flex flex-column overflow-hidden" style="height: calc(100vh - 20px);">
    <!-- Header -->
    <div class="flex-none mb-3">
      <div class="d-flex justify-space-between align-center">
        <div class="d-flex align-center gap-2">
          <v-btn icon variant="text" color="slate-600" @click="router.push({ name: 'SanPham' })" class="rounded-xl"
            size="small">
            <ArrowLeftIcon size="18" />
          </v-btn>
          <h1 class="text-h5 font-weight-bold text-slate-900 mb-0">Quản Lý Biến Thể</h1>
        </div>
        <div class="rounded-lg border border-slate-200 bg-white px-3 py-1 shadow-sm d-flex align-center bg-slate-50">
          <div class="d-flex align-center gap-2">
            <v-avatar color="primary" variant="tonal" size="28" rounded="lg">
              <PackageIcon size="14" />
            </v-avatar>
            <div>
              <p class="text-overline font-weight-bold text-slate-400 mb-0" style="font-size: 8px !important;">Sản phẩm
                đang chọn</p>
              <h2 class="text-caption font-weight-bold text-primary mb-0">{{ selectedProductSummary.title }}</h2>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Filter -->
    <div class="flex-none mb-4">
      <AdminFilter title="Bộ lọc biến thể" @refresh="resetFilters" :loading="loading" class="compact-filter-card">
        <v-col cols="12" md="2" class="py-1">
          <div class="filter-label">Sản phẩm</div>
          <v-autocomplete v-model="selectedProductId" :items="productOptions" item-title="tenSanPham" item-value="id"
            variant="outlined" density="compact" hide-details class="compact-input"></v-autocomplete>
        </v-col>
        <v-col cols="12" md="3" class="py-1">
          <div class="filter-label">Tìm kiếm nhanh</div>
          <v-text-field v-model="filters.keyword" placeholder="Mã SKU, màu, size..." prepend-inner-icon="mdi-magnify"
            variant="outlined" density="compact" hide-details clearable class="compact-input"></v-text-field>
        </v-col>
        <v-col cols="12" md="2" class="py-1">
          <div class="filter-label">Màu sắc</div>
          <v-select v-model="filters.mauSacId"
            :items="[{ title: 'Tất cả', value: '' }, ...formOptions.mauSacs.map(m => ({ title: m.ten, value: m.id }))]"
            variant="outlined" density="compact" hide-details class="compact-input"></v-select>
        </v-col>
        <v-col cols="12" md="2" class="py-1">
          <div class="filter-label">Kích thước</div>
          <v-select v-model="filters.kichThuocId"
            :items="[{ title: 'Tất cả', value: '' }, ...formOptions.kichThuocs.map(k => ({ title: k.ten, value: k.id }))]"
            variant="outlined" density="compact" hide-details class="compact-input"></v-select>
        </v-col>
        <v-col cols="12" md="2" class="py-1">
          <div class="filter-label">Trạng thái</div>
          <v-select v-model="filters.trangThai"
            :items="[{ title: 'Tất cả', value: '' }, ...formOptions.trangThais.map(s => ({ title: getStatusLabel(s), value: s }))]"
            variant="outlined" density="compact" hide-details class="compact-input"></v-select>
        </v-col>
      </AdminFilter>
    </div>

    <div class="flex-grow-1 min-h-0">
      <AdminTable title="Danh sách biến thể" :headers="[
        { text: 'STT', align: 'center', width: '50px' },
        { text: 'Ảnh', align: 'center', width: '70px' },
        { text: 'Mã SKU', align: 'center', width: '150px' },
        { text: 'Màu sắc', align: 'center', width: '100px' },
        { text: 'Kích thước', align: 'center', width: '80px' },
        { text: 'Giá bán', align: 'center', width: '130px' },
        { text: 'Trạng thái', align: 'center', width: '120px' },
        { text: 'Thao tác', align: 'center', width: '130px' }
      ]" :items="paginatedVariants" :loading="loading" :showAddButton="!!selectedProductId"
        addButtonText="Thêm biến thể" @add="openCreateVariantModal" class="h-100">
        <template #top>
          <div class="px-4 py-1 bg-slate-50 border-b d-flex align-center justify-space-between">
            <span class="text-caption font-weight-bold text-slate-500">Hiển thị {{ paginatedVariants.length }} kết
              quả</span>
            <v-btn size="x-small" variant="text" color="primary" @click="fetchSelectedProduct(selectedProductId)"
              :loading="loading">
              <RefreshIcon size="12" class="mr-1" /> Tải lại
            </v-btn>
          </div>
        </template>

        <template #row="{ item, index }">
          <tr class="data-row">
            <td class="data-cell text-center">{{ (pagination.page - 1) * pagination.size + index + 1 }}</td>
            <td class="data-cell text-center">
              <v-avatar rounded="lg" size="36" class="border bg-slate-50 shadow-sm"><v-img
                  :src="getVariantThumbnail(item)" cover></v-img></v-avatar>
            </td>
            <td class="data-cell text-center font-weight-bold text-slate-900">{{ item.maChiTietSanPham }}</td>
            <td class="data-cell text-center"><v-chip size="x-small" variant="tonal" class="font-weight-bold">{{
              item.tenMauSac }}</v-chip></td>
            <td class="data-cell text-center"><v-chip size="x-small" variant="flat" color="slate-100"
                class="font-weight-black">{{ item.tenKichThuoc }}</v-chip></td>
            <td class="data-cell text-center font-weight-black text-primary">{{ formatCurrency(item.giaBan) }}</td>
            <td class="data-cell text-center">
              <v-chip size="x-small" :color="isActiveStatus(item.trangThai) ? 'success' : 'warning'" variant="tonal"
                class="font-weight-bold">
                {{ getStatusLabel(item.trangThai) }}
              </v-chip>
            </td>
            <td class="data-cell text-center">
              <div class="d-flex align-center justify-center action-controls">
                <v-btn icon size="28" variant="text" color="slate-700"
                  @click="variantModal.variant = item; variantModal.mode = 'edit'; variantModal.open = true"
                  class="action-icon-btn">
                  <PencilIcon size="15" />
                  <v-tooltip activator="parent" location="top">Sửa</v-tooltip>
                </v-btn>
                <div class="switch-wrapper d-flex align-center">
                  <v-switch :model-value="isActiveStatus(item.trangThai)" color="primary" hide-details density="compact"
                    class="action-switch-compact ms-0" @click.prevent.stop="handleStatusChange(item)" />
                  <v-tooltip activator="parent" location="top">Trạng thái</v-tooltip>
                </div>
              </div>
            </td>
          </tr>
        </template>

        <template #pagination>
          <AdminPagination v-model="pagination.page" :page-size="pagination.size" :total-pages="totalPages"
            :total-elements="filteredVariants.length" :current-size="paginatedVariants.length" />
        </template>
      </AdminTable>
    </div>

    <!-- Confirmation Dialog -->
    <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
      :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />

    <VariantFormModal :open="variantModal.open" :mode="variantModal.mode" :variant="variantModal.variant"
      :options="formOptions" :submitting="variantModal.submitting" @close="variantModal.open = false"
      @submit="handleVariantSubmit" />
    <VariantManagementDrawer v-model:show="variantDrawer.open" :variant="variantDrawer.variant"
      @saved="fetchSelectedProduct(selectedProductId)" />
    <QrScanner v-model:show="showQrScanner" @scan="handleQrScan" />
  </div>
</template>

<style scoped>
.flex-none {
  flex: none;
}

.flex-grow-1 {
  flex: 1;
}

.min-h-0 {
  min-height: 0;
}

.filter-label {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 2px;
}

:deep(.compact-input .v-field__input) {
  min-height: 36px !important;
  padding: 0 12px !important;
  font-size: 13px;
}

:deep(.compact-input .v-field) {
  border-radius: 8px !important;
}

:deep(.data-cell) {
  padding: 10px 8px !important;
  font-size: 13px !important;
  font-weight: 600 !important;
  text-align: center !important;
}

:deep(.header-cell) {
  padding: 12px 8px !important;
  font-size: 13px !important;
  font-weight: 700 !important;
  text-align: center !important;
}

.action-switch-compact {
  display: inline-flex;
  margin: 0 !important;
}

:deep(.v-selection-control__wrapper) {
  width: 32px !important;
  height: 18px !important;
  margin: 0 !important;
}

:deep(.action-switch-compact .v-switch__track) {
  background: #ffffff !important;
  border: 1px solid #cbd5e1 !important;
  opacity: 1 !important;
  height: 18px !important;
  width: 32px !important;
  border-radius: 99px !important;
}

:deep(.action-switch-compact .v-selection-control--dirty .v-switch__track) {
  background: #d9e6fb !important;
  border-color: #d9e6fb !important;
}

:deep(.action-switch-compact .v-switch__thumb) {
  background: #94a3b8 !important;
  width: 14px !important;
  height: 14px !important;
  box-shadow: none !important;
}

:deep(.action-switch-compact .v-selection-control--dirty .v-switch__thumb) {
  background: #1e3a8a !important;
}

.action-controls {
  gap: 8px;
}

:deep(.action-icon-btn) {
  background: transparent !important;
  min-width: 28px !important;
  width: 28px !important;
  height: 28px !important;
  padding: 0 !important;
}

:deep(.action-icon-btn:hover) {
  background-color: #f1f5f9 !important;
  transform: translateY(-2px);
}
</style>
