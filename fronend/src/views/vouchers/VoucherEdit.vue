<template>
  <div class="page">
    <PageHeader
      title="Sửa phiếu giảm giá"
      :backTo="{ name: 'vouchers.list' }"
    />

    <AppCard title="Thông tin phiếu">
      <div class="form" v-if="loaded">
        <AppInput label="Mã" v-model="form.code" />
        <AppInput label="Tên" v-model="form.name" />
        <AppSelect label="Loại" v-model="form.type" :options="typeOptions" />
        <AppInput label="Giá trị" v-model="form.value" />
        <AppDateInput label="Ngày bắt đầu" v-model="form.start" />
        <AppDateInput label="Ngày kết thúc" v-model="form.end" />
        <AppInput label="Số lượng" v-model="form.qty" />
        <AppSelect
          label="Trạng thái"
          v-model="form.status"
          :options="statusOptions"
        />
      </div>

      <div v-else class="muted">Đang tải…</div>

      <Toolbar>
        <template #right>
          <AppButton variant="ghost" @click="cancel">Hủy</AppButton>
          <AppButton @click="save">Cập nhật</AppButton>
        </template>
      </Toolbar>
    </AppCard>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { voucherService } from "@/services/voucher.service";

import PageHeader from "@/components/page/PageHeader.vue";
import Toolbar from "@/components/page/Toolbar.vue";
import AppCard from "@/components/common/AppCard.vue";
import AppInput from "@/components/common/AppInput.vue";
import AppSelect from "@/components/common/AppSelect.vue";
import AppDateInput from "@/components/common/AppDateInput.vue";
import AppButton from "@/components/common/AppButton.vue";

const route = useRoute();
const router = useRouter();
const loaded = ref(false);

const typeOptions = [
  { value: "PERCENT", label: "Phần trăm" },
  { value: "FIXED", label: "Tiền cố định" },
  { value: "SHIP", label: "Vận chuyển" },
];
const statusOptions = [
  { value: "ACTIVE", label: "Đang áp dụng" },
  { value: "INACTIVE", label: "Chưa áp dụng" },
  { value: "EXPIRED", label: "Hết hạn" },
  { value: "OUT_OF_STOCK", label: "Hết số lượng" },
];

const form = reactive({
  code: "",
  name: "",
  type: "PERCENT",
  value: "",
  start: "",
  end: "",
  qty: "",
  status: "INACTIVE",
});

onMounted(async () => {
  const data = await voucherService.get(route.params.id);
  Object.assign(form, {
    code: data.code,
    name: data.name,
    type: data.type,
    value: String(data.value),
    start: data.start,
    end: data.end,
    qty: String(data.qty),
    status: data.status,
  });
  loaded.value = true;
});

function cancel() {
  router.push({ name: "vouchers.list" });
}

async function save() {
  await voucherService.update(route.params.id, {
    code: form.code,
    name: form.name,
    type: form.type,
    value: Number(form.value || 0),
    start: form.start,
    end: form.end,
    qty: Number(form.qty || 0),
    status: form.status,
  });
  router.push({ name: "vouchers.list" });
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.form {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
</style>
