<template>
  <GenericEdit
    title="Sửa đợt giảm giá"
    :backTo="{ name: 'discounts.list' }"
    :redirectTo="{ name: 'discounts.list' }"
    :service="discountService"
    :fields="fields"
  />
</template>

<script setup>
import GenericEdit from "@/views/_generic/GenericEdit.vue";
import { discountService } from "@/services/discount.service";

const fields = [
  { key: "ma", label: "Mã", type: "text" },
  { key: "ten", label: "Tên", type: "text" },
  { key: "loaiGiamGia", label: "Loại giảm giá", type: "text" },
  { key: "soTienGiam", label: "Số tiền giảm", type: "number" },
  { key: "dieuKienGiamGia", label: "Điều kiện giảm giá", type: "number" },
  { key: "ngayBatDau", label: "Ngày bắt đầu", type: "number" },
  { key: "ngayKetThuc", label: "Ngày kết thúc", type: "number" },
  { key: "mucUuTien", label: "Mức ưu tiên", type: "number" },
];
</script>
