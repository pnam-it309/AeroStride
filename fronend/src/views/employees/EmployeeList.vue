<template>
  <GenericList
    title="Nhân viên"
    listTitle="Danh sách nhân viên"
    searchPlaceholder="Tìm theo mã / tên / email / chức vụ"
    :columns="columns"
    :routes="{
      createName: 'employees.create',
      detailName: 'employees.detail',
      editName: 'employees.edit',
    }"
    :service="employeeService"
    :idKey="'id'"
    :canDelete="false"
    :enableStatusSwitch="true"
  >
    <template #cell:gioiTinhText="{ row }">
      <div class="genderCell">
        <span class="genderBadge" :class="genderTone(row.gioiTinhText)">
          {{ row.gioiTinhText || "-" }}
        </span>
      </div>
    </template>

    <template #cell:contact="{ row }">
      <div class="contactCell">
        <span class="contactPhone">{{ row.sdt || "-" }}</span>
        <span class="contactEmail">{{ row.email || "-" }}</span>
      </div>
    </template>
  </GenericList>
</template>

<script setup>
import GenericList from "@/views/_generic/GenericList.vue";
import { employeeService } from "@/services/employee.service";

const columns = [
  { key: "stt", label: "STT", width: "70px" },
  { key: "ma", label: "Mã nhân viên", width: "120px" },
  { key: "ten", label: "Tên nhân viên", width: "180px" },
  { key: "tenTaiKhoan", label: "Tên tài khoản", width: "150px" },
  { key: "gioiTinhText", label: "Giới tính", width: "120px" },
  { key: "contact", label: "Liên hệ", width: "190px" },
  { key: "chucVuText", label: "Chức vụ", width: "150px" },
  { key: "status", label: "Trạng thái", width: "140px" },
  { key: "createdAt", label: "Ngày tạo", width: "140px" },
  { key: "actions", label: "Hành động", width: "160px" },
];

function genderTone(value) {
  const key = String(value ?? "")
    .trim()
    .toLowerCase();
  if (key === "nam") return "male";
  if (key === "nữ" || key === "nu") return "female";
  return "gray";
}
</script>

<style scoped>
.contactCell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
  line-height: 1.2;
  white-space: normal;
}

.contactPhone {
  color: #1f2f46;
  font-weight: 600;
  font-size: 14px;
}

.contactEmail {
  color: #5f6c80;
  font-size: 12.5px;
  font-weight: 500;
}

.genderCell {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.genderBadge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.genderBadge.male {
  color: #1f6fd6;
  background: #edf4ff;
  border-color: #d5e5ff;
}

.genderBadge.female {
  color: #d97706;
  background: #fff3e3;
  border-color: #ffe0bd;
}

.genderBadge.gray {
  color: #738095;
  background: #f8fafc;
  border-color: #e8edf4;
}
</style>
