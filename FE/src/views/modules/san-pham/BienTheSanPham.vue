<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeftIcon,
  PhotoIcon,
  PencilIcon,
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
import logoPlaceholder from '@/assets/images/logos/logo-light.svg'
import VariantFormModal from './components/VariantFormModal.vue'
import VariantManagementDrawer from './components/VariantManagementDrawer.vue'
import QrScanner from './components/QrScanner.vue'
import SafeProductImage from './components/SafeProductImage.vue'
import QrcodeVue from 'qrcode.vue'
import { exportQrImageZip } from './utils/qrExcelWorkbook'

const MIN_VARIANT_PRICE = 0
const MAX_VARIANT_PRICE = 100000000
const VARIANT_PRICE_STEP = 50000

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
const selectedVariantIds = ref([])
const qrExportItems = ref([])
const qrExportContainer = ref(null)
const dynamicMaxPrice = ref(MAX_VARIANT_PRICE)

const filters = reactive({
  keyword: '',
  mauSacId: '',
  kichThuocId: '',
  trangThai: '',
  khoangGia: [MIN_VARIANT_PRICE, MAX_VARIANT_PRICE],
})

const pagination = reactive({
  page: 1,
  size: 10,
})

const variantModal = reactive({ open: false, mode: 'create', submitting: false, variant: null })
const variantDrawer = reactive({ open: false, variant: null, initialTab: 0 })
const qrDialog = reactive({ open: false, value: '', variant: null })
const showQrScanner = ref(false)
const qrCodeWrapper = ref(null)

const confirmDialog = ref({
  show: false,
  title: '',
  message: '',
  color: 'primary',
  action: null,
  loading: false
})

const numberFormatter = new Intl.NumberFormat('vi-VN')

const getBackendErrorMessage = (error, fallbackMessage) => (
  error?.response?.data?.message
  || error?.response?.data?.errors?.[0]?.defaultMessage
  || error?.message
  || fallbackMessage
)

const toNumber = (value, fallback = 0) => {
  const parsedValue = Number(value)
  return Number.isFinite(parsedValue) ? parsedValue : fallback
}

const formatCurrency = (value) => {
  if (value === null || value === undefined) return '--'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0
  }).format(Number(value))
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

const isActiveStatus = (status) => status === 'DANG_HOAT_DONG'

const selectedProductSummary = computed(() => {
  if (!selectedProductId.value || selectedProductId.value === 'ALL') {
    return {
      title: 'Tất cả sản phẩm',
      subtitle: `Tổng số ${formatNumber(selectedProduct.value?.variants?.length || 0)} biến thể`,
      maSanPham: ''
    }
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

const currentVariants = computed(() => selectedProduct.value?.variants || [])
const filteredVariants = computed(() => currentVariants.value.filter((variant) => {
  const keyword = filters.keyword.trim().toLowerCase()
  const matchesKeyword = !keyword
    || variant.maChiTietSanPham?.toLowerCase().includes(keyword)
    || variant.tenSanPham?.toLowerCase().includes(keyword)
  const variantPrice = toNumber(variant.giaBan, 0)
  const matchesPrice = variantPrice >= filters.khoangGia[0] && variantPrice <= filters.khoangGia[1]
  return matchesKeyword
    && (!filters.mauSacId || variant.idMauSac === filters.mauSacId)
    && (!filters.kichThuocId || variant.idKichThuoc === filters.kichThuocId)
    && (!filters.trangThai || variant.trangThai === filters.trangThai)
    && matchesPrice
}))

const totalElements = computed(() => filteredVariants.value.length)
const totalPages = computed(() => Math.max(Math.ceil(totalElements.value / pagination.size), 1))
const paginatedVariants = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return filteredVariants.value.slice(start, start + pagination.size)
})

const visibleVariantIds = computed(() => paginatedVariants.value.map((item) => item.id))
const selectedVariants = computed(() => filteredVariants.value.filter((item) => selectedVariantIds.value.includes(item.id)))
const allVisibleVariantsSelected = computed(() => visibleVariantIds.value.length > 0
  && visibleVariantIds.value.every((id) => selectedVariantIds.value.includes(id)))
const someVisibleVariantsSelected = computed(() => visibleVariantIds.value.some((id) => selectedVariantIds.value.includes(id))
  && !allVisibleVariantsSelected.value)

const clearVariantSelection = () => {
  selectedVariantIds.value = []
}

const syncVariantSelection = () => {
  const validIds = new Set(filteredVariants.value.map((item) => item.id))
  selectedVariantIds.value = selectedVariantIds.value.filter((id) => validIds.has(id))
}

const resetFilters = () => {
  filters.keyword = ''
  filters.mauSacId = ''
  filters.kichThuocId = ''
  filters.trangThai = ''
  filters.khoangGia = [MIN_VARIANT_PRICE, dynamicMaxPrice.value]
  pagination.page = 1
  clearVariantSelection()
}

const fetchFormOptions = async () => {
  try {
    const response = await dichVuSanPham.layOptionsForm().catch(() => null)
    if (response) {
      formOptions.value = {
        mauSacs: response.mauSacs || [],
        kichThuocs: response.kichThuocs || [],
        trangThais: response.trangThais || [],
      }
      return
    }

    const [mauSacResponse, kichThuocResponse] = await Promise.all([
      dichVuMauSac.layMauSac({ size: 1000 }),
      dichVuKichThuoc.layKichThuoc({ size: 1000 })
    ])

    formOptions.value = {
      mauSacs: mauSacResponse.content || mauSacResponse || [],
      kichThuocs: kichThuocResponse.content || kichThuocResponse || [],
      trangThais: ['DANG_HOAT_DONG', 'KHONG_HOAT_DONG']
    }
  } catch (error) {
    console.error('Lỗi khi tải options:', error)
  }
}

const fetchProductOptions = async () => {
  try {
    const response = await dichVuSanPham.layDanhSachSanPham({ page: 0, size: 100 })
    productOptions.value = response.content || []
  } catch (error) {
    console.error('Lỗi khi tải danh sách sản phẩm:', error)
  }
}

const loadMaxPrice = async () => {
  try {
    const maxPrice = await dichVuSanPham.layGiaLonNhat()
    if (maxPrice) {
      dynamicMaxPrice.value = Math.max(maxPrice, VARIANT_PRICE_STEP)
      filters.khoangGia = [MIN_VARIANT_PRICE, dynamicMaxPrice.value]
    }
  } catch (error) {
    console.error('Error loading max price:', error)
  }
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

    syncVariantSelection()
  } catch (error) {
    selectedProduct.value = null
    clearVariantSelection()
    addNotification({
      title: 'Lỗi',
      subtitle: 'Không thể tải danh sách biến thể',
      color: 'error'
    })
  } finally {
    loading.value = false
  }
}

const getOptionLabel = (items, id) => items.find((item) => item.id === id)?.ten || ''
const getOptionIdByLabel = (items, label) => {
  const normalizedLabel = String(label ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()
  if (!normalizedLabel) return ''

  return (items || []).find((item) => String(item.ten ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim() === normalizedLabel)?.id || ''
}

const getVariantPrimaryImageUrl = (variant) => {
  const mainImage = variant?.images?.find((image) => image?.hinhAnhDaiDien)
  const fallbackImage = variant?.images?.[0] || variant?.hinhAnh?.[0] || variant?.hinhAnh

  return normalizeImageUrl(variant?.urlAnh)
    || normalizeImageUrl(mainImage)
    || normalizeImageUrl(fallbackImage)
}

const buildLocalVariantViewModel = (payload, existingVariant = {}) => ({
  ...existingVariant,
  ...payload,
  tenSanPham: existingVariant.tenSanPham || selectedProduct.value?.tenSanPham || '',
  maSanPham: existingVariant.maSanPham || selectedProduct.value?.maSanPham || '',
  tenMauSac: getOptionLabel(formOptions.value.mauSacs || [], payload.idMauSac) || existingVariant.tenMauSac || '--',
  tenKichThuoc: getOptionLabel(formOptions.value.kichThuocs || [], payload.idKichThuoc) || existingVariant.tenKichThuoc || '--',
  images: (Array.isArray(payload.images) && payload.images.length)
    ? payload.images
    : getVariantPrimaryImageUrl(payload)
      ? [{ duongDanAnh: getVariantPrimaryImageUrl(payload), hinhAnhDaiDien: true }]
      : existingVariant.images || [],
  urlAnh: getVariantPrimaryImageUrl(payload) || getVariantPrimaryImageUrl(existingVariant) || ''
})

const syncVariantToLocalState = (nextVariant) => {
  if (!selectedProduct.value) return

  const currentVariants = Array.isArray(selectedProduct.value.variants)
    ? [...selectedProduct.value.variants]
    : []
  const variantIndex = currentVariants.findIndex((item) => item.id === nextVariant.id)

  if (variantIndex >= 0) {
    currentVariants.splice(variantIndex, 1, {
      ...currentVariants[variantIndex],
      ...nextVariant
    })
  } else {
    currentVariants.unshift(nextVariant)
  }

  selectedProduct.value = {
    ...selectedProduct.value,
    variants: currentVariants
  }
}

const closeVariantModal = () => {
  variantModal.open = false
  variantModal.mode = 'create'
  variantModal.submitting = false
  variantModal.variant = null
}

const handleVariantSubmit = (payload) => {
  confirmDialog.value = {
    show: true,
    title: variantModal.mode === 'create' ? 'Xác nhận thêm' : 'Xác nhận cập nhật',
    message: `Bạn có chắc chắn muốn ${variantModal.mode === 'create' ? 'thêm mới' : 'cập nhật'} biến thể này không?`,
    color: 'primary',
    action: async () => {
      confirmDialog.value.loading = true
      variantModal.submitting = true
      try {
        if (variantModal.mode === 'create') {
          const createdVariant = await dichVuBienThe.taoBienThe(
            selectedProductId.value,
            buildVariantRequestPayload(payload)
          )
          syncVariantToLocalState(buildLocalVariantViewModel(
            { ...payload, ...(createdVariant || {}) },
            createdVariant || {}
          ))
          addNotification({ title: 'Thành công', subtitle: 'Đã thêm biến thể mới', color: 'success' })
        } else {
          const updatedVariant = await dichVuBienThe.capNhatBienThe(
            variantModal.variant.id,
            buildVariantRequestPayload(payload, variantModal.variant)
          )
          syncVariantToLocalState(buildLocalVariantViewModel(
            { ...variantModal.variant, ...payload, ...(updatedVariant || {}), id: variantModal.variant.id },
            updatedVariant || variantModal.variant
          ))
          addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật biến thể', color: 'success' })
        }

        closeVariantModal()
        confirmDialog.value.show = false
        await fetchSelectedProduct(selectedProductId.value)
      } catch (error) {
        console.error(error)
        addNotification({
          title: 'Lỗi',
          subtitle: getBackendErrorMessage(error, 'Thao tác thất bại'),
          color: 'error'
        })
      } finally {
        variantModal.submitting = false
        confirmDialog.value.loading = false
      }
    }
  }
}

const openImageModal = (item) => {
  variantDrawer.variant = item
  variantDrawer.initialTab = 1
  variantDrawer.open = true
}

const openCreateVariantModal = () => {
  variantModal.variant = null
  variantModal.mode = 'create'
  variantModal.open = true
}

const openEditVariantModal = (item) => {
  variantModal.variant = {
    ...item,
    maChiTietSanPham: item.maChiTietSanPham || item.sku || item.maSku || item.maBienThe || item.ma || '',
    idMauSac: item.idMauSac || getOptionIdByLabel(formOptions.value.mauSacs, item.tenMauSac),
    idKichThuoc: item.idKichThuoc || getOptionIdByLabel(formOptions.value.kichThuocs, item.tenKichThuoc),
    tenMauSac: item.tenMauSac || getOptionLabel(formOptions.value.mauSacs, item.idMauSac),
    tenKichThuoc: item.tenKichThuoc || getOptionLabel(formOptions.value.kichThuocs, item.idKichThuoc),
    urlAnh: getVariantPrimaryImageUrl(item)
  }
  variantModal.mode = 'edit'
  variantModal.open = true
}

const handleQrScan = (code) => {
  if (!code) return
  filters.keyword = code
  pagination.page = 1
  syncVariantSelection()
  addNotification({ title: 'Quét mã thành công', subtitle: `Mã: ${code}`, color: 'success' })
}

const normalizeImageUrl = (value) => {
  if (!value) return ''
  if (typeof value === 'string') return value
  return value.duongDanAnh || value.url || value.fileUrl || value.secure_url || value.data || ''
}

const getVariantThumbnail = (item) => {
  return getVariantPrimaryImageUrl(item) || logoPlaceholder
}
const getVariantProductCode = (item) => {
  if (item?.maSanPham) return item.maSanPham
  if (selectedProductSummary.value.maSanPham) return selectedProductSummary.value.maSanPham

  const sku = String(item?.maChiTietSanPham || '').trim()
  if (sku.includes('-')) {
    return sku.split('-')[0] || '--'
  }

  return sku || '--'
}

const getVariantQrValue = (item) => item?.maChiTietSanPham || item?.maSanPham || String(item?.id || '')

const openQrDialog = (item) => {
  qrDialog.variant = item
  qrDialog.value = getVariantQrValue(item)
  qrDialog.open = true
}

const renderVariantQrCanvases = async (variants) => {
  qrExportItems.value = variants.map((variant) => ({
    id: variant.id,
    value: getVariantQrValue(variant)
  }))

  await nextTick()
  await new Promise((resolve) => window.setTimeout(resolve, 80))

  const canvases = Array.from(qrExportContainer.value?.querySelectorAll('canvas') || [])
  const qrDataUrls = canvases.map((canvas) => canvas.toDataURL('image/png'))
  qrExportItems.value = []
  return qrDataUrls
}

const handleExportVariantQrZip = async () => {
  const targetVariants = selectedVariants.value
  if (!targetVariants.length) {
    addNotification({
      title: 'Thông báo',
      subtitle: 'Chọn ít nhất 1 biến thể để xuất ZIP QR',
      color: 'warning'
    })
    return
  }

  try {
    const fileSuffix = selectedProductSummary.value.maSanPham || 'tat_ca'
    const qrDataUrls = await renderVariantQrCanvases(targetVariants)
    if (qrDataUrls.length !== targetVariants.length || qrDataUrls.some((item) => !item)) {
      throw new Error('Không thể tạo đủ dữ liệu QR để xuất ZIP')
    }

    exportQrImageZip({
      fileName: `qrcode_bien_the_da_chon_${fileSuffix}.zip`,
      items: targetVariants.map((variant, index) => ({
        baseName: variant.maChiTietSanPham
          || [variant.tenSanPham, variant.tenMauSac, variant.tenKichThuoc].filter(Boolean).join('_')
          || `variant_${index + 1}`,
        dataUrl: qrDataUrls[index]
      }))
    })

    addNotification({
      title: 'Thành công',
      subtitle: `Đã xuất ZIP QR của ${targetVariants.length} biến thể`,
      color: 'success'
    })
  } catch (error) {
    console.error('QR ZIP export error:', error)
    qrExportItems.value = []
    addNotification({
      title: 'Lỗi',
      subtitle: 'Không thể xuất ZIP QR của biến thể',
      color: 'error'
    })
  }
}

const buildVariantRequestPayload = (payload, existingVariant = null) => {
  const currentImageUrl = getVariantPrimaryImageUrl(existingVariant)
  const nextImageUrl = normalizeImageUrl(payload?.urlAnh)

  const requestPayload = {
    maChiTietSanPham: payload.maChiTietSanPham || null,
    idMauSac: payload.idMauSac,
    idKichThuoc: payload.idKichThuoc,
    soLuong: Number(payload.soLuong ?? 0),
    giaNhap: Number(payload.giaNhap ?? 0),
    giaBan: Number(payload.giaBan ?? 0),
    trangThai: payload.trangThai || 'DANG_HOAT_DONG'
  }

  if (nextImageUrl && (!existingVariant || nextImageUrl !== currentImageUrl)) {
    requestPayload.images = [{
      duongDanAnh: nextImageUrl,
      moTa: `Ảnh của ${payload.maChiTietSanPham || existingVariant?.maChiTietSanPham || 'biến thể'}`,
      hinhAnhDaiDien: true
    }]
  }

  return requestPayload
}

const buildVariantStatusPayload = (variant, nextStatus) => ({
  maChiTietSanPham: variant.maChiTietSanPham || null,
  idMauSac: variant.idMauSac,
  idKichThuoc: variant.idKichThuoc,
  soLuong: Number(variant.soLuong ?? 0),
  giaNhap: Number(variant.giaNhap ?? 0),
  giaBan: Number(variant.giaBan ?? 0),
  trangThai: nextStatus
})

const updateVariantStatusLocally = (variantId, nextStatus) => {
  if (!selectedProduct.value?.variants?.length) return

  selectedProduct.value = {
    ...selectedProduct.value,
    variants: selectedProduct.value.variants.map((item) => (
      item.id === variantId
        ? { ...item, trangThai: nextStatus }
        : item
    ))
  }
}

const handleToggleVariantStatus = (variant) => {
  const nextStatus = isActiveStatus(variant.trangThai) ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG'
  confirmDialog.value = {
    show: true,
    title: 'Đổi trạng thái biến thể',
    message: `Bạn có chắc chắn muốn ${nextStatus === 'DANG_HOAT_DONG' ? 'bật' : 'tắt'} trạng thái cho biến thể này không?`,
    color: 'warning',
    action: async () => {
      confirmDialog.value.loading = true
      try {
        await dichVuBienThe.capNhatBienThe(variant.id, buildVariantStatusPayload(variant, nextStatus))
        updateVariantStatusLocally(variant.id, nextStatus)
        addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật trạng thái biến thể', color: 'success' })
        confirmDialog.value.show = false
      } catch (error) {
        console.error(error)
        addNotification({ title: 'Lỗi', subtitle: 'Không thể cập nhật trạng thái biến thể', color: 'error' })
      } finally {
        confirmDialog.value.loading = false
      }
    }
  }
}

const toggleVariantSelection = (variantId, checked) => {
  if (checked) {
    if (!selectedVariantIds.value.includes(variantId)) {
      selectedVariantIds.value = [...selectedVariantIds.value, variantId]
    }
    return
  }

  selectedVariantIds.value = selectedVariantIds.value.filter((id) => id !== variantId)
}

const toggleSelectVisibleVariants = (checked) => {
  if (checked) {
    const mergedIds = new Set([...selectedVariantIds.value, ...visibleVariantIds.value])
    selectedVariantIds.value = Array.from(mergedIds)
    return
  }

  const visibleIdSet = new Set(visibleVariantIds.value)
  selectedVariantIds.value = selectedVariantIds.value.filter((id) => !visibleIdSet.has(id))
}

const downloadCurrentQrCode = () => {
  const canvas = qrCodeWrapper.value?.querySelector('canvas')
  if (!canvas || !qrDialog.variant) {
    addNotification({ title: 'Lỗi', subtitle: 'Không thể tạo mã QR để tải xuống', color: 'error' })
    return
  }

  const link = document.createElement('a')
  link.href = canvas.toDataURL('image/png')
  link.download = `${getVariantQrValue(qrDialog.variant)}.png`
  document.body.appendChild(link)
  link.click()
  link.remove()
  addNotification({ title: 'Thành công', subtitle: 'Đã tải mã QR của biến thể', color: 'success' })
}

watch(filteredVariants, () => {
  syncVariantSelection()
  if (pagination.page > totalPages.value) {
    pagination.page = totalPages.value
  }
})

watch(() => pagination.size, () => {
  pagination.page = 1
})

watch(
  () => [
    filters.keyword,
    filters.mauSacId,
    filters.kichThuocId,
    filters.trangThai,
    filters.khoangGia[0],
    filters.khoangGia[1]
  ],
  () => {
    pagination.page = 1
    syncVariantSelection()
  }
)

watch(
  () => selectedProductId.value,
  async (value) => {
    clearVariantSelection()
    pagination.page = 1
    await fetchSelectedProduct(value)
  }
)

onMounted(async () => {
  await Promise.all([loadMaxPrice(), fetchFormOptions(), fetchProductOptions()])
  const routeProductId = route.query.productId?.toString()
  selectedProductId.value = routeProductId || 'ALL'
})
</script>

<template>
  <v-container fluid class="pa-4 animate-fade-in font-body"
    style="height: 100% !important; display: flex; flex-direction: column; overflow: hidden !important;">
    <AdminBreadcrumbs :items="[
      { title: 'Quản lý sản phẩm', disabled: false, href: '#' },
      { title: 'Biến thể', disabled: false, to: '/san-pham' },
      { title: selectedProductSummary.title, disabled: true }
    ]" />

    <div class="mb-4"></div>

    <div class="flex-none mb-4">
      <AdminFilter title="Bộ lọc nâng cao" @refresh="resetFilters" :loading="loading">
        <v-col cols="12" md="2">
          <div class="filter-field-label">Sản phẩm</div>
          <v-autocomplete v-model="selectedProductId"
            :items="[{ tenSanPham: 'Tất cả sản phẩm', id: 'ALL' }, ...productOptions]" item-title="tenSanPham"
            item-value="id" variant="outlined" density="compact" hide-details />
        </v-col>
        <v-col cols="12" md="3">
          <div class="filter-field-label">Tìm kiếm nhanh</div>
          <v-text-field v-model="filters.keyword" placeholder="Mã SKU, màu, size..." prepend-inner-icon="mdi-magnify"
            variant="outlined" density="compact" hide-details clearable />
        </v-col>
        <v-col cols="12" md="2">
          <div class="filter-field-label">Màu sắc</div>
          <v-select v-model="filters.mauSacId"
            :items="[{ title: 'Tất cả mầu', value: '' }, ...formOptions.mauSacs.map((mauSac) => ({ title: mauSac.ten, value: mauSac.id }))]"
            variant="outlined" density="compact" hide-details />
        </v-col>
        <v-col cols="12" md="2">
          <div class="filter-field-label">Kích thước</div>
          <v-select v-model="filters.kichThuocId"
            :items="[{ title: 'Tất cả size', value: '' }, ...formOptions.kichThuocs.map((kichThuoc) => ({ title: kichThuoc.ten, value: kichThuoc.id }))]"
            variant="outlined" density="compact" hide-details />
        </v-col>
        <v-col cols="12" md="3">
          <div class="filter-field-label">Trạng thái</div>
          <v-select v-model="filters.trangThai"
            :items="[{ title: 'Tất cả trạng thái', value: '' }, ...formOptions.trangThais.map((trangThai) => ({ title: getStatusLabel(trangThai), value: trangThai }))]"
            variant="outlined" density="compact" hide-details />
        </v-col>
        <v-col cols="12" md="10" lg="11">
          <div class="filter-field-label">Khoảng giá</div>
          <div class="d-flex align-center bg-white pa-4 rounded-lg ">
            <v-icon size="20" class="mr-4 text-primary">mdi-cash-multiple</v-icon>
            <div class="flex-grow-1">
              <div class="d-flex justify-space-between mb-2">
                <span class="text-caption font-weight-black text-slate-600">Lọc theo giá bán biến thể</span>
                <span class="text-caption font-weight-black text-primary">
                  {{ formatCurrency(filters.khoangGia[0]) }} - {{ formatCurrency(filters.khoangGia[1]) }}
                </span>
              </div>
              <v-range-slider v-model="filters.khoangGia" :max="dynamicMaxPrice" :min="MIN_VARIANT_PRICE"
                :step="VARIANT_PRICE_STEP" hide-details color="primary" track-color="slate-200" />
            </div>
          </div>
        </v-col>
      </AdminFilter>
    </div>

    <div class="flex-grow-1 min-h-0">
      <AdminTable title="Danh mục biến thể" :headers="[
        { text: 'Chọn', width: '70px' },
        { text: 'STT', width: '60px' },
        { text: 'Mã SP', width: '120px' },
        { text: 'Hình ảnh', width: '80px' },
        { text: 'Mã SKU', width: '150px' },
        { text: 'Màu sắc', width: '120px' },
        { text: 'Kích thước', width: '100px' },
        { text: 'Giá bán niêm yết', width: '150px' },
        { text: 'Trạng thái', width: '140px' },
        { text: 'Thao tác', width: '200px' }
      ]" :items="paginatedVariants" :loading="loading" :showAddButton="!!selectedProductId && selectedProductId !== 'ALL'"
        addButtonText="Tạo mới" @add="openCreateVariantModal" class="h-100">

        <template #top>
          <div class="px-6 py-3 bg-slate-50 border-b d-flex align-center justify-space-between flex-wrap gap-3">
            <div class="d-flex align-center flex-wrap gap-2">
              <v-checkbox-btn :model-value="allVisibleVariantsSelected"
                :indeterminate="someVisibleVariantsSelected" color="primary" hide-details density="compact"
                @update:model-value="toggleSelectVisibleVariants" />
              <span class="text-caption font-weight-black text-slate-500">
                đã chọn {{ selectedVariantIds.length }} biến thể
              </span>
            </div>

            <div class="d-flex align-center flex-wrap gap-2">
              <v-btn v-if="selectedVariantIds.length > 0" size="small" variant="tonal" color="primary"
                class="text-none font-weight-bold" @click="handleExportVariantQrZip">
                Xuất ZIP QR
                <v-tooltip activator="parent" location="top" text="Xuất ảnh QR của các biến thể đã chọn" />
              </v-btn>
            </div>
          </div>
        </template>

        <template #row="{ item, index }">
          <tr class="data-row">
            <td class="data-cell">
              <v-checkbox-btn :model-value="selectedVariantIds.includes(item.id)" color="primary" hide-details
                density="compact" @update:model-value="toggleVariantSelection(item.id, $event)" />
            </td>
            <td class="data-cell text-slate-400">
              {{ (pagination.page - 1) * pagination.size + index + 1 }}
            </td>
            <td class="data-cell font-black text-slate-700">
              <div class="text-truncate" :title="getVariantProductCode(item)">{{ getVariantProductCode(item) }}</div>
            </td>
            <td class="data-cell">
              <v-avatar rounded="lg" size="44" class="border bg-slate-50 shadow-sm avatar-hover">
                <SafeProductImage :src="getVariantThumbnail(item)" :fallback-src="logoPlaceholder"
                  :alt="item.maChiTietSanPham || 'variant-image'" />
              </v-avatar>
            </td>
            <td class="data-cell font-black text-dark">
              <div class="text-truncate" :title="item.maChiTietSanPham">
                <span class="text-truncate">{{ item.maChiTietSanPham }}</span>
              </div>
            </td>
            <td class="data-cell">
              <div class="text-truncate" :title="item.tenMauSac">
                <span class="text-truncate">{{ item.tenMauSac }}</span>
              </div>
            </td>
            <td class="data-cell">
              <div class="text-truncate" :title="item.tenKichThuoc">
                <span class="text-truncate">{{ item.tenKichThuoc }}</span>
              </div>
            </td>
            <td class="data-cell font-black text-primary">
              <div class="font-weight-black text-primary text-truncate" :title="formatCurrency(item.giaBan)">{{ formatCurrency(item.giaBan) }}</div>
            </td>
            <td class="data-cell">
              <v-chip size="small" :color="isActiveStatus(item.trangThai) ? 'success' : 'warning'" variant="flat"
                class="status-chip font-weight-black text-white">
                {{ getStatusLabel(item.trangThai) }}
              </v-chip>
            </td>
            <td class="data-cell">
              <div class="d-flex align-center justify-center action-controls">
                <v-btn variant="text" class="action-icon-btn" @click="openEditVariantModal(item)">
                  <PencilIcon />
                  <v-tooltip activator="parent" location="top">Chỉnh sửa</v-tooltip>
                </v-btn>
                <v-btn variant="text" class="action-icon-btn" @click="openImageModal(item)">
                  <PhotoIcon />
                  <v-tooltip activator="parent" location="top">Thư viện ảnh</v-tooltip>
                </v-btn>
                <v-btn variant="text" color="primary" class="action-icon-btn qr-action-btn" @click="openQrDialog(item)">
                  <QrcodeIcon />
                  <v-tooltip activator="parent" location="top">QR code</v-tooltip>
                </v-btn>
                <div class="switch-wrapper">
                  <v-switch :model-value="isActiveStatus(item.trangThai)" color="primary" hide-details
                    density="compact" class="tight-switch action-switch"
                    @click.prevent.stop="handleToggleVariantStatus(item)" />
                  <v-tooltip activator="parent" location="top" text="Chuyển đổi trạng thái" />
                </div>
              </div>
            </td>
          </tr>
        </template>

        <template #pagination>
          <AdminPagination v-model="pagination.page" :page-size="pagination.size"
            @update:pageSize="pagination.size = $event" :total-pages="totalPages" :total-elements="totalElements"
            :current-size="paginatedVariants.length" />
        </template>
      </AdminTable>
    </div>

    <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
      :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />

    <VariantFormModal :key="`${variantModal.mode}-${variantModal.variant?.id || 'new'}`"
      :open="variantModal.open" :mode="variantModal.mode" :variant="variantModal.variant"
      :options="formOptions" :submitting="variantModal.submitting" :productCode="selectedProductSummary.maSanPham"
      @close="closeVariantModal" @submit="handleVariantSubmit" @options-refreshed="fetchFormOptions" />
    <VariantManagementDrawer v-model:show="variantDrawer.open" :variant="variantDrawer.variant"
      :initialTab="variantDrawer.initialTab" @saved="fetchSelectedProduct(selectedProductId)" />
    <QrScanner v-model:show="showQrScanner" @scan="handleQrScan" />

    <v-dialog v-model="qrDialog.open" max-width="420">
      <v-card class="pa-4">
        <div class="d-flex justify-space-between align-start mb-4">
          <div>
            <p class="text-overline text-slate-400 mb-1">Mã QR biến thể</p>
            <h3 class="text-h6 font-weight-black text-dark mb-1">{{ qrDialog.variant?.maChiTietSanPham || '--' }}</h3>
            <p class="text-body-2 text-slate-500 mb-0">
              {{ qrDialog.variant?.tenSanPham || selectedProduct.value?.tenSanPham || 'Sản phẩm' }}
            </p>
          </div>
          <v-btn icon variant="text" size="small" @click="qrDialog.open = false">
            <v-icon>mdi-close</v-icon>
            <v-tooltip activator="parent" location="top" text="Đóng hộp thoại QR" />
          </v-btn>
        </div>

        <div ref="qrCodeWrapper" class="d-flex justify-center pa-4 rounded-lg border bg-white mb-4">
          <QrcodeVue :value="qrDialog.value" :size="220" level="H" render-as="canvas" />
        </div>

        <div class="text-center mb-4">
          <p class="text-body-2 font-weight-black mb-1">{{ qrDialog.value }}</p>
          <p class="text-caption text-slate-500 mb-0">Quét mã để tìm nhanh biến thể theo SKU</p>
        </div>

        <div class="d-flex justify-end gap-2">
          <v-btn variant="text" @click="qrDialog.open = false">Đóng</v-btn>
          <v-btn color="primary" variant="flat" @click="downloadCurrentQrCode">Tải mã QR</v-btn>
        </div>
      </v-card>
    </v-dialog>

    <div ref="qrExportContainer" class="qr-export-staging" aria-hidden="true">
      <div v-for="item in qrExportItems" :key="item.id" class="qr-export-item">
        <QrcodeVue :value="item.value" :size="120" level="H" render-as="canvas" />
      </div>
    </div>
  </v-container>
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

.qr-action-btn {
  border-radius: 10px;
}

.qr-action-btn:hover {
  background: rgba(var(--v-theme-primary), 0.08);
}

.qr-export-staging {
  position: fixed;
  left: -9999px;
  top: -9999px;
  opacity: 0;
  pointer-events: none;
}

.qr-export-item {
  display: inline-block;
  padding: 8px;
  background: #ffffff;
}
</style>
