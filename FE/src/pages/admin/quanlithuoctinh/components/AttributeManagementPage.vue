<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Pencil, Plus, RefreshCcw, Search, Trash2 } from 'lucide-vue-next'
import AdminPageHeader from '@/components/admin/shared/AdminPageHeader.vue'
import AeroPagination from '@/components/base/AeroPagination.vue'
import { useToast } from '@/composable/useToast'
import AttributeFormModal from './AttributeFormModal.vue'
import { createAttribute, deleteAttribute, getAttributeDetail, getAttributes, updateAttribute } from '@/services/thuocTinhService'

const props = defineProps({
  endpoint: { type: String, required: true },
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  itemLabel: { type: String, required: true },
  searchPlaceholder: { type: String, default: 'Tìm kiếm...' },
  extraField: { type: Object, default: null },
  listRouteName: { type: String, default: '' },
  createRouteName: { type: String, default: '' },
  editRouteName: { type: String, default: '' },
})

const toast = useToast()
const route = useRoute()
const router = useRouter()
const rows = ref([])
const loading = ref(false)
const filters = reactive({ keyword: '', trangThai: '' })
const pagination = reactive({ page: 1, size: 10, totalPages: 1, totalElements: 0 })
const modal = reactive({ open: false, mode: 'create', submitting: false, item: null })

const statusOptions = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'DANG_HOAT_DONG', label: 'Đang hoạt động' },
  { value: 'KHONG_HOAT_DONG', label: 'Ngừng hoạt động' },
]

const numberFormatter = new Intl.NumberFormat('vi-VN')

const getStatusLabel = (status) => {
  if (status === 'DANG_HOAT_DONG') return 'Đang hoạt động'
  if (status === 'KHONG_HOAT_DONG') return 'Ngừng hoạt động'
  if (status === 'DA_XOA') return 'Đã xóa'
  return status || '--'
}

const getStatusClass = (status) => {
  if (status === 'DANG_HOAT_DONG') return 'bg-emerald-100 text-emerald-700'
  if (status === 'KHONG_HOAT_DONG') return 'bg-amber-100 text-amber-700'
  if (status === 'DA_XOA') return 'bg-rose-100 text-rose-700'
  return 'bg-slate-100 text-slate-600'
}

const formatDate = (timestamp) => {
  if (!timestamp) return '--'
  const numericValue = Number(timestamp)
  const safeValue = numericValue < 1000000000000 ? numericValue * 1000 : numericValue
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(safeValue))
}

const fetchRows = async () => {
  loading.value = true
  try {
    const response = await getAttributes(props.endpoint, {
      page: pagination.page,
      size: pagination.size,
      keyword: filters.keyword || '',
      trangThai: filters.trangThai || '',
      sortBy: 'ngayTao',
      sortDirection: 'desc',
    })

    rows.value = response.content || []
    pagination.totalPages = Math.max(response.totalPages || 1, 1)
    pagination.totalElements = response.totalElements || 0

    if (pagination.page > pagination.totalPages) {
      pagination.page = pagination.totalPages
      await fetchRows()
    }
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  } finally {
    loading.value = false
  }
}

const applyFilters = async () => {
  pagination.page = 1
  await fetchRows()
}

const resetFilters = async () => {
  Object.assign(filters, { keyword: '', trangThai: '' })
  pagination.page = 1
  await fetchRows()
}

const openCreateModal = () => {
  if (!props.createRouteName) return
  router.push({ name: props.createRouteName })
}

const openEditModal = (item) => {
  if (!props.editRouteName) return
  router.push({ name: props.editRouteName, params: { id: item.id } })
}

const closeModal = () => {
  if (props.listRouteName && route.name !== props.listRouteName) {
    router.push({ name: props.listRouteName })
    return
  }
  modal.open = false
  modal.item = null
}

const handleSubmit = async (payload) => {
  modal.submitting = true
  try {
    if (modal.mode === 'create') {
      await createAttribute(props.endpoint, payload)
      toast.success(`Thêm ${props.itemLabel} thành công.`)
      pagination.page = 1
    } else {
      await updateAttribute(props.endpoint, modal.item.id, payload)
      toast.success(`Cập nhật ${props.itemLabel} thành công.`)
    }
    closeModal()
    await fetchRows()
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  } finally {
    modal.submitting = false
  }
}

const requestDelete = async (item) => {
  if (!window.confirm(`Xóa mềm ${props.itemLabel} ${item.ten}?`)) return
  try {
    await deleteAttribute(props.endpoint, item.id)
    if (rows.value.length === 1 && pagination.page > 1) {
      pagination.page -= 1
    }
    toast.success(`Đã xóa mềm ${props.itemLabel}.`)
    await fetchRows()
  } catch (error) {
    // Toast is handled globally by the Axios interceptor.
  }
}

const handlePageChange = async (page) => {
  pagination.page = page
  await fetchRows()
}

const handleSizeChange = async (size) => {
  pagination.size = size
  pagination.page = 1
  await fetchRows()
}

onMounted(fetchRows)

const syncModalWithRoute = async () => {
  const routeName = route.name?.toString()

  if (routeName === props.createRouteName) {
    modal.mode = 'create'
    modal.item = null
    modal.open = true
    return
  }

  if (routeName === props.editRouteName && route.params.id) {
    modal.mode = 'edit'
    modal.open = true

    try {
      modal.item = await getAttributeDetail(props.endpoint, route.params.id)
    } catch (error) {
      if (props.listRouteName) {
        router.push({ name: props.listRouteName })
      }
    }
    return
  }

  modal.open = false
  modal.item = null
}

watch(
  () => [route.name, route.params.id, props.endpoint],
  () => {
    syncModalWithRoute()
  },
  { immediate: true },
)
</script>

<template>
  <div class="space-y-6">
    <AdminPageHeader :title="title" :subtitle="subtitle" />

    <section class="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm">
      <div class="grid gap-4 xl:grid-cols-[1.5fr_0.7fr_auto]">
        <label class="space-y-2">
          <span class="text-sm font-semibold text-slate-700">Tìm kiếm</span>
          <div class="flex items-center rounded-2xl border border-slate-200 px-4 py-3">
            <Search class="mr-3 h-4 w-4 text-slate-400" />
            <input
              v-model="filters.keyword"
              type="text"
              class="w-full text-sm outline-none"
              :placeholder="searchPlaceholder"
              @keyup.enter="applyFilters"
            />
          </div>
        </label>

        <label class="space-y-2">
          <span class="text-sm font-semibold text-slate-700">Trạng thái</span>
          <select
            v-model="filters.trangThai"
            class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none"
          >
            <option v-for="option in statusOptions" :key="option.value || 'all'" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>

        <div class="flex items-end gap-3">
          <button
            class="rounded-2xl bg-aurora px-5 py-3 text-sm font-semibold text-white transition hover:bg-aurora/90"
            @click="applyFilters"
          >
            Áp dụng
          </button>
          <button
            class="rounded-2xl border border-slate-200 px-5 py-3 text-sm font-semibold text-slate-600 transition hover:bg-slate-50"
            @click="resetFilters"
          >
            Làm mới
          </button>
        </div>
      </div>

      <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
        <div class="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-500">
          {{ numberFormatter.format(pagination.totalElements) }} bản ghi
        </div>

        <div class="flex flex-wrap items-center gap-3">
          <button
            class="inline-flex items-center gap-2 rounded-2xl border border-slate-200 px-4 py-3 text-sm font-semibold text-slate-600 transition hover:bg-slate-50"
            @click="fetchRows"
          >
            <RefreshCcw class="h-4 w-4" />
            Tải lại
          </button>
          <button
            class="inline-flex items-center gap-2 rounded-2xl bg-obsidian px-4 py-3 text-sm font-semibold text-white transition hover:bg-obsidian/90"
            @click="openCreateModal"
          >
            <Plus class="h-4 w-4" />
            Thêm {{ itemLabel }}
          </button>
        </div>
      </div>
    </section>

    <section class="rounded-[28px] border border-slate-200 bg-white shadow-sm">
      <div class="border-b border-slate-100 px-5 py-4">
        <h2 class="font-display text-xl font-bold text-obsidian">Danh sách {{ itemLabel }}</h2>
        <p class="text-sm text-slate-500">Quản lý dữ liệu thuộc tính phục vụ form sản phẩm và chi tiết biến thể.</p>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead class="bg-slate-50">
            <tr>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">STT</th>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Mã</th>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Tên</th>
              <th
                v-if="extraField"
                class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400"
              >
                {{ extraField.tableLabel || 'Thông tin bổ sung' }}
              </th>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Trạng thái</th>
              <th class="px-5 py-4 text-left text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Cập nhật</th>
              <th class="px-5 py-4 text-right text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Thao tác</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-if="loading">
              <td :colspan="extraField ? 7 : 6" class="px-5 py-12 text-center text-sm font-medium text-slate-400">
                Đang tải dữ liệu {{ itemLabel }}...
              </td>
            </tr>
            <tr v-else-if="!rows.length">
              <td :colspan="extraField ? 7 : 6" class="px-5 py-12 text-center text-sm font-medium text-slate-400">
                Chưa có dữ liệu {{ itemLabel }} phù hợp bộ lọc.
              </td>
            </tr>
            <tr v-for="(item, index) in rows" :key="item.id" class="transition hover:bg-slate-50/70">
              <td class="px-5 py-4 text-sm font-semibold text-slate-500">
                {{ (pagination.page - 1) * pagination.size + index + 1 }}
              </td>
              <td class="px-5 py-4">
                <p class="text-xs font-semibold uppercase tracking-[0.2em] text-aurora">{{ item.ma || '--' }}</p>
              </td>
              <td class="px-5 py-4 font-semibold text-obsidian">{{ item.ten }}</td>
              <td v-if="extraField" class="px-5 py-4 text-sm text-slate-600">
                <div v-if="extraField.type === 'color' && item.moTa" class="flex items-center gap-3">
                  <span
                    class="h-5 w-5 rounded-full border border-white shadow-sm"
                    :style="{ backgroundColor: item.moTa }"
                  ></span>
                  <span>{{ item.moTa }}</span>
                </div>
                <span v-else>{{ item.moTa || '--' }}</span>
              </td>
              <td class="px-5 py-4">
                <span
                  :class="['inline-flex rounded-full px-3 py-1 text-xs font-semibold', getStatusClass(item.trangThai)]"
                >
                  {{ getStatusLabel(item.trangThai) }}
                </span>
              </td>
              <td class="px-5 py-4 text-sm text-slate-500">{{ formatDate(item.ngayCapNhat || item.ngayTao) }}</td>
              <td class="px-5 py-4">
                <div class="flex justify-end gap-2">
                  <button
                    class="rounded-xl border border-slate-200 p-2 text-slate-500 transition hover:border-aurora hover:bg-aurora/5 hover:text-aurora"
                    @click="openEditModal(item)"
                  >
                    <Pencil class="h-4 w-4" />
                  </button>
                  <button
                    class="rounded-xl border border-slate-200 p-2 text-slate-500 transition hover:border-rose-200 hover:bg-rose-50 hover:text-rose-500"
                    @click="requestDelete(item)"
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

    <AttributeFormModal
      :open="modal.open"
      :mode="modal.mode"
      :item="modal.item"
      :title="subtitle"
      :item-label="itemLabel"
      :extra-field="extraField"
      :submitting="modal.submitting"
      @close="closeModal"
      @submit="handleSubmit"
    />
  </div>
</template>
