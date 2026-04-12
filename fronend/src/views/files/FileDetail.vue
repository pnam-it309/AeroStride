<template>
  <GenericDetail
    title="Chi tiết file"
    :backTo="{ name: 'files.list' }"
    :redirectTo="{ name: 'files.list' }"
    editToName="files.edit"
    :service="fileService"
    :fields="fields"
  />
</template>

<script setup>
import GenericDetail from "@/views/_generic/GenericDetail.vue";
import { fileService } from "@/services/file.service";

const fields = [
  { key: "code", label: "Mã" },
  { key: "name", label: "Tên file" },
  { key: "relatedEntityType", label: "Loại liên kết" },
  { key: "relatedEntityCode", label: "Mã đối tượng" },
  { key: "relatedEntityName", label: "Tên đối tượng" },
  { key: "url", label: "URL" },
  { key: "status", label: "Trạng thái" },
  { key: "createdAt", label: "Ngày tạo" },
];
</script>
