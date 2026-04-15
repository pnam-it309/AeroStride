<template>
  <div class="page">
    <PageHeader :title="title" :backTo="backTo" />

    <AppCard :title="cardTitle">
      <div v-if="loaded" class="detail">
        <div v-for="f in fields" :key="f.key" class="item">
          <span class="muted">{{ f.label }}</span>
          <b>{{ displayValue(f.key, data[f.key]) }}</b>
        </div>
      </div>
      <div v-else class="muted">Đang tải…</div>

      <Toolbar>
        <template #right>
          <AppButton variant="ghost" @click="back">Quay lại</AppButton>
          <AppButton v-if="canEdit" @click="goEdit">Sửa</AppButton>
        </template>
      </Toolbar>
    </AppCard>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import PageHeader from "@/components/page/PageHeader.vue";
import Toolbar from "@/components/page/Toolbar.vue";
import AppCard from "@/components/common/AppCard.vue";
import AppButton from "@/components/common/AppButton.vue";

const props = defineProps({
  title: { type: String, required: true },
  cardTitle: { type: String, default: "Thông tin" },
  backTo: { type: Object, required: true },
  editToName: { type: String, required: true },
  redirectTo: { type: Object, required: true },
  service: { type: Object, required: true }, // get(id)
  fields: { type: Array, default: () => [] },
  canEdit: { type: Boolean, default: true },
});

const route = useRoute();
const router = useRouter();

const loaded = ref(false);
const data = ref({});

onMounted(async () => {
  data.value = await props.service.get(route.params.id);
  loaded.value = true;
});

function displayValue(key, value) {
  if (key === "status" || key === "trangThai") {
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
    const text = String(value ?? "").toUpperCase();
    return map[text] || value;
  }

  const dateKeys = [
    "createdAt",
    "updatedAt",
    "start",
    "end",
    "ngayBatDau",
    "ngayKetThuc",
    "paidAt",
    "ngayTao",
    "ngayCapNhat",
    "ngaySinh",
  ];
  if (dateKeys.includes(key)) {
    if (value == null || value === "") return "-";
    const raw = String(value).trim();
    const numeric = Number(raw);
    const normalized = /^\d{4}-\d{2}-\d{2}$/.test(raw)
      ? `${raw}T00:00:00`
      : raw;
    const date =
      Number.isFinite(numeric) && /^\d+$/.test(raw)
        ? new Date(numeric)
        : new Date(normalized);
    if (Number.isNaN(date.getTime())) return raw;
    const pad = (n) => String(n).padStart(2, "0");
    return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  const moneyKeys = [
    "amount",
    "price",
    "soTienGiam",
    "dieuKienGiamGia",
    "giamToiDa",
    "donHangToiThieu",
  ];
  if (moneyKeys.includes(key)) {
    if (value == null || value === "") return "-";
    const amount = Number(value);
    if (Number.isNaN(amount)) return value;
    return `${amount.toLocaleString("vi-VN")} đ`;
  }

  if (key === "gioiTinh") {
    if (value == null || value === "") return "-";
    if (typeof value === "boolean") return value ? "Nam" : "Nữ";
    const normalized = String(value).trim().toLowerCase();
    if (["true", "1", "nam", "male", "m"].includes(normalized)) return "Nam";
    if (["false", "0", "nữ", "nu", "female", "f"].includes(normalized)) {
      return "Nữ";
    }
    return value;
  }

  return value;
}

function back() {
  router.push(props.redirectTo);
}
function goEdit() {
  if (!props.canEdit) return;
  router.push({ name: props.editToName, params: { id: route.params.id } });
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.detail {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
.item {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
