<script setup>
import AttributeManagementPage from './components/AttributeManagementPage.vue'
import { ATTRIBUTE_PAGE_CONFIGS } from './attributeConfigs'
</script>

<template>
  <AttributeManagementPage v-bind="ATTRIBUTE_PAGE_CONFIGS.MAU_SAC" />
</template>
