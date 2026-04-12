<template>
  <div class="page">
    <PageHeader :title="title" />

    <AppCard title="Bộ lọc">
      <div class="grid">
        <AppInput
          label="Tìm kiếm"
          :placeholder="searchPlaceholder"
          v-model="filters.q"
        />
        <AppSelect
          v-if="statusOptions?.length"
          label="Trạng thái"
          v-model="filters.status"
          :options="statusOptions"
        />
        <AppDateInput label="Từ ngày" v-model="filters.from" />
        <AppDateInput label="Đến ngày" v-model="filters.to" />
        <div class="filterActions">
          <AppButton variant="ghost" @click="reset">Đặt lại</AppButton>
          <AppButton @click="apply">Tìm</AppButton>
        </div>
      </div>
    </AppCard>

    <AppCard>
      <template #header>
        <div>
          <h4>{{ listTitle }}</h4>
        </div>
      </template>

      <template #actions>
        <AppButton v-if="canCreate" @click="goCreate">+ Tạo mới</AppButton>
      </template>

      <AppTable :columns="columns" :rows="pagedRows">
        <template
          v-for="(_, slotName) in $slots"
          :key="slotName"
          v-slot:[slotName]="slotProps"
        >
          <slot :name="slotName" v-bind="slotProps" />
        </template>

        <template #cell:createdAt="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>

        <template #cell:start="{ row }">
          {{ formatDateTime(row.start) }}
        </template>

        <template #cell:end="{ row }">
          {{ formatDateTime(row.end) }}
        </template>

        <template #cell:ngayBatDau="{ row }">
          {{ formatDateTime(row.ngayBatDau) }}
        </template>

        <template #cell:ngayKetThuc="{ row }">
          {{ formatDateTime(row.ngayKetThuc) }}
        </template>

        <template #cell:status="{ row }">
          <div class="statusCell">
            <StatusBadge
              :text="displayStatusText(row.status, row.statusText)"
              :tone="row.statusTone ?? toneOfStatus(row.status, row.statusText)"
            />
          </div>
        </template>

        <template #cell:actions="{ row }">
          <div class="acts">
            <button
              v-if="enableStatusSwitch"
              class="statusSwitch"
              type="button"
              :class="{ on: isStatusActive(row.status, row.statusText) }"
              :disabled="!isToggleableStatus(row.status, row.statusText)"
              :title="
                isStatusActive(row.status, row.statusText)
                  ? 'Tắt trạng thái'
                  : 'Bật trạng thái'
              "
              @click="toggleStatus(row)"
            >
              <span class="thumb"></span>
            </button>
            <button class="iconBtn detail" title="Xem" @click="goDetail(row)">
              <Icon name="eye" />
            </button>
            <button
              v-if="canEdit"
              class="iconBtn edit"
              title="Sửa"
              @click="goEdit(row)"
            >
              <Icon name="edit" />
            </button>
            <button
              v-if="canDelete"
              class="iconBtn danger"
              title="Xóa"
              @click="onRemove(row)"
            >
              <Icon name="trash" />
            </button>
          </div>
        </template>
      </AppTable>

      <div class="tableFooter">
        <p class="muted">
          {{ summaryText }}
        </p>
        <AppPagination
          v-if="shouldShowPagination"
          v-model:page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-size="pageSize"
        />
      </div>
    </AppCard>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";

import PageHeader from "@/components/page/PageHeader.vue";
import AppCard from "@/components/common/AppCard.vue";
import AppInput from "@/components/common/AppInput.vue";
import AppSelect from "@/components/common/AppSelect.vue";
import AppDateInput from "@/components/common/AppDateInput.vue";
import AppButton from "@/components/common/AppButton.vue";
import AppTable from "@/components/common/AppTable.vue";
import AppPagination from "@/components/common/AppPagination.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import Icon from "@/components/common/Icon.vue";

const props = defineProps({
  title: { type: String, required: true },
  listTitle: { type: String, default: "Danh sách" },
  searchPlaceholder: { type: String, default: "Tìm kiếm..." },
  columns: { type: Array, required: true },
  routes: { type: Object, required: true }, // { createName, detailName, editName }
  service: { type: Object, required: true }, // { list(), remove(id) }
  statusOptions: { type: Array, default: () => [] },
  idKey: { type: String, default: "id" },
  canCreate: { type: Boolean, default: true },
  canEdit: { type: Boolean, default: true },
  canDelete: { type: Boolean, default: true },
  enableStatusSwitch: { type: Boolean, default: false },
});

const router = useRouter();

const page = ref(1);
const pageSize = ref(5);

const rows = ref([]);

const filters = reactive({ q: "", status: "", from: "", to: "" });

onMounted(async () => {
  rows.value = await props.service.list();
});

const filtered = computed(() => {
  const q = filters.q.trim().toLowerCase();
  return rows.value.filter((x) => {
    const okQ = !q || JSON.stringify(x).toLowerCase().includes(q);
    const okStatus = !filters.status || x.status === filters.status;
    // from/to: nếu data có createdAt/start/end thì bạn tự sửa theo module
    return okQ && okStatus;
  });
});

const total = computed(() => filtered.value.length);
const shouldShowPagination = computed(() => total.value > 5);
const startIndex = computed(() => {
  if (!total.value) return 0;
  return (page.value - 1) * pageSize.value + 1;
});
const endIndex = computed(() => {
  if (!total.value) return 0;
  return Math.min(page.value * pageSize.value, total.value);
});

const visibleCount = computed(() => {
  if (!total.value) return 0;
  return endIndex.value - startIndex.value + 1;
});

const summaryText = computed(() => {
  if (total.value === 1) {
    return "Đang hiển thị 1 bản ghi trong tổng 1 kết quả";
  }

  return `Đang hiển thị ${visibleCount.value} bản ghi trong tổng ${total.value} kết quả`;
});

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize.value;
  return filtered.value.slice(start, start + pageSize.value).map((x, i) => ({
    ...x,
    _key: x[props.idKey],
    stt: start + i + 1,
  }));
});

function apply() {
  page.value = 1;
}
function reset() {
  filters.q = "";
  filters.status = "";
  filters.from = "";
  filters.to = "";
  page.value = 1;
}

function goCreate() {
  if (!props.canCreate) return;
  router.push({ name: props.routes.createName });
}
function goDetail(row) {
  router.push({ name: props.routes.detailName, params: { id: row._key } });
}
function goEdit(row) {
  if (!props.canEdit) return;
  router.push({ name: props.routes.editName, params: { id: row._key } });
}

async function onRemove(row) {
  if (!props.canDelete) return;
  await props.service.remove(row._key);
  rows.value = await props.service.list();
}

function normalizeStatus(status, statusText) {
  return String(statusText ?? status ?? "")
    .toUpperCase()
    .trim();
}

function isToggleableStatus(status, statusText) {
  const key = normalizeStatus(status, statusText);
  return ["ACTIVE", "INACTIVE", "HOẠT ĐỘNG", "NGỪNG HOẠT ĐỘNG"].includes(key);
}

function isStatusActive(status, statusText) {
  const key = normalizeStatus(status, statusText);
  return key === "ACTIVE" || key === "HOẠT ĐỘNG";
}

async function toggleStatus(row) {
  if (
    !props.enableStatusSwitch ||
    !props.service?.update ||
    !isToggleableStatus(row.status, row.statusText)
  )
    return;
  const nextStatus = isStatusActive(row.status, row.statusText)
    ? "INACTIVE"
    : "ACTIVE";

  if (
    nextStatus === "INACTIVE" &&
    !window.confirm("Bạn có muốn thay đổi trạng thái không?")
  ) {
    return;
  }

  await props.service.update(row._key, { status: nextStatus });
  rows.value = await props.service.list();
}

function toneOfStatus(status, statusText) {
  const key = String(statusText ?? status ?? "")
    .toUpperCase()
    .trim();
  if (key === "ACTIVE" || key === "HOẠT ĐỘNG") return "blue";
  if (key === "INACTIVE" || key === "NGỪNG HOẠT ĐỘNG") return "inactive";
  if (key === "PAID" || key === "ĐÃ THANH TOÁN") return "blue";
  if (
    [
      "PENDING_CONFIRMATION",
      "WAITING_CONFIRMATION",
      "CHO XAC NHAN",
      "CHỜ XÁC NHẬN",
    ].includes(key)
  )
    return "yellow";
  if (["CONFIRMED", "DA XAC NHAN", "ĐÃ XÁC NHẬN"].includes(key)) return "green";
  if (["DELIVERING", "DANG GIAO", "ĐANG GIAO"].includes(key)) return "orange";
  if (["WAITING_DELIVERY", "CHO GIAO", "CHỜ GIAO"].includes(key)) return "teal";
  if (["COMPLETED", "HOAN THANH", "HOÀN THÀNH"].includes(key)) return "blue";
  if (["CANCELED", "DA HUY", "ĐÃ HỦY"].includes(key)) return "red";
  return "gray";
}

function displayStatusText(status, statusText) {
  const raw = String(statusText ?? status ?? "")
    .toUpperCase()
    .trim();
  const map = {
    PAID: "Đã thanh toán",
    PENDING_CONFIRMATION: "Chờ xác nhận",
    WAITING_CONFIRMATION: "Chờ xác nhận",
    CONFIRMED: "Đã xác nhận",
    DELIVERING: "Đang giao",
    WAITING_DELIVERY: "Chờ giao",
    COMPLETED: "Hoàn thành",
    CANCELED: "Đã hủy",
    ACTIVE: "Hoạt động",
    INACTIVE: "Ngừng hoạt động",
  };
  return map[raw] || statusText || status || "-";
}

function formatDateTime(value) {
  if (value == null || value === "") return "-";

  const raw = String(value).trim();
  const numeric = Number(raw);
  const normalized = /^\d{4}-\d{2}-\d{2}$/.test(raw) ? `${raw}T00:00:00` : raw;

  const date =
    Number.isFinite(numeric) && /^\d+$/.test(raw)
      ? new Date(numeric)
      : new Date(normalized);
  if (Number.isNaN(date.getTime())) return raw;

  const pad = (n) => String(n).padStart(2, "0");
  const dd = pad(date.getDate());
  const mm = pad(date.getMonth() + 1);
  const yyyy = date.getFullYear();
  const hh = pad(date.getHours());
  const min = pad(date.getMinutes());

  return `${dd}/${mm}/${yyyy} ${hh}:${min}`;
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.grid {
  display: grid;
  grid-template-columns: 1.3fr 1fr 1fr 1fr auto;
  gap: 14px;
  align-items: end;
}

.filterActions {
  display: inline-flex;
  gap: 8px;
  justify-content: flex-end;
  align-items: center;
}

.filterActions :deep(.btn) {
  min-width: 112px;
}
.acts {
  display: flex;
  gap: 8px;
  justify-content: center;
  align-items: center;
}

.statusCell {
  display: flex;
  justify-content: center;
  align-items: center;
}

.statusSwitch {
  width: 42px;
  height: 24px;
  border-radius: 999px;
  border: 1px solid #d7dfec;
  background: #eef2f7;
  position: relative;
  padding: 0;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.statusSwitch:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.statusSwitch .thumb {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  position: absolute;
  top: 2px;
  left: 2px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.16);
  transition: left 0.2s ease;
}

.statusSwitch.on {
  background: #dce8ff;
  border-color: #bfd3ff;
}

.statusSwitch.on .thumb {
  left: 21px;
  background: #2f5fb8;
}

.iconBtn {
  width: 28px;
  height: 28px;
  border: 0;
  background: transparent;
  cursor: pointer;
  display: inline-grid;
  place-items: center;
  padding: 0;
}

.iconBtn.detail {
  color: #0d9488;
}

.iconBtn.edit {
  color: var(--primary);
}

.iconBtn.danger {
  color: #dc2626;
}

.iconBtn:hover {
  opacity: 0.82;
}
.iconBtn.danger:hover {
  opacity: 0.85;
}

.tableFooter {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.tableFooter .muted {
  font-size: 13px;
}

h2 {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: #1f2f46;
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filterActions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }

  .tableFooter {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
