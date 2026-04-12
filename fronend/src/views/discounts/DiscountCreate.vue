<template>
  <GenericCreate
    title="Tạo đợt giảm giá"
    :backTo="{ name: 'discounts.list' }"
    :redirectTo="{ name: 'discounts.list' }"
    :service="discountService"
    :fields="fields"
    :defaults="defaults"
  />
</template>

<script setup>
import GenericCreate from "@/views/_generic/GenericCreate.vue";
import { discountService } from "@/services/discount.service";

const fields = [
  { key: "ma", label: "Mã", placeholder: "DG-001" },
  { key: "ten", label: "Tên", placeholder: "Đợt giảm giá..." },
  { key: "loaiGiamGia", label: "Loại giảm giá", placeholder: "PERCENT" },
  {
    key: "soTienGiam",
    label: "Số tiền giảm",
    placeholder: "150000",
    type: "number",
  },
  {
    key: "dieuKienGiamGia",
    label: "Điều kiện giảm giá",
    placeholder: "1000000",
    type: "number",
  },
  {
    key: "ngayBatDau",
    label: "Ngày bắt đầu",
    placeholder: "1772323200000",
    type: "number",
  },
  {
    key: "ngayKetThuc",
    label: "Ngày kết thúc",
    placeholder: "1798675200000",
    type: "number",
  },
  { key: "mucUuTien", label: "Mức ưu tiên", placeholder: "1", type: "number" },
];
const defaults = {
  ma: "",
  ten: "",
  loaiGiamGia: "PERCENT",
  soTienGiam: 0,
  dieuKienGiamGia: 0,
  ngayBatDau: Date.now(),
  ngayKetThuc: Date.now(),
  mucUuTien: 1,
};
</script>
