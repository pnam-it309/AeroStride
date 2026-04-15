<template>
  <GenericDetail
    title="Chi tiết đợt giảm giá"
    :backTo="{ name: 'discounts.list' }"
    :redirectTo="{ name: 'discounts.list' }"
    editToName="discounts.edit"
    :service="discountService"
    :fields="fields"
  />
</template>

<script setup>
import GenericDetail from "@/views/_generic/GenericDetail.vue";
import { discountService } from "@/services/discount.service";

const fields = [
  { key: "ma", label: "Mã" },
  { key: "ten", label: "Tên" },
  { key: "loaiGiamGia", label: "Loại giảm giá" },
  { key: "soTienGiam", label: "Số tiền giảm" },
  { key: "dieuKienGiamGia", label: "Điều kiện giảm giá" },
  { key: "ngayBatDau", label: "Ngày bắt đầu" },
  { key: "ngayKetThuc", label: "Ngày kết thúc" },
  { key: "mucUuTien", label: "Mức ưu tiên" },
];
</script>
