<template>
  <div class="page">
    <PageHeader title="Hóa đơn" />

    <!-- Filter Card -->
    <AppCard title="Bộ lọc">
      <div class="grid">
        <AppInput
          label="Tìm kiếm"
          placeholder="Tìm theo mã hóa đơn / khách hàng"
          v-model="filters.q"
        />
        <AppDateInput label="Từ ngày" v-model="filters.from" />
        <AppDateInput label="Đến ngày" v-model="filters.to" />
        <div class="filterActions">
          <AppButton variant="ghost" @click="resetFilters">Đặt lại</AppButton>
          <AppButton @click="applyFilters">Tìm</AppButton>
        </div>
      </div>
    </AppCard>

    <!-- Table with Status Tabs -->
    <AppCard>
      <!-- Status Tabs as part of card -->
      <div class="cardTabs">
        <div class="statusRail">
          <button
            v-for="tab in statusTabs"
            :key="tab.status"
            :class="['statusCard', { active: activeStatus === tab.status }]"
            @click="activeStatus = tab.status"
          >
            <Icon :name="tab.icon" />
            <span class="cardTitle">{{ tab.label }}</span>
            <span v-if="tab.count > 0" class="tabCount">{{ tab.count }}</span>
          </button>
        </div>
      </div>

      <template #header>
        <div>
          <h4>{{ listTitle }}</h4>
        </div>
      </template>

      <AppTable :columns="columns" :rows="filteredRows">
        <template #cell:stt="{ row }">
          <div class="sttCell">{{ row.stt }}</div>
        </template>

        <template #cell:createdAt="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>

        <template #cell:invoiceType="{ row }">
          <div class="invoiceTypeCell">
            <div :class="['invoice-type-badge', row.invoiceType]">
              {{ invoiceTypeLabel(row.invoiceType) }}
            </div>
          </div>
        </template>

        <template #cell:paymentMethodName="{ row }">
          <div class="paymentCell">
            <span class="paymentName">
              {{ row.paymentMethodName || row.paymentMethod || "-" }}
            </span>
          </div>
        </template>

        <template #cell:status="{ row }">
          <div class="statusCell">
            <StatusBadge
              :text="getStatusLabelBadge(row.status, row.invoiceType)"
              :tone="getStatusTone(row.status)"
            />
          </div>
        </template>

        <template #cell:actions="{ row }">
          <div class="acts">
            <button class="iconBtn detail" title="Xem" @click="goDetail(row)">
              <Icon name="eye" />
            </button>
            <button
              class="iconBtn edit"
              title="Sửa trạng thái"
              @click="openStatusDialog(row)"
            >
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

    <div
      v-if="statusDialogOpen"
      class="dialogOverlay"
      @click.self="closeStatusDialog"
    >
      <div class="statusDialog" role="dialog" aria-modal="true">
        <h3>Cập nhật trạng thái hóa đơn</h3>
        <p class="dialogDesc">
          Hóa đơn <b>{{ editingOrder?.code }}</b>
        </p>

        <div class="dialogField">
          <label for="order-status">Trạng thái mới</label>
          <select id="order-status" v-model="selectedStatus">
            <option
              v-for="status in statusDialogOptions"
              :key="status.code"
              :value="status.code"
            >
              {{ status.label }}
            </option>
          </select>
        </div>

        <div class="dialogActions">
          <AppButton
            variant="ghost"
            :disabled="savingStatus"
            @click="closeStatusDialog"
            >Hủy</AppButton
          >
          <AppButton :disabled="savingStatus" @click="requestStatusUpdate"
            >Xác nhận cập nhật</AppButton
          >
        </div>
      </div>
    </div>

    <div v-if="confirmNoticeOpen" class="confirmNotice">
      <div class="confirmNoticeCard" role="alertdialog" aria-modal="false">
        <div class="confirmNoticeTitle">Thông báo</div>
        <p>Bạn có chắc chắn đổi trạng thái không?</p>
        <div class="confirmNoticeActions">
          <AppButton
            variant="ghost"
            :disabled="savingStatus"
            @click="confirmNoticeOpen = false"
            >Không</AppButton
          >
          <AppButton :disabled="savingStatus" @click="executeStatusUpdate">
            {{ savingStatus ? "Đang cập nhật..." : "OK" }}
          </AppButton>
        </div>
      </div>
    </div>

    <div v-if="toast.show" class="toast" :class="toast.type">
      {{ toast.message }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import PageHeader from "@/components/page/PageHeader.vue";
import AppCard from "@/components/common/AppCard.vue";
import AppInput from "@/components/common/AppInput.vue";
import AppDateInput from "@/components/common/AppDateInput.vue";
import AppButton from "@/components/common/AppButton.vue";
import AppTable from "@/components/common/AppTable.vue";
import AppPagination from "@/components/common/AppPagination.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import Icon from "@/components/common/Icon.vue";
import { orderService } from "@/services/order.service";

const router = useRouter();
const activeStatus = ref(null);
const allOrders = ref([]);
const page = ref(1);
const pageSize = ref(10);
const filters = ref({ q: "", from: "", to: "" });
const statusDialogOpen = ref(false);
const editingOrder = ref(null);
const selectedStatus = ref("");
const savingStatus = ref(false);
const confirmNoticeOpen = ref(false);
const pendingUpdate = ref(null);
const toast = ref({ show: false, message: "", type: "success" });
let toastTimer = null;

// Load orders
onMounted(async () => {
  allOrders.value = await orderService.list();
});

// Status definitions with icons
const statuses = [
  {
    key: "WAITING_CONFIRMATION",
    label: "Chờ xác nhận",
    icon: "clock",
    tone: "yellow",
  },
  { key: "CONFIRMED", label: "Đã xác nhận", icon: "check", tone: "blue" },
  { key: "WAITING_DELIVERY", label: "Chờ giao", icon: "box", tone: "orange" },
  { key: "DELIVERING", label: "Đang giao", icon: "truck", tone: "blue" },
  { key: "COMPLETED", label: "Hoàn thành", icon: "checkCircle", tone: "green" },
  { key: "PAID", label: "Đã thanh toán", icon: "creditCard", tone: "green" },
  { key: "CANCELED", label: "Đã hủy", icon: "xCircle", tone: "red" },
];

// Status tabs with counts
const statusTabs = computed(() => {
  const tabs = [
    {
      status: null,
      label: "Tất cả",
      icon: "list",
      count: allOrders.value.length,
    },
  ];

  for (const st of statuses) {
    const count = allOrders.value.filter((o) => o.status === st.key).length;
    tabs.push({
      status: st.key,
      label: st.label,
      icon: st.icon,
      count,
    });
  }

  return tabs;
});

// Filtered and paged rows
const filteredRows = computed(() => {
  let result = allOrders.value;

  // Filter by status
  if (activeStatus.value) {
    result = result.filter((o) => o.status === activeStatus.value);
  }

  // Filter by search
  const q = filters.value.q?.toLowerCase().trim() || "";
  if (q) {
    result = result.filter(
      (o) =>
        o.code?.toLowerCase().includes(q) ||
        o.customer?.toLowerCase().includes(q) ||
        o.phone?.toLowerCase().includes(q),
    );
  }

  // Filter by date range
  if (filters.value.from) {
    result = result.filter((o) => o.createdAt >= filters.value.from);
  }
  if (filters.value.to) {
    result = result.filter((o) => o.createdAt <= filters.value.to);
  }

  // Pagination
  const start = (page.value - 1) * pageSize.value;
  const end = start + pageSize.value;

  return result.slice(start, end).map((row, idx) => ({
    ...row,
    stt: start + idx + 1,
  }));
});

const total = computed(() => {
  let result = allOrders.value;

  if (activeStatus.value) {
    result = result.filter((o) => o.status === activeStatus.value);
  }

  const q = filters.value.q?.toLowerCase().trim() || "";
  if (q) {
    result = result.filter(
      (o) =>
        o.code?.toLowerCase().includes(q) ||
        o.customer?.toLowerCase().includes(q) ||
        o.phone?.toLowerCase().includes(q),
    );
  }

  if (filters.value.from) {
    result = result.filter((o) => o.createdAt >= filters.value.from);
  }
  if (filters.value.to) {
    result = result.filter((o) => o.createdAt <= filters.value.to);
  }

  return result.length;
});

const shouldShowPagination = computed(() => total.value > 5);

const startIndex = computed(() => (page.value - 1) * pageSize.value + 1);
const endIndex = computed(() =>
  Math.min(page.value * pageSize.value, total.value),
);

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

const listTitle = computed(() => {
  if (activeStatus.value) {
    const label =
      statuses.find((s) => s.key === activeStatus.value)?.label || "";
    return `Danh sách hóa đơn - ${label}`;
  }
  return "Danh sách hóa đơn";
});

const columns = [
  { key: "stt", label: "STT", width: "70px" },
  { key: "code", label: "Mã hóa đơn", width: "100px" },
  { key: "customer", label: "Khách hàng", width: "140px" },
  { key: "phone", label: "Số điện thoại", width: "130px" },
  { key: "invoiceType", label: "Loại hóa đơn", width: "100px" },
  { key: "paymentMethodName", label: "Loại thanh toán", width: "190px" },
  { key: "employeeCode", label: "Mã NV", width: "100px" },
  { key: "total", label: "Tổng tiền", width: "130px" },
  { key: "status", label: "Trạng thái", width: "120px" },
  { key: "createdAt", label: "Ngày tạo", width: "110px" },
  { key: "actions", label: "Hành động", width: "120px" },
];

const statusFlowByInvoice = {
  online: [
    "WAITING_CONFIRMATION",
    "CONFIRMED",
    "WAITING_DELIVERY",
    "DELIVERING",
    "PAID",
    "COMPLETED",
    "CANCELED",
  ],
  offline: ["CONFIRMED", "COMPLETED", "CANCELED"],
};

const invoiceTypeLabel = (type) => {
  return String(type).toLowerCase() === "offline" ? "OFFLINE" : "ONLINE";
};

const getStatusLabelBadge = (status, invoiceType) => {
  const st = statuses.find((s) => s.key === status);
  if (status === "WAITING_DELIVERY" && invoiceType === "offline") {
    return "Giao hàng";
  }
  return st ? st.label : status;
};

const getStatusTone = (status) => {
  const st = statuses.find((s) => s.key === status);
  return st ? st.tone : "gray";
};

const resetFilters = () => {
  filters.value = { q: "", from: "", to: "" };
  page.value = 1;
};

const applyFilters = () => {
  page.value = 1;
};

const goDetail = (row) => {
  router.push({ name: "orders.detail", params: { id: row.id } });
};

const getAllowedStatuses = (invoiceType) => {
  const key = String(invoiceType || "offline").toLowerCase();
  const allowed = statusFlowByInvoice[key] || statusFlowByInvoice.offline;
  return allowed
    .map((code) => {
      const matched = statuses.find((x) => x.key === code);
      if (!matched) return null;
      return { code: matched.key, label: matched.label };
    })
    .filter(Boolean);
};

const statusDialogOptions = computed(() =>
  editingOrder.value ? getAllowedStatuses(editingOrder.value.invoiceType) : [],
);

function openStatusDialog(row) {
  editingOrder.value = row;
  selectedStatus.value = String(row.status || "").toUpperCase();
  confirmNoticeOpen.value = false;
  statusDialogOpen.value = true;
}

function closeStatusDialog() {
  if (savingStatus.value) return;
  statusDialogOpen.value = false;
  editingOrder.value = null;
  selectedStatus.value = "";
  confirmNoticeOpen.value = false;
}

function showToast(message, type = "success") {
  if (toastTimer) clearTimeout(toastTimer);
  toast.value = { show: true, message, type };
  toastTimer = setTimeout(() => {
    toast.value.show = false;
  }, 2200);
}

function requestStatusUpdate() {
  if (!editingOrder.value || !selectedStatus.value) return;

  const current = String(editingOrder.value.status || "").toUpperCase();
  if (selectedStatus.value === current) {
    showToast("Trạng thái mới trùng với trạng thái hiện tại.", "error");
    return;
  }

  pendingUpdate.value = {
    id: editingOrder.value.id,
    nextStatus: selectedStatus.value,
  };
  statusDialogOpen.value = false;
  confirmNoticeOpen.value = true;
}

async function executeStatusUpdate() {
  if (!pendingUpdate.value) return;

  confirmNoticeOpen.value = false;

  savingStatus.value = true;
  try {
    await orderService.update(pendingUpdate.value.id, {
      status: pendingUpdate.value.nextStatus,
    });
    allOrders.value = await orderService.list();
    closeStatusDialog();
    pendingUpdate.value = null;
    showToast("Cập nhật trạng thái hóa đơn thành công.", "success");
  } catch (error) {
    console.error("Update order status failed:", error);
    pendingUpdate.value = null;
    showToast("Không thể cập nhật trạng thái hóa đơn.", "error");
  } finally {
    savingStatus.value = false;
  }
}

function formatDateTime(value) {
  if (value == null || value === "") return "-";

  const raw = String(value).trim();
  const normalized = /^\d{4}-\d{2}-\d{2}$/.test(raw) ? `${raw}T00:00:00` : raw;

  const date = new Date(normalized);
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
  grid-template-columns: 1.6fr 1fr 1fr auto;
  gap: 12px;
  align-items: end;
}

.filterActions {
  display: inline-flex;
  justify-content: flex-end;
  gap: 8px;
}

/* Card Tabs */
.cardTabs {
  padding: 0 20px 14px 20px;
  border-bottom: 1px solid var(--border);
  margin: 0 -20px;
}

.statusRail {
  display: flex;
  align-items: stretch;
  justify-content: stretch;
  gap: 0;
  margin: 0 -14px;
}

.statusCard {
  min-width: 0;
  flex: 1 1 0;
  height: 42px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 14px;
  color: #354357;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.statusCard:hover {
  background: #f8fbff;
}

.cardTitle {
  font-size: 14px;
  font-weight: 600;
}

.statusCard.active {
  border-bottom-color: #3f78d0;
  background: #f5f9ff;
  color: var(--primary);
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
  color: white;
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

.invoice-type-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 12px;
  border: 1px solid transparent;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  line-height: 1;
}

.invoice-type-badge.offline {
  background-color: #fff3df;
  border-color: #ffe3b8;
  color: #9a5b00;
}

.invoice-type-badge.online {
  background-color: #e9f9ef;
  border-color: #cdeed9;
  color: #1f7a47;
}

.sttCell,
.invoiceTypeCell,
.statusCell,
.acts {
  display: flex;
  align-items: center;
  justify-content: center;
}

.sttCell {
  width: 100%;
}

.invoiceTypeCell {
  width: 100%;
  justify-content: flex-start;
  padding-left: 0;
  margin-left: -8px;
}

.statusCell {
  width: 100%;
}

.paymentCell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  line-height: 1.2;
}

.paymentName {
  font-size: 13px;
  color: #334155;
}

.acts {
  gap: 8px;
  width: 100%;
}

.iconBtn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  transition: all 0.15s ease;
}

.iconBtn:hover {
  background: #f3f4f6;
}

.iconBtn.detail {
  color: #0d9488;
}

.iconBtn.edit {
  color: #2563eb;
}

.tableFooter {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.muted {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

.dialogOverlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
  display: grid;
  place-items: center;
  z-index: 3000;
  padding: 20px;
}

.statusDialog {
  width: min(460px, 100%);
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #dbe3ef;
  box-shadow: 0 20px 38px rgba(15, 23, 42, 0.18);
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.statusDialog h3 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.dialogDesc {
  margin: 0;
  font-size: 14px;
  color: #475569;
}

.dialogField {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dialogField label {
  font-size: 13px;
  color: #334155;
  font-weight: 600;
}

.dialogField select {
  height: 40px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 14px;
  color: #0f172a;
  background: #fff;
  outline: none;
}

.dialogField select:focus {
  border-color: #3f78d0;
  box-shadow: 0 0 0 3px rgba(63, 120, 208, 0.12);
}

.dialogActions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.confirmNotice {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 3900;
}

.confirmNoticeCard {
  width: min(400px, calc(100vw - 32px));
  background: #fff;
  border: 1px solid #f6dfad;
  border-radius: 8px;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.2);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.confirmNoticeTitle {
  margin: 0;
  font-size: 15px;
  color: #9a6700;
  font-weight: 700;
}

.confirmNoticeCard p {
  margin: 0;
  font-size: 14px;
  color: #7c5a00;
}

.confirmNoticeActions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.toast {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 4000;
  min-width: 260px;
  max-width: min(420px, calc(100vw - 32px));
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.18);
  border: 1px solid transparent;
}

.toast.success {
  background: #e9f9ef;
  color: #1f7a47;
  border-color: #cdeed9;
}

.toast.error {
  background: #fff0f0;
  color: #c23939;
  border-color: #f6d8d8;
}
</style>
