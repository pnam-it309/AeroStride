<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { Layers3, Pencil, Plus, RefreshCcw, Search, Trash2 } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import AdminPageHeader from '@/components/admin/shared/AdminPageHeader.vue'
import AeroPagination from '@/components/base/AeroPagination.vue'
import SafeImage from '@/components/common/SafeImage.vue'
import { useToast } from '@/composable/useToast'
import { ROUTES } from '@/constants'
import ProductFormModal from './components/ProductFormModal.vue'
import {
  createProduct,
  deleteProduct,
  getProductDetail,
  getProductFormOptions,
  getProducts,
  updateProduct,
} from '@/services/sanPhamService'

const toast = useToast()
const route = useRoute()
const router = useRouter()

const products = ref([])
const editingProduct = ref(null)
const listLoading = ref(false)

const filters = reactive({
  keyword: '',
  danhMucId: '',
  thuongHieuId: '',
  trangThai: '',
})

const pagination = reactive({
  page: 1,
  size: 10,
  totalPages: 1,
  totalElements: 0,
})

const formOptions = ref({
  danhMucs: [],
  thuongHieus: [],
  xuatXus: [],
  mucDichChays: [],
  coGiays: [],
  chatLieus: [],
  deGiays: [],
  mauSacs: [],
  kichThuocs: [],
  gioiTinhKhachHangs: [],
  trangThais: [],
})

const productModal = reactive({
  open: false,
  mode: 'create',
  submitting: false,
})

const statusLabelMap = {
  DANG_HOAT_DONG: 'Đang hoạt động',
  KHONG_HOAT_DONG: 'Ngừng bán',
  DA_XOA: 'Đã xóa',
}

const genderLabelMap = {
  NAM: 'Nam',
  NU: 'Nữ',
  UNISEX: 'Unisex',
}

const numberFormatter = new Intl.NumberFormat('vi-VN')
const currencyFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

const getStatusLabel = (status) => statusLabelMap[status] || status || 'Không xác định'
const getGenderLabel = (value) => genderLabelMap[value] || value || 'Không xác định'
const formatCurrency = (value) => (value === null || value === undefined ? '--' : currencyFormatter.format(Number(value)))
const formatNumber = (value) => (value === null || value === undefined ? '--' : numberFormatter.format(Number(value)))
const getStatusClass = (status) => {
  if (status === 'DANG_HOAT_DONG') return 'bg-emerald-100 text-emerald-700'
  if (status === 'KHONG_HOAT_DONG') return 'bg-amber-100 text-amber-700'
  if (status === 'DA_XOA') return 'bg-rose-100 text-rose-700'
  return 'bg-slate-100 text-slate-600'
}
const productPreview = (product) => product?.hinhAnh || 'https://placehold.co/240x240/f8fafc/94a3b8?text=AeroStride'

const fetchFormOptions = async () => {
  try {
    formOptions.value = await getProductFormOptions()
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  }
}

const fetchProductDetail = async (id) => {
  editingProduct.value = await getProductDetail(id)
  return editingProduct.value
}

const fetchProductsPage = async () => {
  listLoading.value = true
  try {
    const response = await getProducts({
      page: pagination.page,
      size: pagination.size,
      keyword: filters.keyword || '',
      danhMucId: filters.danhMucId || '',
      thuongHieuId: filters.thuongHieuId || '',
      trangThai: filters.trangThai || '',
      sortBy: 'ngayTao',
      sortDirection: 'desc',
    })

    products.value = response.content || []
    pagination.totalPages = Math.max(response.totalPages || 1, 1)
    pagination.totalElements = response.totalElements || 0

    if (pagination.page > pagination.totalPages) {
      pagination.page = pagination.totalPages
      await fetchProductsPage()
    }
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  } finally {
    listLoading.value = false
  }
}

const applyFilters = async () => {
  pagination.page = 1
  await fetchProductsPage()
}

const resetFilters = async () => {
  Object.assign(filters, { keyword: '', danhMucId: '', thuongHieuId: '', trangThai: '' })
  pagination.page = 1
  await fetchProductsPage()
}

const openCreateProductModal = () => {
  router.push({ name: 'AdminProductsCreate' })
}

const openEditProductModal = (product) => {
  if (!product?.id) return
  router.push({ name: 'AdminProductsEdit', params: { id: product.id } })
}

const closeProductModal = () => {
  if (route.name !== 'AdminProductsList') {
    router.push({ name: 'AdminProductsList' })
    return
  }
  productModal.open = false
  editingProduct.value = null
}

const handleProductSubmit = async (payload) => {
  productModal.submitting = true
  try {
    if (productModal.mode === 'create') {
      await createProduct(payload)
      toast.success('Tạo sản phẩm thành công.')
      router.push({ name: 'AdminProductsList' })
      pagination.page = 1
      await fetchProductsPage()
      return
    }

    await updateProduct(editingProduct.value.id, payload)
    toast.success('Cập nhật sản phẩm thành công.')
    router.push({ name: 'AdminProductsList' })
    await fetchProductsPage()
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  } finally {
    productModal.submitting = false
  }
}

const requestDeleteProduct = async (product) => {
  if (!product || !window.confirm(`Xóa mềm sản phẩm ${product.tenSanPham}?`)) return
  try {
    await deleteProduct(product.id)
    toast.success('Đã xóa mềm sản phẩm.')
    if (products.value.length === 1 && pagination.page > 1) {
      pagination.page -= 1
    }
    await fetchProductsPage()
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  }
}

const goToVariantManagement = (productId) => {
  if (!productId) return
  router.push({
    path: ROUTES.ADMIN.VARIANTS,
    query: { productId },
  })
}

const handlePageChange = async (page) => {
  pagination.page = page
  await fetchProductsPage()
}

const handleSizeChange = async (size) => {
  pagination.size = size
  pagination.page = 1
  await fetchProductsPage()
}

onMounted(async () => {
  await fetchFormOptions()
  await fetchProductsPage()
})

const syncModalWithRoute = async () => {
  const routeName = route.name?.toString()

  if (routeName === 'AdminProductsCreate') {
    editingProduct.value = null
    productModal.mode = 'create'
    productModal.open = true
    return
  }

  if (routeName === 'AdminProductsEdit' && route.params.id) {
    productModal.mode = 'edit'
    productModal.open = true

    try {
      await fetchProductDetail(route.params.id)
    } catch (error) {
      router.push({ name: 'AdminProductsList' })
    }
    return
  }

  productModal.open = false
  editingProduct.value = null
}

watch(
  () => [route.name, route.params.id],
  () => {
    syncModalWithRoute()
  },
  { immediate: true },
)
</script>

<template>
  <div class="space-y-6">
    <AdminPageHeader
      title="Quản Lý Sản Phẩm"
      subtitle="Màn sản phẩm chỉ quản lý thông tin sản phẩm chính. Biến thể và ảnh biến thể được tách sang màn riêng giống luồng của A."
    />

    <section class="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
      <div class="grid gap-4 xl:grid-cols-[1.6fr_repeat(3,minmax(0,1fr))]">
        <label class="space-y-2">
          <span class="text-sm font-semibold text-slate-700">Tìm kiếm</span>
          <div class="flex items-center rounded-2xl border border-slate-200 px-4 py-3">
            <Search class="mr-3 h-4 w-4 text-slate-400" />
            <input
              v-model="filters.keyword"
              type="text"
              class="w-full text-sm outline-none"
              placeholder="Mã sản phẩm, tên sản phẩm..."
              @keyup.enter="applyFilters"
            />
          </div>
        </label>

        <label class="space-y-2">
          <span class="text-sm font-semibold text-slate-700">Danh mục</span>
          <select v-model="filters.danhMucId" class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none">
            <option value="">Tất cả danh mục</option>
            <option v-for="option in formOptions.danhMucs" :key="option.id" :value="option.id">{{ option.ten }}</option>
          </select>
        </label>

        <label class="space-y-2">
          <span class="text-sm font-semibold text-slate-700">Thương hiệu</span>
          <select v-model="filters.thuongHieuId" class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none">
            <option value="">Tất cả thương hiệu</option>
            <option v-for="option in formOptions.thuongHieus" :key="option.id" :value="option.id">{{ option.ten }}</option>
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
          <button class="inline-flex items-center gap-2 rounded-2xl border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-600 transition hover:bg-slate-50" @click="fetchProductsPage">
            <RefreshCcw class="h-4 w-4" />
            Tải lại
          </button>
          <button class="inline-flex items-center gap-2 rounded-2xl bg-obsidian px-4 py-3 text-sm font-semibold text-white transition hover:bg-obsidian/90" @click="openCreateProductModal">
            <Plus class="h-4 w-4" />
            Thêm sản phẩm
          </button>
        </div>
      </div>
    </section>

    <section class="rounded-[28px] border border-slate-200 bg-white shadow-sm">
      <div class="flex items-center justify-between border-b border-slate-100 px-5 py-4">
        <div>
          <h2 class="font-display text-xl font-bold text-obsidian">Danh sách sản phẩm</h2>
          <p class="text-sm text-slate-500">Quản lý sản phẩm chính, sau đó chuyển sang màn biến thể để thao tác SKU, tồn kho và ảnh.</p>
        </div>
        <div class="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-500">
          {{ formatNumber(pagination.totalElements) }} bản ghi
        </div>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead class="bg-slate-50">
            <tr>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Sản phẩm</th>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Thuộc tính</th>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Tồn kho</th>
              <th class="px-5 py-4 text-right text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Thao tác</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-if="listLoading">
              <td colspan="4" class="px-5 py-12 text-center text-sm font-medium text-slate-400">Đang tải danh sách sản phẩm...</td>
            </tr>
            <tr v-else-if="!products.length">
              <td colspan="4" class="px-5 py-12 text-center text-sm font-medium text-slate-400">Chưa có sản phẩm nào phù hợp bộ lọc.</td>
            </tr>
            <tr v-for="product in products" :key="product.id" class="transition hover:bg-slate-50/70">
              <td class="px-5 py-4">
                <div class="flex items-center gap-3">
                  <SafeImage :src="productPreview(product)" :alt="product.tenSanPham" class="h-16 w-16 rounded-2xl object-cover" />
                  <div class="space-y-1">
                    <p class="text-xs font-semibold uppercase tracking-[0.2em] text-aurora">{{ product.maSanPham }}</p>
                    <p class="font-semibold text-obsidian">{{ product.tenSanPham }}</p>
                    <div class="flex flex-wrap gap-2 text-xs text-slate-500">
                      <span>{{ product.tenDanhMuc }}</span>
                      <span>•</span>
                      <span>{{ product.tenThuongHieu }}</span>
                    </div>
                  </div>
                </div>
              </td>
              <td class="px-5 py-4 text-sm text-slate-600">
                <div>{{ getGenderLabel(product.gioiTinhKhachHang) }}</div>
                <div>{{ product.tenXuatXu }} • {{ product.tenMucDichChay }}</div>
                <span :class="['mt-2 inline-flex rounded-full px-3 py-1 text-xs font-semibold', getStatusClass(product.trangThai)]">
                  {{ getStatusLabel(product.trangThai) }}
                </span>
              </td>
              <td class="px-5 py-4 text-sm text-slate-600">
                <div>{{ formatNumber(product.tongBienThe || 0) }} biến thể</div>
                <div>{{ formatNumber(product.tongSoLuongTon || 0) }} đôi</div>
                <div class="font-semibold text-obsidian">
                  {{ formatCurrency(product.giaBanThapNhat) }}
                  <span v-if="product.giaBanCaoNhat && product.giaBanCaoNhat !== product.giaBanThapNhat">
                    - {{ formatCurrency(product.giaBanCaoNhat) }}
                  </span>
                </div>
              </td>
              <td class="px-5 py-4">
                <div class="flex justify-end gap-2">
                  <button
                    class="rounded-xl border border-slate-200 p-2 text-slate-500 transition hover:border-cyan-200 hover:bg-cyan-50 hover:text-cyan-600"
                    @click="goToVariantManagement(product.id)"
                  >
                    <Layers3 class="h-4 w-4" />
                  </button>
                  <button
                    class="rounded-xl border border-slate-200 p-2 text-slate-500 transition hover:border-aurora hover:bg-aurora/5 hover:text-aurora"
                    @click="openEditProductModal(product)"
                  >
                    <Pencil class="h-4 w-4" />
                  </button>
                  <button
                    class="rounded-xl border border-slate-200 p-2 text-slate-500 transition hover:border-rose-200 hover:bg-rose-50 hover:text-rose-500"
                    @click="requestDeleteProduct(product)"
                  >
                    <Trash2 class="h-4 w-4" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="px-5 py-5">
        <AeroPagination
          :current-page="pagination.page"
          :total-pages="pagination.totalPages"
          :page-size="pagination.size"
          @page-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <ProductFormModal
      :open="productModal.open"
      :mode="productModal.mode"
      :product="productModal.mode === 'edit' ? editingProduct : null"
      :options="formOptions"
      :submitting="productModal.submitting"
      @close="closeProductModal"
      @submit="handleProductSubmit"
      @options-refreshed="fetchFormOptions"
    />
  </div>
</template>
