<script setup>
const props = defineProps({ item: Object, level: Number });
</script>

<template>
  <component
    :is="item"
    size="20"
    stroke-width="1.5"
    class="iconClass"
  ></component>
</template>
