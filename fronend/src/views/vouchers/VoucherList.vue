<template>
  <div class="page">
    <PageHeader title="Quản lý phiếu giảm giá" />

    <AppCard title="Bộ lọc">
      <div class="grid">
        <AppInput
          label="Tìm kiếm"
          placeholder="Nhập mã phiếu / tên"
          v-model="filters.q"
        />
        <AppDateInput label="Ngày bắt đầu" v-model="filters.start" />
        <AppDateInput label="Ngày kết thúc" v-model="filters.end" />
        <AppSelect
          label="Trạng thái"
          v-model="filters.status"
          :options="statusOptions"
        />
        <div class="filterActions">
          <AppButton variant="ghost" @click="reset">Đặt lại</AppButton>
          <AppButton @click="apply">Tìm</AppButton>
        </div>
      </div>
    </AppCard>

    <AppCard>
      <template #header>
        <div>
          <h2>Danh sách phiếu giảm giá</h2>
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

        <template #cell:loaiPhieu="{ row }">
          {{ voucherTypeText(row.loaiPhieu) }}
        </template>

        <template #cell:phanTramGiamGia="{ row }">
          {{ formatPercent(row.phanTramGiamGia) }}
        </template>

        <template #cell:soTienGiam="{ row }">
          {{ formatCurrency(row.soTienGiam) }}
        </template>

        <template #cell:donHangToiThieu="{ row }">
          {{ formatCurrency(row.donHangToiThieu) }}
        </template>

        <template #cell:giamToiDa="{ row }">
          {{ formatCurrency(row.giamToiDa) }}
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
              :text="statusText(row.status)"
              :tone="toneOf(row.status)"
            />
          </div>
        </template>

        <template #cell:actions="{ row }">
          <div class="acts">
            <button
              v-if="isToggleableStatus(row.status)"
              class="statusSwitch"
              type="button"
              :class="{ on: isStatusActive(row.status) }"
              :title="
                isStatusActive(row.status) ? 'Tắt trạng thái' : 'Bật trạng thái'
              "
              @click="toggleStatus(row)"
            >
              <span class="thumb"></span>
            </button>
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
import { voucherService } from "@/services/voucher.service";

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

const router = useRouter();

const page = ref(1);
const pageSize = ref(5);

const rows = ref([]);

const filters = reactive({ q: "", start: "", end: "", status: "" });

const statusOptions = [
  { value: "", label: "Tất cả trạng thái" },
  { value: "ACTIVE", label: "Đang áp dụng" },
  { value: "EXPIRED", label: "Hết hạn" },
  { value: "OUT_OF_STOCK", label: "Hết số lượng" },
  { value: "INACTIVE", label: "Chưa áp dụng" },
];

const columns = [
  { key: "id", label: "STT", width: "70px" },
  { key: "ma", label: "Mã giảm giá", width: "240px" },
  { key: "loaiPhieu", label: "Loại phiếu", width: "130px" },
  { key: "phanTramGiamGia", label: "Giá trị giảm", width: "100px" },
  { key: "soTienGiam", label: "Số tiền giảm", width: "140px" },
  { key: "soLuong", label: "Số lượng", width: "100px" },
  { key: "donHangToiThieu", label: "Đơn tối thiểu", width: "140px" },
  { key: "giamToiDa", label: "Giảm tối đa", width: "130px" },
  { key: "ngayBatDau", label: "Ngày bắt đầu", width: "150px" },
  { key: "ngayKetThuc", label: "Ngày kết thúc", width: "150px" },
  { key: "status", label: "Trạng thái", width: "140px" },
  { key: "ghiChu", label: "Ghi chú", width: "180px" },
  { key: "actions", label: "Hành động", width: "160px" },
];

onMounted(async () => {
  rows.value = await voucherService.list();
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
    const okStatus = !filters.status || x.status === filters.status;
    const from = filters.start
      ? new Date(`${filters.start}T00:00:00`).getTime()
      : null;
    const to = filters.end
      ? new Date(`${filters.end}T23:59:59`).getTime()
      : null;
    const okStart = from == null || Number(x.ngayBatDau || 0) >= from;
    const okEnd = to == null || Number(x.ngayKetThuc || 0) <= to;
    return okQ && okStatus && okStart && okEnd;
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
    _key: x.id,
    id: start + i + 1, // STT hiển thị
  }));
});

function apply() {
  page.value = 1;
}
function reset() {
  filters.q = "";
  filters.start = "";
  filters.end = "";
  filters.status = "";
  page.value = 1;
}

function goCreate() {
  router.push({ name: "vouchers.create" });
}
function goEdit(row) {
  router.push({ name: "vouchers.edit", params: { id: row._key ?? row.id } });
}
function goDetail(row) {
  router.push({ name: "vouchers.detail", params: { id: row._key ?? row.id } });
}

function isToggleableStatus(status) {
  const key = String(status ?? "")
    .toUpperCase()
    .trim();
  return ["ACTIVE", "INACTIVE"].includes(key);
}

function isStatusActive(status) {
  return (
    String(status ?? "")
      .toUpperCase()
      .trim() === "ACTIVE"
  );
}

async function toggleStatus(row) {
  const id = row._key ?? row.id;
  const nextStatus = isStatusActive(row.status) ? "INACTIVE" : "ACTIVE";

  if (
    nextStatus === "INACTIVE" &&
    !window.confirm("Bạn có muốn thay đổi trạng thái không?")
  ) {
    return;
  }

  await voucherService.update(id, { status: nextStatus });
  rows.value = await voucherService.list();
}

function toneOf(st) {
  if (st === "ACTIVE") return "green";
  if (st === "EXPIRED") return "red";
  if (st === "OUT_OF_STOCK") return "red";
  if (st === "INACTIVE") return "yellow";
  return "gray";
}
function statusText(st) {
  return st === "ACTIVE"
    ? "Đang áp dụng"
    : st === "EXPIRED"
      ? "Hết hạn"
      : st === "OUT_OF_STOCK"
        ? "Hết số lượng"
        : st === "INACTIVE"
          ? "Chưa áp dụng"
          : "Không rõ";
}

function voucherTypeText(type) {
  const key = String(type || "")
    .toUpperCase()
    .trim();
  if (key === "PERCENT") return "Phần trăm";
  if (key === "MONEY") return "Tiền mặt";
  return key || "-";
}

function formatPercent(value) {
  if (value == null || value === "") return "-";
  return `${value}%`;
}

function formatCurrency(value) {
  if (value == null || value === "") return "-";
  const amount = Number(value);
  if (Number.isNaN(amount)) return String(value);
  return `${amount.toLocaleString("vi-VN")} đ`;
}

function formatDateTime(value) {
  if (value == null || value === "") return "-";
  const time = Number(value);
  if (Number.isNaN(time)) return String(value);

  const d = new Date(time);
  if (Number.isNaN(d.getTime())) return "-";

  const pad = (n) => String(n).padStart(2, "0");
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
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
  grid-template-columns: 1.2fr 1fr 1fr 1.2fr auto;
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

.page :deep(.filterToolbar .right .btn) {
  min-width: 112px;
  padding-inline: 18px;
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
