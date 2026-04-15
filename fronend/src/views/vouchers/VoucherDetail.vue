<template>
  <div class="page">
    <PageHeader
      title="Chi tiết phiếu giảm giá"
      :backTo="{ name: 'vouchers.list' }"
    />

    <AppCard title="Thông tin">
      <div v-if="loaded" class="detail">
        <div class="item">
          <span class="muted">Mã</span><b>{{ data.code }}</b>
        </div>
        <div class="item">
          <span class="muted">Tên</span><b>{{ data.name }}</b>
        </div>
        <div class="item">
          <span class="muted">Loại</span><b>{{ data.type }}</b>
        </div>
        <div class="item">
          <span class="muted">Giá trị</span><b>{{ data.value }}</b>
        </div>
        <div class="item">
          <span class="muted">Ngày bắt đầu</span><b>{{ data.start }}</b>
        </div>
        <div class="item">
          <span class="muted">Ngày kết thúc</span><b>{{ data.end }}</b>
        </div>
        <div class="item">
          <span class="muted">Số lượng</span><b>{{ data.qty }}</b>
        </div>
        <div class="item">
          <span class="muted">Trạng thái</span><b>{{ data.status }}</b>
        </div>
      </div>

      <Toolbar>
        <template #right>
          <AppButton variant="ghost" @click="back">Quay lại</AppButton>
          <AppButton @click="goEdit">Sửa</AppButton>
        </template>
      </Toolbar>
    </AppCard>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { voucherService } from "@/services/voucher.service";

import PageHeader from "@/components/page/PageHeader.vue";
import Toolbar from "@/components/page/Toolbar.vue";
import AppCard from "@/components/common/AppCard.vue";
import AppButton from "@/components/common/AppButton.vue";

const route = useRoute();
const router = useRouter();

const loaded = ref(false);
const data = ref({});

onMounted(async () => {
  data.value = await voucherService.get(route.params.id);
  loaded.value = true;
});

function back() {
  router.push({ name: "vouchers.list" });
}
function goEdit() {
  router.push({ name: "vouchers.edit", params: { id: route.params.id } });
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
