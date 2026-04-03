<script setup>
import { computed, reactive, watch } from 'vue'
import { useToast } from '@/composable/useToast'

const props = defineProps({
  open: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },
  item: { type: Object, default: null },
  title: { type: String, required: true },
  itemLabel: { type: String, required: true },
  extraField: { type: Object, default: null },
  submitting: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'submit'])
const toast = useToast()

const form = reactive({
  ma: '',
  ten: '',
  moTa: '',
  trangThai: 'DANG_HOAT_DONG',
})

const isCreateMode = computed(() => props.mode === 'create')
const statusOptions = [
  { value: 'DANG_HOAT_DONG', label: 'Đang hoạt động' },
  { value: 'KHONG_HOAT_DONG', label: 'Ngừng hoạt động' },
]

const resetForm = () => {
  Object.assign(form, {
    ma: props.item?.ma || '',
    ten: props.item?.ten || '',
    moTa: props.item?.moTa || '',
    trangThai: props.item?.trangThai || 'DANG_HOAT_DONG',
  })
}

watch(
  () => [props.open, props.mode, props.item],
  () => {
    if (props.open) {
      resetForm()
    }
  },
  { immediate: true },
)

const normalize = (value) => {
  if (typeof value !== 'string') return value ?? null
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

const validateColorHex = () => {
  if (props.extraField?.type !== 'color' || !form.moTa) return true
  return /^#([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6})$/.test(form.moTa.trim())
}

const handleSubmit = () => {
  if (!normalize(form.ten)) {
    toast.error(`Vui lòng nhập tên ${props.itemLabel}.`)
    return
  }

  if (!validateColorHex()) {
    toast.error('Mã màu HEX không hợp lệ. Ví dụ: #0EA5E9')
    return
  }

  emit('submit', {
    ma: normalize(form.ma),
    ten: normalize(form.ten),
    moTa: props.extraField ? normalize(form.moTa) : null,
    trangThai: form.trangThai,
  })
}
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="open"
        class="fixed inset-0 z-[1200] flex items-center justify-center bg-slate-950/65 p-4 backdrop-blur-sm"
        @click.self="$emit('close')"
      >
        <div class="w-full max-w-2xl overflow-hidden rounded-[28px] border border-white/10 bg-white shadow-2xl">
          <div class="border-b border-slate-100 bg-slate-50/80 px-6 py-5">
            <div class="flex items-start justify-between gap-4">
              <div class="space-y-1">
                <h2 class="text-xl font-display font-bold text-obsidian">
                  {{ isCreateMode ? `Thêm ${itemLabel}` : `Cập nhật ${itemLabel}` }}
                </h2>
                <p class="text-sm text-slate-500">{{ title }}</p>
              </div>
              <button
                type="button"
                class="inline-flex h-10 w-10 items-center justify-center rounded-2xl border border-slate-200 text-slate-500 transition hover:border-rose-200 hover:bg-rose-50 hover:text-rose-500"
                @click="$emit('close')"
              >
                <span class="text-lg leading-none">×</span>
              </button>
            </div>
          </div>

          <form class="space-y-6 px-6 py-6" @submit.prevent="handleSubmit">
            <div class="grid gap-4 md:grid-cols-2">
              <label class="space-y-2">
                <span class="text-sm font-semibold text-slate-700">Mã {{ itemLabel }}</span>
                <input
                  v-model="form.ma"
                  type="text"
                  maxlength="50"
                  class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
                  placeholder="Để trống nếu backend tự sinh"
                />
              </label>

              <label class="space-y-2">
                <span class="text-sm font-semibold text-slate-700">Tên {{ itemLabel }} *</span>
                <input
                  v-model="form.ten"
                  type="text"
                  maxlength="255"
                  required
                  class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
                  :placeholder="`Nhập tên ${itemLabel}`"
                />
              </label>

              <label v-if="extraField" class="space-y-2 md:col-span-2">
                <span class="text-sm font-semibold text-slate-700">{{ extraField.label }}</span>
                <div class="flex items-center gap-3">
                  <input
                    v-model="form.moTa"
                    type="text"
                    maxlength="255"
                    class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
                    :placeholder="extraField.placeholder"
                  />
                  <div
                    v-if="extraField.type === 'color' && form.moTa"
                    class="h-12 w-12 rounded-2xl border border-slate-200"
                    :style="{ backgroundColor: form.moTa }"
                  ></div>
                </div>
                <p v-if="extraField.hint" class="text-xs text-slate-500">{{ extraField.hint }}</p>
              </label>

              <label class="space-y-2">
                <span class="text-sm font-semibold text-slate-700">Trạng thái</span>
                <select
                  v-model="form.trangThai"
                  class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-aurora focus:ring-4 focus:ring-aurora/10"
                >
                  <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
            </div>

            <div class="flex items-center justify-end gap-3 border-t border-slate-100 pt-2">
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
                :disabled="submitting"
              >
                {{ submitting ? 'Đang lưu...' : isCreateMode ? 'Tạo mới' : 'Lưu thay đổi' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
