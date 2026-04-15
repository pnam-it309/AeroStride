<template>
  <GenericEdit
    title="Sửa phiên bán hàng"
    :backTo="{ name: 'sales.list' }"
    :redirectTo="{ name: 'sales.list' }"
    :service="salesService"
    :fields="fields"
  />
</template>

<script setup>
import GenericEdit from "@/views/_generic/GenericEdit.vue";
import { salesService } from "@/services/sales.service";

const fields = [
  { key: "code", label: "Mã" },
  { key: "name", label: "Tên" },
  { key: "status", label: "Trạng thái" },
  { key: "createdAt", label: "Ngày tạo" },
  { key: "note", label: "Ghi chú" },
];
</script>
