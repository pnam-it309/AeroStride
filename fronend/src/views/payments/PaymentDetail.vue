<template>
  <GenericDetail
    title="Chi tiết phương thức thanh toán"
    :backTo="{ name: 'payments.list' }"
    :redirectTo="{ name: 'payments.list' }"
    editToName="payments.edit"
    :service="paymentService"
    :fields="fields"
    :canEdit="false"
  />
</template>

<script setup>
import GenericDetail from "@/views/_generic/GenericDetail.vue";
import { paymentService } from "@/services/payment.service";

const fields = [
  { key: "transactionCode", label: "Mã giao dịch" },
  { key: "customer", label: "Khách hàng" },
  { key: "amount", label: "Số tiền" },
  { key: "method", label: "Phương thức" },
  { key: "paidAt", label: "Ngày thanh toán" },
  { key: "status", label: "Trạng thái" },
];
</script>
