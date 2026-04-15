<template>
  <GenericEdit
    title="Sửa nhân viên"
    :backTo="{ name: 'employees.list' }"
    :redirectTo="{ name: 'employees.list' }"
    :service="employeeService"
    :fields="fields"
  />
</template>

<script setup>
import GenericEdit from "@/views/_generic/GenericEdit.vue";
import { employeeService } from "@/services/employee.service";

const fields = [
  { key: "ma", label: "Mã nhân viên" },
  { key: "ten", label: "Tên nhân viên" },
  { key: "email", label: "Email", type: "email" },
  { key: "tenTaiKhoan", label: "Tên tài khoản" },
  { key: "gioiTinh", label: "Giới tính" },
  { key: "sdt", label: "SĐT", type: "tel" },
  { key: "ngaySinh", label: "Ngày sinh", type: "date" },
  { key: "hinhAnh", label: "Hình ảnh" },
  { key: "trangThai", label: "Trạng thái" },
  { key: "maPhanQuyen", label: "Mã phân quyền" },
  { key: "tenPhanQuyen", label: "Tên phân quyền / chức vụ" },
  { key: "quyenHan", label: "Quyền hạn" },
  { key: "ngayTao", label: "Ngày tạo", type: "number" },
  { key: "ngayCapNhat", label: "Ngày cập nhật", type: "number" },
];
</script>
