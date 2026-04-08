<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  dichVuThuongHieu, dichVuDanhMuc, dichVuMauSac, dichVuKichThuoc,
  dichVuChatLieu, dichVuDeGiay, dichVuCoGiay, dichVuXuatXu, dichVuMucDichChay
} from '@/services/product/dichVuThuocTinh';
import { useNotifications } from '@/services/notificationService';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { EditIcon, SettingsIcon } from 'vue-tabler-icons';

const { addNotification } = useNotifications();

const loading = ref(false);
const isRefreshing = ref(false);
const selectedTab = ref('brands');
const searchQuery = ref('');
const statusFilter = ref(null);

const pagination = ref({
  page: 1,
  size: 10,
  totalElements: 0,
  totalPages: 1
});

const showDialog = ref(false);
const selectedItem = ref(null);
const isEditMode = ref(false);

const route = useRoute();
const router = useRouter();

// Confirmation Logic
const confirmDialog = ref({
  show: false, title: '', message: '', color: 'primary', action: null, loading: false
});

const routeMap = {
  'thuong-hieu': 'brands', 'danh-muc': 'categories', 'xuat-xu': 'origins',
  'muc-dich-chay': 'purposes', 'chat-lieu': 'materials', 'de-giay': 'soles',
  'co-giay': 'collars', 'mau-sac': 'colors', 'kich-thuoc': 'sizes'
};

const reverseRouteMap = Object.entries(routeMap).reduce((acc, [k, v]) => { acc[v] = k; return acc; }, {});

const brands = ref([]); const categories = ref([]); const colors = ref([]);
const sizes = ref([]); const materials = ref([]); const soles = ref([]);
const collars = ref([]); const origins = ref([]); const purposes = ref([]);

const itemForm = ref({ ten: '', moTa: '', trangThai: 0 });

const tabs = [
  { value: 'brands', title: 'Thương hiệu', icon: 'mdi-tag' },
  { value: 'categories', title: 'Danh mục', icon: 'mdi-folder' },
  { value: 'colors', title: 'Màu sắc', icon: 'mdi-palette' },
  { value: 'sizes', title: 'Kích thước', icon: 'mdi-ruler' },
  { value: 'materials', title: 'Chất liệu', icon: 'mdi-texture' },
  { value: 'soles', title: 'Đế giày', icon: 'mdi-shoe-sole' },
  { value: 'collars', title: 'Cổ giày', icon: 'mdi-shoe-formal' },
  { value: 'origins', title: 'Xuất xứ', icon: 'mdi-map-marker' },
  { value: 'purposes', title: 'Mục đích', icon: 'mdi-run' }
];

const services = {
  brands: dichVuThuongHieu, categories: dichVuDanhMuc, colors: dichVuMauSac, sizes: dichVuKichThuoc,
  materials: dichVuChatLieu, soles: dichVuDeGiay, collars: dichVuCoGiay, origins: dichVuXuatXu, purposes: dichVuMucDichChay
};

const dataRefs = { brands, categories, colors, sizes, materials, soles, collars, origins, purposes };

const tableHeaders = [
  { text: 'Tên thuộc tính', align: 'center', width: '150px' },
  { text: 'Mô tả chi tiết', align: 'center', width: '150px' },
  { text: 'Trạng thái', align: 'center', width: '180px' },
  { text: 'Thao tác', align: 'center', width: '150px' }
];

const loadItems = async () => {
  loading.value = true;
  try {
    const service = services[selectedTab.value];
    let response;
    const params = {
      page: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
      size: pagination.value.size,
      keyword: searchQuery.value || null,
      trangThai: statusFilter.value !== null ? statusFilter.value : null
    };

    switch (selectedTab.value) {
      case 'brands': response = await service.layThuongHieu(params); break;
      case 'categories': response = await service.layDanhMuc(params); break;
      case 'colors': response = await service.layMauSac(params); break;
      case 'sizes': response = await service.layKichThuoc(params); break;
      case 'materials': response = await service.layChatLieu(params); break;
      case 'soles': response = await service.layDeGiay(params); break;
      case 'collars': response = await service.layCoGiay(params); break;
      case 'origins': response = await service.layXuatXu(params); break;
      case 'purposes': response = await service.layMucDichChay(params); break;
    }

    if (response) {
      dataRefs[selectedTab.value].value = response.content || [];
      pagination.value.totalPages = response.totalPages || 1;
      pagination.value.totalElements = response.totalElements || 0;
    }
  } catch (error) { console.error(error); } finally { loading.value = false; }
};

const confirmSaveItem = () => {
  const modeText = isEditMode.value ? 'CẬP NHẬT' : 'THÊM MỚI';
  confirmDialog.value = {
    show: true, title: `XÁC NHẬN ${modeText}`,
    message: `Bạn có chắc muốn lưu [${itemForm.value.ten}] vào danh sách ${getCurrentTabTitle()}?`,
    color: 'success',
    action: async () => {
      confirmDialog.value.loading = true;
      try {
        if (isEditMode.value) await updateItem(); else await createItem();
        confirmDialog.value.show = false;
      } catch (error) { console.error(error); }
      finally { confirmDialog.value.loading = false; }
    }
  };
};

const createItem = async () => {
  const service = services[selectedTab.value];
  let res;
  switch (selectedTab.value) {
    case 'brands': res = await service.taoThuongHieu(itemForm.value); break;
    case 'categories': res = await service.taoDanhMuc(itemForm.value); break;
    case 'colors': res = await service.taoMauSac(itemForm.value); break;
    case 'sizes': res = await service.taoKichThuoc(itemForm.value); break;
    case 'materials': res = await service.taoChatLieu(itemForm.value); break;
    case 'soles': res = await service.taoDeGiay(itemForm.value); break;
    case 'collars': res = await service.taoCoGiay(itemForm.value); break;
    case 'origins': res = await service.taoXuatXu(itemForm.value); break;
    case 'purposes': res = await service.taoMucDichChay(itemForm.value); break;
  }
  addNotification({ title: 'Thêm mới thành công', subtitle: `Đã thêm [${itemForm.value.ten}] vào danh mục ${getCurrentTabTitle()}`, icon: 'CircleCheckIcon', color: 'success' });
  dataRefs[selectedTab.value].value.unshift(res);
  showDialog.value = false; loadItems();
};

const updateItem = async () => {
  const service = services[selectedTab.value];
  let res; const id = selectedItem.value.id;
  switch (selectedTab.value) {
    case 'brands': res = await service.capNhatThuongHieu(id, itemForm.value); break;
    case 'categories': res = await service.capNhatDanhMuc(id, itemForm.value); break;
    case 'colors': res = await service.capNhatMauSac(id, itemForm.value); break;
    case 'sizes': res = await service.capNhatKichThuoc(id, itemForm.value); break;
    case 'materials': res = await service.capNhatChatLieu(id, itemForm.value); break;
    case 'soles': res = await service.capNhatDeGiay(id, itemForm.value); break;
    case 'collars': res = await service.capNhatCoGiay(id, itemForm.value); break;
    case 'origins': res = await service.capNhatXuatXu(id, itemForm.value); break;
    case 'purposes': res = await service.capNhatMucDichChay(id, itemForm.value); break;
  }
  const idx = dataRefs[selectedTab.value].value.findIndex(i => i.id === id);
  if (idx !== -1) dataRefs[selectedTab.value].value[idx] = res;
  addNotification({ title: 'Cập nhật thành công', subtitle: `Đã cập nhật thông tin cho [${itemForm.value.ten}]`, icon: 'EditIcon', color: 'primary' });
  showDialog.value = false;
};

const confirmChangeStatus = (item) => {
  confirmDialog.value = {
    show: true, title: 'XÁC NHẬN TRẠNG THÁI',
    message: `Bạn có muốn đổi trạng thái của [${item.ten}]?`,
    color: 'warning',
    action: async () => {
      confirmDialog.value.loading = true;
      try { await changeItemStatus(item); confirmDialog.value.show = false; }
      catch (e) { } finally { confirmDialog.value.loading = false; }
    }
  };
};

const changeItemStatus = async (item) => {
  const service = services[selectedTab.value];
  const newS = (item.trangThai === 0 || item.trangThai === '0') ? 1 : 0;
  const pld = { ...item, trangThai: newS };
  switch (selectedTab.value) {
    case 'brands': await service.capNhatThuongHieu(item.id, pld); break;
    case 'categories': await service.capNhatDanhMuc(item.id, pld); break;
    case 'colors': await service.capNhatMauSac(item.id, pld); break;
    case 'sizes': await service.capNhatKichThuoc(item.id, pld); break;
    case 'materials': await service.capNhatChatLieu(item.id, pld); break;
    case 'soles': await service.capNhatDeGiay(item.id, pld); break;
    case 'collars': await service.capNhatCoGiay(item.id, pld); break;
    case 'origins': await service.capNhatXuatXu(item.id, pld); break;
    case 'purposes': await service.capNhatMucDichChay(item.id, pld); break;
  }
  item.trangThai = newS;
  addNotification({ title: 'Đổi trạng thái', subtitle: `Thuộc tính [${item.ten}] đã chuyển trạng thái.`, icon: 'InfoCircleIcon', color: 'warning' });
};

const resetForm = () => { itemForm.value = { ten: '', moTa: '', trangThai: 0 }; selectedItem.value = null; };
const openCreateDialog = () => { resetForm(); isEditMode.value = false; showDialog.value = true; };
const handleRefresh = async () => { isRefreshing.value = true; searchQuery.value = ''; statusFilter.value = null; pagination.value.page = 1; await loadItems(); setTimeout(() => { isRefreshing.value = false; }, 800); };
const editItem = (item) => { selectedItem.value = item; itemForm.value = { ...item }; isEditMode.value = true; showDialog.value = true; };
const getCurrentTabTitle = () => tabs.find(t => t.value === selectedTab.value)?.title || '';

onMounted(() => { if (!route.params.tab) loadItems(); });
watch(() => route.params.tab, (n) => { if (n && routeMap[n]) { selectedTab.value = routeMap[n]; loadItems(); } }, { immediate: true });
watch(selectedTab, (n) => { router.replace(`/thuoc-tinh/${reverseRouteMap[n]}`); });
</script>

<template>
  <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
    <!-- Header -->
    <v-row class="mb-4">
      <v-col cols="12">
        <h2 class="text-h3 font-weight-black tracking-tight text-dark mb-1">Quản lý thuộc tính</h2>
        <div class="text-subtitle-1 text-medium-emphasis">Thiết lập linh hoạt các thông số cơ bản cho hệ thống
          AeroStride</div>
      </v-col>
    </v-row>

    <!-- SQUARE Tabs -->
    <v-card class="rounded-0 mb-6 border shadow-none" elevation="0">
      <v-tabs v-model="selectedTab" grow color="primary" @update:model-value="loadItems">
        <v-tab v-for="tab in tabs" :key="tab.value" :value="tab.value" class="text-none font-weight-black square-tab">
          <v-icon start size="small">{{ tab.icon }}</v-icon> {{ tab.title }}
        </v-tab>
      </v-tabs>
    </v-card>

    <!-- 1. FILTER -->
    <AdminFilter :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
      <v-col cols="12" md="4">
        <v-text-field v-model="searchQuery" placeholder="Tìm tên thuộc tính..." variant="outlined" density="comfortable"
          hide-details prepend-inner-icon="mdi-magnify" class="font-weight-black" rounded="md"
          @keyup.enter="loadItems"></v-text-field>
      </v-col>
      <v-col cols="12" md="3">
        <v-select v-model="statusFilter"
          :items="[{ title: 'Tất cả trạng thái', value: null }, { title: 'Hoạt động', value: 'DANG_HOAT_DONG' }, { title: 'Không hoạt động', value: 'KHONG_HOAT_DONG' }]"
          variant="outlined" density="comfortable" hide-details class="font-weight-black" rounded="md"
          @update:model-value="loadItems"></v-select>
      </v-col>
    </AdminFilter>

    <!-- 2. TABLE -->
    <AdminTable :title="`DANH SÁCH ${getCurrentTabTitle().toUpperCase()}`"
      :addButtonText="`Thêm ${getCurrentTabTitle().toLowerCase()}`" :headers="tableHeaders"
      :items="dataRefs[selectedTab].value" :total-count="pagination.totalElements" :loading="loading"
      @add="openCreateDialog">
      <template #row="{ item }">
        <tr class="data-row">
          <td class="data-cell font-weight-black text-dark">{{ item.ten }}</td>
          <td class="data-cell text-medium-emphasis">{{ item.moTa || '-' }}</td>
          <td class="data-cell" style="text-align: center;">
            <v-chip :color="(item.trangThai === 0 || item.trangThai === '0') ? 'success' : 'error'" size="x-small"
              variant="flat" class="font-weight-black px-4">{{ (item.trangThai === 0 || item.trangThai === '0') ?
                'HOẠT ĐỘNG' : 'NGỪNG HOẠT ĐỘNG' }}</v-chip>
          </td>
          <td class="data-cell" style="text-align: center;">
            <div class="d-flex align-center justify-center">
              <v-switch :model-value="item.trangThai === 0 || item.trangThai === '0'" color="success" inset hide-details
                density="compact" class="tight-switch" @click.stop="confirmChangeStatus(item)"></v-switch>
              <v-btn icon variant="tonal" size="32" color="primary" class="rounded-0" @click.stop="editItem(item)">
                <EditIcon size="18" />
              </v-btn>
            </div>
          </td>
        </tr>
      </template>

      <template #pagination>
        <AdminPagination v-model="pagination.page" 
          :page-size="pagination.size"
          @update:pageSize="pagination.size = $event"
          @update:page-size="pagination.size = $event"
          :total-pages="pagination.totalPages" :total-elements="pagination.totalElements"
          :current-size="dataRefs[selectedTab].value.length" @change="loadItems" />
      </template>
    </AdminTable>

    <!-- DIALOG (SQUARE) -->
    <v-dialog v-model="showDialog" max-width="500">
      <v-card class="rounded-0 border shadow-2xl">
        <v-card-title class="pa-4 font-weight-black border-b bg-grey-lighten-4 uppercase text-primary">{{ isEditMode ?'CẬP NHẬT' : 'THÊM MỚI' }} {{ getCurrentTabTitle() }}</v-card-title>
        <v-card-text class="pa-6">
          <v-form>
            <v-text-field v-model="itemForm.ten" :label="`Tên ${getCurrentTabTitle()}`" variant="outlined"
              class="mb-4 font-weight-black" rounded="0"></v-text-field>
            <v-textarea v-model="itemForm.moTa" label="Mô tả" variant="outlined" rows="3" class="mb-4 font-weight-black"
              rounded="0"></v-textarea>
            <v-select v-model="itemForm.trangThai" label="Trạng thái"
              :items="[{ title: 'Hoạt động', value: 0 }, { title: 'Không hoạt động', value: 1 }]" variant="outlined"
              class="font-weight-black" rounded="0"></v-select>
          </v-form>
        </v-card-text>
        <v-divider></v-divider>
        <v-card-actions class="pa-4 bg-grey-lighten-4">
          <v-spacer></v-spacer>
          <v-btn variant="text" class="text-none font-weight-black" @click="showDialog = false">Hủy bỏ</v-btn>
          <v-btn color="primary" variant="flat" rounded="0" class="px-8 text-none font-weight-black"
            @click="confirmSaveItem" :disabled="!itemForm.ten">Xác nhận</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- SHARED CONFIRM -->
    <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message"
      :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />
  </v-container>
</template>

<style scoped>
.gray-bg {
  background-color: #fdfdfd;
}

.text-dark {
  color: #000000 !important;
}

.tight-switch {
  transform: scale(0.75);
  width: 44px;
}

.font-body {
  font-family: 'Public Sans', sans-serif;
}

.square-tab {
  border-radius: 0 !important;
}

:deep(.v-btn) {
  border-radius: 0 !important;
}
</style>
