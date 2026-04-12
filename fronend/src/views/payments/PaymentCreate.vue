<template>
  <GenericCreate
    title="Tạo phương thức thanh toán"
    :backTo="{ name: 'payments.list' }"
    :redirectTo="{ name: 'payments.list' }"
    :service="paymentService"
    :fields="fields"
    :defaults="defaults"
  />
</template>

<script setup>
import GenericCreate from "@/views/_generic/GenericCreate.vue";
import { paymentService } from "@/services/payment.service";

const fields = [
  { key: "code", label: "Mã", placeholder: "PM02" },
  { key: "name", label: "Tên", placeholder: "Chuyển khoản" },
  { key: "status", label: "Trạng thái", placeholder: "ACTIVE" },
  { key: "note", label: "Ghi chú", placeholder: "" },
];
const defaults = { code: "", name: "", status: "ACTIVE", note: "" };
</script>
