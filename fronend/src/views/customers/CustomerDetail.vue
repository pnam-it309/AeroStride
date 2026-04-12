<template>
  <GenericDetail
    title="Chi tiết khách hàng"
    :backTo="{ name: 'customers.list' }"
    :redirectTo="{ name: 'customers.list' }"
    editToName="customers.edit"
    :service="customerService"
    :fields="fields"
  />
</template>

<script setup>
import GenericDetail from "@/views/_generic/GenericDetail.vue";
import { customerService } from "@/services/customer.service";

const fields = [
  { key: "ma", label: "Mã khách hàng" },
  { key: "ten", label: "Tên khách hàng" },
  { key: "email", label: "Email" },
  { key: "tenTaiKhoan", label: "Tên tài khoản" },
  { key: "gioiTinh", label: "Giới tính" },
  { key: "sdt", label: "SĐT" },
  { key: "ngaySinh", label: "Ngày sinh" },
  { key: "hinhAnh", label: "Hình ảnh" },
  { key: "ghiChu", label: "Ghi chú" },
  { key: "trangThai", label: "Trạng thái" },
  { key: "tinh", label: "Tỉnh" },
  { key: "thanhPho", label: "Thành phố" },
  { key: "phuongXa", label: "Phường/Xã" },
  { key: "diaChiChiTiet", label: "Địa chỉ chi tiết" },
  { key: "tenNguoiNhan", label: "Tên người nhận" },
  { key: "sdtNguoiNhan", label: "SĐT người nhận" },
  { key: "ngayTao", label: "Ngày tạo" },
  { key: "ngayCapNhat", label: "Ngày cập nhật" },
];
</script>
