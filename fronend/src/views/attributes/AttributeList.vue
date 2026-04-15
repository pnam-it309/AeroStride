<template>
  <div class="page">
    <PageHeader title="Thuộc tính" />

    <!-- Filter Card -->
    <AppCard title="Bộ lọc thuộc tính">
      <div class="filters">
        <AppInput
          v-model="filters.q"
          label="Tìm kiếm"
          placeholder="Tìm tên thuộc tính..."
        />
        <AppSelect
          v-model="filters.status"
          label="Trạng thái"
          :options="statusOptions"
        />
        <div class="filterActions">
          <AppButton variant="ghost" @click="resetFilters">Đặt lại</AppButton>
        </div>
      </div>
    </AppCard>

    <!-- Table with Category Tabs -->
    <AppCard>
      <!-- Category Tabs as part of card -->
      <div class="cardTabs">
        <div class="categoryRail">
          <button
            v-for="cat in categories"
            :key="cat.key"
            class="categoryCard"
            :class="{ active: activeCategory === cat.key }"
            @click="setActiveCategory(cat.key)"
          >
            <Icon :name="cat.icon" />
            <span class="cardTitle">{{ cat.label }}</span>
          </button>
        </div>
      </div>

      <template #header>
        <h4>Danh sách {{ activeCategoryLabel.toLowerCase() }}</h4>
      </template>

      <template #actions>
        <AppButton>+ Thêm mới</AppButton>
      </template>

      <AppTable :columns="columns" :rows="pagedRows">
        <template #cell:status="{ row }">
          <div class="statusCell">
            <StatusBadge
              :text="row.status === 'ACTIVE' ? 'Hoạt động' : 'Ngừng hoạt động'"
              :tone="row.status === 'ACTIVE' ? 'green' : 'orange'"
            />
          </div>
        </template>

        <template #cell:actions="{ row }">
          <div class="actionsCell">
            <button
              class="statusSwitch"
              type="button"
              :class="{ on: row.status === 'ACTIVE' }"
              :title="
                row.status === 'ACTIVE' ? 'Tắt trạng thái' : 'Bật trạng thái'
              "
              @click="toggleStatus(row)"
            >
              <span class="thumb"></span>
            </button>
            <button class="editBtn" title="Sửa">
              <Icon name="edit" />
            </button>
          </div>
        </template>
      </AppTable>

      <div class="tableFooter">
        <p class="muted">
          {{ summaryText }}
        </p>
        <AppPagination
          v-if="shouldShowPagination"
          v-model:page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-size="pageSize"
        />
      </div>
    </AppCard>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

import PageHeader from "@/components/page/PageHeader.vue";
import AppCard from "@/components/common/AppCard.vue";
import AppInput from "@/components/common/AppInput.vue";
import AppSelect from "@/components/common/AppSelect.vue";
import AppButton from "@/components/common/AppButton.vue";
import AppTable from "@/components/common/AppTable.vue";
import AppPagination from "@/components/common/AppPagination.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import Icon from "@/components/common/Icon.vue";

const categories = [
  { key: "brand", label: "Thương hiệu", icon: "tag" },
  { key: "category", label: "Danh mục", icon: "folder" },
  { key: "color", label: "Màu sắc", icon: "sliders" },
  { key: "size", label: "Kích thước", icon: "sliders" },
  { key: "material", label: "Chất liệu", icon: "sliders" },
  { key: "sole", label: "Đế giày", icon: "box" },
  { key: "collar", label: "Cổ giày", icon: "box" },
  { key: "origin", label: "Xuất xứ", icon: "tag" },
  { key: "purpose", label: "Mục đích chạy", icon: "users" },
];

const seed = {
  brand: [
    {
      id: 1,
      name: "Nike",
      description: "Dòng chạy bộ cao cấp",
      status: "ACTIVE",
    },
    {
      id: 2,
      name: "Adidas",
      description: "Phong cách thể thao trẻ",
      status: "INACTIVE",
    },
    {
      id: 3,
      name: "Puma",
      description: "Thiết kế gọn nhẹ",
      status: "INACTIVE",
    },
    {
      id: 4,
      name: "New Balance",
      description: "Êm chân cho chạy dài",
      status: "ACTIVE",
    },
    {
      id: 5,
      name: "Converse",
      description: "Phong cách cổ điển",
      status: "INACTIVE",
    },
  ],
  category: [
    {
      id: 11,
      name: "Giày chạy bộ",
      description: "Dùng cho chạy đường dài",
      status: "ACTIVE",
    },
    {
      id: 12,
      name: "Giày lifestyle",
      description: "Dùng hằng ngày",
      status: "ACTIVE",
    },
  ],
  color: [
    { id: 21, name: "Đen", description: "Màu trung tính", status: "ACTIVE" },
    { id: 22, name: "Trắng", description: "Màu sáng", status: "ACTIVE" },
  ],
  size: [
    { id: 31, name: "39", description: "Size tiêu chuẩn", status: "ACTIVE" },
    { id: 32, name: "40", description: "Size tiêu chuẩn", status: "ACTIVE" },
  ],
  material: [
    {
      id: 41,
      name: "Da tổng hợp",
      description: "Bền và dễ vệ sinh",
      status: "ACTIVE",
    },
    { id: 42, name: "Lưới mesh", description: "Thoáng khí", status: "ACTIVE" },
  ],
  sole: [
    { id: 51, name: "Cao su", description: "Bám đường tốt", status: "ACTIVE" },
  ],
  collar: [
    {
      id: 61,
      name: "Low-cut",
      description: "Cổ thấp linh hoạt",
      status: "ACTIVE",
    },
  ],
  origin: [
    {
      id: 71,
      name: "Việt Nam",
      description: "Sản xuất tại Việt Nam",
      status: "ACTIVE",
    },
    {
      id: 72,
      name: "Trung Quốc",
      description: "Gia công số lượng lớn",
      status: "INACTIVE",
    },
  ],
  purpose: [
    {
      id: 81,
      name: "Chạy bộ",
      description: "Tập luyện và thi đấu",
      status: "ACTIVE",
    },
    {
      id: 82,
      name: "Đi học/đi làm",
      description: "Mang hằng ngày",
      status: "ACTIVE",
    },
  ],
};

const activeCategory = ref("brand");
const rowsByCategory = ref(structuredClone(seed));
const filters = reactive({ q: "", status: "" });
const page = ref(1);
const pageSize = ref(5);
const route = useRoute();
const router = useRouter();

const statusOptions = [
  { value: "", label: "Tất cả trạng thái" },
  { value: "ACTIVE", label: "Hoạt động" },
  { value: "INACTIVE", label: "Ngừng hoạt động" },
];

const columns = [
  { key: "name", label: "Tên thuộc tính" },
  { key: "description", label: "Mô tả chi tiết" },
  { key: "status", label: "Trạng thái", width: "180px" },
  { key: "actions", label: "Thao tác", width: "180px" },
];

const activeCategoryLabel = computed(
  () =>
    categories.find((x) => x.key === activeCategory.value)?.label ||
    "thuộc tính",
);

const filteredRows = computed(() => {
  const q = filters.q.trim().toLowerCase();
  const rows = rowsByCategory.value[activeCategory.value] || [];

  return rows.filter((row) => {
    const okQ =
      !q ||
      row.name.toLowerCase().includes(q) ||
      row.description.toLowerCase().includes(q);
    const okStatus = !filters.status || row.status === filters.status;
    return okQ && okStatus;
  });
});

const total = computed(() => filteredRows.value.length);
const shouldShowPagination = computed(() => total.value > 5);

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

const startIndex = computed(() => {
  if (!total.value) return 0;
  return (page.value - 1) * pageSize.value + 1;
});

const endIndex = computed(() => {
  if (!total.value) return 0;
  return Math.min(page.value * pageSize.value, total.value);
});

const visibleCount = computed(() => {
  if (!total.value) return 0;
  return endIndex.value - startIndex.value + 1;
});

const summaryText = computed(() => {
  if (total.value === 1) {
    return "Đang hiển thị 1 bản ghi trong tổng 1 kết quả";
  }

  return `Đang hiển thị ${visibleCount.value} bản ghi trong tổng ${total.value} kết quả`;
});

function resetFilters() {
  filters.q = "";
  filters.status = "";
  page.value = 1;
}

function normalizeCategoryFromQuery(type) {
  const key = String(type ?? "")
    .toLowerCase()
    .trim();

  if (!key) return "brand";
  if (key === "neck") return "collar";

  return categories.some((x) => x.key === key) ? key : "brand";
}

function setActiveCategory(key) {
  activeCategory.value = key;
  page.value = 1;
  router.replace({
    name: "attributes.list",
    query: { ...route.query, type: key },
  });
}

function toggleStatus(row) {
  const next = row.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
  if (
    next === "INACTIVE" &&
    !window.confirm("Bạn có muốn thay đổi trạng thái không?")
  ) {
    return;
  }

  const list = rowsByCategory.value[activeCategory.value] || [];
  const idx = list.findIndex((x) => x.id === row.id);
  if (idx < 0) return;
  list[idx] = { ...list[idx], status: next };
}

watch(
  () => route.query.type,
  (value) => {
    activeCategory.value = normalizeCategoryFromQuery(value);
    page.value = 1;
  },
  { immediate: true },
);

watch(
  () => [filters.q, filters.status],
  () => {
    page.value = 1;
  },
);
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.filters {
  display: grid;
  grid-template-columns: 1.6fr 1fr auto;
  gap: 12px;
  align-items: end;
}

.filterActions {
  display: inline-flex;
  justify-content: flex-end;
}

/* Card Tabs */
.cardTabs {
  padding: 0 20px 14px 20px;
  border-bottom: 1px solid var(--border);
  margin: 0 -20px;
}

.categoryRail {
  display: flex;
  align-items: stretch;
  justify-content: stretch;
  gap: 0;
  margin: 0 -14px;
}

.categoryCard {
  min-width: 0;
  flex: 1 1 0;
  height: 42px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 14px;
  color: #354357;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
}

.categoryCard:hover {
  background: #f8fbff;
}

.cardTitle {
  font-size: 14px;
  font-weight: 600;
}

.categoryCard.active {
  border-bottom-color: #3f78d0;
  background: #f5f9ff;
  color: var(--primary);
}

.categoryCard :deep(.icon) {
  width: 16px;
  height: 16px;
}

.statusCell {
  display: flex;
  justify-content: center;
}

.actionsCell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.tableFooter {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}

.muted {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

.statusSwitch {
  width: 42px;
  height: 24px;
  border-radius: 999px;
  border: 1px solid #d7dfec;
  background: #eef2f7;
  position: relative;
  padding: 0;
  cursor: pointer;
}

.statusSwitch .thumb {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  position: absolute;
  top: 2px;
  left: 2px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.16);
  transition: left 0.2s ease;
}

.statusSwitch.on {
  background: #dce8ff;
  border-color: #bfd3ff;
}

.statusSwitch.on .thumb {
  left: 21px;
  background: #2f5fb8;
}

.editBtn {
  width: 28px;
  height: 28px;
  border: 0;
  background: transparent;
  color: var(--primary);
  display: inline-grid;
  place-items: center;
}

@media (max-width: 1200px) {
  .categoryRail {
    overflow-x: auto;
  }

  .categoryCard {
    min-width: 128px;
    flex: 0 0 auto;
  }
}

@media (max-width: 900px) {
  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
