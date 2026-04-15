<template>
  <GenericCreate
    title="Tạo khách hàng"
    :backTo="{ name: 'customers.list' }"
    :redirectTo="{ name: 'customers.list' }"
    :service="customerService"
    :fields="fields"
    :defaults="defaults"
  />
</template>

<script setup>
import GenericCreate from "@/views/_generic/GenericCreate.vue";
import { customerService } from "@/services/customer.service";

const fields = [
  { key: "ma", label: "Mã khách hàng", placeholder: "KH-002" },
  { key: "ten", label: "Tên khách hàng", placeholder: "Nguyễn Văn C" },
  {
    key: "email",
    label: "Email",
    placeholder: "khachhang@email.com",
    type: "email",
  },
  { key: "tenTaiKhoan", label: "Tên tài khoản", placeholder: "khachhang02" },
  {
    key: "gioiTinh",
    label: "Giới tính",
    placeholder: "Nam/Nữ hoặc true/false",
  },
  { key: "sdt", label: "SĐT", placeholder: "09xxxxxxxx", type: "tel" },
  { key: "ngaySinh", label: "Ngày sinh", type: "date" },
  { key: "hinhAnh", label: "Hình ảnh", placeholder: "https://..." },
  { key: "ghiChu", label: "Ghi chú", placeholder: "Khách mua online/offline" },
  { key: "trangThai", label: "Trạng thái", placeholder: "ACTIVE" },
  { key: "tinh", label: "Tỉnh", placeholder: "Hà Nội" },
  { key: "thanhPho", label: "Thành phố", placeholder: "Nam Từ Liêm" },
  { key: "phuongXa", label: "Phường/Xã", placeholder: "Mỹ Đình 1" },
  {
    key: "diaChiChiTiet",
    label: "Địa chỉ chi tiết",
    placeholder: "Số nhà, tên đường...",
  },
  { key: "tenNguoiNhan", label: "Tên người nhận", placeholder: "Nguyễn Văn C" },
  {
    key: "sdtNguoiNhan",
    label: "SĐT người nhận",
    placeholder: "09xxxxxxxx",
    type: "tel",
  },
];
const defaults = {
  ma: "",
  ten: "",
  email: "",
  tenTaiKhoan: "",
  gioiTinh: "",
  sdt: "",
  ngaySinh: "",
  hinhAnh: "",
  ghiChu: "",
  trangThai: "ACTIVE",
  ngayTao: Date.now(),
  ngayCapNhat: Date.now(),
  tinh: "",
  thanhPho: "",
  phuongXa: "",
  diaChiChiTiet: "",
  tenNguoiNhan: "",
  sdtNguoiNhan: "",
};
</script>
