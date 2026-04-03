<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { Trash2, ImagePlus } from 'lucide-vue-next'
import ProductModalShell from './ProductModalShell.vue'
import AdminFileUploader from '@/components/admin/shared/AdminFileUploader.vue'
import { uploadProductFile } from '@/services/sanPhamService'
import { useToast } from '@/composable/useToast'

const props = defineProps({
  open: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  image: { type: Object, default: null },
  submitting: { type: Boolean, default: false },
  statuses: { type: Array, default: () => ['DANG_HOAT_DONG', 'KHONG_HOAT_DONG'] },
})

const emit = defineEmits(['close', 'submit'])
const toast = useToast()
const uploading = ref(false)

const form = reactive({
  duongDanAnh: '',
  moTa: '',
  hinhAnhDaiDien: false,
  trangThai: 'DANG_HOAT_DONG',
})

const isCreateMode = computed(() => props.mode === 'create')

watch(
  () => [props.open, props.image],
  () => {
    if (!props.open) return
    Object.assign(form, {
      duongDanAnh: props.image?.duongDanAnh || '',
      moTa: props.image?.moTa || '',
      hinhAnhDaiDien: Boolean(props.image?.hinhAnhDaiDien),
      trangThai: props.image?.trangThai || 'DANG_HOAT_DONG',
    })
  },
  { immediate: true },
)

const normalizeText = (value) => {
  if (typeof value !== 'string') return value ?? null
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

const handleUpload = async (file) => {
  if (!file) return
  uploading.value = true
  try {
    const response = await uploadProductFile(file, 'aerostride/products/variants')
    form.duongDanAnh = response.fileUrl || ''
    toast.success('Tải ảnh biến thể thành công.')
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  } finally {
    uploading.value = false
  }
}

const handleSubmit = () => {
  if (!form.duongDanAnh) {
    toast.error('Vui lòng tải hoặc nhập URL ảnh biến thể.')
    return
  }

  emit('submit', {
    duongDanAnh: normalizeText(form.duongDanAnh),
    moTa: normalizeText(form.moTa),
    hinhAnhDaiDien: Boolean(form.hinhAnhDaiDien),
    trangThai: form.trangThai,
  })
}
</script>

<template>
  <ProductModalShell
    :open="open"
    :title="isCreateMode ? 'Thêm ảnh biến thể' : 'Cập nhật ảnh biến thể'"
    description="Mỗi biến thể chỉ nên có một ảnh đại diện đang hoạt động."
    width-class="max-w-3xl"
    @close="$emit('close')"
  >
    <form class="space-y-6 px-6 py-6" @submit.prevent="handleSubmit">
      <div class="grid gap-6 lg:grid-cols-[1fr_0.95fr]">
        <div class="space-y-4">
          <div class="flex items-center justify-between gap-3">
            <div class="text-sm text-slate-500">Ảnh có thể tải lên từ máy hoặc dán URL.</div>
            <div v-if="uploading" class="rounded-full bg-aurora/10 px-3 py-1 text-xs font-semibold text-aurora">
              Đang tải ảnh...
            </div>
          </div>

          <AdminFileUploader type="image" :model-value="form.duongDanAnh" label="Ảnh biến thể" @change="handleUpload" />

          <label class="block space-y-2">
            <span class="text-sm font-semibold text-slate-700">URL ảnh *</span>
            <div class="flex items-center gap-3">
              <input v-model="form.duongDanAnh" type="url" maxlength="1000" class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10" placeholder="https://..." />
              <button v-if="form.duongDanAnh" type="button" class="inline-flex h-11 w-11 items-center justify-center rounded-2xl border border-slate-200 text-slate-500 transition hover:border-rose-200 hover:bg-rose-50 hover:text-rose-500" @click="form.duongDanAnh = ''">
                <Trash2 class="h-4 w-4" />
              </button>
            </div>
          </label>

          <label class="block space-y-2">
            <span class="text-sm font-semibold text-slate-700">Mô tả ảnh</span>
            <textarea v-model="form.moTa" rows="4" maxlength="1000" class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10" placeholder="Góc chụp, phối màu, chi tiết upper..." />
          </label>

          <div class="grid gap-4 md:grid-cols-2">
            <label class="space-y-2">
              <span class="text-sm font-semibold text-slate-700">Trạng thái *</span>
              <select v-model="form.trangThai" class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10">
                <option v-for="status in statuses" :key="status" :value="status">
                  {{ status }}
                </option>
              </select>
            </label>

            <label class="flex items-center justify-between rounded-2xl border border-slate-200 px-4 py-3">
              <div>
                <div class="text-sm font-semibold text-slate-700">Ảnh đại diện</div>
                <div class="text-xs text-slate-500">Nếu bật, backend sẽ tự xử lý ảnh đại diện cũ.</div>
              </div>
              <input v-model="form.hinhAnhDaiDien" type="checkbox" class="h-5 w-5 rounded border-slate-300 text-aurora focus:ring-aurora" />
            </label>
          </div>
        </div>

        <div class="rounded-[28px] border border-slate-200 bg-slate-50/70 p-5">
          <h3 class="mb-4 font-display text-lg font-bold text-obsidian">Xem trước</h3>
          <div class="overflow-hidden rounded-[24px] border border-slate-200 bg-white">
            <div class="flex aspect-[4/3] items-center justify-center bg-slate-100">
              <img v-if="form.duongDanAnh" :src="form.duongDanAnh" alt="Preview" class="h-full w-full object-cover" />
              <div v-else class="flex flex-col items-center gap-2 text-slate-400">
                <ImagePlus class="h-8 w-8" />
                <span class="text-sm font-medium">Chưa có ảnh</span>
              </div>
            </div>
          </div>
          <div class="mt-4 rounded-2xl bg-white p-4 text-sm text-slate-600">
            <p class="font-semibold text-obsidian">{{ form.moTa || 'Chưa có mô tả ảnh.' }}</p>
            <p class="mt-2 text-xs uppercase tracking-[0.2em] text-slate-400">
              {{ form.hinhAnhDaiDien ? 'Ảnh đại diện' : 'Ảnh phụ' }} • {{ form.trangThai }}
            </p>
          </div>
        </div>
      </div>

      <div class="flex flex-wrap items-center justify-end gap-3 border-t border-slate-100 pt-2">
        <button type="button" class="rounded-2xl border border-slate-200 px-5 py-3 text-sm font-semibold text-slate-600 transition hover:bg-slate-50" @click="$emit('close')">
          Hủy
        </button>
        <button type="submit" class="rounded-2xl bg-obsidian px-5 py-3 text-sm font-semibold text-white transition hover:bg-obsidian/90 disabled:cursor-not-allowed disabled:opacity-60" :disabled="submitting || uploading">
          {{ submitting ? 'Đang lưu...' : isCreateMode ? 'Thêm ảnh' : 'Cập nhật ảnh' }}
        </button>
      </div>
    </form>
  </ProductModalShell>
</template>
