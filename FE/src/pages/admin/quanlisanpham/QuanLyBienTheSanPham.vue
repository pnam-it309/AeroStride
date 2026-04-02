<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ImagePlus, Pencil, Plus, RefreshCcw, Search, Trash2 } from 'lucide-vue-next'
import AdminPageHeader from '@/components/admin/shared/AdminPageHeader.vue'
import AeroPagination from '@/components/base/AeroPagination.vue'
import SafeImage from '@/components/common/SafeImage.vue'
import { useToast } from '@/composable/useToast'
import { ROUTES } from '@/constants'
import VariantFormModal from './components/VariantFormModal.vue'
import VariantImageModal from './components/VariantImageModal.vue'
import {
  createVariant,
  createVariantImage,
  deleteVariant,
  deleteVariantImage,
  getProductDetail,
  getProductFormOptions,
  getProducts,
  updateVariant,
  updateVariantImage,
} from '@/services/sanPhamService'

const router = useRouter()
const route = useRoute()
const toast = useToast()

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

const formatDate = (timestamp) => {
  if (!timestamp) return '--'
  const numericValue = Number(timestamp)
  const safeValue = numericValue < 1000000000000 ? numericValue * 1000 : numericValue
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(safeValue))
}

const getStatusLabel = (status) => {
  if (status === 'DANG_HOAT_DONG') return 'Đang hoạt động'
  if (status === 'KHONG_HOAT_DONG') return 'Ngừng bán'
  if (status === 'DA_XOA') return 'Đã xóa'
  return status || 'Không xác định'
}

const getStatusClass = (status) => {
  if (status === 'DANG_HOAT_DONG') return 'bg-emerald-100 text-emerald-700'
  if (status === 'KHONG_HOAT_DONG') return 'bg-amber-100 text-amber-700'
  if (status === 'DA_XOA') return 'bg-rose-100 text-rose-700'
  return 'bg-slate-100 text-slate-600'
}

const getVariantThumbnail = (variant) => {
  const images = variant?.images || []
  const preferred = images.find((image) => image.hinhAnhDaiDien) || images[0]
  return preferred?.duongDanAnh || 'https://placehold.co/120x120/f8fafc/94a3b8?text=SKU'
}

const selectedProductSummary = computed(() => {
  if (!selectedProduct.value) {
    return {
      title: 'Chưa chọn sản phẩm',
      subtitle: 'Chọn một sản phẩm để xem danh sách biến thể, tồn kho và ảnh liên quan.',
    }
  }

  return {
    title: selectedProduct.value.tenSanPham,
    subtitle: `${selectedProduct.value.maSanPham} • ${formatNumber(selectedProduct.value.variants?.length || 0)} biến thể`,
  }
})

const filteredVariants = computed(() => {
  const variants = selectedProduct.value?.variants || []
  return variants.filter((variant) => {
    const keyword = filters.keyword.trim().toLowerCase()
    const matchesKeyword = !keyword
      || variant.maChiTietSanPham?.toLowerCase().includes(keyword)
      || variant.tenMauSac?.toLowerCase().includes(keyword)
      || variant.tenKichThuoc?.toLowerCase().includes(keyword)
      || selectedProduct.value?.tenSanPham?.toLowerCase().includes(keyword)

    const matchesColor = !filters.mauSacId || variant.idMauSac === filters.mauSacId
    const matchesSize = !filters.kichThuocId || variant.idKichThuoc === filters.kichThuocId
    const matchesStatus = !filters.trangThai || variant.trangThai === filters.trangThai

    return matchesKeyword && matchesColor && matchesSize && matchesStatus
  })
})

const totalPages = computed(() => Math.max(Math.ceil(filteredVariants.value.length / pagination.size), 1))

const paginatedVariants = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return filteredVariants.value.slice(start, end)
})

watch(
  () => filteredVariants.value.length,
  () => {
    if (pagination.page > totalPages.value) {
      pagination.page = totalPages.value
    }
  },
)

const fetchFormOptions = async () => {
  try {
    const response = await getProductFormOptions()
    formOptions.value = {
      mauSacs: response.mauSacs || [],
      kichThuocs: response.kichThuocs || [],
      trangThais: response.trangThais || [],
    }
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  }
}

const fetchProductOptions = async () => {
  try {
    const response = await getProducts({
      page: 1,
      size: 100,
      sortBy: 'ten',
      sortDirection: 'asc',
    })
    productOptions.value = response.content || []
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  }
}

const fetchSelectedProduct = async (productId) => {
  if (!productId) {
    selectedProduct.value = null
    return
  }

  loading.value = true
  try {
    selectedProduct.value = await getProductDetail(productId)
  } catch (error) {
    selectedProduct.value = null
    throw error
  } finally {
    loading.value = false
  }
}

const syncProductSelectionWithRoute = async (productId) => {
  resetFilters()
  try {
    await router.replace({
      name: route.name || 'AdminVariantsList',
      params: route.params,
      query: productId ? { ...route.query, productId } : {},
    })
  } catch (error) {
    // ignore navigation duplication
  }

  try {
    await fetchSelectedProduct(productId)
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  }
}

watch(
  () => selectedProductId.value,
  async (value) => {
    await syncProductSelectionWithRoute(value)
  },
)

const hasDuplicateVariantCombination = (payload) => {
  const existingVariants = selectedProduct.value?.variants || []
  return existingVariants.some((variant) => {
    if (variantModal.mode === 'edit' && variant.id === variantModal.variant?.id) {
      return false
    }

    return variant.idMauSac === payload.idMauSac && variant.idKichThuoc === payload.idKichThuoc
  })
}

const openCreateVariantModal = () => {
  if (!selectedProductId.value) {
    toast.error('Vui lòng chọn sản phẩm trước khi thêm biến thể.')
    return
  }

  router.push({
    name: 'AdminVariantsCreate',
    query: { productId: selectedProductId.value },
  })
}

const openEditVariantModal = (variant) => {
  router.push({
    name: 'AdminVariantsEdit',
    params: { id: variant.id },
    query: { productId: selectedProductId.value },
  })
}

const closeVariantModal = () => {
  router.push({
    name: 'AdminVariantsList',
    query: selectedProductId.value ? { productId: selectedProductId.value } : {},
  })
}

const handleVariantSubmit = async (payload) => {
  if (hasDuplicateVariantCombination(payload)) {
    toast.error('Tổ hợp màu sắc và kích thước đã tồn tại cho sản phẩm này.')
    return
  }

  variantModal.submitting = true
  try {
    if (variantModal.mode === 'create') {
      await createVariant(selectedProductId.value, payload)
      toast.success('Thêm biến thể thành công.')
    } else {
      await updateVariant(variantModal.variant.id, payload)
      toast.success('Cập nhật biến thể thành công.')
    }

    await router.push({
      name: 'AdminVariantsList',
      query: selectedProductId.value ? { productId: selectedProductId.value } : {},
    })
    await fetchSelectedProduct(selectedProductId.value)
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  } finally {
    variantModal.submitting = false
  }
}

const requestDeleteVariant = async (variant) => {
  if (!window.confirm(`Xóa mềm biến thể ${variant.maChiTietSanPham}?`)) return
  try {
    await deleteVariant(variant.id)
    toast.success('Đã xóa mềm biến thể.')
    await fetchSelectedProduct(selectedProductId.value)
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  }
}

const openCreateImageModal = (variant) => {
  imageModal.open = true
  imageModal.mode = 'create'
  imageModal.variantId = variant.id
  imageModal.image = {
    duongDanAnh: '',
    moTa: '',
    hinhAnhDaiDien: !variant.images?.length,
    trangThai: 'DANG_HOAT_DONG',
  }
}

const openEditImageModal = (variant, image) => {
  imageModal.open = true
  imageModal.mode = 'edit'
  imageModal.variantId = variant.id
  imageModal.image = image
}

const openManageImageModal = (variant) => {
  const preferred = variant.images?.find((image) => image.hinhAnhDaiDien) || variant.images?.[0]
  if (preferred) {
    openEditImageModal(variant, preferred)
    return
  }
  openCreateImageModal(variant)
}

const closeImageModal = () => {
  imageModal.open = false
  imageModal.image = null
  imageModal.variantId = ''
}

const handleImageSubmit = async (payload) => {
  imageModal.submitting = true
  try {
    if (imageModal.mode === 'create') {
      await createVariantImage(imageModal.variantId, payload)
      toast.success('Thêm ảnh biến thể thành công.')
    } else {
      await updateVariantImage(imageModal.image.id, payload)
      toast.success('Cập nhật ảnh biến thể thành công.')
    }
    closeImageModal()
    await fetchSelectedProduct(selectedProductId.value)
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  } finally {
    imageModal.submitting = false
  }
}

const requestDeleteImage = async (image) => {
  if (!window.confirm('Xóa mềm ảnh biến thể này?')) return
  try {
    await deleteVariantImage(image.id)
    toast.success('Đã xóa mềm ảnh biến thể.')
    await fetchSelectedProduct(selectedProductId.value)
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  }
}

const syncVariantModalWithRoute = async () => {
  const routeName = route.name?.toString()

  if (routeName === 'AdminVariantsCreate') {
    variantModal.mode = 'create'
    variantModal.variant = null
    variantModal.open = true
    return
  }

  if (routeName === 'AdminVariantsEdit' && route.params.id) {
    const matchedVariant = selectedProduct.value?.variants?.find((variant) => variant.id === route.params.id)

    if (!matchedVariant) {
      variantModal.open = false
      variantModal.variant = null
      return
    }

    variantModal.mode = 'edit'
    variantModal.variant = matchedVariant
    variantModal.open = true
    return
  }

  variantModal.open = false
  variantModal.variant = null
}

watch(
  () => [route.name, route.params.id, selectedProduct.value?.id, selectedProduct.value?.variants?.length],
  () => {
    syncVariantModalWithRoute()
  },
  { immediate: true },
)

const handlePageChange = (page) => {
  pagination.page = page
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
}

const applyFilters = () => {
  pagination.page = 1
}

onMounted(async () => {
  await Promise.all([fetchFormOptions(), fetchProductOptions()])
  const routeProductId = route.query.productId?.toString()
  selectedProductId.value = routeProductId || productOptions.value[0]?.id || ''
})
</script>

<template>
  <div class="space-y-6">
    <div class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_320px] xl:items-stretch">
      <AdminPageHeader
        title="Quản Lý Biến Thể SP"
        subtitle="Danh sách biến thể được chuẩn hóa cùng layout với màn sản phẩm, nhưng tối ưu để rà SKU, màu sắc, kích thước, tồn kho và ảnh."
      />
      <section class="flex flex-col justify-between rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
        <div class="space-y-2">
          <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">Sản phẩm đang xem</p>
          <h2 class="font-display text-xl font-bold text-obsidian">{{ selectedProductSummary.title }}</h2>
          <p class="text-sm text-slate-500">{{ selectedProductSummary.subtitle }}</p>
        </div>

        <button
          class="mt-5 inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-600 shadow-sm transition hover:bg-slate-50"
          @click="router.push(ROUTES.ADMIN.PRODUCTS)"
        >
          <ArrowLeft class="h-4 w-4" />
          Quay lại sản phẩm
        </button>
      </section>
    </div>

    <section class="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
      <div class="grid gap-4 xl:grid-cols-[1.5fr_repeat(4,minmax(0,1fr))]">
        <label class="space-y-2">
          <span class="text-sm font-semibold text-slate-700">Sản phẩm</span>
          <select v-model="selectedProductId" class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none">
            <option value="">Chọn sản phẩm</option>
            <option v-for="product in productOptions" :key="product.id" :value="product.id">
              {{ product.maSanPham }} - {{ product.tenSanPham }}
            </option>
          </select>
        </label>

        <label class="space-y-2">
          <span class="text-sm font-semibold text-slate-700">Tìm kiếm biến thể</span>
          <div class="flex items-center rounded-2xl border border-slate-200 px-4 py-3">
            <Search class="mr-3 h-4 w-4 text-slate-400" />
            <input
              v-model="filters.keyword"
              type="text"
              class="w-full text-sm outline-none"
              placeholder="Mã biến thể, màu sắc, kích thước..."
              @keyup.enter="applyFilters"
            />
          </div>
        </label>

        <label class="space-y-2">
          <span class="text-sm font-semibold text-slate-700">Màu sắc</span>
          <select v-model="filters.mauSacId" class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none">
            <option value="">Tất cả màu sắc</option>
            <option v-for="option in formOptions.mauSacs" :key="option.id" :value="option.id">{{ option.ten }}</option>
          </select>
        </label>

        <label class="space-y-2">
          <span class="text-sm font-semibold text-slate-700">Kích thước</span>
          <select v-model="filters.kichThuocId" class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none">
            <option value="">Tất cả kích thước</option>
            <option v-for="option in formOptions.kichThuocs" :key="option.id" :value="option.id">{{ option.ten }}</option>
          </select>
        </label>

        <label class="space-y-2">
          <span class="text-sm font-semibold text-slate-700">Trạng thái</span>
          <select v-model="filters.trangThai" class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none">
            <option value="">Tất cả trạng thái</option>
            <option v-for="status in formOptions.trangThais" :key="status" :value="status">{{ getStatusLabel(status) }}</option>
          </select>
        </label>
      </div>

      <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
        <div class="flex flex-wrap items-center gap-3">
          <button class="rounded-2xl bg-aurora px-5 py-3 text-sm font-semibold text-white transition hover:bg-aurora/90" @click="applyFilters">
            Áp dụng bộ lọc
          </button>
          <button class="rounded-2xl border border-slate-200 px-5 py-3 text-sm font-semibold text-slate-600 transition hover:bg-slate-50" @click="resetFilters">
            Làm mới
          </button>
        </div>

        <div class="flex flex-wrap items-center gap-3">
          <button class="inline-flex items-center gap-2 rounded-2xl border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-600 transition hover:bg-slate-50" @click="fetchSelectedProduct(selectedProductId)">
            <RefreshCcw class="h-4 w-4" />
            Tải lại
          </button>
          <button class="inline-flex items-center gap-2 rounded-2xl bg-obsidian px-4 py-3 text-sm font-semibold text-white transition hover:bg-obsidian/90" @click="openCreateVariantModal">
            <Plus class="h-4 w-4" />
            Thêm biến thể
          </button>
        </div>
      </div>
    </section>

    <section class="rounded-[28px] border border-slate-200 bg-white shadow-sm">
      <div class="flex items-center justify-between border-b border-slate-100 px-5 py-4">
        <div>
          <h2 class="font-display text-xl font-bold text-obsidian">Danh sách biến thể</h2>
          <p class="text-sm text-slate-500">Hiển thị gọn theo table để dễ scan SKU, sản phẩm, màu sắc, kích thước, tồn kho, giá và trạng thái.</p>
        </div>
        <div class="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-500">
          {{ formatNumber(filteredVariants.length) }} bản ghi
        </div>
      </div>

      <div v-if="loading" class="px-5 py-12 text-center text-sm font-medium text-slate-400">Đang tải dữ liệu biến thể...</div>
      <div v-else-if="!selectedProduct" class="px-5 py-12 text-center text-sm font-medium text-slate-400">Chọn sản phẩm để xem danh sách biến thể.</div>
      <div v-else class="overflow-x-auto">
        <table class="min-w-full">
          <thead class="bg-slate-50">
            <tr>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Biến thể</th>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Sản phẩm</th>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Thuộc tính</th>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Tồn kho</th>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Giá bán</th>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Giá nhập</th>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Trạng thái</th>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Cập nhật</th>
              <th class="px-5 py-4 text-right text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Thao tác</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-if="!paginatedVariants.length">
              <td colspan="9" class="px-5 py-12 text-center text-sm font-medium text-slate-400">Không có biến thể nào phù hợp bộ lọc hiện tại.</td>
            </tr>
            <tr v-for="variant in paginatedVariants" :key="variant.id" class="transition hover:bg-slate-50/70">
              <td class="px-5 py-4">
                <div class="flex items-center gap-3">
                  <SafeImage :src="getVariantThumbnail(variant)" :alt="variant.maChiTietSanPham" class="h-14 w-14 rounded-2xl object-cover" />
                  <div class="space-y-1">
                    <p class="font-semibold text-obsidian">{{ variant.maChiTietSanPham }}</p>
                    <p class="text-xs text-slate-500">{{ formatNumber(variant.images?.length || 0) }} ảnh</p>
                  </div>
                </div>
              </td>
              <td class="px-5 py-4 text-sm text-slate-600">
                <div class="font-semibold text-obsidian">{{ selectedProduct.tenSanPham }}</div>
                <div class="text-xs uppercase tracking-[0.2em] text-slate-400">{{ selectedProduct.maSanPham }}</div>
              </td>
              <td class="px-5 py-4 text-sm text-slate-600">
                <div>{{ variant.tenMauSac }}</div>
                <div>{{ variant.tenKichThuoc }}</div>
              </td>
              <td class="px-5 py-4 text-sm font-semibold text-obsidian">{{ formatNumber(variant.soLuong) }} đôi</td>
              <td class="px-5 py-4 text-sm font-semibold text-obsidian">{{ formatCurrency(variant.giaBan) }}</td>
              <td class="px-5 py-4 text-sm text-slate-600">{{ formatCurrency(variant.giaNhap) }}</td>
              <td class="px-5 py-4">
                <span :class="['inline-flex rounded-full px-3 py-1 text-xs font-semibold', getStatusClass(variant.trangThai)]">
                  {{ getStatusLabel(variant.trangThai) }}
                </span>
              </td>
              <td class="px-5 py-4 text-sm text-slate-500">{{ formatDate(variant.ngayCapNhat || variant.ngayTao) }}</td>
              <td class="px-5 py-4">
                <div class="flex justify-end gap-2">
                  <button class="rounded-xl border border-slate-200 p-2 text-slate-500 transition hover:border-cyan-200 hover:bg-cyan-50 hover:text-cyan-600" @click="openManageImageModal(variant)">
                    <ImagePlus class="h-4 w-4" />
                  </button>
                  <button class="rounded-xl border border-slate-200 p-2 text-slate-500 transition hover:border-aurora hover:bg-aurora/5 hover:text-aurora" @click="openEditVariantModal(variant)">
                    <Pencil class="h-4 w-4" />
                  </button>
                  <button class="rounded-xl border border-slate-200 p-2 text-slate-500 transition hover:border-rose-200 hover:bg-rose-50 hover:text-rose-500" @click="requestDeleteVariant(variant)">
                    <Trash2 class="h-4 w-4" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="px-5 py-5" v-if="selectedProduct">
        <AeroPagination
          :current-page="pagination.page"
          :total-pages="totalPages"
          :page-size="pagination.size"
          @page-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <VariantFormModal
      :open="variantModal.open"
      :mode="variantModal.mode"
      :variant="variantModal.variant"
      :options="formOptions"
      :submitting="variantModal.submitting"
      @close="closeVariantModal"
      @submit="handleVariantSubmit"
      @options-refreshed="fetchFormOptions"
    />
    <VariantImageModal
      :open="imageModal.open"
      :mode="imageModal.mode"
      :image="imageModal.image"
      :statuses="formOptions.trangThais"
      :submitting="imageModal.submitting"
      @close="closeImageModal"
      @submit="handleImageSubmit"
    />
  </div>
</template>
