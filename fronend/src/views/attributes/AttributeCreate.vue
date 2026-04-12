<template>
  <div class="page">
    <PageHeader title="Thêm thuộc tính" :backTo="{ name: 'attributes.list' }" />

    <AppCard title="Thông tin thuộc tính">
      <div class="formGrid">
        <AppSelect
          label="Loại thuộc tính"
          v-model="form.target"
          :options="targetOptions"
        />
        <AppInput
          label="Mã thuộc tính *"
          v-model="form.ma"
          placeholder="VD: MS-005"
        />
        <AppInput
          label="Tên thuộc tính *"
          v-model="form.ten"
          placeholder="VD: Xanh navy"
        />
      </div>

      <p v-if="errorMessage" class="errorText">{{ errorMessage }}</p>
      <p v-if="successMessage" class="successText">{{ successMessage }}</p>

      <div class="actions">
        <AppButton variant="ghost" @click="goBack">Hủy</AppButton>
        <AppButton @click="handleCreate">Lưu thuộc tính</AppButton>
      </div>
    </AppCard>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import PageHeader from "@/components/page/PageHeader.vue";
import AppCard from "@/components/common/AppCard.vue";
import AppInput from "@/components/common/AppInput.vue";
import AppSelect from "@/components/common/AppSelect.vue";
import AppButton from "@/components/common/AppButton.vue";
import { db } from "@/services/mock.db";

const router = useRouter();

const tableMap = {
  category: "categories",
  brand: "brands",
  origin: "origins",
  purpose: "purposes",
  shoePart: "shoeParts",
  material: "materials",
  soleCost: "soleCosts",
  size: "attributes",
  color: "colors",
};

const targetOptions = [
  { value: "category", label: "Danh mục" },
  { value: "brand", label: "Thương hiệu" },
  { value: "origin", label: "Xuất xứ" },
  { value: "purpose", label: "Mục đích chạy" },
  { value: "shoePart", label: "Cổ giày" },
  { value: "material", label: "Chất liệu" },
  { value: "soleCost", label: "Đế giày" },
  { value: "size", label: "Kích thước" },
  { value: "color", label: "Màu sắc" },
];

const form = ref({
  target: "category",
  ma: "",
  ten: "",
});

const errorMessage = ref("");
const successMessage = ref("");

function normalize(v) {
  return String(v || "").trim();
}

function goBack() {
  router.push({ name: "attributes.list" });
}

function handleCreate() {
  errorMessage.value = "";
  successMessage.value = "";

  const ma = normalize(form.value.ma);
  const ten = normalize(form.value.ten);

  if (!ma || !ten) {
    errorMessage.value = "Vui lòng nhập đầy đủ mã và tên thuộc tính.";
    return;
  }

  const key = tableMap[form.value.target];
  if (!key) {
    errorMessage.value = "Loại thuộc tính không hợp lệ.";
    return;
  }

  if (!Array.isArray(db[key])) {
    db[key] = [];
  }

  const exists = db[key].some(
    (item) =>
      String(item.id || item.code || "").toUpperCase() === ma.toUpperCase() ||
      String(item.name || item.ten || "").toUpperCase() === ten.toUpperCase(),
  );

  if (exists) {
    errorMessage.value =
      "Mã hoặc tên đã tồn tại trong bảng thuộc tính đã chọn.";
    return;
  }

  const payload = {
    id: ma,
    code: ma,
    name: ten,
    ten,
    status: "ACTIVE",
    trangThai: "ACTIVE",
    createdAt: new Date().toISOString().slice(0, 10),
  };

  db[key].unshift(payload);

  successMessage.value = "Thêm thuộc tính thành công.";
  form.value.ma = "";
  form.value.ten = "";
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.formGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.errorText {
  margin: 12px 0 0;
  color: #dc2626;
  font-size: 13px;
  font-weight: 600;
}

.successText {
  margin: 12px 0 0;
  color: #15803d;
  font-size: 13px;
  font-weight: 600;
}

.actions {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 720px) {
  .formGrid {
    grid-template-columns: 1fr;
  }
}
</style>
