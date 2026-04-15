<template>
  <GenericCreate
    title="Tạo hóa đơn"
    :backTo="{ name: 'orders.list' }"
    :redirectTo="{ name: 'orders.list' }"
    :service="orderService"
    :fields="fields"
    :defaults="defaults"
  />
</template>

<script setup>
import GenericCreate from "@/views/_generic/GenericCreate.vue";
import { orderService } from "@/services/order.service";

const fields = [
  { key: "code", label: "Mã", placeholder: "HD-0002" },
  { key: "customer", label: "Khách hàng", placeholder: "Tên KH" },
  { key: "total", label: "Tổng tiền", placeholder: "1990000" },
  { key: "status", label: "Trạng thái", placeholder: "PAID / PENDING" },
  { key: "createdAt", label: "Ngày tạo", placeholder: "2026-04-11" },
];
const defaults = {
  code: "",
  customer: "",
  total: 0,
  status: "PENDING",
  createdAt: new Date().toISOString().slice(0, 10),
};
</script>
