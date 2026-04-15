<template>
  <div class="page">
    <PageHeader :title="title" :backTo="backTo" />

    <AppCard :title="cardTitle">
      <div v-if="loaded" class="form">
        <AppInput
          v-for="f in fields"
          :key="f.key"
          :label="f.label"
          :type="f.type || 'text'"
          v-model="form[f.key]"
        />
      </div>
      <div v-else class="muted">Đang tải…</div>

      <Toolbar>
        <template #right>
          <AppButton variant="ghost" @click="cancel">Hủy</AppButton>
          <AppButton @click="save">Cập nhật</AppButton>
        </template>
      </Toolbar>
    </AppCard>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import PageHeader from "@/components/page/PageHeader.vue";
import Toolbar from "@/components/page/Toolbar.vue";
import AppCard from "@/components/common/AppCard.vue";
import AppInput from "@/components/common/AppInput.vue";
import AppButton from "@/components/common/AppButton.vue";

const props = defineProps({
  title: { type: String, required: true },
  cardTitle: { type: String, default: "Thông tin" },
  backTo: { type: Object, required: true },
  service: { type: Object, required: true }, // get(id), update(id,payload)
  redirectTo: { type: Object, required: true },
  fields: { type: Array, default: () => [] },
});

const route = useRoute();
const router = useRouter();
const loaded = ref(false);
const form = reactive({});

onMounted(async () => {
  const data = await props.service.get(route.params.id);
  Object.assign(form, data);
  loaded.value = true;
});

function cancel() {
  router.push(props.redirectTo);
}
async function save() {
  await props.service.update(route.params.id, structuredClone(form));
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
