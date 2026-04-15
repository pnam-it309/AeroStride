<template>
  <div class="page">
    <PageHeader title="Chi tiết hóa đơn" :backTo="{ name: 'orders.list' }" />

    <div v-if="loaded" ref="invoicePrintRef" class="detailContent">
      <!-- Header Section -->
      <AppCard>
        <div class="headerSection">
          <div class="headerInfo">
            <h2>Hóa đơn {{ order.code }}</h2>
            <div class="statusBadge">
              <StatusBadge
                :text="getStatusLabel(order.status, order.invoiceType)"
                :tone="getStatusTone(order.status)"
              />
            </div>
          </div>
          <div class="headerRight">
            <div class="datetime">
              <Icon name="calendar" />
              <span>{{ formatDateTime(order.createdAt) }}</span>
            </div>

            <AppButton
              class="updateBtn excludeFromPdf"
              :disabled="updatingStatus"
              @click="openStatusDialog"
            >
              <span class="btnInline">
                <Icon name="edit" />
                <span>Cập nhật trạng thái</span>
              </span>
            </AppButton>

            <AppButton
              class="printBtn excludeFromPdf"
              :disabled="exportingPdf"
              @click="exportInvoicePdf"
            >
              <span class="btnInline">
                <Icon name="printer" />
                <span>{{
                  exportingPdf ? "Đang tạo PDF..." : "In hóa đơn PDF"
                }}</span>
              </span>
            </AppButton>
          </div>
        </div>
      </AppCard>

      <!-- Status Timeline -->
      <AppCard title="Trạng thái đơn hàng" class="timelineCard">
        <div class="timelineWrap">
          <div
            v-for="(step, index) in timelineSteps"
            :key="step.key"
            :class="[
              'timelineStep',
              step.state,
              step.tone,
              { last: index === timelineSteps.length - 1 },
            ]"
          >
            <div class="timelineNodeRow">
              <div
                class="timelineLine left"
                :class="[
                  { active: index > 0 && index <= activeStepIndex },
                  index > 0 && index <= activeStepIndex
                    ? timelineSteps[index - 1].tone
                    : '',
                ]"
              ></div>
              <div class="timelineNode" :class="[step.state, step.tone]">
                <Icon :name="step.icon" />
              </div>
              <div
                class="timelineLine right"
                :class="[
                  { active: index < activeStepIndex },
                  index < activeStepIndex ? step.tone : '',
                ]"
              ></div>
            </div>
            <div class="timelineText">
              <div class="timelineLabel">{{ step.label }}</div>
              <div class="timelineNote">{{ step.note }}</div>
              <div v-if="step.active" class="timelineMeta">{{ step.meta }}</div>
            </div>
          </div>
        </div>
      </AppCard>

      <!-- Main Content: Left (Products) + Right (Customer) -->
      <div class="mainGrid">
        <!-- LEFT: Products Table -->
        <AppCard title="Sản phẩm đã đặt" class="productsCard">
          <AppTable :columns="productColumns" :rows="order.items">
            <template #cell:unitPrice="{ row }">
              {{ formatCurrency(row.unitPrice) }}
            </template>
            <template #cell:discount="{ row }">
              {{ formatCurrency(row.discount) }}
            </template>
            <template #cell:total="{ row }">
              {{ formatCurrency(row.total) }}
            </template>
          </AppTable>
          <div class="productSummary">
            <div class="summaryRow">
              <span class="muted">Tổng tiền hàng</span>
              <b>{{ formatCurrency(calculateSubtotal()) }}</b>
            </div>
            <div class="summaryRow">
              <span class="muted">Tổng giảm giá</span>
              <b class="discount">-{{ formatCurrency(calculateDiscount()) }}</b>
            </div>
            <div class="summaryRow highlight">
              <span class="muted">Tổng cộng</span>
              <b>{{ formatCurrency(order.total) }}</b>
            </div>
          </div>
        </AppCard>

        <!-- RIGHT: Customer Info -->
        <div class="rightSection">
          <!-- Customer Card -->
          <AppCard title="Khách hàng">
            <div class="customerInfo">
              <div class="infoRow">
                <span class="label">Tên khách hàng</span>
                <span class="value">{{ order.customer }}</span>
              </div>
              <div class="infoRow">
                <span class="label">Liên hệ</span>
                <div class="contactInfo">
                  <div v-if="order.phone" class="contact-item">
                    <Icon name="phone" class="icon" />
                    <span>{{ order.phone }}</span>
                  </div>
                  <div v-if="order.email" class="contact-item">
                    <Icon name="mail" class="icon" />
                    <span>{{ order.email }}</span>
                  </div>
                </div>
              </div>
              <div class="infoRow">
                <span class="label">Địa chỉ</span>
                <span class="value">{{ order.address }}</span>
              </div>

              <div v-if="isOnline" class="shippingSection">
                <div class="shippingTitle">
                  <Icon name="truck" class="icon" />
                  <span>Thông tin giao hàng</span>
                </div>

                <div class="shippingGrid">
                  <div class="shippingRow">
                    <span class="shippingLabel">Địa chỉ</span>
                    <span class="shippingValue">{{
                      order.address || "-"
                    }}</span>
                  </div>
                  <div class="shippingRow">
                    <span class="shippingLabel">Loại đơn</span>
                    <span class="shippingValue">{{ invoiceTypeText }}</span>
                  </div>
                  <div class="shippingRow">
                    <span class="shippingLabel">Ghi chú</span>
                    <span class="shippingValue">{{ shippingNoteText }}</span>
                  </div>
                </div>
              </div>
            </div>
          </AppCard>

          <!-- Payment Card -->
          <AppCard title="Phương thức thanh toán">
            <div class="paymentInfo">
              <div class="method">
                <Icon name="creditCard" />
                <div class="methodText">
                  <div class="methodName">
                    {{
                      paymentMethodInfo.label ||
                      order.paymentMethodName ||
                      order.paymentMethod ||
                      "-"
                    }}
                  </div>
                </div>
              </div>
            </div>
          </AppCard>
        </div>
      </div>

      <!-- Actions -->
      <div class="actions">
        <AppButton class="excludeFromPdf" variant="ghost" @click="goBack"
          >Quay lại</AppButton
        >
      </div>
    </div>

    <div v-else class="loading">
      <p>Đang tải…</p>
    </div>

    <div
      v-if="statusDialogOpen"
      class="dialogOverlay"
      @click.self="closeStatusDialog"
    >
      <div class="statusDialog" role="dialog" aria-modal="true">
        <h3>Cập nhật trạng thái hóa đơn</h3>
        <p class="dialogDesc">
          Hóa đơn <b>{{ order.code }}</b>
        </p>

        <div class="dialogField">
          <label for="detail-order-status">Trạng thái mới</label>
          <select id="detail-order-status" v-model="selectedStatus">
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
            :disabled="updatingStatus"
            @click="closeStatusDialog"
            >Hủy</AppButton
          >
          <AppButton :disabled="updatingStatus" @click="requestStatusUpdate"
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
            :disabled="updatingStatus"
            @click="confirmNoticeOpen = false"
            >Không</AppButton
          >
          <AppButton :disabled="updatingStatus" @click="executeStatusUpdate">
            {{ updatingStatus ? "Đang cập nhật..." : "OK" }}
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
import { useRoute, useRouter } from "vue-router";
import PageHeader from "@/components/page/PageHeader.vue";
import AppCard from "@/components/common/AppCard.vue";
import AppTable from "@/components/common/AppTable.vue";
import AppButton from "@/components/common/AppButton.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import Icon from "@/components/common/Icon.vue";
import { orderService } from "@/services/order.service";
import { db } from "@/services/mock.db";

const route = useRoute();
const router = useRouter();

const loaded = ref(false);
const exportingPdf = ref(false);
const updatingStatus = ref(false);
const statusDialogOpen = ref(false);
const selectedStatus = ref("");
const confirmNoticeOpen = ref(false);
const pendingStatus = ref("");
const toast = ref({ show: false, message: "", type: "success" });
let toastTimer = null;
const invoicePrintRef = ref(null);
const order = ref({
  code: "",
  customer: "",
  email: "",
  phone: "",
  status: "",
  createdAt: "",
  total: 0,
  items: [],
  paymentMethod: "",
  paymentMethodCode: "",
  paymentMethodName: "",
  paymentType: "",
  address: "",
  note: "",
  shippingNote: "",
  invoiceType: "offline",
  recipientName: "",
  recipientPhone: "",
  shippingMethod: "",
});

const productColumns = [
  { key: "productName", label: "Tên sản phẩm", width: "200px" },
  { key: "qty", label: "Số lượng", width: "80px" },
  { key: "unitPrice", label: "Đơn giá", width: "120px" },
  { key: "discount", label: "Giảm giá", width: "120px" },
  { key: "total", label: "Thành tiền", width: "120px" },
];

const statusDefinitions = [
  { code: "WAITING_CONFIRMATION", label: "Chờ xác nhận" },
  { code: "CONFIRMED", label: "Đã xác nhận" },
  { code: "WAITING_DELIVERY", label: "Chờ giao" },
  { code: "DELIVERING", label: "Đang giao" },
  { code: "PAID", label: "Đã thanh toán" },
  { code: "COMPLETED", label: "Hoàn thành" },
  { code: "CANCELED", label: "Đã hủy" },
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

onMounted(async () => {
  order.value = await orderService.get(route.params.id);
  loaded.value = true;
});

const isOnline = computed(() => order.value.invoiceType === "online");

const invoiceTypeText = computed(() =>
  String(order.value.invoiceType || "").toLowerCase() === "online"
    ? "Online"
    : "Offline",
);

const shippingNoteText = computed(() => {
  const note = String(
    order.value.shippingNote || order.value.note || order.value.ghiChu || "",
  ).trim();
  return note || "-";
});

const paymentMethodInfo = computed(() => {
  const methods = db.paymentMethods || [];
  const code = String(order.value.paymentMethodCode || "").trim();
  const name = String(
    order.value.paymentMethodName || order.value.paymentMethod || "",
  ).trim();

  const byCode = methods.find(
    (item) => String(item.ma_phuong_thuc_thanh_toan) === code,
  );
  if (byCode) {
    return {
      code: byCode.ma_phuong_thuc_thanh_toan,
      label: byCode.ten_phuong_thuc_thanh_toan,
      type: byCode.loaiThanhToan,
    };
  }

  const byName = methods.find(
    (item) => item.ten_phuong_thuc_thanh_toan === name,
  );
  if (byName) {
    return {
      code: byName.ma_phuong_thuc_thanh_toan,
      label: byName.ten_phuong_thuc_thanh_toan,
      type: byName.loaiThanhToan,
    };
  }

  return {
    code,
    label: name,
    type: String(order.value.paymentType || "").toUpperCase(),
  };
});

const timelineSteps = computed(() => {
  const canceled = order.value.status === "CANCELED";
  const completedLabel = canceled ? "Đã hủy" : "Hoàn thành";
  const completedIcon = canceled ? "xCircle" : "checkCircle";
  const completedStatus = canceled ? "CANCELED" : "COMPLETED";

  const onlineSteps = [
    {
      key: "WAITING_CONFIRMATION",
      label: "Chờ xác nhận",
      note: "Đơn hàng đã được tạo và đang chờ xử lý.",
      icon: "checkCircle",
      doneStatus: "WAITING_CONFIRMATION",
    },
    {
      key: "CONFIRMED",
      label: "Đã xác nhận",
      note: "Đơn hàng đã được xác nhận.",
      icon: "check",
      doneStatus: "CONFIRMED",
    },
    {
      key: "WAITING_DELIVERY",
      label: "Chờ giao",
      note: "Đang chờ lấy hàng và bàn giao cho đơn vị vận chuyển.",
      icon: "box",
      doneStatus: "WAITING_DELIVERY",
    },
    {
      key: "DELIVERING",
      label: "Đang giao",
      note: "Đơn đang trên đường giao đến khách hàng.",
      icon: "truck",
      doneStatus: "DELIVERING",
    },
    {
      key: "PAID",
      label: "Đã thanh toán",
      note: "Thanh toán đã được ghi nhận.",
      icon: "creditCard",
      doneStatus: "PAID",
    },
    {
      key: completedStatus,
      label: completedLabel,
      note: canceled
        ? "Đơn hàng đã bị hủy và dừng xử lý."
        : "Đơn hàng đã được hoàn tất.",
      icon: completedIcon,
      doneStatus: completedStatus,
      terminal: true,
    },
  ];

  const offlineSteps = [
    {
      key: "CONFIRMED",
      label: "Đã xác nhận",
      note: "Đơn hàng tại quầy đã được xác nhận.",
      icon: "check",
      doneStatus: "CONFIRMED",
    },
    {
      key: completedStatus,
      label: completedLabel,
      note: canceled
        ? "Đơn hàng đã bị hủy."
        : "Đơn hàng đã hoàn tất giao dịch.",
      icon: completedIcon,
      doneStatus: completedStatus,
      terminal: true,
    },
  ];

  const steps = isOnline.value ? onlineSteps : offlineSteps;
  const currentIndex = steps.findIndex(
    (step) => step.doneStatus === order.value.status,
  );

  return steps.map((step, index) => ({
    ...step,
    tone: getStatusTone(step.doneStatus),
    active: currentIndex === index,
    done:
      currentIndex > index ||
      (order.value.status === "CANCELED" && step.doneStatus === "CANCELED"),
    state:
      currentIndex === index
        ? "active"
        : currentIndex > index
          ? "done"
          : "pending",
    meta: index === currentIndex ? getStatusLabel(order.value.status) : "",
  }));
});

const activeStepIndex = computed(() =>
  timelineSteps.value.findIndex((step) => step.active),
);

const statusDialogOptions = computed(() => {
  const type = String(order.value.invoiceType || "offline").toLowerCase();
  const allowed = statusFlowByInvoice[type] || statusFlowByInvoice.offline;
  return allowed
    .map((code) => statusDefinitions.find((x) => x.code === code))
    .filter(Boolean);
});

function formatCurrency(value) {
  if (value == null) return "0 ₫";
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(value);
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

  return `${hh}:${min} ${dd}/${mm}/${yyyy}`;
}

function getStatusLabel(status) {
  const map = {
    WAITING_CONFIRMATION: "Chờ xác nhận",
    CONFIRMED: "Đã xác nhận",
    WAITING_DELIVERY: isOnline.value ? "Chờ giao" : "Giao hàng",
    DELIVERING: "Đang giao",
    PAID: "Đã thanh toán",
    COMPLETED: "Hoàn thành",
    CANCELED: "Đã hủy",
  };
  return map[status] || status;
}

function getStatusTone(status) {
  const map = {
    WAITING_CONFIRMATION: "yellow",
    CONFIRMED: "blue",
    WAITING_DELIVERY: "orange",
    DELIVERING: "blue",
    COMPLETED: "green",
    CANCELED: "red",
    PAID: "green",
  };
  return map[status] || "gray";
}

function calculateSubtotal() {
  return order.value.items.reduce(
    (sum, item) => sum + item.unitPrice * item.qty,
    0,
  );
}

function calculateDiscount() {
  return order.value.items.reduce((sum, item) => sum + item.discount, 0);
}

function goBack() {
  router.push({ name: "orders.list" });
}

function openStatusDialog() {
  selectedStatus.value = String(order.value.status || "").toUpperCase();
  confirmNoticeOpen.value = false;
  statusDialogOpen.value = true;
}

function closeStatusDialog() {
  if (updatingStatus.value) return;
  statusDialogOpen.value = false;
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
  const current = String(order.value.status || "").toUpperCase();
  if (!selectedStatus.value) return;
  if (selectedStatus.value === current) {
    showToast("Trạng thái mới trùng với trạng thái hiện tại.", "error");
    return;
  }

  pendingStatus.value = selectedStatus.value;
  statusDialogOpen.value = false;
  confirmNoticeOpen.value = true;
}

async function executeStatusUpdate() {
  if (!pendingStatus.value) return;

  confirmNoticeOpen.value = false;

  updatingStatus.value = true;
  try {
    await orderService.update(order.value.id, { status: pendingStatus.value });
    order.value = await orderService.get(route.params.id);
    closeStatusDialog();
    pendingStatus.value = "";
    showToast("Cập nhật trạng thái hóa đơn thành công.", "success");
  } catch (error) {
    console.error("Update order status failed:", error);
    pendingStatus.value = "";
    showToast("Không thể cập nhật trạng thái hóa đơn.", "error");
  } finally {
    updatingStatus.value = false;
  }
}

async function exportInvoicePdf() {
  if (!invoicePrintRef.value || exportingPdf.value) return;

  exportingPdf.value = true;
  try {
    const { jsPDF } = await import("jspdf");
    const fileName = `hoa-don-${order.value.code || "chi-tiet"}.pdf`;
    const doc = new jsPDF({
      orientation: "p",
      unit: "pt",
      format: "a4",
      compress: true,
    });

    await doc.html(invoicePrintRef.value, {
      margin: [24, 24, 24, 24],
      autoPaging: "text",
      x: 24,
      y: 24,
      width: 547,
      windowWidth: invoicePrintRef.value.scrollWidth,
      html2canvas: {
        scale: 1,
        useCORS: true,
        ignoreElements: (element) =>
          element.classList?.contains("excludeFromPdf"),
      },
    });

    doc.save(fileName);
  } catch (error) {
    console.error("Export invoice PDF failed:", error);
  } finally {
    exportingPdf.value = false;
  }
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  color: #6b7280;
}

/* Header Section */
.headerSection {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.headerInfo {
  display: flex;
  align-items: center;
  gap: 16px;
}

.headerInfo h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
}

.statusBadge {
  display: flex;
  align-items: center;
}

.headerRight {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.printBtn {
  height: 34px;
  padding: 0 12px;
  font-size: 13px;
}

.updateBtn {
  height: 34px;
  padding: 0 12px;
  font-size: 13px;
}

.timelineCard {
  overflow: hidden;
  margin-top: 10px;
  margin-bottom: 14px;
}

.timelineCard :deep(.head) {
  margin-bottom: 18px;
}

.timelineCard :deep(.body) {
  padding-top: 6px;
  padding-bottom: 14px;
}

.timelineWrap {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(145px, 1fr));
  gap: 14px;
  align-items: start;
  padding-top: 4px;
}

.timelineStep {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
}

.timelineNodeRow {
  width: 100%;
  display: flex;
  align-items: center;
}

.timelineLine {
  flex: 1;
  height: 3px;
  background: #e5e7eb;
  border-radius: 999px;
}

.timelineLine.active {
  background: #60a5fa;
}

.timelineLine.active.yellow {
  background: #d6a82b;
}

.timelineLine.active.blue {
  background: #4f8df3;
}

.timelineLine.active.orange {
  background: #f59e0b;
}

.timelineLine.active.green {
  background: #3fb47d;
}

.timelineLine.active.red {
  background: #ef4444;
}

.timelineStep:first-child .timelineLine.left {
  background: transparent;
}

.timelineStep:last-child .timelineLine.right {
  background: transparent;
}

.timelineNode {
  width: 54px;
  height: 54px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #fff;
  border: 2px solid #d1d5db;
  color: #94a3b8;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.06);
}

.timelineNode :deep(.icon) {
  width: 22px;
  height: 22px;
}

.timelineNode.done {
  background: #e9f8ed;
  border-color: #7fce8e;
  color: #3f9f55;
}

.timelineNode.active {
  background: #eaf2ff;
  border-color: #6aa1f4;
  color: #2f7be7;
}

.timelineNode.done.yellow,
.timelineNode.active.yellow {
  background: #fff8e6;
  border-color: #f5d083;
  color: #b88112;
}

.timelineNode.done.blue,
.timelineNode.active.blue {
  background: #eaf2ff;
  border-color: #8eb6f8;
  color: #2f7be7;
}

.timelineNode.done.orange,
.timelineNode.active.orange {
  background: #fff3e3;
  border-color: #ffc27a;
  color: #d97706;
}

.timelineNode.done.green,
.timelineNode.active.green {
  background: #e8f8ef;
  border-color: #8ad7ad;
  color: #1f8a56;
}

.timelineNode.done.red,
.timelineNode.active.red {
  background: #feecec;
  border-color: #f5a3a3;
  color: #dc2626;
}

.timelineNode.pending {
  background: #f3f4f6;
  color: #9ca3af;
}

.timelineText {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}

.timelineLabel {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}

.timelineNote {
  font-size: 12px;
  line-height: 1.45;
  color: #64748b;
}

.timelineMeta {
  font-size: 12px;
  font-weight: 700;
  color: #3f78d0;
}

.timelineStep.yellow .timelineLabel,
.timelineStep.yellow .timelineMeta {
  color: #b88112;
}

.timelineStep.blue .timelineLabel,
.timelineStep.blue .timelineMeta {
  color: #2f7be7;
}

.timelineStep.orange .timelineLabel,
.timelineStep.orange .timelineMeta {
  color: #d97706;
}

.timelineStep.green .timelineLabel,
.timelineStep.green .timelineMeta {
  color: #1f8a56;
}

.timelineStep.red .timelineLabel,
.timelineStep.red .timelineMeta {
  color: #dc2626;
}

.datetime {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-size: 14px;
}

.datetime :deep(.icon) {
  width: 16px;
  height: 16px;
  color: #9ca3af;
}

/* Main Grid Layout */
.mainGrid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 1200px) {
  .mainGrid {
    grid-template-columns: 1fr;
  }

  .timelineWrap {
    grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  }
}

@media (max-width: 720px) {
  .headerSection {
    flex-direction: column;
    align-items: flex-start;
  }

  .headerRight {
    width: 100%;
    align-items: flex-start;
    justify-content: flex-start;
  }

  .timelineWrap {
    grid-template-columns: 1fr;
  }

  .timelineStep {
    align-items: flex-start;
    text-align: left;
  }

  .timelineNodeRow {
    max-width: 260px;
  }
}

.productsCard {
  grid-column: 1;
}

.rightSection {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Product Summary */
.productSummary {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #edf2fa;
  margin-top: 16px;
}

.summaryRow {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.summaryRow.highlight {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  padding-top: 8px;
  border-top: 1px solid #edf2fa;
}

.discount {
  color: #ef4444;
}

/* Customer Info */
.customerInfo,
.paymentInfo {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.shippingSection {
  border-top: 1px dashed #dbe3ef;
  margin-top: 4px;
  padding-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.shippingTitle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #334155;
}

.shippingTitle .icon {
  width: 16px;
  height: 16px;
  color: #64748b;
}

.shippingGrid {
  display: grid;
  gap: 8px;
}

.shippingRow {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  align-items: start;
  column-gap: 10px;
}

.shippingLabel {
  font-size: 13px;
  color: #64748b;
  font-weight: 600;
}

.shippingValue {
  font-size: 13px;
  color: #1f2937;
  line-height: 1.45;
  word-break: break-word;
}

.infoRow {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 600;
  text-transform: uppercase;
}

.value {
  font-size: 14px;
  color: #1f2937;
  font-weight: 500;
}

.contactInfo {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #374151;
}

.contact-item .icon {
  width: 16px;
  height: 16px;
  color: #6b7280;
}

.methodText {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.methodName {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}

.method :deep(.icon) {
  width: 20px;
  height: 20px;
  color: #3f78d0;
}

/* Actions */
.actions {
  display: flex;
  justify-content: flex-start;
  gap: 8px;
}

.btnInline {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btnInline :deep(.icon) {
  width: 16px;
  height: 16px;
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
