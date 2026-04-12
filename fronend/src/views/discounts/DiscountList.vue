<template>
  <div class="page">
    <PageHeader title="Đợt giảm giá" />

    <AppCard title="Bộ lọc">
      <div class="grid">
        <AppInput
          label="Tìm kiếm"
          placeholder="Tìm theo mã / tên đợt giảm"
          v-model="filters.q"
        />
        <AppDateInput label="Ngày bắt đầu" v-model="filters.start" />
        <AppDateInput label="Ngày kết thúc" v-model="filters.end" />
        <div class="filterActions">
          <AppButton variant="ghost" @click="reset">Đặt lại</AppButton>
          <AppButton @click="apply">Tìm</AppButton>
        </div>
      </div>
    </AppCard>

    <AppCard>
      <template #header>
        <div>
          <h4>Danh sách đợt giảm giá</h4>
        </div>
      </template>

      <template #actions>
        <AppButton @click="goCreate">+ Tạo mới</AppButton>
      </template>

      <AppTable :columns="columns" :rows="pagedRows">
        <template #cell:ma="{ row }">
          <div class="codeCell">
            <span class="codeText">{{ row.ma }}</span>
            <span class="nameText">{{ row.ten }}</span>
          </div>
        </template>

        <template #cell:loaiGiamGia="{ row }">
          {{ typeLabel(row.loaiGiamGia) }}
        </template>

        <template #cell:soTienGiam="{ row }">
          {{ formatDiscountValue(row.loaiGiamGia, row.soTienGiam) }}
        </template>

        <template #cell:dieuKienGiamGia="{ row }">
          {{ formatMoney(row.dieuKienGiamGia) }}
        </template>

        <template #cell:ngayBatDau="{ row }">
          {{ formatDateTime(row.ngayBatDau) }}
        </template>

        <template #cell:ngayKetThuc="{ row }">
          {{ formatDateTime(row.ngayKetThuc) }}
        </template>

        <template #cell:mucUuTien="{ row }">
          {{ row.mucUuTien ?? "-" }}
        </template>

        <template #cell:actions="{ row }">
          <div class="acts">
            <button class="iconBtn detail" title="Xem" @click="goDetail(row)">
              <Icon name="eye" />
            </button>
            <button class="iconBtn edit" title="Sửa" @click="goEdit(row)">
              <Icon name="edit" />
            </button>
          </div>
        </template>
      </AppTable>

      <div class="tableFooter">
        <p class="muted">{{ summaryText }}</p>
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
import { discountService } from "@/services/discount.service";

import PageHeader from "@/components/page/PageHeader.vue";
import AppCard from "@/components/common/AppCard.vue";
import AppInput from "@/components/common/AppInput.vue";
import AppDateInput from "@/components/common/AppDateInput.vue";
import AppButton from "@/components/common/AppButton.vue";
import AppTable from "@/components/common/AppTable.vue";
import AppPagination from "@/components/common/AppPagination.vue";
import Icon from "@/components/common/Icon.vue";

const router = useRouter();
const page = ref(1);
const pageSize = ref(5);
const rows = ref([]);
const filters = reactive({ q: "", start: "", end: "" });

const columns = [
  { key: "stt", label: "STT", width: "70px" },
  { key: "ma", label: "Mã giảm giá", width: "240px" },
  { key: "loaiGiamGia", label: "Loại giảm giá", width: "130px" },
  { key: "soTienGiam", label: "Số tiền giảm", width: "130px" },
  { key: "dieuKienGiamGia", label: "Điều kiện giảm giá", width: "150px" },
  { key: "ngayBatDau", label: "Ngày bắt đầu", width: "150px" },
  { key: "ngayKetThuc", label: "Ngày kết thúc", width: "150px" },
  { key: "mucUuTien", label: "Mức ưu tiên", width: "110px" },
  { key: "actions", label: "Hành động", width: "140px" },
];

onMounted(async () => {
  rows.value = await discountService.list();
});

const filtered = computed(() => {
  const q = filters.q.trim().toLowerCase();
  return rows.value.filter((x) => {
    const okQ =
      !q ||
      String(x.ma || "")
        .toLowerCase()
        .includes(q) ||
      String(x.ten || "")
        .toLowerCase()
        .includes(q);

    const from = filters.start
      ? new Date(`${filters.start}T00:00:00`).getTime()
      : null;
    const to = filters.end
      ? new Date(`${filters.end}T23:59:59`).getTime()
      : null;
    const okStart = from == null || Number(x.ngayBatDau || 0) >= from;
    const okEnd = to == null || Number(x.ngayKetThuc || 0) <= to;
    return okQ && okStart && okEnd;
  });
});

const total = computed(() => filtered.value.length);
const shouldShowPagination = computed(() => total.value > 5);
const startIndex = computed(() =>
  total.value ? (page.value - 1) * pageSize.value + 1 : 0,
);
const endIndex = computed(() =>
  total.value ? Math.min(page.value * pageSize.value, total.value) : 0,
);
const visibleCount = computed(() =>
  total.value ? endIndex.value - startIndex.value + 1 : 0,
);
const summaryText = computed(() =>
  total.value === 1
    ? "Đang hiển thị 1 bản ghi trong tổng 1 kết quả"
    : `Đang hiển thị ${visibleCount.value} bản ghi trong tổng ${total.value} kết quả`,
);

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize.value;
  return filtered.value.slice(start, start + pageSize.value).map((x, i) => ({
    ...x,
    _key: x.id,
    stt: start + i + 1,
  }));
});

function apply() {
  page.value = 1;
}
function reset() {
  filters.q = "";
  filters.start = "";
  filters.end = "";
  page.value = 1;
}
function goCreate() {
  router.push({ name: "discounts.create" });
}
function goEdit(row) {
  router.push({ name: "discounts.edit", params: { id: row._key ?? row.id } });
}
function goDetail(row) {
  router.push({ name: "discounts.detail", params: { id: row._key ?? row.id } });
}

function typeLabel(value) {
  const key = String(value || "")
    .toUpperCase()
    .trim();
  if (key === "PERCENT") return "Phần trăm";
  if (key === "AMOUNT" || key === "MONEY") return "Số tiền";
  return key || "-";
}

function formatDiscountValue(type, value) {
  if (value == null || value === "") return "-";
  const amount = Number(value);
  if (Number.isNaN(amount)) return String(value);

  const key = String(type || "")
    .toUpperCase()
    .trim();

  if (key === "PERCENT") {
    return `${amount.toLocaleString("vi-VN")}%`;
  }

  return `${amount.toLocaleString("vi-VN")} đ`;
}

function formatMoney(value) {
  if (value == null || value === "") return "-";
  const amount = Number(value);
  if (Number.isNaN(amount)) return String(value);
  return `${amount.toLocaleString("vi-VN")} đ`;
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
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
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
  grid-template-columns: 1.3fr 1fr 1fr auto;
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

.codeCell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
  line-height: 1.2;
  white-space: normal;
}

.codeText {
  color: var(--primary);
  font-weight: 700;
  font-size: 14px;
}

.nameText {
  color: #5f6c80;
  font-size: 12.5px;
  font-weight: 500;
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

.iconBtn:hover {
  opacity: 0.82;
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
