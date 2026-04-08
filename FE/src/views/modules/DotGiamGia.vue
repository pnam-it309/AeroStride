<script setup>
import { ref, onMounted } from 'vue';
import { dichVuDotGiamGia } from '@/services/admin/dichVuDotGiamGia';

// REUSABLE COMPONENTS
import AdminFilter from '@/components/common/AdminFilter.vue';
import AdminTable from '@/components/common/AdminTable.vue';
import AdminPagination from '@/components/common/AdminPagination.vue';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { EditIcon, GiftIcon } from 'vue-tabler-icons';

const loading = ref(false);
const isRefreshing = ref(false);
const campaigns = ref([]);
const showCampaignDialog = ref(false);
const selectedCampaign = ref(null);
const isEditMode = ref(false);

const campaignForm = ref({ name: '', description: '', discountType: 'percentage', discountValue: '', startDate: '', endDate: '', status: 'active' });
const pagination = ref({ page: 1, size: 5, totalElements: 0, totalPages: 1 });
const filters = ref({ keyword: '', loaiGiamGia: null });

// Confirmation Logic
const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const loadCampaigns = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.value.page > 0 ? pagination.value.page - 1 : 0,
      size: pagination.value.size,
      keyword: filters.value.keyword || null,
      loaiGiamGia: filters.value.loaiGiamGia || null
    };
    const response = await dichVuDotGiamGia.layDotGiamGiaPhanTrang(params);
    campaigns.value = response.content || response;
    pagination.value.totalElements = response.totalElements || campaigns.value.length;
    pagination.value.totalPages = response.totalPages || 1;
  } catch (error) { console.error(error); } finally { loading.value = false; }
};

const handleRefresh = async () => {
  isRefreshing.value = true; filters.value = { keyword: '', loaiGiamGia: null };
  pagination.value.page = 1; await loadCampaigns();
  setTimeout(() => isRefreshing.value = false, 800);
};

const confirmToggleStatus = (item) => {
  confirmDialog.value = {
    show: true, title: 'THAY ĐỔI TRẠNG THÁI',
    message: `Bạn có muốn đổi trạng thái của chiến dịch [${item.ten}]?`,
    color: 'warning',
    action: async () => {
      confirmDialog.value.loading = true;
      try {
        const newS = (item.trangThai === 'DANG_HOAT_DONG' || item.status === 'active') ? 'KHONG_HOAT_DONG' : 'DANG_HOAT_DONG';
        await dichVuDotGiamGia.thayDoiTrangThaiDotGiamGia(item.id, newS);
        item.trangThai = newS; item.status = newS === 'DANG_HOAT_DONG' ? 'active' : 'inactive';
        confirmDialog.value.show = false;
      } catch (e) {} finally { confirmDialog.value.loading = false; }
    }
  };
};

const confirmSaveCampaign = () => {
  confirmDialog.value = {
    show: true, title: isEditMode.value ? 'CẬP NHẬT CHIẾN DỊCH' : 'TẠO CHIẾN DỊCH',
    message: `Bạn có muốn lưu thông tin đợt giảm giá [${campaignForm.value.name}]?`,
    color: 'success',
    action: async () => {
      confirmDialog.value.loading = true;
      try {
        if (isEditMode.value) await updateCampaign(); else await createCampaign();
        confirmDialog.value.show = false;
      } catch (e) {} finally { confirmDialog.value.loading = false; }
    }
  };
};

const updateCampaign = async () => {
  const updated = await dichVuDotGiamGia.capNhatDotGiamGia(selectedCampaign.value.id, campaignForm.value);
  const idx = campaigns.value.findIndex(c => c.id === selectedCampaign.value.id);
  if (idx !== -1) campaigns.value[idx] = updated; showCampaignDialog.value = false;
};

const createCampaign = async () => {
  const created = await dichVuDotGiamGia.taoDotGiamGia(campaignForm.value);
  campaigns.value.unshift(created); showCampaignDialog.value = false; loadCampaigns();
};

const editCampaign = (c) => { selectedCampaign.value = c; campaignForm.value = { ...c }; isEditMode.value = true; showCampaignDialog.value = true; };
const openCreateDialog = () => { campaignForm.value = { name: '', description: '', discountType: 'percentage', discountValue: '', startDate: '', endDate: '', status: 'active' }; isEditMode.value = false; showCampaignDialog.value = true; };
const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
const formatDate = (timestamp) => timestamp ? new Date(timestamp).toLocaleDateString('vi-VN') : 'N/A';
const getDiscountDisplay = (c) => c.loaiGiamGia === 'PHAN_TRAM' ? { val: `${c.soTienGiam}%`, color: 'orange' } : { val: formatCurrency(c.soTienGiam), color: 'green' };
const getCampaignStatusLabel = (c) => { const now = new Date().getTime(); if (now < c.ngayBatDau) return { text: 'SẮP DIỄN RA', color: 'warning' }; if (now > c.ngayKetThuc) return { text: 'ĐÃ KẾT THÚC', color: 'grey' }; return { text: 'ĐANG DIỄN RA', color: 'success' }; };

onMounted(() => loadCampaigns());
</script>

<template>
  <v-container fluid class="pa-6 gray-bg min-h-screen font-body">
    <!-- Header -->
    <v-row class="mb-4">
      <v-col cols="12">
        <h2 class="text-h3 font-weight-black tracking-tight text-dark mb-1">Quản lý đợt giảm giá</h2>
        <div class="text-subtitle-1 text-medium-emphasis">Thiết lập các chương trình khuyến mãi AeroStride</div>
      </v-col>
    </v-row>

    <!-- 1. FILTER -->
    <AdminFilter :loading="loading" :is-refreshing="isRefreshing" @refresh="handleRefresh">
      <v-col cols="12" md="4"><v-text-field v-model="filters.keyword" placeholder="Tìm tên đợt khuyến mãi..." variant="outlined" density="comfortable" hide-details prepend-inner-icon="mdi-magnify" class="font-weight-black" @keyup.enter="loadCampaigns"></v-text-field></v-col>
      <v-col cols="12" md="3"><v-select v-model="filters.loaiGiamGia" :items="[{title:'Tất cả loại',value:null}, {title:'Giảm theo %',value:'PHAN_TRAM'}, {title:'Giảm theo số tiền',value:'TIEN_MAT'}]" variant="outlined" density="comfortable" hide-details class="font-weight-black" @update:model-value="loadCampaigns"></v-select></v-col>
    </AdminFilter>

    <!-- 2. TABLE -->
    <AdminTable title="DANH SÁCH CHIẾN DỊCH" addButtonText="Tạo chiến dịch" :headers="[{text:'Thông tin chiến dịch', align:'center'}, {text:'Giá trị giảm', align:'center'}, {text:'Thời gian', align:'center'}, {text:'Trạng thái', align:'center'}, {text:'Thao tác', align:'center'}]" :items="campaigns" :total-count="pagination.totalElements" :loading="loading" @add="openCreateDialog">
      <template #row="{ item }">
        <tr class="data-row">
          <td class="data-cell">
            <div class="d-flex flex-column align-center justify-center">
              <div class="text-subtitle-1 font-weight-black text-dark text-center">{{ item.ten }}</div>
              <div class="text-caption font-weight-bold text-medium-emphasis text-center line-clamp-1">{{ item.moTa || 'N/A' }}</div>
            </div>
          </td>
          <td class="data-cell"><v-chip :color="getDiscountDisplay(item).color" variant="flat" size="small" class="font-weight-black px-4">{{ getDiscountDisplay(item).val }}</v-chip></td>
          <td class="data-cell"><div class="text-caption font-weight-bold text-dark">{{ formatDate(item.ngayBatDau) }}</div><v-icon size="x-small" color="grey">mdi-arrow-down-bold</v-icon><div class="text-caption font-weight-bold text-dark">{{ formatDate(item.ngayKetThuc) }}</div></td>
          <td class="data-cell"><v-chip :color="getCampaignStatusLabel(item).color" size="x-small" variant="flat" class="font-weight-black px-4">{{ getCampaignStatusLabel(item).text }}</v-chip></td>
          <td class="data-cell">
            <div class="d-flex align-center justify-center">
              <v-switch :model-value="item.trangThai === 'DANG_HOAT_DONG' || item.status === 'active'" color="success" inset hide-details density="compact" class="tight-switch" @click.stop="confirmToggleStatus(item)"></v-switch>
              <v-btn icon variant="tonal" size="32" color="primary" class="rounded-0" @click.stop="editCampaign(item)"><EditIcon size="18" /></v-btn>
            </div>
          </td>
        </tr>
      </template>
      <template #pagination>
        <AdminPagination v-model="pagination.page" v-model:page-size="pagination.size" :total-pages="pagination.totalPages" :total-elements="pagination.totalElements" :current-size="campaigns.length" @change="loadCampaigns" />
      </template>
    </AdminTable>

    <!-- Dialog (SQUARE) -->
    <v-dialog v-model="showCampaignDialog" max-width="700">
      <v-card class="rounded-0 border shadow-2xl">
        <v-card-title class="pa-4 font-weight-black border-b bg-grey-lighten-4 uppercase text-primary">{{ isEditMode ? 'CẬP NHẬT CHIẾN DỊCH' : 'TẠO ĐỢT GIẢM GIÁ MỚI' }}</v-card-title>
        <v-card-text class="pa-6">
          <v-form>
            <v-row>
              <v-col cols="12"><v-text-field v-model="campaignForm.name" label="Tên đợt giảm giá" variant="outlined" class="font-weight-black" rounded="0"></v-text-field></v-col>
              <v-col cols="12" md="6"><v-select v-model="campaignForm.discountType" label="Loại giảm giá" :items="[{title:'Giảm theo %',value:'percentage'},{title:'Giảm theo tiền',value:'fixed'}]" variant="outlined" rounded="0"></v-select></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="campaignForm.discountValue" label="Giá trị giảm" variant="outlined" type="number" class="font-weight-black" rounded="0"></v-text-field></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="campaignForm.startDate" label="Ngày bắt đầu" variant="outlined" type="datetime-local" rounded="0"></v-text-field></v-col>
              <v-col cols="12" md="6"><v-text-field v-model="campaignForm.endDate" label="Ngày kết thúc" variant="outlined" type="datetime-local" rounded="0"></v-text-field></v-col>
            </v-row>
          </v-form>
        </v-card-text>
        <v-divider></v-divider>
        <v-card-actions class="pa-4 bg-grey-lighten-4">
          <v-spacer></v-spacer>
          <v-btn variant="text" class="text-none font-weight-black" @click="showCampaignDialog = false">Đóng</v-btn>
          <v-btn color="primary" variant="flat" rounded="0" class="px-8 text-none font-weight-black" @click="confirmSaveCampaign">Xác nhận</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- SHARED CONFIRM -->
    <AdminConfirm v-model:show="confirmDialog.show" :title="confirmDialog.title" :message="confirmDialog.message" :color="confirmDialog.color" :loading="confirmDialog.loading" @confirm="confirmDialog.action" />
  </v-container>
</template>

<style scoped>
.gray-bg { background-color: #fdfdfd; } .text-dark { color: #1e293b !important; } .tight-switch { transform: scale(0.75); width: 44px; } .font-body { font-family: 'Public Sans', sans-serif; } .line-clamp-1 { display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; } :deep(.v-btn) { border-radius: 0 !important; }
</style>
