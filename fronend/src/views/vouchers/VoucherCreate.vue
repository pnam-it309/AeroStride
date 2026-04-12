<template>
  <div class="page">
    <PageHeader
      title="Tạo phiếu giảm giá"
      :backTo="{ name: 'vouchers.list' }"
    />

    <AppCard title="Thông tin phiếu">
      <div class="form">
        <AppInput label="Mã" placeholder="VD: V06" v-model="form.code" />
        <AppInput label="Tên" placeholder="VD: Giảm 15%" v-model="form.name" />
        <AppSelect label="Loại" v-model="form.type" :options="typeOptions" />
        <AppInput
          label="Giá trị"
          placeholder="VD: 15 hoặc 50000"
          v-model="form.value"
        />
        <AppDateInput label="Ngày bắt đầu" v-model="form.start" />
        <AppDateInput label="Ngày kết thúc" v-model="form.end" />
        <AppInput label="Số lượng" placeholder="VD: 100" v-model="form.qty" />
        <AppSelect
          label="Trạng thái"
          v-model="form.status"
          :options="statusOptions"
        />
      </div>

      <Toolbar>
        <template #right>
          <AppButton variant="ghost" @click="cancel">Hủy</AppButton>
          <AppButton @click="save">Lưu</AppButton>
        </template>
      </Toolbar>
    </AppCard>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import { useRouter } from "vue-router";
import { voucherService } from "@/services/voucher.service";

import PageHeader from "@/components/page/PageHeader.vue";
import Toolbar from "@/components/page/Toolbar.vue";
import AppCard from "@/components/common/AppCard.vue";
import AppInput from "@/components/common/AppInput.vue";
import AppSelect from "@/components/common/AppSelect.vue";
import AppDateInput from "@/components/common/AppDateInput.vue";
import AppButton from "@/components/common/AppButton.vue";

const router = useRouter();

const typeOptions = [
  { value: "PERCENT", label: "Phần trăm" },
  { value: "FIXED", label: "Tiền cố định" },
  { value: "SHIP", label: "Vận chuyển" },
];

const statusOptions = [
  { value: "ACTIVE", label: "Đang áp dụng" },
  { value: "INACTIVE", label: "Chưa áp dụng" },
];

const form = reactive({
  code: "",
  name: "",
  type: "PERCENT",
  value: "",
  start: "",
  end: "",
  qty: "100",
  status: "INACTIVE",
});

function cancel() {
  router.push({ name: "vouchers.list" });
}

async function save() {
  // validate tối thiểu cho UI
  if (!form.code || !form.name) return alert("Vui lòng nhập Mã và Tên");
  await voucherService.create({
    code: form.code,
    name: form.name,
    type: form.type,
    value: Number(form.value || 0),
    start: form.start || new Date().toISOString().slice(0, 10),
    end: form.end || new Date().toISOString().slice(0, 10),
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
