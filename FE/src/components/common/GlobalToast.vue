<template>
  <v-snackbar
    v-model="toast.show"
    :color="toast.color"
    :timeout="toast.timeout"
    location="top right"
    class="premium-toast"
    elevation="24"
  >
    <div class="d-flex align-center">
      <v-icon :icon="toast.icon" class="mr-3 text-white" size="24"></v-icon>
      <span class="text-white font-weight-bold">{{ toast.message }}</span>
    </div>

    <template v-slot:actions>
      <v-btn
        variant="text"
        icon="mdi-close"
        color="white"
        @click="toast.show = false"
      ></v-btn>
    </template>
  </v-snackbar>
</template>

<script setup>
import { useToastStore } from "@/stores/toastStore";
import { watch } from "vue";

const toast = useToastStore();

// Reset show state if a new message arrives while one is already showing
watch(() => toast.message, () => {
  if (toast.show) {
    toast.show = false;
    setTimeout(() => {
      toast.show = true;
    }, 100);
  }
});
</script>

<style scoped>
.premium-toast :deep(.v-snackbar__wrapper) {
  border-radius: 12px !important;
  padding: 8px 16px !important;
}

/* Ensure text is always white even if color changes */
.text-white {
  color: white !important;
}
</style>
