<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { Plus } from 'lucide-vue-next'
import ProductModalShell from './ProductModalShell.vue'
import AttributeFormModal from '@/pages/admin/quanlithuoctinh/components/AttributeFormModal.vue'
import { useToast } from '@/composable/useToast'
import { ATTRIBUTE_PAGE_CONFIGS } from '@/pages/admin/quanlithuoctinh/attributeConfigs'
import { createAttribute } from '@/services/thuocTinhService'
import { getProductFormOptions } from '@/services/sanPhamService'

const props = defineProps({
  open: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  variant: { type: Object, default: null },
  options: { type: Object, default: () => ({}) },
  submitting: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'submit', 'options-refreshed'])
const toast = useToast()
const optionState = ref(createOptionState())

const quickAttributeModal = reactive({
  open: false,
  submitting: false,
  field: null,
})

function createOptionState(source = {}) {
  return {
    mauSacs: [...(source.mauSacs || [])],
    kichThuocs: [...(source.kichThuocs || [])],
    trangThais: [...(source.trangThais || [])],
  }
}

const form = reactive({
  idMauSac: '',
  idKichThuoc: '',
  maChiTietSanPham: '',
  soLuong: 0,
  giaNhap: '',
  giaBan: '',
  trangThai: 'DANG_HOAT_DONG',
})

const isCreateMode = computed(() => props.mode === 'create')
const statusOptions = computed(() => optionState.value.trangThais || ['DANG_HOAT_DONG', 'KHONG_HOAT_DONG'])
const generatedVariantCodeLabel = computed(() =>
  isCreateMode.value ? 'Hệ thống sẽ tự sinh sau khi lưu' : props.variant?.maChiTietSanPham || '--',
)

watch(
  () => props.options,
  (value) => {
    optionState.value = createOptionState(value)
  },
  { immediate: true, deep: true },
)

watch(
  () => [props.open, props.variant],
  () => {
    if (!props.open) return
    Object.assign(form, {
      idMauSac: props.variant?.idMauSac || '',
      idKichThuoc: props.variant?.idKichThuoc || '',
      maChiTietSanPham: props.variant?.maChiTietSanPham || '',
      soLuong: props.variant?.soLuong ?? 0,
      giaNhap: props.variant?.giaNhap ?? '',
      giaBan: props.variant?.giaBan ?? '',
      trangThai: props.variant?.trangThai || 'DANG_HOAT_DONG',
    })
  },
  { immediate: true },
)

const normalizeText = (value) => {
  if (typeof value !== 'string') return value ?? null
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

const normalizeNumber = (value) => {
  if (value === '' || value === null || typeof value === 'undefined') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const formatOptionLabel = (option) => {
  if (!option) return ''
  return option.moTa ? `${option.ten} (${option.moTa})` : option.ten
}

const refreshOptions = async () => {
  const response = await getProductFormOptions()
  optionState.value = createOptionState(response)
  emit('options-refreshed', response)
}

const openQuickAddAttribute = (fieldDescriptor) => {
  quickAttributeModal.field = fieldDescriptor
  quickAttributeModal.open = true
}

const openColorQuickAdd = () => {
  openQuickAddAttribute({
    config: ATTRIBUTE_PAGE_CONFIGS.MAU_SAC,
    applyCreatedId: (id) => {
      form.idMauSac = id
    },
  })
}

const openSizeQuickAdd = () => {
  openQuickAddAttribute({
    config: ATTRIBUTE_PAGE_CONFIGS.KICH_THUOC,
    applyCreatedId: (id) => {
      form.idKichThuoc = id
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

const handleSubmit = () => {
  if (!form.idMauSac || !form.idKichThuoc) {
    toast.error('Vui lòng chọn đầy đủ màu sắc và kích thước cho biến thể.')
    return
  }
  if (normalizeNumber(form.soLuong) === null || normalizeNumber(form.giaBan) === null) {
    toast.error('Số lượng và giá bán của biến thể không được để trống.')
    return
  }

  emit('submit', {
    idMauSac: form.idMauSac,
    idKichThuoc: form.idKichThuoc,
    maChiTietSanPham: null,
    soLuong: normalizeNumber(form.soLuong) ?? 0,
    giaNhap: normalizeNumber(form.giaNhap),
    giaBan: normalizeNumber(form.giaBan) ?? 0,
    trangThai: form.trangThai,
    images: [],
  })
}
</script>

<template>
  <ProductModalShell
    :open="open"
    :title="isCreateMode ? 'Thêm biến thể sản phẩm' : 'Cập nhật biến thể'"
    description="Biến thể là tổ hợp duy nhất của sản phẩm, màu sắc và kích thước."
    width-class="max-w-3xl"
    @close="$emit('close')"
  >
    <form class="space-y-6 px-6 py-6" @submit.prevent="handleSubmit">
      <div class="grid gap-4 md:grid-cols-2">
        <label class="space-y-2">
          <span class="flex items-center justify-between gap-3 text-sm font-semibold text-slate-700">
            <span>Màu sắc *</span>
            <button
              type="button"
              class="inline-flex h-8 w-8 items-center justify-center rounded-xl border border-slate-200 text-slate-500 transition hover:border-aurora hover:bg-aurora/5 hover:text-aurora"
              @click="openColorQuickAdd"
            >
              <Plus class="h-4 w-4" />
            </button>
          </span>
          <select
            v-model="form.idMauSac"
            required
            class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
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
              @click="openSizeQuickAdd"
            >
              <Plus class="h-4 w-4" />
            </button>
          </span>
          <select
            v-model="form.idKichThuoc"
            required
            class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
          >
            <option value="">Chọn kích thước</option>
            <option v-for="option in optionState.kichThuocs" :key="option.id" :value="option.id">
              {{ formatOptionLabel(option) }}
            </option>
          </select>
        </label>

        <div class="space-y-2">
          <span class="text-sm font-semibold text-slate-700">Mã biến thể</span>
          <div class="rounded-2xl border border-dashed border-slate-200 bg-slate-50/80 px-4 py-3 text-sm font-medium text-slate-600">
            {{ generatedVariantCodeLabel }}
          </div>
        </div>

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

        <label class="space-y-2">
          <span class="text-sm font-semibold text-slate-700">Số lượng *</span>
          <input
            v-model.number="form.soLuong"
            type="number"
            min="0"
            class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
          />
        </label>

        <label class="space-y-2">
          <span class="text-sm font-semibold text-slate-700">Giá nhập</span>
          <input
            v-model.number="form.giaNhap"
            type="number"
            min="0"
            step="1000"
            class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
          />
        </label>

        <label class="space-y-2 md:col-span-2">
          <span class="text-sm font-semibold text-slate-700">Giá bán *</span>
          <input
            v-model.number="form.giaBan"
            type="number"
            min="0"
            step="1000"
            class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
          />
        </label>
      </div>

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
          class="rounded-2xl bg-obsidian px-5 py-3 text-sm font-semibold text-white transition hover:bg-obsidian/90 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="submitting"
        >
          {{ submitting ? 'Đang lưu...' : isCreateMode ? 'Thêm biến thể' : 'Cập nhật biến thể' }}
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
