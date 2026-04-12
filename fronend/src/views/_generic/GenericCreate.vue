<template>
  <div class="page">
    <PageHeader :title="title" :backTo="backTo" />

    <AppCard :title="cardTitle">
      <div class="form">
        <AppInput
          v-for="f in fields"
          :key="f.key"
          :label="f.label"
          :type="f.type || 'text'"
          :placeholder="f.placeholder"
          v-model="form[f.key]"
        />
      </div>

      <Toolbar>
        <template #right>
          <AppButton variant="ghost" @click="cancel">Hủy</AppButton>
          <AppButton @click="save">Lưu</AppButton>
        </template>
      </Toolbar>
    </AppCard>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import { useRouter } from "vue-router";

import PageHeader from "@/components/page/PageHeader.vue";
import Toolbar from "@/components/page/Toolbar.vue";
import AppCard from "@/components/common/AppCard.vue";
import AppInput from "@/components/common/AppInput.vue";
import AppButton from "@/components/common/AppButton.vue";

const props = defineProps({
  title: { type: String, required: true },
  cardTitle: { type: String, default: "Thông tin" },
  backTo: { type: Object, required: true },
  service: { type: Object, required: true }, // create(payload)
  redirectTo: { type: Object, required: true },
  fields: { type: Array, default: () => [] }, // [{key,label,placeholder}]
  defaults: { type: Object, default: () => ({}) },
});

const router = useRouter();

const form = reactive({ ...props.defaults });

function cancel() {
  router.push(props.redirectTo);
}
async function save() {
  await props.service.create(structuredClone(form));
  router.push(props.redirectTo);
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.form {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
</style>
