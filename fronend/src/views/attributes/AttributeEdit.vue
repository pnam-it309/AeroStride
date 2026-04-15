<template>
  <GenericEdit
    title="Sửa thuộc tính"
    :backTo="{ name: 'attributes.list' }"
    :redirectTo="{ name: 'attributes.list' }"
    :service="attributeService"
    :fields="fields"
  />
</template>

<script setup>
import GenericEdit from "@/views/_generic/GenericEdit.vue";
import { attributeService } from "@/services/attribute.service";

const fields = [
  { key: "code", label: "Mã" },
  { key: "name", label: "Tên" },
  { key: "status", label: "Trạng thái" },
  { key: "createdAt", label: "Ngày tạo" },
];
</script>
