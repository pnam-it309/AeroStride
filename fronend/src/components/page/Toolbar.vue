<template>
  <div class="bar">
    <div class="left"><slot name="left" /></div>
    <div class="right"><slot name="right" /></div>
  </div>
</template>

<script setup></script>

<style scoped>
.bar {
  padding-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.right {
  display: flex;
  gap: 10px;
}
</style>
