<script setup>
import AttributeManagementPage from './components/AttributeManagementPage.vue'
import { ATTRIBUTE_PAGE_CONFIGS } from './attributeConfigs'
</script>

<template>
  <AttributeManagementPage v-bind="ATTRIBUTE_PAGE_CONFIGS.DE_GIAY" />
</template>
