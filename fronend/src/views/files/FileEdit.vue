<template>
  <GenericEdit
    title="Sửa file"
    :backTo="{ name: 'files.list' }"
    :redirectTo="{ name: 'files.list' }"
    :service="fileService"
    :fields="fields"
  />
</template>

<script setup>
import GenericEdit from "@/views/_generic/GenericEdit.vue";
import { fileService } from "@/services/file.service";

const fields = [
  { key: "code", label: "Mã" },
  { key: "name", label: "Tên file" },
  { key: "url", label: "URL" },
  { key: "status", label: "Trạng thái" },
  { key: "createdAt", label: "Ngày tạo" },
];
</script>
