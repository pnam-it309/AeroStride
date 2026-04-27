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
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue'
import { useNotifications } from '@/services/notificationService'
import { dichVuSanPham } from '@/services/product/dichVuSanPham'
import { dichVuBienThe } from '@/services/product/dichVuBienThe'
import { dichVuMauSac, dichVuKichThuoc } from '@/services/product/dichVuThuocTinh'
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
const variantDrawer = reactive({ open: false, variant: null, initialTab: 0 })
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
  if (!selectedProductId.value || selectedProductId.value === 'ALL') {
    return { title: 'Tất cả sản phẩm', subtitle: `Tổng số ${formatNumber(selectedProduct.value?.variants?.length || 0)} biến thể`, maSanPham: '' }
  }
  if (!selectedProduct.value) {
    return { title: 'Đang tải...', subtitle: 'Vui lòng đợi', maSanPham: '' }
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
    // Try aggregate endpoint first
    const response = await dichVuSanPham.layOptionsForm().catch(() => null)
    if (response) {
      formOptions.value = {
        mauSacs: response.mauSacs || [],
        kichThuocs: response.kichThuocs || [],
        trangThais: response.trangThais || [],
      }
    } else {
      // Fallback: fetch individually if aggregate fails
      const [m, k] = await Promise.all([
        dichVuMauSac.layMauSac({ size: 1000 }),
        dichVuKichThuoc.layKichThuoc({ size: 1000 })
      ])
      formOptions.value = {
        mauSacs: m.content || m || [],
        kichThuocs: k.content || k || [],
        trangThais: ['DANG_HOAT_DONG', 'KHONG_HOAT_DONG'] // Default statuses
      }
    }
  } catch (error) {
    console.error('Lỗi khi tải options:', error)
  }
}

const fetchProductOptions = async () => {
  try {
    const response = await dichVuSanPham.layDanhSachSanPham({ page: 0, size: 100 })
    productOptions.value = response.content || []
  } catch (error) { }
}

const fetchSelectedProduct = async (productId) => {
  loading.value = true
  try {
    if (!productId || productId === 'ALL') {
      const allVariants = await dichVuBienThe.layTatCaBienThe()
      selectedProduct.value = {
        tenSanPham: 'Tất cả sản phẩm',
        variants: allVariants || []
      }
    } else {
      selectedProduct.value = await dichVuSanPham.layChiTietSanPham(productId)
    }
  } catch (error) {
    selectedProduct.value = null
    addNotification({ title: 'Lỗi', subtitle: 'Không thể tải danh sách biến thể', color: 'error' })
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

const openImageModal = (item) => {
  variantDrawer.variant = item;
  variantDrawer.initialTab = 1; // Tab 1 is Images
  variantDrawer.open = true;
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
  selectedProductId.value = routeProductId || 'ALL'
})

watch(() => selectedProductId.value, (val) => fetchSelectedProduct(val))
</script>

<template>
  <v-container fluid class="pa-4 animate-fade-in font-body"
    style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important;">
    <!-- Breadcrumbs -->
    <AdminBreadcrumbs :items="[
      { title: 'Quản lý sản phẩm', disabled: false, href: '#' },
      { title: 'Biến thể', disabled: false, to: '/san-pham' },
      { title: selectedProductSummary.title, disabled: true }
    ]" />

    <div class="mb-4"></div>

    <!-- Header -->
    <div class="header-section mb-6">
      <div class="d-flex align-center gap-3">
        <v-btn v-if="selectedProductId && selectedProductId !== 'ALL'" icon variant="flat"
          @click="router.push({ name: 'SanPham' })" class="btn-back-header">
          <ArrowLeftIcon size="20" />
        </v-btn>

        <div v-if="selectedProductId && selectedProductId !== 'ALL'"
          class="premium-card px-4 py-2 bg-slate-50 d-flex align-center shadow-none border">
          <v-avatar color="primary" variant="flat" size="32" rounded="lg" class="mr-3">
            <PackageIcon size="18" color="white" />
          </v-avatar>
          <div>
            <p class="text-overline font-bold text-slate-400 mb-0" style="line-height: 1;">Mã Sản Phẩm</p>
            <h2 class="text-body-2 font-bold text-dark mb-0">{{ selectedProductSummary.maSanPham }}</h2>
          </div>
        </div>
      </div>
    </div>

    <!-- Filter -->
    <div class="flex-none mb-4">
      <AdminFilter title="Bộ lọc nâng cao" @refresh="resetFilters" :loading="loading">
        <v-col cols="12" md="2">
          <div class="filter-field-label">Sản phẩm</div>
          <v-autocomplete v-model="selectedProductId"
            :items="[{ tenSanPham: 'Tất cả sản phẩm', id: 'ALL' }, ...productOptions]" item-title="tenSanPham"
            item-value="id" variant="outlined" density="compact" hide-details></v-autocomplete>
        </v-col>
        <v-col cols="12" md="3">
          <div class="filter-field-label">Tìm kiếm nhanh</div>
          <v-text-field v-model="filters.keyword" placeholder="Mã sản phẩm chi tiết, màu, size..." prepend-inner-icon="mdi-magnify"
            variant="outlined" density="compact" hide-details clearable></v-text-field>
        </v-col>
        <v-col cols="12" md="2">
          <div class="filter-field-label">Màu sắc</div>
          <v-select v-model="filters.mauSacId"
            :items="[{ title: 'Tất cả mầu', value: '' }, ...formOptions.mauSacs.map(m => ({ title: m.ten, value: m.id }))]"
            variant="outlined" density="compact" hide-details></v-select>
        </v-col>
        <v-col cols="12" md="2">
          <div class="filter-field-label">Kích thước</div>
          <v-select v-model="filters.kichThuocId"
            :items="[{ title: 'Tất cả size', value: '' }, ...formOptions.kichThuocs.map(k => ({ title: k.ten, value: k.id }))]"
            variant="outlined" density="compact" hide-details></v-select>
        </v-col>
        <v-col cols="12" md="3">
          <div class="filter-field-label">Trạng thái kinh doanh</div>
          <v-select v-model="filters.trangThai"
            :items="[{ title: 'Tất cả trạng thái', value: '' }, ...formOptions.trangThais.map(s => ({ title: getStatusLabel(s), value: s }))]"
            variant="outlined" density="compact" hide-details></v-select>
        </v-col>
      </AdminFilter>
    </div>

    <!-- Table content -->
    <div class="flex-grow-1 min-h-0">
      <AdminTable title="Danh mục biến thể" :headers="[
        { text: 'STT', align: 'center', width: '60px' },
        { text: 'Hình ảnh', align: 'center', width: '80px' },
        { text: 'Mã sản phẩm chi tiết', align: 'left', width: '150px' },
        { text: 'Màu sắc', align: 'center', width: '120px' },
        { text: 'Kích thước', align: 'center', width: '100px' },
        { text: 'Giá bán niêm yết', align: 'center', width: '150px' },
        { text: 'Trạng thái', align: 'center', width: '140px' },
        { text: 'Thao tác', align: 'center', width: '140px' }
      ]" :items="paginatedVariants" :loading="loading"
        :showAddButton="!!selectedProductId && selectedProductId !== 'ALL'" addButtonText="Thêm biến thể"
        @add="openCreateVariantModal" class="h-100">

        <template #top>
          <div class="px-6 py-2 bg-slate-50 border-b d-flex align-center justify-space-between">
            <span class="text-caption font-bold text-slate-500">Tìm thấy {{ filteredVariants.length }} biến thể tương
              ứng</span>
            <v-btn size="x-small" variant="text" color="primary" class="font-bold"
              @click="fetchSelectedProduct(selectedProductId)" :loading="loading">
              <RefreshIcon size="14" class="mr-1" /> Đồng bộ dữ liệu
            </v-btn>
          </div>
        </template>

        <template #row="{ item, index }">
          <tr class="data-row">
            <td class="data-cell text-center text-slate-400">{{ (pagination.page - 1) * pagination.size + index + 1 }}
            </td>
            <td class="data-cell text-center">
              <v-avatar rounded="lg" size="44" class="border bg-slate-50 shadow-sm avatar-hover">
                <v-img :src="getVariantThumbnail(item)" cover></v-img>
              </v-avatar>
            </td>
            <td class="data-cell text-left px-6">
              <div class="text-slate-800" style="font-size: 13px;">{{ item.maChiTietSanPham }}</div>
            </td>
            <td class="data-cell text-center">
              <div class="text-slate-600" style="font-size: 13px;">{{ item.tenMauSac }}</div>
            </td>
            <td class="data-cell text-center">
              <div class="text-slate-600" style="font-size: 13px;">{{ item.tenKichThuoc }}</div>
            </td>
            <td class="data-cell text-center">
              <div class="text-primary" style="font-size: 13px;">{{ formatCurrency(item.giaBan) }}</div>
            </td>
            <td class="data-cell">
              <v-chip size="small" :color="isActiveStatus(item.trangThai) ? 'success' : 'warning'" variant="tonal"
                class="status-chip">
                {{ getStatusLabel(item.trangThai) }}
              </v-chip>
            </td>
            <td class="data-cell">
              <div class="d-flex align-center justify-center action-controls">
                <v-btn variant="text" class="action-icon-btn"
                  @click="variantModal.variant = item; variantModal.mode = 'edit'; variantModal.open = true">
                  <PencilIcon />
                  <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                </v-btn>
                <v-btn variant="text" class="action-icon-btn" @click="openImageModal(item)">
                  <PhotoIcon />
                  <v-tooltip activator="parent" location="top">Thư viện ảnh</v-tooltip>
                </v-btn>
                <div class="switch-wrapper">
                  <v-switch :model-value="isActiveStatus(item.trangThai)" color="#000" hide-details density="compact"
                    class="tight-switch action-switch" @click.prevent.stop="handleStatusChange(item)" />
                  <v-tooltip activator="parent" location="top">Bật/Tắt kinh doanh</v-tooltip>
                </div>
              </div>
            </td>
          </tr>
        </template>

        <template #pagination>
          <AdminPagination v-model="pagination.page" :page-size="pagination.size"
            @update:pageSize="pagination.size = $event" :total-pages="totalPages"
            :total-elements="filteredVariants.length" :current-size="paginatedVariants.length" />
        </template>
      </AdminTable>
    </div>

    <!-- Confirmation Dialog -->
    <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
      :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />

    <VariantFormModal :open="variantModal.open" :mode="variantModal.mode" :variant="variantModal.variant"
      :options="formOptions" :submitting="variantModal.submitting" :productCode="selectedProductSummary.maSanPham"
      @close="variantModal.open = false" @submit="handleVariantSubmit" @options-refreshed="fetchFormOptions" />
    <VariantManagementDrawer v-model:show="variantDrawer.open" :variant="variantDrawer.variant"
      :initialTab="variantDrawer.initialTab" @saved="fetchSelectedProduct(selectedProductId)" />
    <QrScanner v-model:show="showQrScanner" @scan="handleQrScan" />
  </v-container>
</template>

<style scoped>
/* Scoped styles removed in favor of global _admin-common.scss */
.flex-none {
  flex: none;
}

.flex-grow-1 {
  flex: 1;
}

.min-h-0 {
  min-height: 0;
}
</style>
