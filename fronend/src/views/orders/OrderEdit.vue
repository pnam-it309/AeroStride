<template>
  <GenericEdit
    title="Sửa hóa đơn"
    :backTo="{ name: 'orders.list' }"
    :redirectTo="{ name: 'orders.list' }"
    :service="orderService"
    :fields="fields"
  />
</template>

<script setup>
import GenericEdit from "@/views/_generic/GenericEdit.vue";
import { orderService } from "@/services/order.service";

const fields = [
  { key: "code", label: "Mã" },
  { key: "customer", label: "Khách hàng" },
  { key: "total", label: "Tổng tiền" },
  { key: "status", label: "Trạng thái" },
  { key: "createdAt", label: "Ngày tạo" },
];
</script>
