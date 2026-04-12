<template>
  <GenericDetail
    title="Chi tiết phiên bán hàng"
    :backTo="{ name: 'sales.list' }"
    :redirectTo="{ name: 'sales.list' }"
    editToName="sales.edit"
    :service="salesService"
    :fields="fields"
  />
</template>

<script setup>
import GenericDetail from "@/views/_generic/GenericDetail.vue";
import { salesService } from "@/services/sales.service";

const fields = [
  { key: "code", label: "Mã" },
  { key: "name", label: "Tên" },
  { key: "status", label: "Trạng thái" },
  { key: "createdAt", label: "Ngày tạo" },
  { key: "note", label: "Ghi chú" },
];
</script>
