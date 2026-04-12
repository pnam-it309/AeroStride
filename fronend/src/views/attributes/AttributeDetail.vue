<template>
  <GenericDetail
    title="Chi tiết thuộc tính"
    :backTo="{ name: 'attributes.list' }"
    :redirectTo="{ name: 'attributes.list' }"
    editToName="attributes.edit"
    :service="attributeService"
    :fields="fields"
  />
</template>

<script setup>
import GenericDetail from "@/views/_generic/GenericDetail.vue";
import { attributeService } from "@/services/attribute.service";

const fields = [
  { key: "code", label: "Mã" },
  { key: "name", label: "Tên" },
  { key: "status", label: "Trạng thái" },
  { key: "createdAt", label: "Ngày tạo" },
];
</script>
