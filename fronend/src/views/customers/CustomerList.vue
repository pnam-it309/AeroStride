<template>
  <div class="page">
    <PageHeader title="Khách hàng" />

    <AppCard title="Bộ lọc">
      <div class="grid">
        <AppInput
          label="Tìm kiếm"
          placeholder="Tìm theo mã / tên / SĐT / email"
          v-model="filters.q"
        />
        <AppSelect
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
      <div class="cardTabs">
        <div class="statusRail">
          <button
            class="statusCard"
            :class="{ active: activeTab === 'ONLINE' }"
            @click="switchTab('ONLINE')"
          >
            <Icon name="users" />
            <span class="cardTitle">Khách hàng online</span>
            <span class="tabCount">{{ onlineCount }}</span>
          </button>

          <button
            class="statusCard"
            :class="{ active: activeTab === 'RETAIL' }"
            @click="switchTab('RETAIL')"
          >
            <Icon name="user" />
            <span class="cardTitle">Khách hàng lẻ</span>
            <span class="tabCount">{{ retailCount }}</span>
          </button>
        </div>
      </div>

      <template #header>
        <div>
          <h4>{{ listTitle }}</h4>
        </div>
      </template>

      <template #actions>
        <AppButton @click="goCreate">+ Tạo mới</AppButton>
      </template>

      <AppTable :columns="activeColumns" :rows="pagedRows">
        <template #cell:gioiTinhText="{ row }">
          <div class="genderCell">
            <span class="genderBadge" :class="genderTone(row.gioiTinhText)">
              {{ row.gioiTinhText || "-" }}
            </span>
          </div>
        </template>

        <template #cell:createdAt="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>

        <template #cell:status="{ row }">
          <div class="statusCell">
            <StatusBadge
              :text="displayStatusText(row.status)"
              :tone="toneOf(row.status)"
            />
          </div>
        </template>

        <template #cell:actions="{ row }">
          <div class="acts">
            <button
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
import { customerService } from "@/services/customer.service";

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
const rows = ref([]);
const page = ref(1);
const pageSize = ref(5);
const activeTab = ref("ONLINE");

const filters = reactive({ q: "", status: "", from: "", to: "" });

const statusOptions = [
  { value: "", label: "Tất cả trạng thái" },
  { value: "ACTIVE", label: "Hoạt động" },
  { value: "INACTIVE", label: "Ngừng hoạt động" },
];

const onlineColumns = [
  { key: "stt", label: "STT", width: "70px" },
  { key: "ma", label: "Mã khách hàng", width: "120px" },
  { key: "ten", label: "Tên khách hàng", width: "180px" },
  { key: "sdt", label: "SĐT", width: "130px" },
  { key: "tenNguoiNhan", label: "Người nhận", width: "150px" },
  { key: "sdtNguoiNhan", label: "SĐT người nhận", width: "140px" },
  { key: "email", label: "Email", width: "190px" },
  { key: "tenTaiKhoan", label: "Tài khoản", width: "130px" },
  { key: "gioiTinhText", label: "Giới tính", width: "120px" },
  { key: "diaChi", label: "Địa chỉ", width: "220px" },
  { key: "status", label: "Trạng thái", width: "140px" },
  { key: "createdAt", label: "Ngày tạo", width: "140px" },
  { key: "actions", label: "Hành động", width: "160px" },
];

const retailColumns = [
  { key: "stt", label: "STT", width: "70px" },
  { key: "ma", label: "Mã khách hàng", width: "120px" },
  { key: "ten", label: "Tên khách hàng", width: "180px" },
  { key: "sdt", label: "SĐT", width: "130px" },
  { key: "gioiTinhText", label: "Giới tính", width: "120px" },
  { key: "ngaySinh", label: "Ngày sinh", width: "130px" },
  { key: "diaChi", label: "Địa chỉ", width: "220px" },
  { key: "status", label: "Trạng thái", width: "140px" },
  { key: "createdAt", label: "Ngày tạo", width: "140px" },
  { key: "actions", label: "Hành động", width: "160px" },
];

const activeColumns = computed(() =>
  activeTab.value === "ONLINE" ? onlineColumns : retailColumns,
);

onMounted(async () => {
  rows.value = await customerService.list();
});

const onlineCount = computed(
  () => rows.value.filter((x) => x.customerType === "ONLINE").length,
);
const retailCount = computed(
  () => rows.value.filter((x) => x.customerType === "RETAIL").length,
);

const filtered = computed(() => {
  const q = filters.q.trim().toLowerCase();
  const from = filters.from
    ? new Date(`${filters.from}T00:00:00`).getTime()
    : null;
  const to = filters.to ? new Date(`${filters.to}T23:59:59`).getTime() : null;

  return rows.value.filter((x) => {
    const matchTab = x.customerType === activeTab.value;
    if (!matchTab) return false;

    const matchQ =
      !q ||
      String(x.ma || "")
        .toLowerCase()
        .includes(q) ||
      String(x.ten || "")
        .toLowerCase()
        .includes(q) ||
      String(x.sdt || "")
        .toLowerCase()
        .includes(q) ||
      String(x.tenNguoiNhan || "")
        .toLowerCase()
        .includes(q) ||
      String(x.sdtNguoiNhan || "")
        .toLowerCase()
        .includes(q) ||
      String(x.email || "")
        .toLowerCase()
        .includes(q);

    const matchStatus =
      !filters.status || String(x.status) === String(filters.status);
    const time = Number(x.createdAt || 0);
    const matchFrom = from == null || time >= from;
    const matchTo = to == null || time <= to;

    return matchQ && matchStatus && matchFrom && matchTo;
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

const listTitle = computed(() =>
  activeTab.value === "ONLINE"
    ? "Danh sách khách hàng online"
    : "Danh sách khách hàng lẻ tại quầy",
);

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize.value;
  return filtered.value.slice(start, start + pageSize.value).map((x, i) => ({
    ...x,
    _key: x.id,
    stt: start + i + 1,
    ngaySinh: formatDateOnly(x.ngaySinh),
  }));
});

function switchTab(tab) {
  activeTab.value = tab;
  page.value = 1;
}

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
  router.push({ name: "customers.create" });
}

function goDetail(row) {
  router.push({ name: "customers.detail", params: { id: row._key ?? row.id } });
}

function goEdit(row) {
  router.push({ name: "customers.edit", params: { id: row._key ?? row.id } });
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
  await customerService.update(id, { status: nextStatus });
  rows.value = await customerService.list();
}

function toneOf(status) {
  const key = String(status ?? "")
    .toUpperCase()
    .trim();
  if (key === "ACTIVE") return "blue";
  if (key === "INACTIVE") return "inactive";
  return "gray";
}

function displayStatusText(status) {
  const key = String(status ?? "")
    .toUpperCase()
    .trim();
  if (key === "ACTIVE") return "Hoạt động";
  if (key === "INACTIVE") return "Ngừng hoạt động";
  return status || "-";
}

function formatDateOnly(value) {
  if (value == null || value === "") return "-";
  const date = new Date(String(value));
  if (Number.isNaN(date.getTime())) return String(value);
  const pad = (n) => String(n).padStart(2, "0");
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
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

function genderTone(value) {
  const key = String(value ?? "")
    .trim()
    .toLowerCase();
  if (key === "nam") return "male";
  if (key === "nữ" || key === "nu") return "female";
  return "gray";
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

.cardTabs {
  padding: 0 20px 14px 20px;
  border-bottom: 1px solid var(--border);
  margin: 0 -20px;
}

.statusRail {
  display: flex;
  align-items: stretch;
  justify-content: flex-start;
  gap: 8px;
  margin: 0;
}

.statusCard {
  min-width: 0;
  flex: 0 0 auto;
  height: 42px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 14px;
  white-space: nowrap;
  cursor: pointer;
  color: #354357;
  transition: all 0.15s ease;
}

.statusCard:hover {
  background: #f8fbff;
}

.statusCard.active {
  border-bottom-color: #3f78d0;
  background: #f5f9ff;
  color: var(--primary);
}

.cardTitle {
  font-size: 14px;
  font-weight: 600;
}

.statusCard :deep(.icon) {
  width: 16px;
  height: 16px;
}

.tabCount {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  background-color: #ef4444;
  color: #fff;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  padding: 0 4px;
  margin-left: 4px;
}

.statusCard.active .tabCount {
  background-color: rgba(63, 120, 208, 0.2);
  color: #3f78d0;
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

.genderCell {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.genderBadge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.genderBadge.male {
  color: #1f6fd6;
  background: #edf4ff;
  border-color: #d5e5ff;
}

.genderBadge.female {
  color: #d97706;
  background: #fff3e3;
  border-color: #ffe0bd;
}

.genderBadge.gray {
  color: #738095;
  background: #f8fafc;
  border-color: #e8edf4;
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
