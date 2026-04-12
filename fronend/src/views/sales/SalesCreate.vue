<template>
  <GenericCreate
    title="Tạo phiên bán hàng"
    :backTo="{ name: 'sales.list' }"
    :redirectTo="{ name: 'sales.list' }"
    :service="salesService"
    :fields="fields"
    :defaults="defaults"
  />
</template>

<script setup>
import GenericCreate from "@/views/_generic/GenericCreate.vue";
import { salesService } from "@/services/sales.service";

const fields = [
  { key: "code", label: "Mã", placeholder: "SALE-002" },
  { key: "name", label: "Tên", placeholder: "Phiên bán hàng..." },
  { key: "status", label: "Trạng thái", placeholder: "ACTIVE" },
  { key: "createdAt", label: "Ngày tạo", placeholder: "2026-04-11" },
  { key: "note", label: "Ghi chú", placeholder: "" },
];
const defaults = {
  code: "",
  name: "",
  status: "ACTIVE",
  createdAt: new Date().toISOString().slice(0, 10),
  note: "",
};
</script>
