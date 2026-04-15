<template>
  <GenericEdit
    title="Sửa khách hàng"
    :backTo="{ name: 'customers.list' }"
    :redirectTo="{ name: 'customers.list' }"
    :service="customerService"
    :fields="fields"
  />
</template>

<script setup>
import GenericEdit from "@/views/_generic/GenericEdit.vue";
import { customerService } from "@/services/customer.service";

const fields = [
  { key: "ma", label: "Mã khách hàng" },
  { key: "ten", label: "Tên khách hàng" },
  { key: "email", label: "Email", type: "email" },
  { key: "tenTaiKhoan", label: "Tên tài khoản" },
  { key: "gioiTinh", label: "Giới tính" },
  { key: "sdt", label: "SĐT", type: "tel" },
  { key: "ngaySinh", label: "Ngày sinh", type: "date" },
  { key: "hinhAnh", label: "Hình ảnh" },
  { key: "ghiChu", label: "Ghi chú" },
  { key: "trangThai", label: "Trạng thái" },
  { key: "tinh", label: "Tỉnh" },
  { key: "thanhPho", label: "Thành phố" },
  { key: "phuongXa", label: "Phường/Xã" },
  { key: "diaChiChiTiet", label: "Địa chỉ chi tiết" },
  { key: "tenNguoiNhan", label: "Tên người nhận" },
  { key: "sdtNguoiNhan", label: "SĐT người nhận", type: "tel" },
  { key: "ngayTao", label: "Ngày tạo", type: "number" },
  { key: "ngayCapNhat", label: "Ngày cập nhật", type: "number" },
];
</script>
