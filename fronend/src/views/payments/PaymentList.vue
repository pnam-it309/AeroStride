<template>
  <GenericList
    title="Thanh toán"
    listTitle="Danh sách thanh toán"
    searchPlaceholder="Tìm theo mã giao dịch / khách hàng"
    :columns="columns"
    :routes="{
      createName: 'payments.create',
      detailName: 'payments.detail',
      editName: 'payments.edit',
    }"
    :service="paymentService"
    :idKey="'id'"
    :canCreate="false"
    :canEdit="false"
    :canDelete="false"
  />
</template>

<script setup>
import GenericList from "@/views/_generic/GenericList.vue";
import { paymentService } from "@/services/payment.service";

const columns = [
  { key: "stt", label: "STT", width: "70px" },
  { key: "transactionCode", label: "Mã giao dịch", width: "170px" },
  { key: "customer", label: "Khách hàng", width: "170px" },
  { key: "amount", label: "Số tiền", width: "130px" },
  { key: "method", label: "Phương thức", width: "140px" },
  { key: "paidAt", label: "Ngày thanh toán", width: "140px" },
  { key: "status", label: "Trạng thái", width: "140px" },
  { key: "actions", label: "Hành động", width: "160px" },
];
</script>
