<template>
  <GenericCreate
    title="Thêm file"
    :backTo="{ name: 'files.list' }"
    :redirectTo="{ name: 'files.list' }"
    :service="fileService"
    :fields="fields"
    :defaults="defaults"
  />
</template>

<script setup>
import GenericCreate from "@/views/_generic/GenericCreate.vue";
import { fileService } from "@/services/file.service";

const fields = [
  { key: "code", label: "Mã", placeholder: "F002" },
  { key: "name", label: "Tên file", placeholder: "shoe-2.png" },
  { key: "url", label: "URL", placeholder: "/uploads/shoe-2.png" },
  { key: "status", label: "Trạng thái", placeholder: "ACTIVE" },
  { key: "createdAt", label: "Ngày tạo", placeholder: "2026-04-11" },
];
const defaults = {
  code: "",
  name: "",
  url: "",
  status: "ACTIVE",
  createdAt: new Date().toISOString().slice(0, 10),
};
</script>
