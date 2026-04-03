<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ImagePlus, Plus, Trash2 } from 'lucide-vue-next'
import ProductModalShell from './ProductModalShell.vue'
import AdminFileUploader from '@/components/admin/shared/AdminFileUploader.vue'
import SafeImage from '@/components/common/SafeImage.vue'
import AttributeFormModal from '@/pages/admin/quanlithuoctinh/components/AttributeFormModal.vue'
import { useToast } from '@/composable/useToast'
import { ATTRIBUTE_PAGE_CONFIGS } from '@/pages/admin/quanlithuoctinh/attributeConfigs'
import { createAttribute } from '@/services/thuocTinhService'
import { getProductFormOptions, uploadProductFile } from '@/services/sanPhamService'

const props = defineProps({
  open: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  product: { type: Object, default: null },
  options: { type: Object, default: () => ({}) },
  submitting: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'submit', 'options-refreshed'])
const toast = useToast()
const uploadingImage = ref(false)
const optionState = ref(createOptionState())

const quickAttributeModal = reactive({
  open: false,
  submitting: false,
  field: null,
})

const createEmptyVariant = () => ({
  idMauSac: '',
  idKichThuoc: '',
  maChiTietSanPham: '',
  soLuong: 0,
  giaNhap: '',
  giaBan: '',
  trangThai: 'DANG_HOAT_DONG',
})

const createEmptyForm = () => ({
  maSanPham: '',
  tenSanPham: '',
  idDanhMuc: '',
  idThuongHieu: '',
  idXuatXu: '',
  idMucDichChay: '',
  idCoGiay: '',
  idChatLieu: '',
  idDeGiay: '',
  gioiTinhKhachHang: 'UNISEX',
  hinhAnh: '',
  moTaNgan: '',
  moTaChiTiet: '',
  trangThai: 'DANG_HOAT_DONG',
  variants: [],
})

function createOptionState(source = {}) {
  return {
    danhMucs: [...(source.danhMucs || [])],
    thuongHieus: [...(source.thuongHieus || [])],
    xuatXus: [...(source.xuatXus || [])],
    mucDichChays: [...(source.mucDichChays || [])],
    coGiays: [...(source.coGiays || [])],
    chatLieus: [...(source.chatLieus || [])],
    deGiays: [...(source.deGiays || [])],
    mauSacs: [...(source.mauSacs || [])],
    kichThuocs: [...(source.kichThuocs || [])],
    gioiTinhKhachHangs: [...(source.gioiTinhKhachHangs || [])],
    trangThais: [...(source.trangThais || [])],
  }
}

const form = reactive(createEmptyForm())

const isCreateMode = computed(() => props.mode === 'create')
const genderOptions = computed(() => optionState.value.gioiTinhKhachHangs || ['NAM', 'NU', 'UNISEX'])
const statusOptions = computed(() => optionState.value.trangThais || ['DANG_HOAT_DONG', 'KHONG_HOAT_DONG'])
const generatedProductCodeLabel = computed(() =>
  isCreateMode.value ? 'Hệ thống sẽ tự sinh sau khi lưu' : props.product?.maSanPham || '--',
)

const productAttributeFields = computed(() => [
  {
    key: 'idDanhMuc',
    label: 'Danh mục *',
    optionsKey: 'danhMucs',
    config: ATTRIBUTE_PAGE_CONFIGS.DANH_MUC,
    applyCreatedId: (id) => {
      form.idDanhMuc = id
    },
  },
  {
    key: 'idThuongHieu',
    label: 'Thương hiệu *',
    optionsKey: 'thuongHieus',
    config: ATTRIBUTE_PAGE_CONFIGS.THUONG_HIEU,
    applyCreatedId: (id) => {
      form.idThuongHieu = id
    },
  },
  {
    key: 'idXuatXu',
    label: 'Xuất xứ *',
    optionsKey: 'xuatXus',
    config: ATTRIBUTE_PAGE_CONFIGS.XUAT_XU,
    applyCreatedId: (id) => {
      form.idXuatXu = id
    },
  },
  {
    key: 'idMucDichChay',
    label: 'Mục đích chạy *',
    optionsKey: 'mucDichChays',
    config: ATTRIBUTE_PAGE_CONFIGS.MUC_DICH_CHAY,
    applyCreatedId: (id) => {
      form.idMucDichChay = id
    },
  },
  {
    key: 'idCoGiay',
    label: 'Cổ giày *',
    optionsKey: 'coGiays',
    config: ATTRIBUTE_PAGE_CONFIGS.CO_GIAY,
    applyCreatedId: (id) => {
      form.idCoGiay = id
    },
  },
  {
    key: 'idChatLieu',
    label: 'Chất liệu *',
    optionsKey: 'chatLieus',
    config: ATTRIBUTE_PAGE_CONFIGS.CHAT_LIEU,
    applyCreatedId: (id) => {
      form.idChatLieu = id
    },
  },
  {
    key: 'idDeGiay',
    label: 'Đế giày *',
    optionsKey: 'deGiays',
    config: ATTRIBUTE_PAGE_CONFIGS.DE_GIAY,
    applyCreatedId: (id) => {
      form.idDeGiay = id
    },
  },
].map((field) => ({
  ...field,
  options: optionState.value[field.optionsKey] || [],
})))

const resetForm = () => {
  const source = props.product || {}
  Object.assign(form, createEmptyForm(), {
    maSanPham: source.maSanPham || '',
    tenSanPham: source.tenSanPham || '',
    idDanhMuc: source.idDanhMuc || '',
    idThuongHieu: source.idThuongHieu || '',
    idXuatXu: source.idXuatXu || '',
    idMucDichChay: source.idMucDichChay || '',
    idCoGiay: source.idCoGiay || '',
    idChatLieu: source.idChatLieu || '',
    idDeGiay: source.idDeGiay || '',
    gioiTinhKhachHang: source.gioiTinhKhachHang || 'UNISEX',
    hinhAnh: source.hinhAnh || '',
    moTaNgan: source.moTaNgan || '',
    moTaChiTiet: source.moTaChiTiet || '',
    trangThai: source.trangThai || 'DANG_HOAT_DONG',
    variants: [],
  })
}

watch(
  () => props.options,
  (value) => {
    optionState.value = createOptionState(value)
  },
  { immediate: true, deep: true },
)

watch(
  () => [props.open, props.mode, props.product],
  () => {
    if (props.open) {
      resetForm()
    }
  },
  { immediate: true },
)

const normalizeText = (value) => {
  if (typeof value !== 'string') return value ?? null
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

const normalizeNumber = (value) => {
  if (value === '' || value === null || typeof value === 'undefined') {
    return null
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const formatOptionLabel = (option) => {
  if (!option) return ''
  return option.moTa ? `${option.ten} (${option.moTa})` : option.ten
}

const addInitialVariant = () => {
  form.variants.push(createEmptyVariant())
}

const removeInitialVariant = (index) => {
  form.variants.splice(index, 1)
}

const validateInitialVariants = () => {
  const combinations = new Set()
  for (const variant of form.variants) {
    if (!variant.idMauSac || !variant.idKichThuoc) {
      toast.error('Biến thể khởi tạo phải chọn đủ màu sắc và kích thước.')
      return false
    }
    const key = `${variant.idMauSac}::${variant.idKichThuoc}`
    if (combinations.has(key)) {
      toast.error('Danh sách biến thể khởi tạo đang bị trùng tổ hợp màu sắc và kích thước.')
      return false
    }
    combinations.add(key)
  }
  return true
}

const refreshOptions = async () => {
  const response = await getProductFormOptions()
  optionState.value = createOptionState(response)
  emit('options-refreshed', response)
  return response
}

const openQuickAddAttribute = (fieldDescriptor) => {
  quickAttributeModal.field = fieldDescriptor
  quickAttributeModal.open = true
}

const openVariantColorQuickAdd = (variant) => {
  openQuickAddAttribute({
    config: ATTRIBUTE_PAGE_CONFIGS.MAU_SAC,
    applyCreatedId: (id) => {
      variant.idMauSac = id
    },
  })
}

const openVariantSizeQuickAdd = (variant) => {
  openQuickAddAttribute({
    config: ATTRIBUTE_PAGE_CONFIGS.KICH_THUOC,
    applyCreatedId: (id) => {
      variant.idKichThuoc = id
    },
  })
}

const closeQuickAddAttribute = () => {
  quickAttributeModal.open = false
  quickAttributeModal.field = null
}

const handleQuickAddAttribute = async (payload) => {
  if (!quickAttributeModal.field?.config?.endpoint) return
  quickAttributeModal.submitting = true
  try {
    const created = await createAttribute(quickAttributeModal.field.config.endpoint, payload)
    await refreshOptions()
    quickAttributeModal.field.applyCreatedId?.(created.id)
    toast.success(`Đã thêm nhanh ${quickAttributeModal.field.config.itemLabel}.`)
    closeQuickAddAttribute()
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  } finally {
    quickAttributeModal.submitting = false
  }
}

const handleMainImageUpload = async (file) => {
  if (!file) return
  uploadingImage.value = true
  try {
    const response = await uploadProductFile(file, 'aerostride/products/main')
    form.hinhAnh = response.fileUrl || ''
    toast.success('Tải ảnh đại diện thành công.')
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  } finally {
    uploadingImage.value = false
  }
}

const handleSubmit = () => {
  const requiredFields = productAttributeFields.value.filter((field) => !form[field.key])
  if (!form.tenSanPham || requiredFields.length) {
    toast.error('Vui lòng nhập đủ thông tin bắt buộc của sản phẩm.')
    return
  }

  if (isCreateMode.value && !validateInitialVariants()) {
    return
  }

  const payload = {
    maSanPham: null,
    tenSanPham: normalizeText(form.tenSanPham),
    idDanhMuc: form.idDanhMuc,
    idThuongHieu: form.idThuongHieu,
    idXuatXu: form.idXuatXu,
    idMucDichChay: form.idMucDichChay,
    idCoGiay: form.idCoGiay,
    idChatLieu: form.idChatLieu,
    idDeGiay: form.idDeGiay,
    gioiTinhKhachHang: form.gioiTinhKhachHang,
    hinhAnh: normalizeText(form.hinhAnh),
    moTaNgan: normalizeText(form.moTaNgan),
    moTaChiTiet: normalizeText(form.moTaChiTiet),
    trangThai: form.trangThai,
  }

  if (isCreateMode.value) {
    payload.variants = form.variants.map((variant) => ({
      idMauSac: variant.idMauSac,
      idKichThuoc: variant.idKichThuoc,
      maChiTietSanPham: null,
      soLuong: normalizeNumber(variant.soLuong) ?? 0,
      giaNhap: normalizeNumber(variant.giaNhap),
      giaBan: normalizeNumber(variant.giaBan) ?? 0,
      trangThai: variant.trangThai,
      images: [],
    }))
  }

  emit('submit', payload)
}
</script>

<template>
  <ProductModalShell
    :open="open"
    :title="isCreateMode ? 'Tạo sản phẩm mới' : 'Cập nhật sản phẩm'"
    description="Màn sản phẩm chỉ quản lý thông tin sản phẩm chính; biến thể và ảnh biến thể được xử lý ở màn riêng."
    width-class="max-w-6xl"
    @close="$emit('close')"
  >
    <form class="space-y-8 px-6 py-6" @submit.prevent="handleSubmit">
      <section class="grid gap-6 lg:grid-cols-[1.25fr_0.9fr]">
        <div class="space-y-6">
          <div class="grid gap-4 md:grid-cols-[1.1fr_1fr]">
            <div class="rounded-3xl border border-dashed border-slate-200 bg-slate-50/80 px-4 py-3">
              <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Mã sản phẩm</p>
              <p class="mt-2 text-sm font-semibold text-obsidian">{{ generatedProductCodeLabel }}</p>
              <p class="mt-1 text-xs text-slate-500">FE không cho nhập tay, backend sẽ tự tạo và giữ ổn định mã.</p>
            </div>
            <label class="space-y-2">
              <span class="text-sm font-semibold text-slate-700">Tên sản phẩm *</span>
              <input
                v-model="form.tenSanPham"
                type="text"
                required
                maxlength="255"
                class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
                placeholder="Nike Pegasus 41"
              />
            </label>
          </div>

          <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            <label
              v-for="field in productAttributeFields"
              :key="field.key"
              class="space-y-2"
            >
              <span class="flex items-center justify-between gap-3 text-sm font-semibold text-slate-700">
                <span>{{ field.label }}</span>
                <button
                  type="button"
                  class="inline-flex h-8 w-8 items-center justify-center rounded-xl border border-slate-200 text-slate-500 transition hover:border-aurora hover:bg-aurora/5 hover:text-aurora"
                  @click="openQuickAddAttribute(field)"
                >
                  <Plus class="h-4 w-4" />
                </button>
              </span>
              <select
                v-model="form[field.key]"
                required
                class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
              >
                <option value="">Chọn</option>
                <option v-for="option in field.options" :key="option.id" :value="option.id">
                  {{ formatOptionLabel(option) }}
                </option>
              </select>
            </label>

            <label class="space-y-2">
              <span class="text-sm font-semibold text-slate-700">Giới tính khách hàng *</span>
              <select
                v-model="form.gioiTinhKhachHang"
                required
                class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
              >
                <option v-for="option in genderOptions" :key="option" :value="option">
                  {{ option }}
                </option>
              </select>
            </label>

            <label class="space-y-2">
              <span class="text-sm font-semibold text-slate-700">Trạng thái *</span>
              <select
                v-model="form.trangThai"
                required
                class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
              >
                <option v-for="option in statusOptions" :key="option" :value="option">
                  {{ option }}
                </option>
              </select>
            </label>
          </div>

          <div class="grid gap-4 md:grid-cols-2">
            <label class="space-y-2">
              <span class="text-sm font-semibold text-slate-700">Mô tả ngắn</span>
              <textarea
                v-model="form.moTaNgan"
                rows="4"
                maxlength="1000"
                class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
                placeholder="Daily trainer, êm, nhẹ, bền..."
              />
            </label>
            <label class="space-y-2">
              <span class="text-sm font-semibold text-slate-700">Mô tả chi tiết</span>
              <textarea
                v-model="form.moTaChiTiet"
                rows="4"
                class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
                placeholder="Thông số upper, midsole, outsole, mục đích sử dụng..."
              />
            </label>
          </div>
        </div>

        <div class="rounded-[28px] border border-slate-200 bg-slate-50/70 p-5">
          <div class="mb-4 flex items-center justify-between gap-3">
            <div>
              <h3 class="font-display text-lg font-bold text-obsidian">Ảnh đại diện sản phẩm</h3>
              <p class="text-sm text-slate-500">Có thể tải file lên hoặc dán URL.</p>
            </div>
            <div v-if="uploadingImage" class="rounded-full bg-aurora/10 px-3 py-1 text-xs font-semibold text-aurora">
              Đang tải ảnh...
            </div>
          </div>

          <AdminFileUploader type="image" :model-value="form.hinhAnh" label="Ảnh đại diện" @change="handleMainImageUpload" />

          <label class="mt-4 block space-y-2">
            <span class="text-sm font-semibold text-slate-700">Hoặc nhập URL ảnh</span>
            <div class="flex items-center gap-3">
              <input
                v-model="form.hinhAnh"
                type="url"
                maxlength="1000"
                class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
                placeholder="https://..."
              />
              <button
                v-if="form.hinhAnh"
                type="button"
                class="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-slate-200 text-slate-500 transition hover:border-rose-200 hover:bg-rose-50 hover:text-rose-500"
                @click="form.hinhAnh = ''"
              >
                <Trash2 class="h-4 w-4" />
              </button>
            </div>
          </label>

          <div class="mt-4 overflow-hidden rounded-[24px] border border-slate-200 bg-white">
            <div class="flex aspect-[4/3] items-center justify-center bg-slate-100">
              <SafeImage
                v-if="form.hinhAnh"
                :src="form.hinhAnh"
                alt="Ảnh sản phẩm"
                fallback="https://placehold.co/600x400/f8fafc/94a3b8?text=AeroStride"
                class="h-full w-full object-cover"
              />
              <div v-else class="flex flex-col items-center gap-2 text-slate-400">
                <ImagePlus class="h-8 w-8" />
                <span class="text-sm font-medium">Chưa có ảnh đại diện</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-if="isCreateMode" class="rounded-[28px] border border-slate-200 bg-white">
        <div class="flex items-center justify-between gap-3 border-b border-slate-100 px-5 py-4">
          <div>
            <h3 class="font-display text-lg font-bold text-obsidian">Biến thể khởi tạo</h3>
            <p class="text-sm text-slate-500">Có thể tạo sẵn biến thể ngay khi thêm sản phẩm. Chi tiết biến thể sẽ quản lý ở màn riêng.</p>
          </div>
          <button
            type="button"
            class="inline-flex items-center gap-2 rounded-2xl bg-obsidian px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-obsidian/90"
            @click="addInitialVariant"
          >
            <Plus class="h-4 w-4" />
            Thêm biến thể
          </button>
        </div>

        <div class="space-y-4 px-5 py-5">
          <div v-if="form.variants.length === 0" class="rounded-3xl border border-dashed border-slate-200 bg-slate-50 px-6 py-8 text-center">
            <p class="text-sm font-medium text-slate-500">Chưa có biến thể khởi tạo. Bạn có thể thêm sau ở màn quản lý biến thể.</p>
          </div>

          <div
            v-for="(variant, index) in form.variants"
            :key="`${index}-${variant.idMauSac}-${variant.idKichThuoc}`"
            class="rounded-3xl border border-slate-200 bg-slate-50/70 p-4"
          >
            <div class="mb-4 flex items-center justify-between gap-3">
              <div class="text-sm font-semibold text-obsidian">Biến thể {{ index + 1 }}</div>
              <button
                type="button"
                class="inline-flex h-10 w-10 items-center justify-center rounded-2xl border border-slate-200 text-slate-500 transition hover:border-rose-200 hover:bg-rose-50 hover:text-rose-500"
                @click="removeInitialVariant(index)"
              >
                <Trash2 class="h-4 w-4" />
              </button>
            </div>

            <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              <label class="space-y-2">
                <span class="flex items-center justify-between gap-3 text-sm font-semibold text-slate-700">
                  <span>Màu sắc *</span>
                  <button
                    type="button"
                    class="inline-flex h-8 w-8 items-center justify-center rounded-xl border border-slate-200 text-slate-500 transition hover:border-aurora hover:bg-aurora/5 hover:text-aurora"
                    @click="openVariantColorQuickAdd(variant)"
                  >
                    <Plus class="h-4 w-4" />
                  </button>
                </span>
                <select
                  v-model="variant.idMauSac"
                  required
                  class="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
                >
                  <option value="">Chọn màu sắc</option>
                  <option v-for="option in optionState.mauSacs" :key="option.id" :value="option.id">
                    {{ formatOptionLabel(option) }}
                  </option>
                </select>
              </label>

              <label class="space-y-2">
                <span class="flex items-center justify-between gap-3 text-sm font-semibold text-slate-700">
                  <span>Kích thước *</span>
                  <button
                    type="button"
                    class="inline-flex h-8 w-8 items-center justify-center rounded-xl border border-slate-200 text-slate-500 transition hover:border-aurora hover:bg-aurora/5 hover:text-aurora"
                    @click="openVariantSizeQuickAdd(variant)"
                  >
                    <Plus class="h-4 w-4" />
                  </button>
                </span>
                <select
                  v-model="variant.idKichThuoc"
                  required
                  class="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
                >
                  <option value="">Chọn kích thước</option>
                  <option v-for="option in optionState.kichThuocs" :key="option.id" :value="option.id">
                    {{ formatOptionLabel(option) }}
                  </option>
                </select>
              </label>

              <div class="space-y-2">
                <span class="text-sm font-semibold text-slate-700">Mã biến thể</span>
                <div class="rounded-2xl border border-dashed border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-500">
                  Hệ thống sẽ tự sinh sau khi lưu sản phẩm
                </div>
              </div>
              <label class="space-y-2">
                <span class="text-sm font-semibold text-slate-700">Số lượng *</span>
                <input
                  v-model.number="variant.soLuong"
                  type="number"
                  min="0"
                  class="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
                />
              </label>
              <label class="space-y-2">
                <span class="text-sm font-semibold text-slate-700">Giá nhập</span>
                <input
                  v-model.number="variant.giaNhap"
                  type="number"
                  min="0"
                  step="1000"
                  class="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
                />
              </label>
              <label class="space-y-2">
                <span class="text-sm font-semibold text-slate-700">Giá bán *</span>
                <input
                  v-model.number="variant.giaBan"
                  type="number"
                  min="0"
                  step="1000"
                  class="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
                />
              </label>
            </div>
          </div>
        </div>
      </section>

      <div class="flex flex-wrap items-center justify-end gap-3 border-t border-slate-100 pt-2">
        <button
          type="button"
          class="rounded-2xl border border-slate-200 px-5 py-3 text-sm font-semibold text-slate-600 transition hover:bg-slate-50"
          @click="$emit('close')"
        >
          Hủy
        </button>
        <button
          type="submit"
          class="rounded-2xl bg-aurora px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-aurora/20 transition hover:bg-aurora/90 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="submitting || uploadingImage"
        >
          {{ submitting ? 'Đang lưu...' : isCreateMode ? 'Tạo sản phẩm' : 'Lưu thay đổi' }}
        </button>
      </div>
    </form>

    <AttributeFormModal
      :open="quickAttributeModal.open"
      :mode="'create'"
      :item="null"
      :title="quickAttributeModal.field?.config?.subtitle || ''"
      :item-label="quickAttributeModal.field?.config?.itemLabel || 'thuộc tính'"
      :extra-field="quickAttributeModal.field?.config?.extraField || null"
      :submitting="quickAttributeModal.submitting"
      @close="closeQuickAddAttribute"
      @submit="handleQuickAddAttribute"
    />
  </ProductModalShell>
</template>
