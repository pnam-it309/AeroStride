<template>
  <GenericCreate
    title="Tạo nhân viên"
    :backTo="{ name: 'employees.list' }"
    :redirectTo="{ name: 'employees.list' }"
    :service="employeeService"
    :fields="fields"
    :defaults="defaults"
  />
</template>

<script setup>
import GenericCreate from "@/views/_generic/GenericCreate.vue";
import { employeeService } from "@/services/employee.service";

const fields = [
  { key: "ma", label: "Mã nhân viên", placeholder: "NV-002" },
  { key: "ten", label: "Tên nhân viên", placeholder: "Trần Văn D" },
  {
    key: "email",
    label: "Email",
    placeholder: "nhanvien@gmail.com",
    type: "email",
  },
  { key: "tenTaiKhoan", label: "Tên tài khoản", placeholder: "tranvand" },
  { key: "gioiTinh", label: "Giới tính", placeholder: "Nam/Nữ" },
  { key: "sdt", label: "SĐT", placeholder: "09xxxxxxxx", type: "tel" },
  { key: "ngaySinh", label: "Ngày sinh", type: "date" },
  { key: "hinhAnh", label: "Hình ảnh", placeholder: "https://..." },
  { key: "trangThai", label: "Trạng thái", placeholder: "ACTIVE" },
  { key: "maPhanQuyen", label: "Mã phân quyền", placeholder: "PQ-01" },
  {
    key: "tenPhanQuyen",
    label: "Tên phân quyền / chức vụ",
    placeholder: "Quản lý",
  },
  { key: "quyenHan", label: "Quyền hạn", placeholder: "ADMIN" },
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
  trangThai: "ACTIVE",
  maPhanQuyen: "",
  tenPhanQuyen: "",
  quyenHan: "",
  ngayTao: Date.now(),
  ngayCapNhat: Date.now(),
};
</script>
