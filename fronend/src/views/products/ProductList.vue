<template>
  <GenericList
    title="Sản phẩm"
    listTitle="Danh sách sản phẩm"
    searchPlaceholder="Tìm theo mã sản phẩm / tên / thương hiệu"
    :columns="columns"
    :routes="{
      createName: 'products.create',
      detailName: 'products.detail',
      editName: 'products.edit',
    }"
    :service="productService"
    :idKey="'id'"
    :canDelete="false"
    :enableStatusSwitch="true"
  />
</template>

<script setup>
import GenericList from "@/views/_generic/GenericList.vue";
import { productService } from "@/services/product.service";

const columns = [
  { key: "stt", label: "STT", width: "120px" },
  { key: "sku", label: "Mã sản phẩm", width: "140px" },
  { key: "name", label: "Tên sản phẩm", width: "140px" },
  { key: "brandName", label: "Tên thương hiệu", width: "140px" },
  { key: "categoryName", label: "Danh mục", width: "140px" },
  { key: "qty", label: "Số lượng", width: "140px" },
  { key: "price", label: "Giá", width: "140px" },
  { key: "status", label: "Trạng thái", width: "140px" },
  { key: "createdAt", label: "Ngày tạo", width: "140px" },
  { key: "actions", label: "Hành động", width: "140px" },
];
</script>
