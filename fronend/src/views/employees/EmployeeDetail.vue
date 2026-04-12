<template>
  <GenericDetail
    title="Chi tiết nhân viên"
    :backTo="{ name: 'employees.list' }"
    :redirectTo="{ name: 'employees.list' }"
    editToName="employees.edit"
    :service="employeeService"
    :fields="fields"
  />
</template>

<script setup>
import GenericDetail from "@/views/_generic/GenericDetail.vue";
import { employeeService } from "@/services/employee.service";

const fields = [
  { key: "ma", label: "Mã nhân viên" },
  { key: "ten", label: "Tên nhân viên" },
  { key: "email", label: "Email" },
  { key: "tenTaiKhoan", label: "Tên tài khoản" },
  { key: "gioiTinh", label: "Giới tính" },
  { key: "sdt", label: "SĐT" },
  { key: "ngaySinh", label: "Ngày sinh" },
  { key: "hinhAnh", label: "Hình ảnh" },
  { key: "trangThai", label: "Trạng thái" },
  { key: "maPhanQuyen", label: "Mã phân quyền" },
  { key: "tenPhanQuyen", label: "Tên phân quyền / chức vụ" },
  { key: "quyenHan", label: "Quyền hạn" },
  { key: "ngayTao", label: "Ngày tạo" },
  { key: "ngayCapNhat", label: "Ngày cập nhật" },
];
</script>
