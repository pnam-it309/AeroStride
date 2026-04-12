<template>
  <GenericList
    title="Quản lý file"
    listTitle="Danh sách file"
    searchPlaceholder="Tìm theo tên file"
    :columns="columns"
    :routes="{
      createName: 'files.create',
      detailName: 'files.detail',
      editName: 'files.edit',
    }"
    :service="fileService"
    :idKey="'id'"
    :canDelete="false"
    :enableStatusSwitch="true"
  />
</template>

<script setup>
import GenericList from "@/views/_generic/GenericList.vue";
import { fileService } from "@/services/file.service";

const columns = [
  { key: "stt", label: "STT", width: "70px" },
  { key: "code", label: "Mã", width: "120px" },
  { key: "name", label: "Tên file" },
  { key: "relatedEntityType", label: "Loại liên kết", width: "130px" },
  { key: "relatedEntityName", label: "Đối tượng liên kết", width: "190px" },
  { key: "url", label: "URL" },
  { key: "status", label: "Trạng thái", width: "140px" },
  { key: "createdAt", label: "Ngày tạo", width: "140px" },
  { key: "actions", label: "Hành động", width: "160px" },
];
</script>
