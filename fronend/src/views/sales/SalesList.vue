<template>
  <GenericList
    title="Bán hàng"
    listTitle="Danh sách phiên bán hàng"
    searchPlaceholder="Tìm theo mã / ghi chú"
    :columns="columns"
    :routes="{
      createName: 'sales.create',
      detailName: 'sales.detail',
      editName: 'sales.edit',
    }"
    :service="salesService"
    :idKey="'id'"
    :canDelete="false"
    :enableStatusSwitch="true"
  />
</template>

<script setup>
import GenericList from "@/views/_generic/GenericList.vue";
import { salesService } from "@/services/sales.service";

const columns = [
  { key: "stt", label: "STT", width: "70px" },
  { key: "code", label: "Mã", width: "120px" },
  { key: "name", label: "Tên" },
  { key: "status", label: "Trạng thái", width: "140px" },
  { key: "createdAt", label: "Ngày tạo", width: "140px" },
  { key: "actions", label: "Hành động", width: "160px" },
];
</script>
