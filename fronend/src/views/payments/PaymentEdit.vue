<template>
  <GenericEdit
    title="Sửa phương thức thanh toán"
    :backTo="{ name: 'payments.list' }"
    :redirectTo="{ name: 'payments.list' }"
    :service="paymentService"
    :fields="fields"
  />
</template>

<script setup>
import GenericEdit from "@/views/_generic/GenericEdit.vue";
import { paymentService } from "@/services/payment.service";

const fields = [
  { key: "code", label: "Mã" },
  { key: "name", label: "Tên" },
  { key: "status", label: "Trạng thái" },
  { key: "note", label: "Ghi chú" },
];
</script>
